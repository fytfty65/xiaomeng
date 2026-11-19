package com.omniroute.app.feature.trip

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniroute.app.R
import com.omniroute.app.core.network.dto.TripPlanDto
import com.omniroute.app.core.ui.components.CircleIconButton
import com.omniroute.app.core.ui.components.OmniLogoMark
import com.omniroute.app.core.ui.theme.LocalAppPalette
import com.omniroute.app.feature.auth.BrandOrange
import com.omniroute.app.feature.auth.ErrorBg
import com.omniroute.app.feature.auth.ErrorFg
import com.omniroute.app.feature.auth.InfoBg
import com.omniroute.app.feature.auth.InfoFg
import com.omniroute.app.feature.auth.Ink
import com.omniroute.app.feature.auth.InlineBanner
import com.omniroute.app.feature.auth.Muted

// 质感配色系统
internal val PageBg: Color
    @Composable get() = LocalAppPalette.current.background
private val CardBg: Color
    @Composable get() = LocalAppPalette.current.surface
private val CardLineDivider: Color
    @Composable get() = LocalAppPalette.current.divider
private val SubtleBorder: Color
    @Composable get() = LocalAppPalette.current.border
private val TagBg: Color
    @Composable get() = LocalAppPalette.current.tagBg
private val OrangeLightBg: Color
    @Composable get() = LocalAppPalette.current.brandTint
private val VerifiedGreen: Color
    @Composable get() = LocalAppPalette.current.verifiedFg
private val VerifiedGreenBg: Color
    @Composable get() = LocalAppPalette.current.verifiedBg

/** 顶部品牌导航栏 */
@Composable
internal fun TripTopBar(
    cityName: String?,
    actionsEnabled: Boolean,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircleIconButton(
                icon = Icons.Filled.ArrowBack,
                contentDescription = stringResource(R.string.common_back),
                onClick = onBack,
                background = CardBg,
                borderColor = SubtleBorder,
                tint = Ink,
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = stringResource(R.string.trip_title),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                )
                Text(
                    text = if (!cityName.isNullOrBlank()) {
                        stringResource(R.string.trip_subtitle, cityName)
                    } else {
                        stringResource(R.string.trip_subtitle_default)
                    },
                    fontSize = 12.sp,
                    color = Muted,
                )
            }
        }

        // 行程分享 / 编辑入口 + 统一品牌矢量 Logo
        Row(verticalAlignment = Alignment.CenterVertically) {
            TopBarAction(
                icon = Icons.Filled.Share,
                description = stringResource(R.string.trip_share),
                enabled = actionsEnabled,
                onClick = onShare,
            )
            Spacer(modifier = Modifier.width(8.dp))
            TopBarAction(
                icon = Icons.Filled.Edit,
                description = stringResource(R.string.trip_rename_desc),
                enabled = actionsEnabled,
                onClick = onEdit,
            )
            Spacer(modifier = Modifier.width(8.dp))
            TopBarAction(
                icon = Icons.Filled.Delete,
                description = stringResource(R.string.trip_delete_desc),
                enabled = actionsEnabled,
                onClick = onDelete,
            )
            Spacer(modifier = Modifier.width(10.dp))
            OmniLogoMark(size = 30.dp)
        }
    }
}

@Composable
private fun TopBarAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    CircleIconButton(
        icon = icon,
        contentDescription = description,
        onClick = onClick,
        enabled = enabled,
        visualSize = 34.dp,
        iconSize = 16.dp,
        background = CardBg,
        borderColor = SubtleBorder,
        tint = if (enabled) Ink else Muted.copy(alpha = 0.45f),
    )
}

