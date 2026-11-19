package com.omniroute.app.feature.room

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omniroute.app.AppContainer
import com.omniroute.app.R
import com.omniroute.app.core.planning.PlanResult
import com.omniroute.app.core.planning.PlanningStreamRepository
import com.omniroute.app.core.room.RoomMember
import com.omniroute.app.core.room.RoomSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 房间方案池里的一个候选方案。 */
internal data class PlanVariantUi(
    val id: String,
    val name: String,
    val style: String,
    val votes: Int,
    val winner: Boolean,
)

/** 协作房间页的完整可观察状态。 */
internal data class RoomUiState(
    val room: RoomSession? = null,
    val members: List<RoomMember> = emptyList(),
    val connected: Boolean = false,
    val working: Boolean = false,
    val errorText: String? = null,
    val createdName: String = "",
    val inviteInput: String = "",
    val variants: List<PlanVariantUi> = emptyList(),
    val loadingVariants: Boolean = false,
    val votingVariantId: String? = null,
    val adoptingVariantId: String? = null,
    val variantNotice: String? = null,
    val variantError: String? = null,
)

/**
 * 协作房间状态持有者。
 *
 * <p>成员推送的 WebSocket 会话挂在 ViewModel 上，旋转屏幕或短暂切页不会重连，
 * 只有真正离开房间页时才取消。
 */
internal class RoomViewModel(
    private val container: AppContainer,
    private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(
        RoomUiState(
            room = container.roomRepository.currentRoom(),
            createdName = context.getString(R.string.room_default_name),
        ),
    )
    val state: StateFlow<RoomUiState> = _state.asStateFlow()

    private var watchSession: PlanningStreamRepository.Session? = null

    private val listener = object : PlanningStreamRepository.Listener {
        override fun onOpen() {
            _state.update { it.copy(connected = true) }
        }

        override fun onMembers(list: List<RoomMember>) {
            _state.update { it.copy(members = list) }
        }

        override fun onVariantSync() {
            loadVariants(silent = true)
        }

        override fun onStage(label: String, detail: String) {
            // 房间页不展示推演阶段。
        }

        override fun onToken(token: String) {
            // 房间页不展示推理片段。
        }

        override fun onError(message: String) {
            _state.update { it.copy(errorText = message, connected = false) }
        }

        override fun onFinished(result: PlanResult) {
            // 房间页不消费规划结果。
        }

        override fun onClosed(reason: String) {
            _state.update { it.copy(connected = false) }
        }
    }

    init {
        _state.value.room?.let {
            watch(it)
            loadVariants()
        }
    }

    fun onNameChange(value: String) {
        _state.update { it.copy(createdName = value) }
    }

    fun onInviteChange(raw: String) {
        val next = raw.uppercase().filter { ch -> ch.isLetterOrDigit() }.take(6)
        _state.update { it.copy(inviteInput = next, errorText = null) }
    }

    fun createRoom() {
        if (_state.value.working) return
        _state.update { it.copy(working = true, errorText = null) }
        val name = _state.value.createdName
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching { container.roomRepository.createRoom(name) }
            }
            outcome.onSuccess { room ->
                _state.update { it.copy(room = room, members = emptyList(), variants = emptyList()) }
                watch(room)
                loadVariants()
            }.onFailure { error ->
                _state.update {
                    it.copy(errorText = error.message ?: context.getString(R.string.room_create_failed))
                }
            }
            _state.update { it.copy(working = false) }
        }
    }

    fun joinRoom() {
        if (_state.value.working) return
        _state.update { it.copy(working = true, errorText = null) }
        val code = _state.value.inviteInput
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching { container.roomRepository.joinRoom(code) }
            }
            outcome.onSuccess { room ->
                _state.update { it.copy(room = room, members = emptyList(), variants = emptyList()) }
                watch(room)
                loadVariants()
            }.onFailure { error ->
                _state.update {
                    it.copy(errorText = error.message ?: context.getString(R.string.room_join_failed))
                }
            }
            _state.update { it.copy(working = false) }
        }
    }

    fun leaveRoom() {
        watchSession?.cancel()
        watchSession = null
        container.roomRepository.clear()
        _state.update {
            it.copy(
                room = null,
                members = emptyList(),
                connected = false,
                errorText = null,
                variants = emptyList(),
                loadingVariants = false,
                variantNotice = null,
                variantError = null,
            )
        }
    }

    /** 拉取房间方案池；静默刷新（推送触发）不显示加载态。 */
    fun loadVariants(silent: Boolean = false) {
        val room = _state.value.room ?: return
        if (!silent) {
            _state.update { it.copy(loadingVariants = true, variantError = null) }
        }
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching { container.variantRepository.list(room.roomId) }
            }
            outcome.onSuccess { variants ->
                _state.update {
                    it.copy(
                        loadingVariants = false,
                        variantError = null,
                        variants = variants.map { dto ->
                            PlanVariantUi(
                                id = dto.id.orEmpty(),
                                name = dto.name.orEmpty(),
                                style = dto.style.orEmpty(),
                                votes = dto.votes,
                                winner = dto.winner,
                            )
                        },
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        loadingVariants = false,
                        variantError = if (silent) {
                            it.variantError
                        } else {
                            error.message ?: context.getString(R.string.variant_error_load)
                        },
                    )
                }
            }
        }
    }

    /** 投票：网关一人一票，后票覆盖前票，成功后重新拉取计票。 */
    fun voteVariant(variantId: String) {
        val current = _state.value
        if (current.room == null || current.votingVariantId != null) return
        _state.update {
            it.copy(votingVariantId = variantId, variantError = null, variantNotice = null)
        }
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching { container.variantRepository.vote(variantId) }
            }
            outcome.onSuccess {
                _state.update { it.copy(votingVariantId = null) }
                loadVariants(silent = true)
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        votingVariantId = null,
                        variantError = error.message ?: context.getString(R.string.variant_error_vote),
                    )
                }
            }
        }
    }

    /** 采纳：网关会按方案路线费用建立预算基线。 */
    fun adoptVariant(variantId: String) {
        val current = _state.value
        if (current.room == null || current.adoptingVariantId != null) return
        _state.update {
            it.copy(adoptingVariantId = variantId, variantError = null, variantNotice = null)
        }
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching { container.variantRepository.adopt(variantId) }
            }
            outcome.onSuccess {
                _state.update {
                    it.copy(
                        adoptingVariantId = null,
                        variantNotice = context.getString(R.string.room_variants_adopted),
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        adoptingVariantId = null,
                        variantError = error.message ?: context.getString(R.string.variant_error_adopt),
                    )
                }
            }
        }
    }

    fun clearVariantNotice() = _state.update { it.copy(variantNotice = null, variantError = null) }

    override fun onCleared() {
        watchSession?.cancel()
        watchSession = null
        super.onCleared()
    }

    private fun watch(room: RoomSession) {
        watchSession?.cancel()
        _state.update { it.copy(connected = false, members = emptyList()) }
        watchSession = container.planningStreamRepository.watchRoom(room, listener)
    }
}
