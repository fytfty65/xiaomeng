package com.omniroute.app.ui

import androidx.navigation.NavBackStackEntry
import com.omniroute.app.core.ui.theme.SeasonEditionTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.omniroute.app.AppContainer
import com.omniroute.app.R
import com.omniroute.app.core.ui.components.LoadingScreen
import com.omniroute.app.core.ui.theme.JourneySeason
import com.omniroute.app.feature.auth.ForgotPasswordScreen
import com.omniroute.app.feature.auth.LoginScreen
import com.omniroute.app.feature.home.HomeScreen
import com.omniroute.app.feature.notify.NotificationsScreen
import com.omniroute.app.feature.plan.CandidateDetailScreen
import com.omniroute.app.feature.plan.CandidateDetailViewModel
import com.omniroute.app.feature.plan.PlanningPrefill
import com.omniroute.app.feature.plan.PlanningScreen
import com.omniroute.app.feature.plan.QUICK_PLAN_CITY
import com.omniroute.app.feature.plan.QUICK_PLAN_DAYS
import com.omniroute.app.feature.profile.ProfileScreen
import com.omniroute.app.feature.room.RoomScreen
import com.omniroute.app.feature.season.SeasonDetailScreen
import com.omniroute.app.feature.budget.BudgetScreen
import com.omniroute.app.feature.community.CommunityScreen
import com.omniroute.app.feature.gallery.GalleryScreen
import com.omniroute.app.feature.trip.JourneyScreen
import com.omniroute.app.feature.trip.TripDetailScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private object Routes {
    const val LOGIN = "login"
    const val FORGOT_PASSWORD = "forgot_password"
    const val HOME = "home"
    const val PROFILE = "profile"
    const val ROOM = "room"
    const val COMMUNITY = "community"
    const val GALLERY = "gallery"
    const val NOTIFICATIONS = "notifications"
    const val SEASON = "season"
    const val PLANNING = "planning"
    const val PLANNING_ROUTE = "planning?$QUICK_PLAN_CITY={$QUICK_PLAN_CITY}&$QUICK_PLAN_DAYS={$QUICK_PLAN_DAYS}&edition={edition}&autostart={autostart}"
    private const val EDITION_PATTERN = "?edition={edition}"
    const val TRIP_DETAIL = "trip_detail/{tripId}$EDITION_PATTERN"
    const val JOURNEY = "journey/{tripId}$EDITION_PATTERN"
    const val BUDGET = "budget/{tripId}$EDITION_PATTERN"
    const val CANDIDATE_DETAIL = "candidate_detail/{group}/{candidateId}$EDITION_PATTERN"

    fun planningFromPost(city: String, days: Int): String = quickPlanning(city, days) + "&autostart=false"

    fun seasonalPlanning(season: JourneySeason?): String = PLANNING + query(season)

    fun tripDetail(tripId: String, season: JourneySeason? = null): String =
        "trip_detail/" + android.net.Uri.encode(tripId) + query(season)

    fun quickPlanning(city: String, days: Int): String =
        "$PLANNING?$QUICK_PLAN_CITY=${android.net.Uri.encode(city)}&$QUICK_PLAN_DAYS=$days"

    fun journey(tripId: String, season: JourneySeason?): String =
        "journey/" + android.net.Uri.encode(tripId) + query(season)

    fun budget(tripId: String, season: JourneySeason?): String =
        "budget/" + android.net.Uri.encode(tripId) + query(season)

    fun candidateDetail(group: String, candidateId: String, season: JourneySeason?): String =
        "candidate_detail/" + android.net.Uri.encode(group) + "/" + android.net.Uri.encode(candidateId) + query(season)

    private fun query(season: JourneySeason?) = season?.let { "?edition=${it.key}" }.orEmpty()
}

private fun editionArgument() = navArgument("edition") {
    type = NavType.StringType
    nullable = true
    defaultValue = null
}

private fun NavBackStackEntry.lockedSeason(): JourneySeason? {
    val key = arguments?.getString("edition") ?: return null
    return JourneySeason.entries.firstOrNull { it.key == key }
}

