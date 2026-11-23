package com.omniroute.app.feature.trip

import com.omniroute.app.core.network.dto.BudgetSummaryDto
import com.omniroute.app.core.network.dto.ExecutionSummaryDto
import kotlinx.coroutines.Job
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.omniroute.app.AppContainer
import com.omniroute.app.R
import com.omniroute.app.core.data.TripBookingStore
import com.omniroute.app.core.network.dto.TripPlanDto
import com.omniroute.app.core.planning.RouteNode
import com.omniroute.app.core.planning.TripContentEditor
import com.omniroute.app.core.planning.TripItinerary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 行程详情页完整可观察状态。 */
internal data class TripDetailUiState(
    val bookingMarks: TripBookingStore.Marks,
    val trip: TripPlanDto? = null,
    val journeyProgress: ExecutionSummaryDto? = null,
    val budgetProgress: BudgetSummaryDto? = null,
    val loading: Boolean = true,
    val errorText: String? = null,
    val renameOpen: Boolean = false,
    val renameDraft: String = "",
    val deleteOpen: Boolean = false,
    val actionBusy: Boolean = false,
    val actionError: String? = null,
    val actionItem: TripItinerary.Item? = null,
    val actionDay: Int = 0,
    val editorOpen: Boolean = false,
    val editorNodes: List<RouteNode> = emptyList(),
    val editorNotes: Map<String, String> = emptyMap(),
)

internal sealed interface TripDetailEvent {
    /** 删除成功：页面据此退回列表。 */
    data object Deleted : TripDetailEvent

    /** 需要在界面弹出的短提示（Toast）。 */
    data class Message(val text: String) : TripDetailEvent
}

/**
 * 行程详情页状态持有者：加载、重命名、删除、节点操作与路线编辑都由这里驱动，
 * 页面只渲染状态并转发点击；弹窗与编辑草稿通过 [SavedStateHandle] 跨进程恢复。
 */
