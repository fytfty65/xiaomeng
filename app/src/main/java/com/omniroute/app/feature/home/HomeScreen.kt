package com.omniroute.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.omniroute.app.AppContainer
import com.omniroute.app.R
import com.omniroute.app.core.network.dto.UserDto
import com.omniroute.app.core.ui.ContentMaxWidth
import com.omniroute.app.core.ui.wheelScroll
import com.omniroute.app.feature.auth.BrandOrange
import com.omniroute.app.feature.auth.CreamBg
import com.omniroute.app.feature.auth.ErrorBg
import com.omniroute.app.feature.auth.ErrorFg
import com.omniroute.app.feature.auth.InlineBanner
import com.omniroute.app.feature.auth.Ink

/** 登录后的首页：展示账号资料、已保存行程，并提供规划入口与退出登录。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    container: AppContainer,
    initialUser: UserDto?,
    refreshTick: Int,
    onStartPlanning: () -> Unit,
    onOpenSeason: () -> Unit,
    onOpenTrip: (String) -> Unit,
    onOpenRoom: () -> Unit,
    onOpenCommunity: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit,
    onLoggedOut: () -> Unit,
) {
    val context = LocalContext.current
    // 页面状态统一由 ViewModel 持有：旋转、分屏或短暂离开都不会丢数据。
    val viewModel: HomeViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                HomeViewModel(container, context.applicationContext, initialUser)
            }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val user = state.user
    val tripsError = state.tripsError

    LaunchedEffect(refreshTick) { viewModel.onRefreshTick(refreshTick) }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { onLoggedOut() }
    }

    val listState = rememberLazyListState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBg),
        contentAlignment = Alignment.TopCenter,
    ) {
        // 平板/扩展屏上正文限制最大宽度，避免卡片被横向拉成长条。
        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = ContentMaxWidth),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .wheelScroll(listState)
                    .windowInsetsPadding(WindowInsets.safeDrawing),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            ) {
                item(key = "home_header") {
                    TopBrandBar(user, onClick = onOpenProfile)
                    state.profileError?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        InlineBanner(text = it, bg = ErrorBg, fg = ErrorFg, icon = Icons.Filled.Warning)
                        Text(
                            text = stringResource(R.string.home_reload_profile),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandOrange,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clickable(role = Role.Button) { viewModel.loadProfile() },
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.home_heading),
                        style = MaterialTheme.typography.headlineSmall,
                        fontSize = 29.sp,
                        lineHeight = 38.sp,
                        color = Ink,
                        modifier = Modifier.semantics { heading() },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SeasonCover(onClick = onOpenSeason)
                    Spacer(modifier = Modifier.height(12.dp))
                    NewTripButton(onClick = onStartPlanning)
                    Spacer(modifier = Modifier.height(12.dp))
                    QuickEntriesRow(
                        unreadCount = state.unreadCount,
                        onOpenRoom = onOpenRoom,
                        onOpenNotifications = onOpenNotifications,
                        onOpenCommunity = onOpenCommunity,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    TripsSectionHeader(
                        count = state.trips.size.takeIf { !state.loadingTrips && tripsError == null },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 行程卡片按 id 使用稳定 key，避免局部刷新时整列表重组。
                when {
                    state.loadingTrips -> item(key = "home_trips_loading") { TripsLoadingCard() }

                    tripsError != null -> item(key = "home_trips_error") {
                        TripsErrorCard(
                            errorText = tripsError,
                            onRetry = { viewModel.loadTrips() },
                        )
                    }

                    state.trips.isEmpty() -> item(key = "home_trips_empty") {
                        EmptyTrips()
                    }

                    else -> items(
                        items = state.trips,
                        key = { trip -> trip.id ?: trip.hashCode() },
                    ) { trip ->
                        SavedTripRow(
                            trip = trip,
                            onClick = { onOpenTrip(trip.id.orEmpty()) },
                        )
                    }
                }

                item(key = "home_footer") {
                    CleanAccountFooter(
                        loggingOut = state.loggingOut,
                        onLogout = { viewModel.logout() },
                    )
                }
            }
        }
    }
}
