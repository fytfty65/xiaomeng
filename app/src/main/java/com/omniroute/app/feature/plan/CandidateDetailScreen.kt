package com.omniroute.app.feature.plan

import androidx.compose.material3.minimumInteractiveComponentSize

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.omniroute.app.feature.auth.BrandOrange
import com.omniroute.app.feature.auth.BrandOrangeLight
import com.omniroute.app.feature.auth.CardBorder
import com.omniroute.app.feature.auth.InfoBg
import com.omniroute.app.feature.auth.InfoFg
import com.omniroute.app.feature.auth.Ink
import com.omniroute.app.feature.auth.InlineBanner
import com.omniroute.app.feature.auth.Muted
import java.util.Locale
import com.omniroute.app.core.ui.components.CircleIconButton
import com.omniroute.app.core.ui.components.OmniLogoMark
import com.omniroute.app.core.ui.theme.LocalAppPalette
import com.omniroute.app.core.ui.wheelScroll

// 质感配色体系：统一现代米白与克制基准色
private val PageBg: Color
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
private val WarningAmber: Color
    @Composable get() = LocalAppPalette.current.warningFg
private val WarningAmberBg: Color
    @Composable get() = LocalAppPalette.current.warningBg

/**
 * 候选方案详情页：结构化解析单项方案的推荐逻辑、指标事实、履约约束与官方数据源
 */
@Composable
fun CandidateDetailScreen(
    detail: CandidateDetailData,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val bookingUrl = detail.bookingUrl
    val disclaimer = detail.disclaimer
    val detailScrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            // 1. 顶部全局导航栏（集成星轨 Logo 与分类来源）
            DetailTopBar(
                groupTitle = detail.groupTitle,
                onBack = onBack,
            )

            // 2. 主体可滚动信息流
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(detailScrollState)
                    .wheelScroll(detailScrollState)
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // 核心概览卡片：名次、得分、全名、标签
                HeroCandidateCard(
                    detail = detail,
                    groupTitle = detail.groupTitle,
                    rank = detail.rank,
                )

                // 价格评估面板（如果包含价格参考）
                val priceText = buildPriceText(detail)
                if (priceText != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    PriceMetricCard(
                        detail = detail,
                        priceText = priceText,
                    )
                }

                // 核心指标与事实参数
                val facts = buildFacts(context, detail)
                if (facts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    FactsMetricCard(facts = facts)
                }

                // 推荐理由列表
                val reasons = detail.reasons
                if (reasons.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    StructuredListCard(
                        title = stringResource(R.string.plan_detail_reasons_title),
                        badge = stringResource(R.string.plan_detail_reasons_badge, reasons.size),
                        items = reasons,
                        isWarning = false,
                    )
                }

                // 约束与出行提示
                val constraints = detail.constraints
                if (constraints.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    StructuredListCard(
                        title = stringResource(R.string.plan_detail_constraints_title),
                        badge = stringResource(R.string.plan_detail_constraints_badge),
                        items = constraints,
                        isWarning = true,
                    )
                }

                // 数据源与溯源证据
                if (detail.sourceProvider != null || detail.sourceRetrievedAt != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    SourceAuditCard(
                        provider = detail.sourceProvider,
                        retrievedAt = detail.sourceRetrievedAt,
                        estimated = detail.estimated,
                    )
                }

                // 提示免责申明
                if (disclaimer != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    InlineBanner(
                        text = disclaimer,
                        bg = InfoBg,
                        fg = InfoFg,
                        icon = Icons.Filled.Info,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // 3. 底部操作栏（如果提供官方核验链接则吸底展示，避免滚至最底端才能点击）
            if (bookingUrl != null) {
                StickyVerificationFooter(
                    bookingLabel = detail.bookingLabel
                        ?: stringResource(R.string.plan_detail_booking_fallback),
                    onOpenUrl = {
                        val opened = runCatching {
                            if (!bookingUrl.startsWith("http://") && !bookingUrl.startsWith("https://")) {
                                throw IllegalArgumentException("unsupported scheme")
                            }
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(bookingUrl)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                },
                            )
                        }
                        if (opened.isFailure) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.plan_detail_link_error),
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    },
                )
            }
        }
    }
}

/** 顶部导航栏：带返回键、当前分类徽标与统一内嵌矢量 Logo */
@Composable
private fun DetailTopBar(
    groupTitle: String,
    onBack: () -> Unit,
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
                contentDescription = stringResource(R.string.plan_detail_back),
                onClick = onBack,
                background = CardBg,
                borderColor = SubtleBorder,
                tint = Ink,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.plan_detail_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = Ink,
                )
                Text(
                    text = groupTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
            }
        }

        OmniLogoMark(size = 32.dp)
    }
}

