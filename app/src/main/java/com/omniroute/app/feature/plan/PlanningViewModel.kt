package com.omniroute.app.feature.plan

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.omniroute.app.AppContainer
import com.omniroute.app.R
import com.omniroute.app.core.planning.PlanRequest
import com.omniroute.app.core.planning.PlanResult
import com.omniroute.app.core.planning.PlanningStreamRepository
import com.omniroute.app.core.planning.RouteNode
import com.omniroute.app.core.room.RoomMember
import com.omniroute.app.core.room.RoomSession
import java.time.LocalDate
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

internal enum class PlanPhase { FORM, RUNNING, DONE, FAILED }

internal data class StageEntry(val label: String, val detail: String)

/**
 * 规划页的完整可观察状态。
 *
 * <p>页面只渲染这里的状态并调用事件方法；所有会影响下一屏/保存结果的数据都放在这里，
 * 旋转屏幕、返回候选详情再回来都不会丢。
 */
internal data class PlanningUiState(
    val phase: PlanPhase = PlanPhase.FORM,
    val destination: String = "",
    val origin: String = "",
    val startDate: String = "",
    val days: Int = 3,
    val travelers: Int = 2,
    val budget: String = "",
    val preferences: Set<String> = emptySet(),
    val chatText: String = "",
    val chatHint: String? = null,
    val chatParsing: Boolean = false,
    val chatParsed: ParsedIntent? = null,
    val room: RoomSession? = null,
    val preparing: Boolean = false,
    val errorText: String? = null,
    val stages: List<StageEntry> = emptyList(),
    val streamText: String = "",
    val members: List<RoomMember> = emptyList(),
    val connected: Boolean = false,
    val result: PlanResult? = null,
    val activeRequest: PlanRequest? = null,
    val selectedDay: Int? = null,
    val saving: Boolean = false,
    val saveError: String? = null,
    val feedbackDone: Int? = null,
    val feedbackReason: String = "",
    val feedbackBusy: Boolean = false,
    val feedbackError: String? = null,
    val editorOpen: Boolean = false,
    val editedNodes: List<RouteNode>? = null,
    val notes: Map<String, String> = emptyMap(),
    val hasUnsavedEdits: Boolean = false,
    val showDiscardPrompt: Boolean = false,
    val variantSaving: Boolean = false,
    val variantNotice: String? = null,
    val variantError: String? = null,
)

/** 服务端解析出的一句话结构；用户确认后才会填入表单。 */
internal data class ParsedIntent(
    val destination: String = "",
    val origin: String = "",
    val days: Int = 0,
    val startDate: String = "",
    val preferences: List<String> = emptyList(),
)

internal sealed interface PlanningEvent {
    /** 保存成功：页面据此跳转行程详情。 */
    data class Saved(val tripId: String) : PlanningEvent
}

/**
 * 规划流程的状态持有者。
 *
 * <p>WebSocket 会话挂在 ViewModel 上而不是 Composable 的 DisposableEffect 上：
 * 旋转屏幕或短暂切到候选详情时推演继续跑，只有真正离开规划页（ViewModel 被清理）才取消。
 */
