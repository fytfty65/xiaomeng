package com.omniroute.app.feature.trip

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.omniroute.app.AppContainer
import com.omniroute.app.R
import com.omniroute.app.core.network.dto.JourneyStateData
import com.omniroute.app.core.network.dto.PackingChecklistData
import com.omniroute.app.core.network.dto.PackingItemDto
import com.omniroute.app.core.network.dto.ReplanProposalDto
import com.omniroute.app.core.network.dto.TripPlanDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 节点在旅途页的展示模型，key 与服务端执行的 node_key 完全一致。 */
data class JourneyNodeUi(
    val key: String,
    val name: String,
    val day: Int,
    val status: String,
    val delayMinutes: Int,
)

data class ReplanCandidateUi(
    val id: String,
    val title: String,
    val meta: String,
    val payload: JsonObject,
)

data class JourneyUiState(
    val loading: Boolean = true,
    val loadError: String? = null,
    val tripTitle: String = "",
    val phase: String = "planned",
    val progress: Int = 0,
    val completed: Int = 0,
    val total: Int = 0,
    val nextActions: List<String> = emptyList(),
    val nodes: List<JourneyNodeUi> = emptyList(),
    val updatingNodeKey: String? = null,
    val delayDialogNode: JourneyNodeUi? = null,
    val packing: List<PackingItemDto> = emptyList(),
    val checkedPackingIds: Set<String> = emptySet(),
    val packingLoading: Boolean = false,
    val proposal: ReplanProposalDto? = null,
    val candidates: List<ReplanCandidateUi> = emptyList(),
    val candidatesLoading: Boolean = false,
    val applyingCandidateId: String? = null,
    val replanError: String? = null,
    val notice: String? = null,
)

/**
 * 旅途助手：执行态、风险提案与行前清单。
 * 所有写操作都以服务端返回为准；重规划必须经过 validate + 用户确认，避免客户端擅自改行程。
 */