/** 核心概览卡片 */
@Composable
private fun HeroCandidateCard(
    detail: CandidateDetailData,
    groupTitle: String,
    rank: Int,
) {
    val isTopRank = rank == 1

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = CardBg,
        border = BorderStroke(1.dp, if (isTopRank) BrandOrange.copy(alpha = 0.35f) else SubtleBorder),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // 排名标识
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isTopRank) OrangeLightBg else TagBg,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isTopRank) BrandOrange else Muted),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isTopRank) {
                                stringResource(R.string.plan_detail_rank_top)
                            } else {
                                stringResource(R.string.plan_detail_rank_other, rank)
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isTopRank) BrandOrange else Ink,
                        )
                    }
                }

                // 综合评分徽标：路书节点没有评分时改为展示数据时效
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = OrangeLightBg,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (detail.score > 0) {
                            Text(
                                text = stringResource(R.string.plan_detail_match),
                                fontSize = 12.sp,
                                color = BrandOrange.copy(alpha = 0.8f),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${formatScore(detail.score)} / 5.0",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = BrandOrange,
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.plan_detail_freshness),
                                fontSize = 12.sp,
                                color = BrandOrange.copy(alpha = 0.8f),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = detail.trustLabel ?: stringResource(
                                    if (detail.estimated) {
                                        R.string.plan_detail_estimated
                                    } else {
                                        R.string.plan_detail_realtime
                                    },
                                ),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = BrandOrange,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 候选主标题
            Text(
                text = detail.name,
                style = MaterialTheme.typography.headlineSmall,
                color = Ink,
            )

            // 标签栏
            val tags = detail.tags
            if (tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    tags.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = TagBg,
                        ) {
                            Text(
                                text = tag,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Ink.copy(alpha = 0.8f),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 价格与区间指标卡 */
@Composable
private fun PriceMetricCard(
    detail: CandidateDetailData,
    priceText: String,
) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = CardBg,
        border = BorderStroke(1.dp, SubtleBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.plan_detail_cost_title),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Muted,
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (detail.estimated) TagBg else VerifiedGreenBg,
                ) {
                    Text(
                        text = stringResource(
                            if (detail.estimated) {
                                R.string.plan_detail_cost_estimated
                            } else {
                                R.string.plan_detail_cost_realtime
                            },
                        ),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (detail.estimated) Muted else VerifiedGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = priceText,
                style = MaterialTheme.typography.headlineSmall,
                color = Ink,
            )

            val range = detail.priceRange
            if (range != null && range != priceText) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.plan_detail_range, range),
                    fontSize = 12.sp,
                    color = Muted,
                )
            }

            val statusText = priceStatusText(context, detail)
            if (statusText != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = statusText,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = Muted.copy(alpha = 0.85f),
                )
            }
        }
    }
}

/** 核心事实参数矩阵（两列网格/紧凑指标流） */
@Composable
private fun FactsMetricCard(facts: List<Pair<String, String>>) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = CardBg,
        border = BorderStroke(1.dp, SubtleBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
            facts.forEachIndexed { index, fact ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = fact.first,
                        fontSize = 13.sp,
                        color = Muted,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = fact.second,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink,
                        textAlign = TextAlign.End,
                    )
                }
                if (index != facts.lastIndex) {
                    HorizontalDivider(color = CardLineDivider, thickness = 1.dp)
                }
            }
        }
    }
}

/** 结构化列表卡（推荐理由或约束事项） */
@Composable
private fun StructuredListCard(
    title: String,
    badge: String,
    items: List<String>,
    isWarning: Boolean,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = CardBg,
        border = BorderStroke(1.dp, SubtleBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isWarning) WarningAmberBg else TagBg,
                ) {
                    Text(
                        text = badge,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isWarning) WarningAmber else Muted,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(if (isWarning) WarningAmberBg else OrangeLightBg),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (isWarning) Icons.Filled.Warning else Icons.Filled.Check,
                                contentDescription = null,
                                tint = if (isWarning) WarningAmber else BrandOrange,
                                modifier = Modifier.size(9.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = item,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = Ink.copy(alpha = 0.9f),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/** 数据溯源与供应商证据卡 */
@Composable
private fun SourceAuditCard(
    provider: String?,
    retrievedAt: String?,
    estimated: Boolean,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = CardBg,
        border = BorderStroke(1.dp, SubtleBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = stringResource(R.string.plan_detail_audit_title),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Muted,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = provider?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.plan_detail_source_fallback),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
            )

            if (estimated) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.plan_detail_disclaimer),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = Muted,
                )
            }

            val time = retrievedAt?.take(19)?.replace("T", " ")
            if (!time.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(Muted.copy(alpha = 0.5f)),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.plan_detail_timestamp, time),
                        fontSize = 12.sp,
                        color = Muted.copy(alpha = 0.75f),
                    )
                }
            }
        }
    }
}

