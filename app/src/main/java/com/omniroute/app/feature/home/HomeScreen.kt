package com.omniroute.app.feature.home

import android.animation.ValueAnimator
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.omniroute.app.feature.auth.PortalScenery
import com.omniroute.app.feature.auth.stepBy
import com.omniroute.app.core.ui.theme.SeasonEditionTheme
import kotlinx.coroutines.delay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.omniroute.app.AppContainer
import com.omniroute.app.R
import com.omniroute.app.core.network.dto.UserDto
import com.omniroute.app.core.ui.ContentMaxWidth
import com.omniroute.app.core.ui.theme.JourneySeason
import com.omniroute.app.core.ui.theme.LocalAppPalette
import com.omniroute.app.core.ui.theme.LocalJourneySeason
import com.omniroute.app.core.ui.wheelScroll
import com.omniroute.app.feature.auth.InlineBanner
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    container: AppContainer,
    initialUser: UserDto?,
    refreshTick: Int,
    onStartPlanning: () -> Unit,
    onQuickPlan: (String, Int) -> Unit,
    onSeasonChange: (JourneySeason) -> Unit,
    onOpenSeason: () -> Unit,
    onOpenTrip: (String) -> Unit,
    onOpenRoom: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit,
    onLoggedOut: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel(
        factory = viewModelFactory {
            initializer { HomeViewModel(container, context.applicationContext, initialUser) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val preference = LocalJourneySeason.current
    var season by rememberSaveable(preference) { mutableStateOf(preference) }
    var playing by rememberSaveable { mutableStateOf(true) }
    val listState = rememberLazyListState()
    var accountOpen by rememberSaveable { mutableStateOf(false) }
    val tripsError = state.tripsError

    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val moving = lifecycle == Lifecycle.State.RESUMED && ValueAnimator.areAnimatorsEnabled()
    val keyboard = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    LaunchedEffect(season, playing, moving, keyboard, accountOpen, listState.isScrollInProgress) {
        if (playing && moving && !keyboard && !accountOpen && !listState.isScrollInProgress && listState.firstVisibleItemIndex < 2) {
            delay(8000)
            season = season.stepBy(1)
        }
    }
    fun choose(choice: JourneySeason) {
        playing = false
        season = choice
        onSeasonChange(choice)
    }
    fun enter(action: () -> Unit) {
        onSeasonChange(season)
        action()
    }

    LaunchedEffect(refreshTick) { viewModel.onRefreshTick(refreshTick) }
    LaunchedEffect(viewModel) { viewModel.events.collect { onLoggedOut() } }

    val greeting = stringResource(
        when (LocalTime.now().hour) {
            in 0..5 -> R.string.home_greeting_night
            in 6..10 -> R.string.home_greeting_morning
            in 11..13 -> R.string.home_greeting_noon
            in 14..17 -> R.string.home_greeting_afternoon
            else -> R.string.home_greeting_evening
        },
    )

    SeasonEditionTheme(season, scenery = { PortalScenery(season, moving) }) {
        val palette = LocalAppPalette.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                TopBrandBar(
                    user = state.user,
                    loading = state.loadingProfile,
                    onClick = { accountOpen = true },
                    modifier = Modifier.widthIn(max = ContentMaxWidth).padding(horizontal = 18.dp),
                )
            }

            PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.widthIn(max = ContentMaxWidth).fillMaxSize(),
            ) {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize().wheelScroll(listState),
                ) {
                    item(key = "welcome") { WelcomeHeadline(listOfNotNull(greeting, state.user?.displayName?.takeIf { it.isNotBlank() }).joinToString("，")) }

                    state.profileError?.let { error ->
                        item(key = "profile_error") {
                            Column {
                                InlineBanner(
                                    text = error,
                                    bg = palette.errorBg,
                                    fg = palette.errorFg,
                                    icon = Icons.Filled.Warning,
                                )
                                TextButton(onClick = viewModel::loadProfile) {
                                    Text(stringResource(R.string.home_reload_profile))
                                }
                            }
                        }
                    }

                    item(key = "planner") {
                        RoutePlannerHeroCard(
                            season = season,
                            onSeasonChange = ::choose,
                            onOpenSeason = { enter(onOpenSeason) },
                            onQuickPlan = { city, days -> enter { onQuickPlan(city, days) } },
                            onOpenPlanner = { enter(onStartPlanning) },
                            onInteract = { playing = false },
                        )
                    }

                    item(key = "quick_entries") {
                        QuickEntriesRow(
                            alerts = state.alerts,
                            alertsUnavailable = state.alertsUnavailable,
                            onOpenRoom = { enter(onOpenRoom) },
                            onOpenNotifications = { enter(onOpenNotifications) },
                        )
                    }

                    item(key = "trips_header") {
                        ModernSectionHeader(
                            title = stringResource(R.string.home_my_trips),
                            count = state.trips.size.takeIf { !state.loadingTrips && tripsError == null },
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }

                    when {
                        state.loadingTrips && state.trips.isEmpty() ->
                            item(key = "trips_loading") { TripsLoadingCard() }
                        tripsError != null -> item(key = "trips_error") {
                            TripsErrorCard(errorText = tripsError, onRetry = viewModel::loadTrips)
                        }
                        state.trips.isEmpty() -> item(key = "trips_empty") {
                            ModernEmptyTripsCard(onStartPlanning = { enter(onStartPlanning) })
                        }
                    }

                    items(state.trips, key = { it.id ?: it.hashCode() }) { trip ->
                        ModernTripCard(trip = trip, onClick = { enter { onOpenTrip(trip.id.orEmpty()) } })
                    }
                }
            }
        }

        if (accountOpen) {
            HomeAccountDrawer(
                state = state,
                season = season,
                onDismiss = { accountOpen = false },
                onSeasonChange = ::choose,
                onOpenProfile = {
                    accountOpen = false
                    enter(onOpenProfile)
                },
                onLogout = viewModel::logout,
            )
        }
    }
}
