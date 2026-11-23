package com.omniroute.app.feature.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omniroute.app.AppContainer
import com.omniroute.app.R
import com.omniroute.app.core.network.dto.TripPlanDto
import com.omniroute.app.core.network.dto.UserDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 首页的完整可观察状态，旋转屏幕或短暂离开页面都不会丢失。 */
internal data class HomeUiState(
    val user: UserDto? = null,
    val loadingProfile: Boolean = true,
    val profileError: String? = null,
    val loggingOut: Boolean = false,
    val trips: List<TripPlanDto> = emptyList(),
    val loadingTrips: Boolean = true,
    val tripsError: String? = null,
    val alerts: HomeAlerts? = null,
    val alertsUnavailable: Boolean = false,
    val lastRefreshTick: Int = 0,
    val refreshing: Boolean = false,
)

internal data class HomeAlerts(val unreadCount: Int, val hasWarning: Boolean)
private val WarningSeverities = setOf("medium", "moderate", "high", "critical", "severe")

internal sealed interface HomeEvent {
    /** 退出登录完成：页面据此回到登录页。 */
    data object LoggedOut : HomeEvent
}

/**
 * 首页状态持有者。
 *
 * <p>页面只渲染 {@link HomeUiState} 并调用事件方法：旋转、分屏或从子页面返回
 * 都复用同一份数据，不再依赖 Composable 里的 remember。
 */
internal class HomeViewModel(
    private val container: AppContainer,
    private val context: Context,
    initialUser: UserDto?,
) : ViewModel() {

    private val _state = MutableStateFlow(
        HomeUiState(user = initialUser ?: container.authRepository.user),
    )
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<HomeEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()
    private var alertsJob: Job? = null

    init {
        loadProfile()
        loadTrips()
        loadAlerts()
    }

    /** 从其它页面返回时递增的刷新信号；重复值直接忽略，避免重建时重复拉取。 */
    fun onRefreshTick(tick: Int) {
        if (tick == _state.value.lastRefreshTick) return
        _state.update { it.copy(lastRefreshTick = tick) }
        loadTrips()
        loadAlerts()
    }

    /** 下拉刷新：行程与未读数一起重拉，失败时保留旧列表只提示错误。 */
    fun refresh() {
        if (_state.value.refreshing) return
        _state.update { it.copy(refreshing = true) }
        val alertRefresh = loadAlerts()
        viewModelScope.launch {
            val tripsOutcome = withContext(Dispatchers.IO) {
                runCatching { container.planningRepository.listTrips() }
            }
            alertRefresh.join()
            _state.update { current ->
                val error = tripsOutcome.exceptionOrNull()?.let {
                    it.message ?: context.getString(R.string.home_error_trips)
                }
                current.copy(
                    trips = tripsOutcome.getOrNull() ?: current.trips,
                    tripsError = error,
                    loadingTrips = false,
                    refreshing = false,
                )
            }
        }
    }

    fun loadProfile() {
        // 已有用户快照时后台刷新，不闪加载态。
        if (_state.value.user == null) _state.update { it.copy(loadingProfile = true) }
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching { container.authRepository.loadProfile() }
            }
            outcome.onSuccess { user ->
                _state.update { it.copy(user = user, profileError = null, loadingProfile = false) }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        profileError = error.message ?: context.getString(R.string.home_error_profile),
                        loadingProfile = false,
                    )
                }
            }
        }
    }

    fun loadTrips() {
        _state.update { it.copy(loadingTrips = true) }
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching { container.planningRepository.listTrips() }
            }
            outcome.onSuccess { trips ->
                _state.update { it.copy(trips = trips, tripsError = null, loadingTrips = false) }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        tripsError = error.message ?: context.getString(R.string.home_error_trips),
                        loadingTrips = false,
                    )
                }
            }
        }
    }

    private fun loadAlerts(): Job {
        alertsJob?.cancel()
        return viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching { container.notificationRepository.unreadRiskNotifications() }
            }
            outcome.onSuccess { response ->
                val warning = response.notifications.any { item ->
                    val type = item.type.orEmpty().lowercase()
                    type.startsWith("weather") || type.startsWith("traffic") ||
                        item.severity?.lowercase() in WarningSeverities
                }
                val count = maxOf(response.unreadCount, response.notifications.size)
                _state.update { it.copy(alerts = HomeAlerts(count, warning), alertsUnavailable = false) }
            }.onFailure {
                _state.update { it.copy(alertsUnavailable = true) }
            }
        }.also { alertsJob = it }
    }

    fun logout() {
        if (_state.value.loggingOut) return
        _state.update { it.copy(loggingOut = true) }
        viewModelScope.launch {
            withContext(Dispatchers.IO) { runCatching { container.authRepository.logout() } }
            _state.update { it.copy(loggingOut = false) }
            _events.emit(HomeEvent.LoggedOut)
        }
    }
}
