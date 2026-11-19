package com.omniroute.app.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniroute.app.R
import com.omniroute.app.core.network.dto.UserDto
import com.omniroute.app.core.ui.components.OmniLogoMark
import com.omniroute.app.core.ui.components.RemoteAvatar
import com.omniroute.app.core.ui.theme.LocalAppPalette

@Composable
internal fun TopBrandBar(user: UserDto?, onClick: () -> Unit) {
    val palette = LocalAppPalette.current
    val account = stringResource(R.string.home_profile)
    val displayName = user?.displayName?.trim().orEmpty().ifEmpty { account }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OmniLogoMark(size = 32.dp)
            Text(
                text = "OmniRoute",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = palette.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .clickable(role = Role.Button, onClickLabel = account, onClick = onClick)
                .padding(horizontal = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        ) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.labelLarge,
                color = palette.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            RemoteAvatar(
                path = user?.avatarUrl,
                fallback = displayName.take(1),
                size = 32.dp,
                background = palette.brandTint,
                foreground = palette.brandDark,
                fontSize = 13.sp,
                contentDescription = null,
            )
        }
    }
}

@Composable
internal fun QuickEntriesRow(
    unreadCount: Int,
    onOpenRoom: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenCommunity: () -> Unit,
) {
    val palette = LocalAppPalette.current
    Column {
        HorizontalDivider(color = palette.border)
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
            HomeShortcut(
                Icons.Outlined.Person,
                stringResource(R.string.home_room_title),
                onOpenRoom,
                Modifier.weight(1f),
            )
            HomeShortcut(
                Icons.Outlined.Share,
                stringResource(R.string.home_community),
                onOpenCommunity,
                Modifier.weight(1f),
            )
            HomeShortcut(
                Icons.Outlined.Notifications,
                stringResource(R.string.home_risk_title),
                onOpenNotifications,
                Modifier.weight(1f),
                unreadCount = unreadCount,
            )
        }
        HorizontalDivider(color = palette.border)
    }
}

@Composable
private fun HomeShortcut(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier,
    unreadCount: Int = 0,
) {
    val palette = LocalAppPalette.current
    Column(
        modifier = modifier
            .heightIn(min = 64.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
    ) {
        BadgedBox(
            badge = {
                if (unreadCount > 0) {
                    val unreadLabel = stringResource(R.string.home_unread, unreadCount)
                    Badge(
                        containerColor = palette.brandDark,
                        contentColor = palette.onBrand,
                        modifier = Modifier.semantics { contentDescription = unreadLabel },
                    ) {
                        Text(if (unreadCount > 99) "99+" else unreadCount.toString())
                    }
                }
            },
        ) {
            Icon(icon, contentDescription = null, tint = palette.brandDark, modifier = Modifier.size(21.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = palette.ink,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun TripsSectionHeader(count: Int?) {
    val palette = LocalAppPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.home_my_trips),
            style = MaterialTheme.typography.titleLarge,
            color = palette.ink,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        if (count != null && count > 0) {
            Text(
                text = stringResource(R.string.home_trip_count, count),
                style = MaterialTheme.typography.bodySmall,
                color = palette.muted,
            )
        }
    }
}

@Composable
internal fun TripsLoadingCard() {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = LocalAppPalette.current.surface,
        border = BorderStroke(1.dp, LocalAppPalette.current.border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = LocalAppPalette.current.brandDark,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.home_syncing),
                fontSize = 13.sp,
                color = LocalAppPalette.current.muted,
            )
        }
    }
}

@Composable
internal fun TripsErrorCard(
    errorText: String,
    onRetry: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = LocalAppPalette.current.surface,
        border = BorderStroke(1.dp, LocalAppPalette.current.border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = errorText,
                fontSize = 12.5.sp,
                color = LocalAppPalette.current.errorFg,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.home_retry),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = LocalAppPalette.current.brandDark,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clickable(role = Role.Button, onClick = onRetry)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
internal fun CleanAccountFooter(loggingOut: Boolean, onLogout: () -> Unit) {
    val palette = LocalAppPalette.current
    Box(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        TextButton(enabled = !loggingOut, onClick = onLogout) {
            Text(
                text = stringResource(if (loggingOut) R.string.home_logging_out else R.string.home_logout),
                color = palette.muted,
            )
        }
    }
}
