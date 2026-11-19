package com.omniroute.app.feature.plan

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.omniroute.app.AppContainer
import com.omniroute.app.core.planning.PlanResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class CandidateDetailUiState(
    val loading: Boolean = true,
    val detail: CandidateDetailData? = null,
)

/**
 * 候选详情页状态：导航只传 (分组, 稳定 ID)，详情按以下顺序恢复：
 * SavedStateHandle 缓存 → 进程内注册表 → 持久化的最近一次规划结果。
 */
internal class CandidateDetailViewModel(
    container: AppContainer,
    private val handle: SavedStateHandle,
    private val context: Context,
    private val group: String,
    private val candidateId: String,
) : ViewModel() {

    private val gson = Gson()
    private val snapshotStore = container.planningSnapshotStore

    private val _state = MutableStateFlow(CandidateDetailUiState())
    val state: StateFlow<CandidateDetailUiState> = _state.asStateFlow()

    init {
        val inMemory = restoreFromHandle()
            ?: CandidateDetailStore.get(group, candidateId)?.also { cacheToHandle(it) }
        if (inMemory != null) {
            _state.value = CandidateDetailUiState(loading = false, detail = inMemory)
        } else {
            viewModelScope.launch {
                val restored = withContext(Dispatchers.IO) { restoreFromSnapshot() }
                if (restored != null) {
                    CandidateDetailStore.put(restored)
                    cacheToHandle(restored)
                    _state.value = CandidateDetailUiState(loading = false, detail = restored)
                } else {
                    _state.value = CandidateDetailUiState(loading = false, detail = null)
                }
            }
        }
    }

    private fun restoreFromHandle(): CandidateDetailData? {
        val raw = handle.get<String>(KEY_DETAIL) ?: return null
        return runCatching { gson.fromJson(raw, CandidateDetailData::class.java) }.getOrNull()
    }

    private fun cacheToHandle(detail: CandidateDetailData) {
        runCatching { handle[KEY_DETAIL] = gson.toJson(detail) }
    }

    private fun restoreFromSnapshot(): CandidateDetailData? {
        val payload = snapshotStore.payload
        if (payload.isBlank()) return null
        return runCatching {
            val result = PlanResult.parse(JsonParser.parseString(payload)) ?: return@runCatching null
            result.toDetailDataList(context)
                .firstOrNull { it.groupTitle == group && it.id == candidateId }
        }.getOrNull()
    }

    private companion object {
        const val KEY_DETAIL = "candidate_detail"
    }
}
