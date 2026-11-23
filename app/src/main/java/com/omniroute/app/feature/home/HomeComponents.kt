package com.omniroute.app.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniroute.app.R
import com.omniroute.app.core.network.dto.UserDto
import com.omniroute.app.core.ui.components.OmniLogoMark
import com.omniroute.app.core.ui.theme.JourneySeason
import com.omniroute.app.core.ui.theme.LocalAppPalette

@Composable
internal fun TopBrandBar(
    user: UserDto?,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalAppPalette.current
    val displayName = user?.displayName?.takeIf { it.isNotBlank() }
        ?: stringResource(if (loading) R.string.home_loading else R.string.home_account_title)
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OmniLogoMark(size = 32.dp)
        Text(
            text = "OmniRoute",
            color = palette.ink,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = palette.surface,
            border = BorderStroke(1.dp, palette.border),
        ) {
            Row(
                modifier = Modifier.heightIn(min = 44.dp).padding(start = 7.dp, end = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(25.dp).clip(CircleShape).background(palette.brandTint),
                    contentAlignment = Alignment.Center,
                ) {
                    if (user != null) {
                        Text(displayName.take(1).uppercase(), color = palette.brand, fontSize = 11.sp)
                    } else {
                        Icon(Icons.Filled.Person, null, tint = palette.brand, modifier = Modifier.size(14.dp))
                    }
                }
                Text(
                    text = displayName,
                    color = palette.ink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 105.dp),
                )
                Icon(Icons.Filled.KeyboardArrowDown, null, tint = palette.muted, modifier = Modifier.size(15.dp))
            }
        }
    }
}

@Composable
internal fun WelcomeHeadline(greeting: String, season: JourneySeason) {
    val palette = LocalAppPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(greeting, color = palette.muted, fontSize = 12.sp)
            Spacer(Modifier.height(2.dp))
            Text(
                stringResource(R.string.home_greeting_line),
                color = palette.ink,
                fontSize = 23.sp,
                lineHeight = 29.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Surface(shape = CircleShape, color = palette.brandTint) {
            Text(
                stringResource(R.string.home_current_season, stringResource(season.labelRes)),
                color = palette.brandDark,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
    }
}

@Composable
internal fun QuickEntriesRow(
    alerts: HomeAlerts?,
    alertsUnavailable: Boolean,
    onOpenRoom: () -> Unit,
    onOpenNotifications: () -> Unit,
) {
    val palette = LocalAppPalette.current
    val warning = alerts?.hasWarning == true
    val unread = alerts?.unreadCount ?: 0
    val calm = alerts != null && unread == 0 && !alertsUnavailable
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        QuickEntryCard(
            icon = Icons.Filled.Person,
            title = stringResource(R.string.home_room_title),
            subtitle = stringResource(R.string.home_room_sub),
            iconBackground = palette.brandTint,
            iconColor = palette.brandDark,
            onClick = onOpenRoom,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
        QuickEntryCard(
            icon = when {
                warning -> Icons.Filled.Warning
                calm -> Icons.Filled.CheckCircle
                else -> Icons.Filled.Info
            },
            title = stringResource(R.string.home_risk_title),
            subtitle = when {
                warning -> stringResource(R.string.home_risk_warning, unread)
                alertsUnavailable -> stringResource(R.string.home_risk_unavailable)
                unread > 0 -> stringResource(R.string.home_unread, unread)
                calm -> stringResource(R.string.home_no_alerts)
                else -> stringResource(R.string.home_risk_checking)
            },
            iconBackground = when {
                warning -> palette.warningBg
                calm -> palette.infoBg
                else -> palette.tagBg
            },
            iconColor = when {
                warning -> palette.warningFg
                calm -> palette.infoFg
                else -> palette.muted
            },
            onClick = onOpenNotifications,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
    }
}

@Composable
private fun QuickEntryCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconBackground: Color,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalAppPalette.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(15.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Box(
                    modifier = Modifier.size(25.dp).clip(CircleShape).background(iconBackground),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, null, tint = iconColor, modifier = Modifier.size(14.dp))
                }
                Text(title, color = palette.ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(subtitle, color = palette.muted, fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeAccountDrawer(
    state: HomeUiState,
    season: JourneySeason,
    onDismiss: () -> Unit,
    onSeasonChange: (JourneySeason) -> Unit,
    onOpenProfile: () -> Unit,
    onLogout: () -> Unit,
) {
    val palette = LocalAppPalette.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = palette.surface,
        contentColor = palette.ink,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(start = 22.dp, end = 22.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val name = state.user?.displayName?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.home_account_title)
            Text(name, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            state.user?.username?.takeIf { it.isNotBlank() }?.let {
                Text("@$it", color = palette.muted, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
            HorizontalDivider(color = palette.border)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.home_web_sync), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                val (status, color) = when {
                    state.loadingTrips || state.refreshing -> R.string.home_web_syncing to palette.muted
                    state.tripsError != null -> R.string.home_web_sync_unavailable to palette.warningFg
                    else -> R.string.home_web_synced to palette.infoFg
                }
                Text(stringResource(status), color = color, fontSize = 12.sp)
            }
            Text(
                stringResource(R.string.home_switch_season),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                JourneySeason.entries.forEach { choice ->
                    FilterChip(
                        selected = season == choice,
                        onClick = { onSeasonChange(choice) },
                        label = { Text(stringResource(R.string.home_season_choice, stringResource(choice.labelRes))) },
                    )
                }
            }
            HorizontalDivider(color = palette.border)
            TextButton(onClick = onOpenProfile) {
                Text(stringResource(R.string.home_account_profile))
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Filled.KeyboardArrowRight, null, modifier = Modifier.size(16.dp))
            }
            TextButton(onClick = onLogout, enabled = !state.loggingOut) {
                Text(
                    stringResource(if (state.loggingOut) R.string.home_logging_out else R.string.home_logout),
                    color = palette.errorFg,
                )
            }
        }
    }
}

@Composable
internal fun ModernSectionHeader(title: String, count: Int?, modifier: Modifier = Modifier) {
    val palette = LocalAppPalette.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, color = palette.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        count?.let {
            Text(stringResource(R.string.home_trip_count, it), color = palette.muted, fontSize = 12.sp)
        }
    }
}

@Composable
internal fun TripsLoadingCard() {
    val palette = LocalAppPalette.current
    Surface(
        shape = RoundedCornerShape(15.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(Modifier.size(16.dp), color = palette.brand, strokeWidth = 2.dp)
            Text(stringResource(R.string.home_syncing), fontSize = 13.sp, color = palette.muted)
        }
    }
}

@Composable
internal fun TripsErrorCard(errorText: String, onRetry: () -> Unit) {
    val palette = LocalAppPalette.current
    Surface(
        shape = RoundedCornerShape(15.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(errorText, color = palette.errorFg, fontSize = 12.sp, modifier = Modifier.weight(1f))
            TextButton(onClick = onRetry) { Text(stringResource(R.string.home_retry)) }
        }
    }
}