internal class TripDetailViewModel(
    private val container: AppContainer,
    private val handle: SavedStateHandle,
    private val context: Context,
    private val tripId: String,
) : ViewModel() {

    private val gson = Gson()
    private val bookingStore = TripBookingStore(context)
    private var toolsJob: Job? = null

    /** 进程重建后动作面板的目标节点只存名称，等行程拉回后再从 content 解析。 */
    private var pendingActionItemName: String? = null

    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<TripDetailUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<TripDetailEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<TripDetailEvent> = _events.asSharedFlow()

    init {
        load()
        viewModelScope.launch {
            _state
                .map { it.toPersisted() }
                .distinctUntilChanged()
                .collect { persist(it) }
        }
    }

    fun load() {
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching { container.planningRepository.getTrip(tripId) }
            }
            outcome.onSuccess { trip -> onTripLoaded(trip) }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            loading = false,
                            errorText = error.message
                                ?: context.getString(R.string.trip_sync_failed),
                        )
                    }
                }
        }
    }

    fun refreshTools() {
        if (tripId.isBlank()) return
        toolsJob?.cancel()
        toolsJob = viewModelScope.launch {
            val (journey, budget) = withContext(Dispatchers.IO) {
                val execution = runCatching { container.planningRepository.journeyState(tripId).execution }.getOrNull()
                val ledger = runCatching { container.budgetRepository.summary(tripId) }.getOrNull()
                execution to ledger
            }
            _state.update { it.copy(journeyProgress = journey, budgetProgress = budget) }
        }
    }

    fun openRename() {
        val trip = _state.value.trip ?: return
        _state.update {
            it.copy(
                renameOpen = true,
                renameDraft = tripTitle(context, trip),
                actionError = null,
            )
        }
    }

    fun onRenameDraftChange(value: String) = _state.update { it.copy(renameDraft = value.take(200)) }

    fun dismissRename() {
        if (_state.value.actionBusy) return
        _state.update { it.copy(renameOpen = false) }
    }

    fun rename() {
        val current = _state.value
        val draft = current.renameDraft.trim()
        if (draft.isEmpty() || current.actionBusy) return
        _state.update { it.copy(actionBusy = true, actionError = null) }
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching { container.planningRepository.renameTrip(tripId, draft) }
            }
            outcome.onSuccess {
                _state.update { it.copy(renameOpen = false, actionBusy = false) }
                load()
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        actionBusy = false,
                        actionError = error.message
                            ?: context.getString(R.string.trip_rename_failed),
                    )
                }
            }
        }
    }

    fun openDelete() = _state.update { it.copy(deleteOpen = true, actionError = null) }

    fun dismissDelete() {
        if (_state.value.actionBusy) return
        _state.update { it.copy(deleteOpen = false) }
    }

    fun delete() {
        if (_state.value.actionBusy) return
        _state.update { it.copy(actionBusy = true, actionError = null) }
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching { container.planningRepository.deleteTrip(tripId) }
            }
            outcome.onSuccess {
                bookingStore.clear(tripId)
                _state.update { it.copy(deleteOpen = false, actionBusy = false) }
                _events.emit(TripDetailEvent.Deleted)
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        actionBusy = false,
                        actionError = error.message
                            ?: context.getString(R.string.trip_delete_failed),
                    )
                }
            }
        }
    }

    fun openItem(day: Int, item: TripItinerary.Item) {
        pendingActionItemName = item.name
        _state.update { it.copy(actionDay = day, actionItem = item) }
    }

    fun dismissItem() {
        pendingActionItemName = null
        _state.update { it.copy(actionItem = null, actionDay = 0) }
    }

    /** 切换某节点的预订标记，返回切换后的状态供页面提示。 */
    fun toggleBooked(): Boolean {
        val current = _state.value
        val item = current.actionItem ?: return false
        val nowBooked = bookingStore.toggle(tripId, current.actionDay, item.name.orEmpty())
        _state.update { it.copy(bookingMarks = bookingStore.marks(tripId)) }
        return nowBooked
    }

    fun openEditor() {
        val trip = _state.value.trip ?: return
        val nodes = TripContentEditor.readNodes(trip.content)
        if (nodes.isEmpty()) return
        _state.update {
            it.copy(
                editorNodes = nodes,
                editorNotes = TripContentEditor.readNotes(trip.content),
                editorOpen = true,
                actionError = null,
            )
        }
    }

    fun closeEditor() = _state.update {
        it.copy(editorOpen = false, editorNodes = emptyList(), editorNotes = emptyMap())
    }

    fun onNoteChange(key: String, value: String) {
        _state.update {
            it.copy(editorNotes = if (value.isBlank()) it.editorNotes - key else it.editorNotes + (key to value))
        }
    }

    fun applyEdits(nodes: List<RouteNode>) {
        val current = _state.value
        val trip = current.trip ?: return
        val dayCount = maxOf(trip.days, nodes.maxOfOrNull { it.day } ?: 1, 1)
        val content = TripContentEditor.apply(
            trip.content,
            trip.destCity,
            nodes,
            current.editorNotes,
            dayCount,
        )
        _state.update {
            it.copy(editorOpen = false, actionBusy = true, actionError = null, editorNodes = nodes)
        }
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching { container.planningRepository.updateTripContent(tripId, content) }
            }
            _state.update { it.copy(actionBusy = false) }
            outcome.onSuccess {
                _state.update { state ->
                    state.copy(editorNodes = emptyList(), editorNotes = emptyMap())
                }
                load()
            }
                .onFailure { error ->
                    val message = error.message ?: context.getString(R.string.trip_edit_route_failed)
                    // 保存失败时把这次排序留在编辑页里，用户可以直接再点完成重试。
                    _state.update {
                        it.copy(editorOpen = true, editorNodes = nodes, actionError = message)
                    }
                    _events.emit(TripDetailEvent.Message(message))
                }
        }
    }

    /** 底图加载：编辑页需要 suspend 回调，直接接到仓库。 */
    suspend fun loadBaseMap(center: String, zoom: Int, width: Int, height: Int) =
        withContext(Dispatchers.IO) {
            runCatching { container.staticMapRepository.load(center, zoom, width, height) }.getOrNull()
        }

    private fun onTripLoaded(trip: TripPlanDto) {
        _state.update { state ->
            var next = state.copy(trip = trip, loading = false, errorText = null)
            // 进程重建时路线编辑页还开着：从恢复出来的 content 重建节点与备注。
            if (next.editorOpen && next.editorNodes.isEmpty()) {
                val nodes = TripContentEditor.readNodes(trip.content)
                if (nodes.isNotEmpty()) {
                    next = next.copy(
                        editorNodes = nodes,
                        editorNotes = TripContentEditor.readNotes(trip.content),
                    )
                }
            }
            if (next.renameOpen && next.renameDraft.isBlank()) {
                next = next.copy(renameDraft = tripTitle(context, trip))
            }
            val name = pendingActionItemName
            if (name != null) {
                val item = TripItinerary.parse(trip.content).days
                    .firstOrNull { it.dayNumber == next.actionDay }
                    ?.items
                    ?.firstOrNull { it.name == name }
                next = next.copy(actionItem = item)
                pendingActionItemName = null
            }
            next
        }
    }

    private fun initialState(): TripDetailUiState {
        val saved = handle.get<String>(KEY_STATE)
            ?.let { runCatching { gson.fromJson(it, PersistedTripDetailState::class.java) }.getOrNull() }
        pendingActionItemName = saved?.actionItemName
        return TripDetailUiState(
            bookingMarks = bookingStore.marks(tripId),
            renameOpen = saved?.renameOpen ?: false,
            renameDraft = saved?.renameDraft.orEmpty(),
            deleteOpen = saved?.deleteOpen ?: false,
            actionDay = saved?.actionDay ?: 0,
            editorOpen = saved?.editorOpen ?: false,
            editorNodes = saved?.editorNodes ?: emptyList(),
            editorNotes = saved?.editorNotes ?: emptyMap(),
        )
    }

    private fun persist(snapshot: PersistedTripDetailState) {
        handle[KEY_STATE] = gson.toJson(snapshot)
    }

    companion object {
        private const val KEY_STATE = "trip_detail_state"
    }
}

