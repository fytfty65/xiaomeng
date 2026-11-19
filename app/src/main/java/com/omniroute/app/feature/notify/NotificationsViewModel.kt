package com.omniroute.app.feature.notify

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omniroute.app.AppContainer
import com.omniroute.app.R
import com.omniroute.app.core.network.dto.RiskNotificationDto
import com.omniroute.app.core.notify.RiskAlertScheduler
import com.omniroute.app.core.notify.RiskNotifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 通知中心的完整可观察状态。 */
internal data class NotificationsUiState(
    val items: List<RiskNotificationDto> = emptyList(),
    val loading: Boolean = true,
    val errorText: String? = null,
    val unreadOnly: Boolean = false,
    val busyId: String? = null,
    val refreshing: Boolean = false,
    val markingAllRead: Boolean = false,
    val pushEnabled: Boolean = false,
    val pushBusy: Boolean = false,
    val pushCities: List<String> = emptyList(),
    val pushNotice: String? = null,
)

/**
 * 通知中心状态持有者：列表、未读过滤与逐条已读都在这里，
 * 页面只负责渲染与转发点击。
 */
internal class NotificationsViewModel(
    private val container: AppContainer,
    private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    init {
        RiskNotifier.ensureChannel(context)
        _state.update { it.copy(pushEnabled = RiskAlertScheduler.isEnabled(context)) }
        RiskAlertScheduler.sync(context)
        load()
        refreshSubscriptions()
    }

    /** 已订阅的监控城市，用于在开关卡片下回显。 */
    fun refreshSubscriptions() {
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching { container.notificationRepository.subscribedCities() }
            }
            outcome.onSuccess { cities ->
                _state.update { it.copy(pushCities = cities) }
            }
        }
    }

    /**
     * 开关系统通知。
     *
     * <p>首次开启时把最近行程的目的地纳入风险订阅 —— 没有订阅城市，后台巡检就拿不到任何告警。
     * 订阅失败不阻塞开关本身，用户仍可手动「立即检查」看到已有通知。
     */
    fun setPushEnabled(enabled: Boolean) {
        if (_state.value.pushBusy) return
        _state.update { it.copy(pushBusy = true, pushNotice = null) }
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching {
                    if (enabled) subscribeRecentDestinations()
                    enabled
                }
            }
            outcome.onSuccess {
                RiskAlertScheduler.setEnabled(context, enabled)
                RiskAlertScheduler.sync(context)
                if (enabled) RiskAlertScheduler.runOnce(context)
                _state.update { it.copy(pushEnabled = enabled, pushBusy = false) }
                refreshSubscriptions()
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        pushBusy = false,
                        errorText = error.message ?: context.getString(R.string.notify_push_denied),
                    )
                }
            }
        }
    }

    fun onPushPermissionDenied() {
        RiskAlertScheduler.setEnabled(context, false)
        RiskAlertScheduler.sync(context)
        _state.update {
            it.copy(pushEnabled = false, errorText = context.getString(R.string.notify_push_denied))
        }
    }

    /** 手动巡检：立即拉一次风险变更，并把新的未读直接弹成系统通知。 */
    fun checkNow() {
        if (_state.value.pushBusy) return
        _state.update { it.copy(pushBusy = true, pushNotice = null) }
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching {
                    container.notificationRepository.checkRiskSubscriptions()
                    container.notificationRepository.listRiskNotifications("unread")
                }
            }
            outcome.onSuccess { items ->
                items.forEach { RiskNotifier.notify(context, it) }
                _state.update {
                    it.copy(
                        pushBusy = false,
                        pushNotice = context.getString(R.string.notify_push_checked, items.size),
                    )
                }
                load()
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        pushBusy = false,
                        errorText = error.message ?: context.getString(R.string.notify_load_failed),
                    )
                }
            }
        }
    }

    private fun subscribeRecentDestinations() {
        val trips = container.planningRepository.listTrips()
        trips.asSequence()
            .mapNotNull { it.destCity?.trim()?.takeIf(String::isNotEmpty) }
            .distinct()
            .take(3)
            .forEach { city ->
                runCatching { container.notificationRepository.subscribeCity(city) }
            }
    }

    fun load() {
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching { container.notificationRepository.listRiskNotifications("") }
            }
            outcome.onSuccess { items ->
                _state.update { it.copy(items = items, errorText = null, loading = false) }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        errorText = error.message ?: context.getString(R.string.notify_load_failed),
                        loading = false,
                    )
                }
            }
        }
    }

    fun setUnreadOnly(value: Boolean) {
        _state.update { it.copy(unreadOnly = value) }
    }

    /** 下拉刷新：失败时保留旧列表，只更新错误提示。 */
    fun refresh() {
        if (_state.value.refreshing) return
        _state.update { it.copy(refreshing = true) }
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching { container.notificationRepository.listRiskNotifications("") }
            }
            _state.update { current ->
                current.copy(
                    items = outcome.getOrNull() ?: current.items,
                    errorText = outcome.exceptionOrNull()?.let {
                        it.message ?: context.getString(R.string.notify_load_failed)
                    },
                    loading = false,
                    refreshing = false,
                )
            }
        }
    }

    fun markRead(item: RiskNotificationDto) {
        val id = item.id
        if (id.isNullOrBlank() || _state.value.busyId != null || _state.value.markingAllRead) return
        _state.update { it.copy(busyId = id) }
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching { container.notificationRepository.markRead(id, "read") }
            }
            _state.update { it.copy(busyId = null) }
            outcome.onSuccess { load() }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            errorText = error.message
                                ?: context.getString(R.string.notify_mark_read_failed),
                        )
                    }
                }
        }
    }

    /** 一键已读：请求成功后重拉列表，未读过滤会自动变为空态。 */
    fun markAllRead() {
        if (_state.value.markingAllRead || _state.value.busyId != null) return
        if (_state.value.items.none { it.isUnread }) return
        _state.update { it.copy(markingAllRead = true) }
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching { container.notificationRepository.markAllRead("read") }
            }
            outcome.onSuccess { load() }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            errorText = error.message
                                ?: context.getString(R.string.notify_mark_all_read_failed),
                        )
                    }
                }
            _state.update { it.copy(markingAllRead = false) }
        }
    }
}