@Composable
fun AppNavHost(
    container: AppContainer,
    season: JourneySeason,
    onSeasonChange: (JourneySeason) -> Unit,
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    // null 表示启动检查尚未结束：先显示加载，避免登录页与首页闪现切换。
    var startRoute by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    // Java 数据层是阻塞式 API，这里只维护一份 Compose 可观察的用户快照。
    var currentUser by remember { mutableStateOf(container.authRepository.user) }
    // 保存行程后回到首页时递增，触发首页重新拉取行程列表。
    var homeRefreshTick by remember { mutableIntStateOf(0) }
    val lastUsername = container.settingsStore.lastUsername

    LaunchedEffect(Unit) {
        val restored = withContext(Dispatchers.IO) { container.authRepository.refreshSilent() }
        startRoute = if (restored) Routes.HOME else Routes.LOGIN
    }

    DisposableEffect(container) {
        container.authRepository.setUserListener { currentUser = it }
        onDispose { container.authRepository.setUserListener(null) }
    }

    // 刷新失败或服务端撤销会话时，统一回到登录页。
    // 该回调可能在 NavHost 设置导航图之前触发（启动检查阶段），因此只置标记，
    // 真正跳转放到 NavHost 组合完成后的 LaunchedEffect 里，避免 getGraph() 抛异常。
    var sessionExpired by remember { mutableStateOf(false) }
    DisposableEffect(navController) {
        container.authRepository.setSessionExpiredListener { sessionExpired = true }
        onDispose { container.authRepository.setSessionExpiredListener(null) }
    }

    val resolvedStart = startRoute
    if (resolvedStart == null) {
        LoadingScreen(text = stringResource(R.string.auth_checking_login))
        return
    }

    NavHost(navController = navController, startDestination = resolvedStart) {
        composable(Routes.LOGIN) {
            LoginScreen(
                initialUsername = lastUsername,
                notice = notice,
                season = season,
                onSeasonChange = onSeasonChange,
                onLogin = { username, password ->
                    withContext(Dispatchers.IO) {
                        runCatching { container.authRepository.login(username, password) }
                    }
                },
                onRegister = { username, password, nickname ->
                    withContext(Dispatchers.IO) {
                        runCatching { container.authRepository.register(username, password, nickname) }
                    }
                },
                onLoggedIn = { user ->
                    notice = null
                    container.settingsStore.setLastUsername(user.username)
                    navController.navigate(Routes.HOME) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onForgotPassword = {
                    notice = null
                    navController.navigate(Routes.FORGOT_PASSWORD)
                },
            )
        }

        composable(Routes.FORGOT_PASSWORD) {
            SeasonEditionTheme(season) {
                ForgotPasswordScreen(
                    onRequestReset = { username ->
                        withContext(Dispatchers.IO) {
                            runCatching { container.authRepository.forgotPassword(username) }
                        }
                    },
                    onResetPassword = { token, newPassword ->
                        withContext(Dispatchers.IO) {
                            runCatching { container.authRepository.resetPassword(token, newPassword) }
                        }
                    },
                    onBackToLogin = { navController.popBackStack() },
                )
            }
        }

        composable(Routes.HOME) {
            HomeScreen(
                container = container,
                initialUser = currentUser,
                refreshTick = homeRefreshTick,
                onStartPlanning = { navController.navigate(Routes.PLANNING) },
                onQuickPlan = { city, days ->
                    navController.navigate(Routes.quickPlanning(city, days)) { launchSingleTop = true }
                },
                onSeasonChange = onSeasonChange,
                onOpenSeason = { navController.navigate(Routes.SEASON) },
                onOpenTrip = { tripId ->
                    if (tripId.isNotBlank()) {
                        navController.navigate(Routes.tripDetail(tripId))
                    }
                },
                onOpenRoom = { navController.navigate(Routes.ROOM) },
                onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                onOpenProfile = { navController.navigate(Routes.PROFILE) },
                onLoggedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(Routes.SEASON) {
            SeasonDetailScreen(
                season = season,
                onStartPlanning = { preview -> navController.navigate(Routes.seasonalPlanning(preview)) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.PROFILE) {
            SeasonEditionTheme(season) {
                ProfileScreen(
                    container = container,
                    initialUser = currentUser,
                    season = season,
                    onSeasonChange = onSeasonChange,
                    onOpenTrips = {
                        homeRefreshTick++
                        navController.popBackStack()
                    },
                    onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                    onOpenRoom = { navController.navigate(Routes.ROOM) },
                    onOpenCommunity = { navController.navigate(Routes.COMMUNITY) },
                    onOpenGallery = { navController.navigate(Routes.GALLERY) },
                    onLoggedOut = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onBack = {
                        homeRefreshTick++
                        navController.popBackStack()
                    },
                )
            }
        }

        composable(Routes.ROOM) {
            SeasonEditionTheme(season) {
                RoomScreen(
                    container = container,
                    onBack = { navController.popBackStack() },
                    onStartPlanning = { navController.navigate(Routes.PLANNING) },
                )
            }
        }

        composable(Routes.NOTIFICATIONS) {
            SeasonEditionTheme(season) {
                NotificationsScreen(
                    container = container,
                    onStartPlanning = { navController.navigate(Routes.PLANNING) },
                    onBack = {
                        homeRefreshTick++
                        navController.popBackStack()
                    },
                )
            }
        }

        composable(
            route = Routes.PLANNING_ROUTE,
            arguments = listOf(
                navArgument("autostart") {
                    type = NavType.BoolType
                    defaultValue = true
                },
                editionArgument(),
                navArgument(QUICK_PLAN_CITY) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument(QUICK_PLAN_DAYS) {
                    type = NavType.IntType
                    defaultValue = 3
                },
            ),
        ) { entry ->
            val lockedSeason = entry.lockedSeason()
            SeasonEditionTheme(lockedSeason ?: season) {
                PlanningScreen(
                    container = container,
                    onBack = { navController.popBackStack() },
                    onOpenCandidate = { group, candidateId ->
                        navController.navigate(Routes.candidateDetail(group, candidateId, lockedSeason))
                    },
                    onSaved = { tripId ->
                        homeRefreshTick++
                        navController.navigate(Routes.tripDetail(tripId, lockedSeason)) {
                            popUpTo(Routes.PLANNING_ROUTE) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
        }

        composable(Routes.CANDIDATE_DETAIL, arguments = listOf(editionArgument())) { entry ->
            val group = entry.arguments?.getString("group").orEmpty()
            val candidateId = entry.arguments?.getString("candidateId").orEmpty()
            val viewModel: CandidateDetailViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        CandidateDetailViewModel(
                            container,
                            createSavedStateHandle(),
                            context.applicationContext,
                            group,
                            candidateId,
                        )
                    }
                },
            )
            val state by viewModel.state.collectAsStateWithLifecycle()
            val detail = state.detail
            SeasonEditionTheme(entry.lockedSeason() ?: season) {
                when {
                    detail != null ->
                        CandidateDetailScreen(detail = detail, onBack = { navController.popBackStack() })
                    state.loading ->
                        LoadingScreen(text = stringResource(R.string.home_loading))
                    else ->
                        MissingDetailScreen(onBack = { navController.popBackStack() })
                }
            }
        }

        composable(Routes.TRIP_DETAIL, arguments = listOf(editionArgument())) { entry ->
            val tripId = entry.arguments?.getString("tripId").orEmpty()
            val lockedSeason = entry.lockedSeason()
            SeasonEditionTheme(lockedSeason ?: season) {
                TripDetailScreen(
                    container = container,
                    tripId = tripId,
                    onReplanNode = { city, nodeName ->
                        PlanningPrefill.set(city, nodeName)
                        navController.navigate(Routes.seasonalPlanning(lockedSeason))
                    },
                    onOpenJourney = { navController.navigate(Routes.journey(tripId, lockedSeason)) },
                    onOpenBudget = { navController.navigate(Routes.budget(tripId, lockedSeason)) },
                    onDeleted = {
                        homeRefreshTick++
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() },
                )
            }
        }

        composable(Routes.COMMUNITY) {
            SeasonEditionTheme(season) {
                CommunityScreen(
                    container = container,
                    onUsePlan = { city, days -> navController.navigate(Routes.planningFromPost(city, days)) },
                    onBack = { navController.popBackStack() },
                )
            }
        }

        composable(Routes.GALLERY) {
            SeasonEditionTheme(season) {
                GalleryScreen(
                    container = container,
                    onBack = { navController.popBackStack() },
                )
            }
        }

        composable(Routes.JOURNEY, arguments = listOf(editionArgument())) { entry ->
            val tripId = entry.arguments?.getString("tripId").orEmpty()
            val lockedSeason = entry.lockedSeason()
            SeasonEditionTheme(lockedSeason ?: season) {
                JourneyScreen(
                    container = container,
                    tripId = tripId,
                    onBack = { navController.popBackStack() },
                )
            }
        }

        composable(Routes.BUDGET, arguments = listOf(editionArgument())) { entry ->
            val tripId = entry.arguments?.getString("tripId").orEmpty()
            val lockedSeason = entry.lockedSeason()
            SeasonEditionTheme(lockedSeason ?: season) {
                BudgetScreen(
                    container = container,
                    tripId = tripId,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }

    // NavHost 组合完成后导航图才可用，此时再执行会话失效跳转。
    LaunchedEffect(sessionExpired) {
        if (!sessionExpired) return@LaunchedEffect
        sessionExpired = false
        navController.navigate(Routes.LOGIN) {
            popUpTo(navController.graph.startDestinationId) { inclusive = true }
            launchSingleTop = true
        }
    }
}

/** 详情页依赖进程内的路书快照；进程重建后快照丢失时给出明确出口。 */
@Composable
private fun MissingDetailScreen(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.plan_detail_expired_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2B2118),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.plan_detail_expired_message),
                fontSize = 13.sp,
                color = Color(0xFF6F6256),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onBack) {
                Text(text = stringResource(R.string.plan_detail_back_to_result), color = Color(0xFFE8571F))
            }
        }
    }
}