internal class PlanningViewModel(
    private val container: AppContainer,
    private val handle: SavedStateHandle,
    private val context: Context,
) : ViewModel() {

    private val gson = Gson()

    private var session: PlanningStreamRepository.Session? = null

    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<PlanningUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<PlanningEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<PlanningEvent> = _events.asSharedFlow()

    init {
        // 恢复出来的结果需要重建候选详情快照，否则点卡片会落到「方案已过期」。
        _state.value.result?.let { putCandidateDetails(it) }
        viewModelScope.launch {
            _state
                .map { it.toPersisted() }
                .distinctUntilChanged()
                .collect { persist(it) }
        }
    }

    /** 表单字段：所有修改都走这里，页面不再持有可变状态。 */
    fun onChatTextChange(value: String) {
        if (value.length <= 200) {
            // 描述改了，之前的解析结果就不再可信，交给用户重新解析。
            update { it.copy(chatText = value, chatParsed = null, chatHint = null) }
        }
    }

    fun onDestinationChange(value: String) {
        update { it.copy(destination = value, errorText = null) }
    }

    fun onOriginChange(value: String) = update { it.copy(origin = value) }

    /** DatePicker 给出的是 UTC 零点毫秒；这里统一转成 ISO 日期，后续校验只认 LocalDate。 */
    fun onStartDateSelected(epochMillis: Long) {
        val date = LocalDate.ofEpochDay(epochMillis / MILLIS_PER_DAY)
        update { it.copy(startDate = date.toString(), errorText = null) }
    }

    fun onStartDateClear() = update { it.copy(startDate = "") }

    /** 出发日期校验：必须是真实日期，且不早于今天；空值表示选填。 */
    private fun validateStartDate(raw: String): String? {
        if (raw.isBlank()) return null
        val date = runCatching { LocalDate.parse(raw) }.getOrNull()
            ?: return context.getString(R.string.plan_error_date_invalid)
        if (date.isBefore(LocalDate.now())) {
            return context.getString(R.string.plan_error_date_past)
        }
        return null
    }

    fun onDaysChange(value: Int) = update { it.copy(days = value) }

    fun onTravelersChange(value: Int) = update { it.copy(travelers = value) }

    fun onBudgetChange(raw: String) {
        update { it.copy(budget = raw.filter { ch -> ch.isDigit() }.take(6)) }
    }

    fun onTogglePreference(label: String) {
        update { current ->
            val next = if (current.preferences.contains(label)) {
                current.preferences - label
            } else {
                current.preferences + label
            }
            current.copy(preferences = next)
        }
    }

    /**
     * 一句话 → 结构化结果：解析交给后端，前端只展示识别结果，
     * 用户点「确认填入」后才会修改表单，避免误识别直接覆盖已填内容。
     */
    fun applyChatIdea() {
        val current = _state.value
        val text = current.chatText.trim()
        if (text.isEmpty()) {
            update { it.copy(errorText = context.getString(R.string.plan_error_chat_empty)) }
            return
        }
        if (current.chatParsing) return
        update {
            it.copy(chatParsing = true, chatParsed = null, chatHint = null, errorText = null)
        }
        viewModelScope.launch {
            val parsed = withContext(Dispatchers.IO) {
                runCatching { container.planningRepository.parseIntent(text) }
            }
            parsed.onSuccess { data ->
                val city = data.city.trim()
                if (city.isEmpty()) {
                    update {
                        it.copy(
                            chatParsing = false,
                            chatHint = context.getString(R.string.plan_hint_city_missing),
                        )
                    }
                } else {
                    val intent = ParsedIntent(
                        destination = city,
                        origin = data.origin.trim(),
                        days = data.days,
                        startDate = data.startDate.trim(),
                        preferences = data.preferences.filter { PreferenceOptions.contains(it) },
                    )
                    update {
                        it.copy(
                            chatParsing = false,
                            chatParsed = intent,
                            chatHint = parsedSummary(intent),
                        )
                    }
                }
            }.onFailure { error ->
                update {
                    it.copy(
                        chatParsing = false,
                        errorText = error.message ?: context.getString(R.string.error_network),
                    )
                }
            }
        }
    }

    /** 用户确认后把解析结果填入表单。 */
    fun confirmChatIdea() {
        val parsed = _state.value.chatParsed ?: return
        update { state ->
            val mergedPreferences = state.preferences + parsed.preferences
            val nextDays = parsed.days.takeIf { it in 1..30 } ?: state.days
            val nextStart = parsed.startDate
                .takeIf { it.isNotBlank() && validateStartDate(it) == null }
                ?: state.startDate
            state.copy(
                destination = parsed.destination,
                origin = parsed.origin.ifBlank { state.origin },
                days = nextDays,
                startDate = nextStart,
                preferences = mergedPreferences,
                chatParsed = null,
                chatHint = retrievalHint(context, parsed.destination, nextDays, mergedPreferences),
                errorText = null,
            )
        }
    }

    private fun parsedSummary(parsed: ParsedIntent): String {
        val parts = mutableListOf(context.getString(R.string.plan_parsed_city, parsed.destination))
        if (parsed.origin.isNotBlank()) {
            parts += context.getString(R.string.plan_parsed_origin, parsed.origin)
        }
        if (parsed.days in 1..30) {
            parts += context.getString(R.string.plan_parsed_days, parsed.days)
        }
        if (parsed.startDate.isNotBlank()) {
            parts += context.getString(R.string.plan_parsed_start, parsed.startDate)
        }
        if (parsed.preferences.isNotEmpty()) {
            parts += context.getString(
                R.string.plan_parsed_prefs,
                parsed.preferences.joinToString("/"),
            )
        }
        return context.getString(
            R.string.plan_parsed_summary,
            parts.joinToString(context.getString(R.string.plan_parsed_separator)),
        )
    }

    fun submit() {
        val current = _state.value
        val city = current.destination.trim()
        when {
            city.isEmpty() -> {
                update { it.copy(errorText = context.getString(R.string.plan_error_destination_missing)) }
                return
            }
            !PlanRequest.isSupportedDestination(city) -> {
                update { it.copy(errorText = context.getString(R.string.plan_error_overseas)) }
                return
            }
            validateStartDate(current.startDate) != null -> {
                update { it.copy(errorText = validateStartDate(current.startDate)) }
                return
            }
            current.preparing -> return
        }
        update { it.copy(preparing = true, errorText = null) }
        val request = PlanRequest(
            city,
            current.origin.trim(),
            current.startDate.trim(),
            current.days,
            current.travelers,
            current.budget.trim().toIntOrNull() ?: 0,
            current.preferences.toList(),
        )
        viewModelScope.launch {
            val roomResult = withContext(Dispatchers.IO) {
                runCatching { container.roomRepository.ensureRoom() }
            }
            roomResult.onSuccess { room ->
                update { it.copy(preparing = false) }
                startRun(room, request)
            }.onFailure { error ->
                update {
                    it.copy(
                        preparing = false,
                        phase = PlanPhase.FORM,
                        errorText = error.message
                            ?: context.getString(R.string.plan_error_room_failed),
                    )
                }
            }
        }
    }

    fun stop() {
        session?.cancel()
        session = null
        update {
            it.copy(
                phase = PlanPhase.FORM,
                connected = false,
                errorText = null,
                streamText = "",
                stages = emptyList(),
                members = emptyList(),
            )
        }
    }

    fun retry() {
        val current = _state.value
        val request = current.activeRequest
        val room = current.room
        if (request != null && room != null) {
            startRun(room, request)
        } else {
            update { it.copy(phase = PlanPhase.FORM) }
        }
    }

    fun resetToForm() {
        session?.cancel()
        session = null
        update {
            it.copy(
                phase = PlanPhase.FORM,
                result = null,
                stages = emptyList(),
                streamText = "",
                members = emptyList(),
                connected = false,
                errorText = null,
                saveError = null,
                chatHint = null,
                editedNodes = null,
                notes = emptyMap(),
                hasUnsavedEdits = false,
                editorOpen = false,
                variantSaving = false,
                variantNotice = null,
                variantError = null,
            )
        }
    }

    fun save() {
        val current = _state.value
        val planResult = current.result
        val request = current.activeRequest
        if (planResult == null || request == null || current.saving) return
        update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching {
                    container.planningRepository.savePlan(
                        request,
                        planResult,
                        current.editedNodes,
                        current.notes,
                    )
                }
            }
            outcome.onSuccess { tripId ->
                update { it.copy(saving = false, hasUnsavedEdits = false, showDiscardPrompt = false) }
                _events.emit(PlanningEvent.Saved(tripId))
            }.onFailure { error ->
                update {
                    it.copy(
                        saving = false,
                        saveError = error.message ?: context.getString(R.string.plan_error_save_failed),
                    )
                }
            }
        }
    }

    /**
     * 把当前结果加入房间方案池，供成员投票。
     *
     * <p>路线按编辑后的节点序列化；预算基线由网关在采纳时按节点费用估算，
     * 因此这里必须带上 cost_estimate，否则采纳后预算为 0。
     */
    fun saveVariant() {
        val current = _state.value
        val room = current.room ?: return
        val result = current.result ?: return
        val request = current.activeRequest ?: return
        if (current.variantSaving) return
        val nodes = (current.editedNodes ?: result.route).filter { it.day >= 1 }
        if (nodes.isEmpty()) return
        val days = maxOf(result.days, request.days)
        val name = context.getString(R.string.plan_variant_name, request.destination, days)
        val style = request.preferences.firstOrNull() ?: context.getString(R.string.plan_variant_style)
        update { it.copy(variantSaving = true, variantNotice = null, variantError = null) }
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching {
                    container.variantRepository.save(room.roomId, name, style, variantRouteJson(nodes))
                }
            }
            outcome.onSuccess {
                update {
                    it.copy(
                        variantSaving = false,
                        variantNotice = context.getString(R.string.plan_variant_saved),
                    )
                }
            }.onFailure { error ->
                update {
                    it.copy(
                        variantSaving = false,
                        variantError = error.message ?: context.getString(R.string.variant_error_save),
                    )
                }
            }
        }
    }

    fun clearVariantNotice() = update { it.copy(variantNotice = null, variantError = null) }

    /** 方案池只认数组结构；字段名与网关估算预算的口径保持一致。 */
    private fun variantRouteJson(nodes: List<RouteNode>): String {
        val routes = nodes.map { node ->
            buildMap<String, Any> {
                put("name", node.name)
                put("day", node.day)
                if (node.time.isNotBlank()) put("time", node.time)
                if (node.category.isNotBlank()) put("type", node.category)
                if (node.costEstimate.isNotBlank()) put("cost_estimate", node.costEstimate)
                if (node.address.isNotBlank()) put("address", node.address)
                if (node.hasCoordinate()) {
                    put("lng", node.lng)
                    put("lat", node.lat)
                }
            }
        }
        return gson.toJson(routes)
    }

    fun sendFeedback(score: Int) {
        val current = _state.value
        val request = current.activeRequest
        val room = current.room
        if (request == null || room == null || current.feedbackBusy) return
        update { it.copy(feedbackBusy = true, feedbackError = null) }
        val target = context.getString(R.string.plan_feedback_target, request.destination, request.days)
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching {
                    container.feedbackRepository.submit(
                        room,
                        target,
                        score,
                        current.feedbackReason.trim(),
                    )
                }
            }
            outcome.onSuccess {
                update { it.copy(feedbackBusy = false, feedbackDone = score) }
            }.onFailure { error ->
                update {
                    it.copy(
                        feedbackBusy = false,
                        feedbackError = error.message
                            ?: context.getString(R.string.plan_error_feedback_failed),
                    )
                }
            }
        }
    }

    fun onFeedbackReasonChange(value: String) = update { it.copy(feedbackReason = value) }

    fun selectDay(day: Int?) = update { it.copy(selectedDay = day) }

    fun openEditor() = update { it.copy(editorOpen = true) }

    fun closeEditor() = update { it.copy(editorOpen = false) }

    fun onNoteChange(key: String, value: String) {
        update {
            it.copy(
                notes = if (value.isBlank()) it.notes - key else it.notes + (key to value),
                hasUnsavedEdits = true,
            )
        }
    }

    fun applyEdits(nodes: List<RouteNode>) {
        update { it.copy(editedNodes = nodes, hasUnsavedEdits = true, editorOpen = false) }
    }

    /** 有未保存修改时离开前的确认弹层。 */
    fun requestDiscard() = update { it.copy(showDiscardPrompt = true) }

    fun dismissDiscard() = update { it.copy(showDiscardPrompt = false) }

    override fun onCleared() {
        session?.cancel()
        session = null
        super.onCleared()
    }

    private fun startRun(room: RoomSession, request: PlanRequest) {
        session?.cancel()
        session = null
        CandidateDetailStore.clear()
        container.planningSnapshotStore.clear()
        update {
            it.copy(
                phase = PlanPhase.RUNNING,
                room = room,
                activeRequest = request,
                stages = emptyList(),
                streamText = "",
                members = emptyList(),
                connected = false,
                errorText = null,
                saveError = null,
                result = null,
                selectedDay = null,
                feedbackDone = null,
                feedbackReason = "",
                feedbackError = null,
                editedNodes = null,
                notes = emptyMap(),
                hasUnsavedEdits = false,
                editorOpen = false,
                variantSaving = false,
                variantNotice = null,
                variantError = null,
            )
        }

        val listener = object : PlanningStreamRepository.Listener {
            override fun onOpen() {
                update { it.copy(connected = true) }
            }

            override fun onMembers(list: List<RoomMember>) {
                update { it.copy(members = list) }
            }

            override fun onVariantSync() {
                // 规划页不展示方案池，入口在协作房间。
            }

            override fun onStage(label: String, detail: String) {
                update { current ->
                    val last = current.stages.lastOrNull()
                    if (last != null && last.label == label && last.detail == detail) {
                        current
                    } else {
                        current.copy(stages = (current.stages + StageEntry(label, detail)).takeLast(14))
                    }
                }
            }

            override fun onToken(token: String) {
                update { it.copy(streamText = (it.streamText + token).takeLast(600)) }
            }

            override fun onError(message: String) {
                update { it.copy(errorText = message, phase = if (it.result == null) PlanPhase.FAILED else it.phase) }
            }

            override fun onFinished(value: PlanResult) {
                session?.cancel()
                session = null
                update { it.copy(result = value, phase = PlanPhase.DONE) }
                putCandidateDetails(value)
            }

            override fun onClosed(reason: String) {
                update { current ->
                    if (current.phase != PlanPhase.RUNNING) {
                        current.copy(connected = false)
                    } else {
                        current.copy(
                            connected = false,
                            phase = PlanPhase.FAILED,
                            errorText = current.errorText
                                ?: context.getString(R.string.plan_error_stream_closed, reason),
                        )
                    }
                }
            }
        }
        session = container.planningStreamRepository.startPlan(room, request, listener)
    }

    private fun putCandidateDetails(result: PlanResult) {
        val details = result.toDetailDataList(context)
        CandidateDetailStore.putAll(details)
        // 落盘一份规划结果快照：进程重建后候选详情仍能按稳定 ID 恢复。
        val payload = result.payload?.toString().orEmpty()
        if (payload.isNotBlank()) {
            container.planningSnapshotStore.savePayload(payload)
        }
    }

    private fun update(transform: (PlanningUiState) -> PlanningUiState) {
        _state.update(transform)
    }

    /**
     * 进程被系统回收后只恢复「能重放」的部分：表单、已完成结果与用户编辑。
     * 推演中的实时通道无法恢复，恢复时按失败态提示重试。
     */
    private fun initialState(): PlanningUiState {
        val restored = restorePersisted()
        if (restored != null) return restored

        val prefill = PlanningPrefill.consume()
        return PlanningUiState(
            destination = prefill?.first.orEmpty(),
            chatText = prefill?.second?.takeIf { it.isNotBlank() }
                ?.let { context.getString(R.string.plan_chat_replan, it) }
                .orEmpty(),
            room = container.roomRepository.currentRoom(),
        )
    }

    private fun restorePersisted(): PlanningUiState? {
        val raw = handle.get<String>(KEY_STATE) ?: return null
        val saved = runCatching { gson.fromJson(raw, PersistedPlanningState::class.java) }.getOrNull() ?: return null
        val restoredResult = saved.payload?.let { payload ->
            runCatching { PlanResult.parse(JsonParser.parseString(payload)) }.getOrNull()
        }
        val storedPhase = runCatching { PlanPhase.valueOf(saved.phase) }.getOrDefault(PlanPhase.FORM)
        val interrupted = storedPhase == PlanPhase.RUNNING
        val phase = when {
            interrupted -> PlanPhase.FAILED
            storedPhase == PlanPhase.DONE && restoredResult == null -> PlanPhase.FAILED
            else -> storedPhase
        }
        return PlanningUiState(
            phase = phase,
            destination = saved.destination,
            origin = saved.origin,
            startDate = saved.startDate,
            days = saved.days,
            travelers = saved.travelers,
            budget = saved.budget,
            preferences = saved.preferences.toSet(),
            chatText = saved.chatText,
            chatHint = saved.chatHint,
            chatParsed = saved.chatParsed,
            room = saved.room,
            errorText = if (interrupted) {
                context.getString(R.string.plan_error_interrupted)
            } else {
                saved.errorText
            },
            result = restoredResult,
            activeRequest = saved.request?.toRequest(),
            selectedDay = saved.selectedDay,
            editedNodes = saved.editedNodes,
            notes = saved.notes ?: emptyMap(),
            hasUnsavedEdits = saved.hasUnsavedEdits,
        )
    }

    private fun persist(snapshot: PersistedPlanningState) {
        handle[KEY_STATE] = gson.toJson(snapshot)
    }

    companion object {
        private const val KEY_STATE = "planning_state"
        private const val MILLIS_PER_DAY = 86_400_000L
    }
}