/** 吸底官方核验按钮 */
@Composable
private fun StickyVerificationFooter(
    bookingLabel: String,
    onOpenUrl: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.985f else 1f, label = "btn_scale")
    val onBrand = LocalAppPalette.current.onBrand

    Surface(
        color = CardBg,
        border = BorderStroke(1.dp, SubtleBorder.copy(alpha = 0.7f)),
        shadowElevation = 12.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .scale(scale)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(BrandOrange, BrandOrangeLight),
                        ),
                    )
                    .minimumInteractiveComponentSize().clickable(
                        role = Role.Button,
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onOpenUrl,
                    ),
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = bookingLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = onBrand,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "↗",
                        style = MaterialTheme.typography.titleLarge,
                        color = onBrand,
                    )
                }
            }
        }
    }
}

/** 价格主文案：优先官方面价，其次区间价；都没有就不展示价格面板。 */
private fun buildPriceText(detail: CandidateDetailData): String? {
    val label = detail.priceLabel?.trim().orEmpty()
    if (label.isNotEmpty()) return label
    val range = detail.priceRange?.trim().orEmpty()
    return range.ifEmpty { null }
}

/** 价格口径说明：未知/估算各给一句提示，实时数据不再额外解释。 */
private fun priceStatusText(context: Context, detail: CandidateDetailData): String? {
    return when (detail.priceStatus) {
        "unknown" -> context.getString(R.string.plan_detail_price_unknown)
        "estimated" -> context.getString(R.string.plan_detail_price_estimated)
        else -> null
    }
}

/** 综合评分保留一位小数。 */
private fun formatScore(score: Double): String {
    return String.format(Locale.US, "%.1f", score)
}

/** 事实参数矩阵：只展示确实有值的指标，避免空行占位。 */
private fun buildFacts(context: Context, detail: CandidateDetailData): List<Pair<String, String>> {
    val facts = mutableListOf<Pair<String, String>>()
    if (detail.segments.isNotEmpty()) {
        facts += context.getString(R.string.plan_detail_fact_segments) to
            detail.segments.joinToString(" → ")
    }
    if (detail.durationMinutes > 0) {
        facts += context.getString(R.string.plan_detail_fact_duration) to
            formatMinutes(context, detail.durationMinutes)
    }
    if (detail.transferBufferMinutes > 0) {
        facts += context.getString(R.string.plan_detail_fact_buffer) to
            context.getString(R.string.plan_detail_buffer_value, detail.transferBufferMinutes)
    }
    if (detail.carbonKgEstimate > 0) {
        facts += context.getString(R.string.plan_detail_fact_carbon) to
            context.getString(R.string.plan_detail_carbon_value, detail.carbonKgEstimate.toString())
    }
    detail.area?.takeIf { it.isNotBlank() }?.let {
        facts += context.getString(R.string.plan_detail_fact_area) to it
    }
    if (detail.confidence > 0) {
        facts += context.getString(R.string.plan_detail_fact_confidence) to
            "${(detail.confidence * 100).toInt()}%"
    }
    if (detail.freshnessSeconds > 0) {
        facts += context.getString(R.string.plan_detail_fact_freshness) to
            formatFreshness(context, detail.freshnessSeconds)
    }
    return facts
}

private fun formatMinutes(context: Context, minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours <= 0 -> context.getString(R.string.plan_detail_duration_minutes, rest)
        rest == 0 -> context.getString(R.string.plan_detail_duration_hours, hours)
        else -> context.getString(R.string.plan_detail_duration_hours_minutes, hours, rest)
    }
}

private fun formatFreshness(context: Context, seconds: Int): String {
    return when {
        seconds < 60 -> context.getString(R.string.plan_detail_fresh_seconds, seconds)
        seconds < 3600 -> context.getString(R.string.plan_detail_fresh_minutes, seconds / 60)
        seconds < 86_400 -> context.getString(R.string.plan_detail_fresh_hours, seconds / 3600)
        else -> context.getString(R.string.plan_detail_fresh_days, seconds / 86_400)
    }
}