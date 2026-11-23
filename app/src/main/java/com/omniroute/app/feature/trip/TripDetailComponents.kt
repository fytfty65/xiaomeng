package com.omniroute.app.feature.trip

import com.omniroute.app.core.ui.theme.FolioSerif
import com.omniroute.app.core.ui.theme.FolioDigits
import androidx.compose.foundation.layout.*
import com.omniroute.app.core.ui.components.DestinationCover
import androidx.compose.foundation.layout.heightIn
import com.omniroute.app.core.network.dto.BudgetSummaryDto
import com.omniroute.app.core.network.dto.ExecutionSummaryDto
import androidx.compose.material3.LinearProgressIndicator
import java.text.NumberFormat
import java.util.Currency
import com.omniroute.app.core.ui.theme.LocalJourneySeason
import com.omniroute.app.core.ui.theme.edition
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniroute.app.R
import com.omniroute.app.core.network.dto.TripPlanDto
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
    SummaryNote(trip, summary)

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
    Row(modifier.heightIn(min = 68.dp).clickable(role = Role.Button, onClick = onClick)
        .padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        val fraction = progress?.takeIf(Float::isFinite)?.coerceIn(0f, 1f)
        Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
            if (journey && fraction != null) CircularProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxSize(),
                strokeWidth = 2.dp, color = palette.brand, trackColor = palette.divider)
            Icon(icon, null, tint = palette.brand, modifier = Modifier.size(17.dp))
        }
        Column(Modifier.padding(start = 9.dp)) {
            Text(stringResource(title), fontFamily = FolioSerif, fontSize = 16.sp, color = Ink)
            Text(subtitle, fontSize = 12.sp, color = Muted, lineHeight = 17.sp)
            if (!journey && fraction != null) LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth().height(2.dp))
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
internal fun GuideCover(trip: TripPlanDto, toolbar: @Composable () -> Unit) {
    val context = LocalContext.current
    DestinationCover(trip.destCity, trip.content.orEmpty().contains("天池"), Modifier.fillMaxWidth(),
        creditAlignment = Alignment.BottomEnd) {
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(0f to Color(0x420B1116), 0.34f to Color.Transparent,
            0.73f to Color(0x52121B20), 1f to Color(0xD8111C21))))
        Column(Modifier.fillMaxWidth().heightIn(min = 275.dp).padding(start = 22.dp, end = 44.dp, bottom = 19.dp, top = 85.dp),
            verticalArrangement = Arrangement.Bottom) {
            Text("OMNIROUTE   /   TRAVEL GUIDE", color = Color.White, fontFamily = FolioDigits, fontSize = 13.sp, letterSpacing = 2.sp)
            Text(tripTitle(context, trip), color = Color.White, fontSize = 32.sp, lineHeight = 43.sp, fontFamily = FolioSerif)
            val meta = listOfNotNull(trip.days.takeIf { it > 0 }?.let { "$it DAYS" },
                trip.travelers.takeIf { it > 0 }?.let { "$it GUESTS" },
                trip.createdAt?.take(10)?.takeIf { it.length == 10 }?.let { "归档  ${it.replace('-', '.')}" })
            Text(meta.joinToString("   ·   "), color = Color.White, fontFamily = FolioDigits, fontSize = 14.sp, letterSpacing = 0.8.sp)
        }
        toolbar()
    }
}

@Composable
private fun SummaryNote(trip: TripPlanDto, summary: String?) {
    var showSummary by rememberSaveable(trip.id) { mutableStateOf(false) }
    if (!summary.isNullOrBlank()) {
        Spacer(Modifier.height(10.dp))
        val note = RoundedCornerShape(0.dp)
        Surface(onClick = { showSummary = !showSummary }, modifier = Modifier.fillMaxWidth(),
            shape = note, color = Color.Transparent) {
            Column(Modifier.padding(vertical = 4.dp)) {
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