/** 需要跨进程重建保留的字段；流式进度、成员列表这类瞬时数据不落盘。 */
private data class PersistedPlanningState(
    val phase: String = PlanPhase.FORM.name,
    val destination: String = "",
    val origin: String = "",
    val startDate: String = "",
    val days: Int = 3,
    val travelers: Int = 2,
    val budget: String = "",
    val preferences: List<String> = emptyList(),
    val chatText: String = "",
    val chatHint: String? = null,
    val chatParsed: ParsedIntent? = null,
    val room: RoomSession? = null,
    val errorText: String? = null,
    val request: PersistedRequest? = null,
    val payload: String? = null,
    val selectedDay: Int? = null,
    val editedNodes: List<RouteNode>? = null,
    val notes: Map<String, String> = emptyMap(),
    val hasUnsavedEdits: Boolean = false,
)

/** PlanRequest 是 Java 不可变对象，落盘时只存原始字段，恢复时重新构造。 */
private data class PersistedRequest(
    val destination: String = "",
    val origin: String = "",
    val startDate: String = "",
    val days: Int = 1,
    val travelers: Int = 1,
    val budget: Int = 0,
    val preferences: List<String> = emptyList(),
) {
    fun toRequest(): PlanRequest {
        return PlanRequest(destination, origin, startDate, days, travelers, budget, preferences)
    }
}

private fun PlanningUiState.toPersisted(): PersistedPlanningState {
    return PersistedPlanningState(
        phase = phase.name,
        destination = destination,
        origin = origin,
        startDate = startDate,
        days = days,
        travelers = travelers,
        budget = budget,
        preferences = preferences.toList(),
        chatText = chatText,
        chatHint = chatHint,
        chatParsed = chatParsed,
        room = room,
        errorText = errorText,
        request = activeRequest?.let {
            PersistedRequest(
                it.destination,
                it.origin,
                it.startDate,
                it.days,
                it.travelers,
                it.budget,
                it.preferences.toList(),
            )
        },
        payload = result?.payload?.toString(),
        selectedDay = selectedDay,
        editedNodes = editedNodes,
        notes = notes,
        hasUnsavedEdits = hasUnsavedEdits,
    )
}