/** 需要跨进程重建保留的字段；行程本体重新从仓库拉取，不入盘。 */
private data class PersistedTripDetailState(
    val renameOpen: Boolean = false,
    val renameDraft: String = "",
    val deleteOpen: Boolean = false,
    val editorOpen: Boolean = false,
    val editorNodes: List<RouteNode>? = null,
    val editorNotes: Map<String, String> = emptyMap(),
    val actionDay: Int = 0,
    val actionItemName: String? = null,
)

private fun TripDetailUiState.toPersisted(): PersistedTripDetailState {
    return PersistedTripDetailState(
        renameOpen = renameOpen,
        renameDraft = renameDraft,
        deleteOpen = deleteOpen,
        editorOpen = editorOpen,
        editorNodes = editorNodes.takeIf { it.isNotEmpty() },
        editorNotes = editorNotes,
        actionDay = actionDay,
        actionItemName = actionItem?.name,
    )
}

/** 行程标题：优先用服务端标题，缺失时按目的地生成兜底文案。 */
internal fun tripTitle(context: Context, trip: TripPlanDto): String {
    val title = trip.title?.trim().orEmpty()
    if (title.isNotEmpty()) {
        return title
    }
    val city = trip.destCity?.trim().orEmpty()
    return if (city.isNotEmpty()) {
        context.getString(R.string.trip_fallback_title, city)
    } else {
        context.getString(R.string.trip_fallback_title_default)
    }
}
