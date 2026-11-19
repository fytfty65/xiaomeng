package com.omniroute.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import java.time.LocalTime

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

    val greeting = stringResource(
        when (LocalTime.now().hour) {
            in 0..5 -> R.string.home_greeting_night
            in 6..10 -> R.string.home_greeting_morning
            in 11..13 -> R.string.home_greeting_noon
            in 14..17 -> R.string.home_greeting_afternoon
            else -> R.string.home_greeting_evening
        },
    )

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
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                item(key = "home_header") {
                    Column {
                        // 1. 全新设计的顶部品牌导航栏（新矢量 Logo + 右侧用户胶囊）
                        TopBrandBar(
                            user = user,
                            loading = state.loadingProfile,
                            onClick = onOpenProfile,
                        )

                        Spacer(modifier = Modifier.height(22.dp))

                        // 2. 干净利落的问候标题区（去除了丑陋的宋体和巨大的 admin1）
                        WelcomeHeadline(
                            greeting = greeting,
                            user = user,
                        )

                        state.profileError?.let {
                            Spacer(modifier = Modifier.height(12.dp))
                            InlineBanner(text = it, bg = ErrorBg, fg = ErrorFg, icon = Icons.Filled.Warning)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.home_reload_profile),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BrandOrange,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clickable(role = Role.Button) { viewModel.loadProfile() }
                                    .padding(vertical = 4.dp),
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // 3. 核心视觉：带微缩航迹图表的行程规划主卡
                        RoutePlannerHeroCard(
                            onClick = onStartPlanning,
                            onOpenSeason = onOpenSeason,
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 3.1 协作房间与风险通知入口
                        QuickEntriesRow(
                            unreadCount = state.unreadCount,
                            onOpenRoom = onOpenRoom,
                            onOpenNotifications = onOpenNotifications,
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // 4. 我的行程列表区
                        ModernSectionHeader(
                            title = stringResource(R.string.home_my_trips),
                            count = if (!state.loadingTrips) state.trips.size else null,
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                    }
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
                        ModernEmptyTripsCard(onStartPlanning = onStartPlanning)
                    }

                    else -> items(
                        items = state.trips,
                        key = { trip -> trip.id ?: trip.hashCode() },
                    ) { trip ->
                        ModernTripCard(
                            trip = trip,
                            onClick = { onOpenTrip(trip.id.orEmpty()) },
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                item(key = "home_footer") {
                    Column {
                        Spacer(modifier = Modifier.height(18.dp))

                        // 5. 紧凑清爽的账号与退出底栏
                        CleanAccountFooter(
                            user = user,
                            loggingOut = state.loggingOut,
                            onLogout = { viewModel.logout() },
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