class JourneyViewModel(
    private val container: AppContainer,
    private val tripId: String,
    private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(JourneyUiState())
    val state: StateFlow<JourneyUiState> = _state.asStateFlow()

    private var trip: TripPlanDto? = null

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, loadError = null, replanError = null) }
            val outcome = withContext(Dispatchers.IO) {
                runCatching {
                    val found = container.planningRepository.listTrips()
                        .firstOrNull { it.id == tripId }
                        ?: throw IllegalStateException(context.getString(R.string.plan_error_trip_missing))
                    trip = found
                    val journey = runCatching {
                        container.planningRepository.journeyState(tripId)
                    }.getOrNull()
                    val packing = runCatching {
                        container.planningRepository.packingChecklist(found)
                    }.getOrDefault(PackingChecklistData())
                    val routes = container.planningRepository.readRoute(found.content)
                    Loaded(found, journey, packing, routes)
                }
            }
            outcome.onSuccess { loaded ->
                trip = loaded.trip
                _state.update { previous ->
                    buildState(previous, loaded)
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        loading = false,
                        loadError = error.message ?: context.getString(R.string.journey_error_load),
                    )
                }
            }
        }
    }

    /** planned / in_progress / visited / skipped / delayed 五种执行动作。 */
    fun updateStatus(node: JourneyNodeUi, status: String, delayMinutes: Int = 0) {
        if (_state.value.updatingNodeKey != null) return
        _state.update { it.copy(updatingNodeKey = node.key, delayDialogNode = null, replanError = null) }
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching {
                    container.planningRepository.updateExecution(
                        tripId,
                        node.key,
                        status,
                        delayMinutes,
                        "",
                        null,
                    )
                }
            }
            outcome.onSuccess { update ->
                _state.update { current ->
                    val nextNodes = current.nodes.map {
                        if (it.key == node.key) {
                            it.copy(
                                status = update.state?.status ?: status,
                                delayMinutes = update.state?.delayMinutes ?: delayMinutes,
                            )
                        } else {
                            it
                        }
                    }
                    current.copy(
                        nodes = nextNodes,
                        updatingNodeKey = null,
                        proposal = update.replanProposal ?: current.proposal,
                        notice = if (delayMinutes > 0) {
                            context.getString(R.string.journey_delay_recorded, delayMinutes)
                        } else {
                            current.notice
                        },
                    )
                }
                // 进度与下一站由服务端快照决定，动作成功后同步一次。
                refreshJourneyOnly()
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        updatingNodeKey = null,
                        replanError = error.message ?: context.getString(R.string.journey_error_load),
                    )
                }
            }
        }
    }

    fun openDelayDialog(node: JourneyNodeUi) {
        if (_state.value.updatingNodeKey != null) return
        _state.update { it.copy(delayDialogNode = node) }
    }

    fun dismissDelayDialog() = _state.update { it.copy(delayDialogNode = null) }

    /** 为当前提案拉取受影响节点的替代候选。 */
    fun loadCandidates() {
        val proposal = _state.value.proposal ?: return
        if (_state.value.candidatesLoading) return
        _state.update { it.copy(candidatesLoading = true, replanError = null, candidates = emptyList()) }
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching {
                    val data = container.planningRepository.requestReplan(
                        trip,
                        proposal.affectedNodeName,
                        proposal.delayMinutes,
                    )
                    parseCandidates(data)
                }
            }
            outcome.onSuccess { candidates ->
                _state.update { it.copy(candidatesLoading = false, candidates = candidates) }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        candidatesLoading = false,
                        replanError = error.message ?: context.getString(R.string.journey_error_load),
                    )
                }
            }
        }
    }

    /** 先校验硬约束，通过后才允许确认替换。 */
    fun applyCandidate(candidate: ReplanCandidateUi) {
        val proposal = _state.value.proposal ?: return
        if (_state.value.applyingCandidateId != null) return
        _state.update { it.copy(applyingCandidateId = candidate.id, replanError = null) }
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching {
                    val validation = container.planningRepository.validateReplan(
                        trip,
                        proposal.affectedNodeName,
                        candidate.payload,
                    )
                    if (!validation.bool("hard_satisfied", false)) {
                        throw IllegalStateException(
                            context.getString(
                                R.string.journey_candidate_invalid,
                                validation.violationText().ifBlank {
                                    context.getString(R.string.journey_candidates_empty)
                                },
                            ),
                        )
                    }
                    container.planningRepository.confirmReplan(
                        tripId,
                        proposal.proposalId,
                        proposal.affectedNodeName,
                        candidate.payload,
                    )
                }
            }
            outcome.onSuccess {
                _state.update {
                    it.copy(
                        applyingCandidateId = null,
                        proposal = null,
                        candidates = emptyList(),
                        replanError = null,
                        notice = context.getString(R.string.journey_candidate_applied),
                    )
                }
                refresh()
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        applyingCandidateId = null,
                        replanError = error.message ?: context.getString(R.string.journey_error_load),
                    )
                }
            }
        }
    }

    fun rejectProposal() {
        val proposal = _state.value.proposal ?: return
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching {
                    container.planningRepository.rejectReplan(
                        tripId,
                        proposal.proposalId,
                        "keep_original",
                    )
                }
            }
            outcome.onSuccess {
                _state.update {
                    it.copy(
                        proposal = null,
                        candidates = emptyList(),
                        replanError = null,
                        notice = context.getString(R.string.journey_risk_rejected),
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(replanError = error.message ?: context.getString(R.string.journey_error_load))
                }
            }
        }
    }

    fun togglePacking(item: PackingItemDto) {
        val id = item.id ?: item.name ?: return
        _state.update {
            val next = it.checkedPackingIds.toMutableSet()
            if (!next.add(id)) next.remove(id)
            it.copy(checkedPackingIds = next)
        }
    }

    fun clearNotice() = _state.update { it.copy(notice = null) }

    fun clearReplanError() = _state.update { it.copy(replanError = null) }

    private suspend fun refreshJourneyOnly() {
        val outcome = withContext(Dispatchers.IO) {
            runCatching { container.planningRepository.journeyState(tripId) }
        }
        outcome.onSuccess { journey ->
            _state.update { current ->
                current.copy(
                    phase = journey.phase ?: current.phase,
                    progress = normalizeProgress(journey.progress),
                    completed = journey.execution?.completed ?: current.completed,
                    total = journey.execution?.total ?: current.total,
                    nextActions = journey.nextActions,
                    proposal = journey.risk?.proposal ?: current.proposal,
                )
            }
        }
    }

    private fun buildState(previous: JourneyUiState, loaded: Loaded): JourneyUiState {
        val states = loaded.journey?.execution?.states.orEmpty().associateBy { it.nodeKey }
        val nodes = loaded.routes.mapIndexedNotNull { index, route ->
            val name = (route["name"] ?: route["location"])?.toString()?.trim().orEmpty()
            if (name.isEmpty()) return@mapIndexedNotNull null
            val day = route["day"]?.toString()?.toIntOrNull() ?: 1
            // 与服务端 nodeKey(node) 的 day:name 约定保持一致。
            val key = "$day:$name"
            val state = states[key]
            JourneyNodeUi(
                key = key,
                name = name,
                day = day,
                status = state?.status ?: "planned",
                delayMinutes = state?.delayMinutes ?: 0,
            )
        }
        val journey = loaded.journey
        val total = journey?.execution?.total?.takeIf { it > 0 } ?: nodes.size
        val completed = journey?.execution?.completed
            ?: nodes.count { it.status == "visited" }
        return previous.copy(
            loading = false,
            loadError = null,
            tripTitle = loaded.trip.title ?: loaded.trip.destCity.orEmpty(),
            phase = journey?.phase ?: "planned",
            progress = normalizeProgress(journey?.progress ?: computedProgress(completed, total)),
            completed = completed,
            total = total,
            nextActions = journey?.nextActions.orEmpty(),
            nodes = nodes,
            packing = loaded.packing.items,
            packingLoading = false,
            proposal = journey?.risk?.proposal ?: previous.proposal,
        )
    }

    private fun computedProgress(completed: Int, total: Int): Double {
        return if (total <= 0) 0.0 else completed.toDouble() / total
    }

    /** 服务端 progress 是 0..1 的小数；部分实现可能已返回百分数，这里统一归一。 */
    private fun normalizeProgress(value: Double): Int {
        val ratio = if (value > 1.0) value / 100.0 else value
        return (ratio.coerceIn(0.0, 1.0) * 100).toInt()
    }

    private fun parseCandidates(data: JsonObject): List<ReplanCandidateUi> {
        val array: JsonArray = data.getAsJsonArray("replacement_candidates") ?: return emptyList()
        return array.mapIndexedNotNull { index, element ->
            if (!element.isJsonObject) return@mapIndexedNotNull null
            val candidate = element.asJsonObject
            val title = candidate.string("name")
                ?: candidate.string("location")
                ?: context.getString(R.string.journey_candidates_title)
            val price = candidate.string("price_label") ?: candidate.string("price")
            val provider = candidate.getAsJsonObject("source")?.string("provider")
                ?: candidate.string("source_label")
            val meta = listOfNotNull(price, provider).joinToString(" · ")
            ReplanCandidateUi(
                id = "${candidate.string("location") ?: title}#$index",
                title = title,
                meta = meta,
                payload = candidate,
            )
        }
    }

    private data class Loaded(
        val trip: TripPlanDto,
        val journey: JourneyStateData?,
        val packing: PackingChecklistData,
        val routes: List<Map<String, Any>>,
    )

    companion object {
        fun factory(container: AppContainer, tripId: String, context: Context): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    JourneyViewModel(container, tripId, context.applicationContext)
                }
            }
    }
}

private fun JsonObject.string(key: String): String? {
    val element = get(key) ?: return null
    if (element.isJsonNull || !element.isJsonPrimitive) return null
    return element.asString.trim().takeIf { it.isNotEmpty() }
}

private fun JsonObject.bool(key: String, fallback: Boolean): Boolean {
    val element = get(key) ?: return fallback
    if (element.isJsonNull || !element.isJsonPrimitive) return fallback
    return runCatching { element.asBoolean }.getOrDefault(fallback)
}

/** violations 既可能是 ["..."] 也可能是 [{"message":"..."}]，统一抽成可读文案。 */
private fun JsonObject.violationText(): String {
    val array = getAsJsonArray("violations") ?: return ""
    val messages = array.mapNotNull { element ->
        when {
            element.isJsonPrimitive -> element.asString
            element.isJsonObject -> element.asJsonObject.string("message")
            else -> null
        }
    }
    return messages.joinToString("；")
}