/** 详情页头部：主看板 + 日程概览标题；日程本体交给 LazyColumn 逐项组合。 */
@Composable
internal fun TripDetailHeader(
    trip: TripPlanDto,
    summary: String?,
    hasContent: Boolean,
    daysCount: Int,
    onEditRoute: () -> Unit,
    onOpenJourney: () -> Unit,
    onOpenBudget: () -> Unit,
) {
    // 1. 路书主看板卡片
    EditorialHeroCard(
        trip = trip,
        summary = summary,
    )

    // 2. 空日程校验与提示
    if (!hasContent) {
        Spacer(modifier = Modifier.height(14.dp))
        InlineBanner(
            text = stringResource(R.string.trip_empty_itinerary),
            bg = InfoBg,
            fg = InfoFg,
            icon = Icons.Filled.Info,
        )
        return
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 旅途助手入口：执行进度、风险调整与行前清单集中在这里，避免详情页继续堆操作。
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardBg,
        border = BorderStroke(1.dp, SubtleBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onOpenJourney),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(OrangeLightBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.List,
                    contentDescription = null,
                    tint = BrandOrange,
                    modifier = Modifier.size(17.dp),
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.journey_open),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                )
                Text(
                    text = stringResource(R.string.journey_sub),
                    fontSize = 11.sp,
                    color = Muted,
                )
            }
            Icon(
                imageVector = Icons.Filled.ArrowForward,
                contentDescription = null,
                tint = Muted,
                modifier = Modifier.size(16.dp),
            )
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 预算管家入口：旅中记账与超支预警独立成页，详情页只保留入口。
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardBg,
        border = BorderStroke(1.dp, SubtleBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onOpenBudget),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(OrangeLightBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.ShoppingCart,
                    contentDescription = null,
                    tint = BrandOrange,
                    modifier = Modifier.size(17.dp),
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.label_budget),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                )
                Text(
                    text = stringResource(R.string.budget_entry_sub),
                    fontSize = 11.sp,
                    color = Muted,
                )
            }
            Icon(
                imageVector = Icons.Filled.ArrowForward,
                contentDescription = null,
                tint = Muted,
                modifier = Modifier.size(16.dp),
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // 3. 日程概览标题栏
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(4.dp, 14.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(BrandOrange),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.trip_daily_schedule),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Ink,
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = onEditRoute,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null,
                    tint = BrandOrange,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.trip_edit_route),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandOrange,
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = OrangeLightBg,
            ) {
                Text(
                    text = stringResource(R.string.trip_days_count, daysCount),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandOrange,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))
}

/** 结构化主抬头卡片 */
@Composable
private fun EditorialHeroCard(trip: TripPlanDto, summary: String?) {
    val context = LocalContext.current
    val city = trip.destCity?.trim().orEmpty()

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = CardBg,
        border = BorderStroke(1.dp, SubtleBorder),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.02f),
                spotColor = BrandOrange.copy(alpha = 0.08f),
            ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // 顶部状态条：目的地胶囊 + 同步状态
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (city.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = OrangeLightBg,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = BrandOrange,
                                modifier = Modifier.size(12.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = city,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandOrange,
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = VerifiedGreenBg,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(VerifiedGreen),
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = stringResource(R.string.trip_synced),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VerifiedGreen,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 行程大标题
            Text(
                text = tripTitle(context, trip),
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 30.sp,
                color = Ink,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 核心属性微标行
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (trip.days > 0) {
                    PropertyTag(
                        label = stringResource(R.string.trip_meta_days),
                        value = stringResource(R.string.trip_meta_days_value, trip.days),
                    )
                }
                if (trip.travelers > 0) {
                    PropertyTag(
                        label = stringResource(R.string.trip_meta_travelers),
                        value = stringResource(R.string.trip_meta_travelers_value, trip.travelers),
                    )
                }
                val date = trip.createdAt?.take(10).orEmpty()
                if (date.length == 10) {
                    PropertyTag(
                        label = stringResource(R.string.trip_meta_archived),
                        value = date.replace("-", "."),
                    )
                }
            }

            // 摘要或路书导语
            if (!summary.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = CardLineDivider, thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(TagBg)
                        .padding(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(BrandOrange),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = summary,
                        fontSize = 12.5.sp,
                        lineHeight = 19.sp,
                        color = Ink.copy(alpha = 0.85f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PropertyTag(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = TagBg,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$label ",
                fontSize = 12.sp,
                color = Muted,
            )
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
            )
        }
    }
}

/** 加载态 */
@Composable
internal fun ShimmerLoadingView() {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = CardBg,
        border = BorderStroke(1.dp, SubtleBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.4.dp,
                color = BrandOrange,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.trip_syncing_detail),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Muted,
            )
        }
    }
}

/** 错误态 */
@Composable
internal fun RefinedErrorBlock(message: String, onRetry: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = CardBg,
        border = BorderStroke(1.dp, SubtleBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            InlineBanner(text = message, bg = ErrorBg, fg = ErrorFg, icon = Icons.Filled.Warning)
            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = OrangeLightBg,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button, onClick = onRetry),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        tint = BrandOrange,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.trip_retry_sync),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandOrange,
                    )
                }
            }
        }
    }
}

