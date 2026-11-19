package com.omniroute.app.feature.home

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniroute.app.R
import com.omniroute.app.core.network.dto.TripPlanDto
import com.omniroute.app.core.network.dto.UserDto
import com.omniroute.app.core.ui.components.OmniLogoMark
import com.omniroute.app.core.ui.theme.LocalAppPalette
import com.omniroute.app.feature.auth.BrandOrange
import com.omniroute.app.feature.auth.CardBorder
import com.omniroute.app.feature.auth.ErrorFg
import com.omniroute.app.feature.auth.Ink
import com.omniroute.app.feature.auth.Muted

// 统一柔和的现代配色体系（行程卡片拆分到 HomeTripComponents 后仍共用）
internal val PureWhite: Color
    @Composable get() = LocalAppPalette.current.surface
internal val WarmCanvas: Color
    @Composable get() = LocalAppPalette.current.brandTint
internal val SoftTagBg: Color
    @Composable get() = LocalAppPalette.current.tagBg
internal val AccentOrangeBg: Color
    @Composable get() = LocalAppPalette.current.brandTint
internal val SubtleLine: Color
    @Composable get() = LocalAppPalette.current.divider

/** 顶部品牌栏：左侧新 Logo + 品牌名，右侧精致的用户胶囊 */
@Composable
internal fun TopBrandBar(
    user: UserDto?,
    loading: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OmniLogoMark(size = 40.dp)
            Spacer(modifier = Modifier.width(11.dp))
            Column {
                Text(
                    text = "OmniRoute",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Ink,
                )
                Text(
                    text = stringResource(R.string.home_engine_subtitle),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Muted,
                )
            }
        }

        // 右上角用户状态胶囊：点击进入「我的 / 设置」
        Surface(
            shape = RoundedCornerShape(50),
            color = PureWhite,
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(role = Role.Button, onClick = onClick),
        ) {
            Row(
                modifier = Modifier.padding(start = 5.dp, end = 11.dp, top = 5.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(AccentOrangeBg),
                    contentAlignment = Alignment.Center,
                ) {
                    val initial = user?.displayName?.trim()?.firstOrNull()?.uppercase() ?: "U"
                    Text(
                        text = initial,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandOrange,
                    )
                }
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = user?.displayName?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.home_loading),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink,
                    maxLines = 1,
                )
                if (loading) {
                    Spacer(modifier = Modifier.width(6.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(10.dp),
                        strokeWidth = 1.5.dp,
                        color = BrandOrange,
                    )
                } else {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2E9E6B)),
                    )
                }
            }
        }
    }
}

/** 问候标题区：现代无衬线字体，克制、自然 */
@Composable
internal fun WelcomeHeadline(
    greeting: String,
    user: UserDto?,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.home_greeting_line, greeting),
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Ink,
        )
        val signature = user?.signature?.takeIf { it.isNotBlank() }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = signature ?: stringResource(R.string.home_signature_default),
            fontSize = 13.sp,
            color = Muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 首页快捷入口：协作房间与风险通知，两列等宽卡片。 */
@Composable
internal fun QuickEntriesRow(
    unreadCount: Int,
    onOpenRoom: () -> Unit,
    onOpenNotifications: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        QuickEntryCard(
            icon = Icons.Filled.Person,
            title = stringResource(R.string.home_room_title),
            subtitle = stringResource(R.string.home_room_sub),
            badge = 0,
            onClick = onOpenRoom,
            modifier = Modifier.weight(1f),
        )
        QuickEntryCard(
            icon = Icons.Filled.Warning,
            title = stringResource(R.string.home_risk_title),
            subtitle = if (unreadCount > 0) {
                stringResource(R.string.home_unread, unreadCount)
            } else {
                stringResource(R.string.home_no_alerts)
            },
            badge = unreadCount,
            onClick = onOpenNotifications,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun QuickEntryCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, CardBorder),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (badge > 0) AccentOrangeBg else SoftTagBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (badge > 0) BrandOrange else Muted,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(modifier = Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = if (badge > 0) BrandOrange else Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** 现代简洁的模块标题栏 */
@Composable
internal fun ModernSectionHeader(
    title: String,
    count: Int? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Ink,
        )
        if (count != null) {
            Surface(
                shape = RoundedCornerShape(50),
                color = SoftTagBg,
            ) {
                Text(
                    text = stringResource(R.string.home_trip_count, count),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Muted,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                )
            }
        }
    }
}

@Composable
internal fun TripsLoadingCard() {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, CardBorder),
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
                color = BrandOrange,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.home_syncing),
                fontSize = 13.sp,
                color = Muted,
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
        color = PureWhite,
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = errorText,
                fontSize = 12.5.sp,
                color = ErrorFg,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.home_retry),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = BrandOrange,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clickable(role = Role.Button, onClick = onRetry)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

/** 底部一体式账号与退出登录卡片：清爽克制，不再拆成冗长的表格 */
@Composable
internal fun CleanAccountFooter(
    user: UserDto?,
    loggingOut: Boolean,
    onLogout: () -> Unit,
) {
    val username = user?.username?.takeIf { it.isNotBlank() } ?: "—"
    val createdAt = user?.createdAt?.take(10)?.takeIf { it.isNotBlank() } ?: "—"

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.home_account, username),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink,
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.home_registered, createdAt),
                    fontSize = 12.sp,
                    color = Muted,
                )
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SoftTagBg,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(enabled = !loggingOut, role = Role.Button, onClick = onLogout),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (loggingOut) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.8.dp,
                            color = Ink,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.home_logging_out),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Muted,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.home_logout),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Ink,
                        )
                    }
                }
            }
        }
    }
}
