package com.omniroute.app.feature.trip

import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.remember
import com.omniroute.app.core.ui.components.DestinationCover
import com.omniroute.app.core.ui.theme.editionRim
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontFamily
import com.omniroute.app.core.network.dto.BudgetSummaryDto
import com.omniroute.app.core.network.dto.ExecutionSummaryDto
import androidx.compose.material3.LinearProgressIndicator
import java.text.NumberFormat
import java.util.Currency
import com.omniroute.app.core.ui.components.SeasonArtwork
import com.omniroute.app.core.ui.theme.EditionSurface
import com.omniroute.app.core.ui.theme.LocalJourneySeason
import com.omniroute.app.core.ui.theme.edition
import com.omniroute.app.core.ui.theme.OmniSeasonLogo
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.style.TextOverflow
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
    val compact = LocalConfiguration.current.screenWidthDp < 395 || LocalDensity.current.fontScale > 1.15f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
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
                    fontSize = 19.sp,
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
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (compact) {
            var open by remember { mutableStateOf(false) }
            Box {
                TopBarAction(Icons.Filled.MoreVert, "更多行程操作", actionsEnabled) { open = true }
                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    listOf(R.string.trip_share to onShare, R.string.trip_rename_desc to onEdit,
                        R.string.trip_delete_desc to onDelete).forEach { (label, action) ->
                        DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = { open = false; action() })
                    }
                }
            }
        } else Row(verticalAlignment = Alignment.CenterVertically) {
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
    journeyProgress: ExecutionSummaryDto?,
    budgetProgress: BudgetSummaryDto?,
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

    val execution = journeyProgress?.takeIf { it.total > 0 }
    val ledger = budgetProgress?.takeIf { it.totalBudgetCents > 0 }
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        TripToolTile(
            icon = Icons.Filled.List,
            title = R.string.journey_open,
            subtitle = execution?.let { "已打卡 ${it.completed} / ${it.total} 站" }
                ?: stringResource(R.string.journey_sub),
            progress = execution?.let { it.completed.toFloat() / it.total },
            onClick = onOpenJourney,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
        TripToolTile(
            icon = Icons.Filled.ShoppingCart,
            title = R.string.label_budget,
            subtitle = ledger?.let { "${ticketMoney(it.spentCents, it.currency)} / ${ticketMoney(it.totalBudgetCents, it.currency)}" }
                ?: if (budgetProgress != null) "尚未设定总预算" else "查看旅行账本",
            progress = ledger?.ratio?.toFloat(),
            onClick = onOpenBudget,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
    }

    Spacer(modifier = Modifier.height(24.dp))

    // 3. 日程概览标题栏
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
private fun TripToolTile(
    icon: ImageVector,
    title: Int,
    subtitle: String,
    progress: Float?,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val palette = LocalAppPalette.current
    val style = LocalJourneySeason.current.edition
    val journey = title == R.string.journey_open
    val tint = if (journey) style.glow else style.secondary
    val shape = RoundedCornerShape(18.dp)
    Surface(onClick = onClick, modifier = modifier.editionRim(shape), shape = shape, color = EditionSurface) {
        Column(Modifier.background(Brush.linearGradient(listOf(tint.copy(alpha = 0.26f), Color.Transparent)))
            .padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = palette.brand, modifier = Modifier.size(20.dp))
                val fraction = progress?.takeIf(Float::isFinite)?.coerceIn(0f, 1f)
                if (journey && fraction != null) {
                    Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxSize(),
                            strokeWidth = 3.dp, color = palette.brand, trackColor = palette.brandTint)
                        Text("${(fraction * 100).toInt()}%", fontSize = 9.sp, color = palette.brand)
                    }
                } else {
                    Icon(Icons.Filled.ArrowForward, null, tint = palette.muted, modifier = Modifier.size(15.dp))
                }
            }
            Text(stringResource(title), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Text(subtitle, fontSize = 12.sp, lineHeight = 17.sp, color = Muted)
            if (!journey) {
                LinearProgressIndicator(progress = { progress?.takeIf(Float::isFinite)?.coerceIn(0f, 1f) ?: 0f },
                    modifier = Modifier.fillMaxWidth().padding(top = 3.dp).height(6.dp).clip(CircleShape),
                    color = palette.brand, trackColor = palette.ink.copy(alpha = 0.09f))
            }
        }
    }
}

private fun ticketMoney(cents: Long, code: String?): String {
    val formatter = NumberFormat.getCurrencyInstance()
    formatter.currency = runCatching { Currency.getInstance(code ?: "CNY") }.getOrDefault(Currency.getInstance("CNY"))
    formatter.maximumFractionDigits = if (cents % 100L == 0L) 0 else 2
    return formatter.format(cents / 100.0)
}

@Composable
private fun EditorialHeroCard(trip: TripPlanDto, summary: String?) {
    val context = LocalContext.current
    val style = LocalJourneySeason.current.edition
    var showSummary by rememberSaveable(trip.id) { mutableStateOf(false) }

    val shape = RoundedCornerShape(24.dp)
    DestinationCover(trip.destCity, trip.content.orEmpty().contains("天池"), Modifier.fillMaxWidth().editionRim(shape).clip(shape)) {
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(
            0f to Color.Transparent, 0.30f to Color.Transparent, 0.75f to Color(0x75151D29), 1f to Color(0xD4181D2B))))
        Column(Modifier.fillMaxWidth().heightIn(min = 210.dp).padding(18.dp), verticalArrangement = Arrangement.Bottom) {
            Text(tripTitle(context, trip), color = Color.White, fontSize = 27.sp, lineHeight = 34.sp,
                fontFamily = style.titleFont, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val date = trip.createdAt?.take(10).orEmpty()
                val tags = listOfNotNull(
                    if (trip.days > 0) stringResource(R.string.trip_meta_days_value, trip.days) else null,
                    if (trip.travelers > 0) stringResource(R.string.trip_meta_travelers_value, trip.travelers) else null,
                    if (date.length == 10) "${date.replace('-', '.')} 归档" else null)
                tags.forEach { Text(it, color = Color.White, fontSize = 11.sp, modifier = Modifier.clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.19f)).padding(horizontal = 9.dp, vertical = 4.dp)) }
            }
        }
    }
    if (!summary.isNullOrBlank()) {
        Spacer(Modifier.height(10.dp))
        val note = RoundedCornerShape(16.dp)
        Surface(onClick = { showSummary = !showSummary }, modifier = Modifier.fillMaxWidth().editionRim(note),
            shape = note, color = EditionSurface) {
            Column(Modifier.padding(horizontal = 15.dp, vertical = 11.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("路书导语", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(if (showSummary) "收起  −" else "展开  +", color = BrandOrange, fontSize = 12.sp)
                }
                Text(summary, color = Muted, lineHeight = 19.sp, fontSize = 12.sp,
                    maxLines = if (showSummary) Int.MAX_VALUE else 1, overflow = TextOverflow.Ellipsis)
            }
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
