package com.omniroute.app.feature.plan

import com.omniroute.app.core.ui.theme.LocalAppPalette

import androidx.compose.material3.minimumInteractiveComponentSize

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniroute.app.R
import com.omniroute.app.core.planning.PlanRequest
import com.omniroute.app.core.planning.PlanResult
import com.omniroute.app.core.planning.RouteNode
import com.omniroute.app.core.ui.AppWidthClass
import com.omniroute.app.core.ui.components.RouteMapView
import com.omniroute.app.core.ui.rememberAppWidthClass
import com.omniroute.app.feature.auth.BrandOrange
import com.omniroute.app.feature.auth.InfoBg
import com.omniroute.app.feature.auth.InfoFg
import com.omniroute.app.feature.auth.Ink
import com.omniroute.app.feature.auth.InlineBanner
import com.omniroute.app.feature.auth.Muted

@Composable
internal fun ResultContent(
    result: PlanResult,
    request: PlanRequest?,
    nodes: List<RouteNode>,
    notes: Map<String, String>,
    selectedDay: Int?,
    onSelectDay: (Int?) -> Unit,
    onOpenEditor: () -> Unit,
    onOpenCandidate: (String, String) -> Unit,
    loadBaseMap: suspend (center: String, zoom: Int, width: Int, height: Int) -> Bitmap?,
    feedbackDone: Int?,
    feedbackReason: String,
    onFeedbackReasonChange: (String) -> Unit,
    feedbackBusy: Boolean,
    feedbackError: String?,
    onFeedback: (Int) -> Unit,
) {
    val dayCount = remember(nodes, request) {
        maxOf(nodes.maxOfOrNull { it.day } ?: 1, result.days, request?.days ?: 1, 1)
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = PlanCardBg,
        border = BorderStroke(1.dp, PlanSubtleBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            R.string.plan_result_book,
                            request?.destination ?: stringResource(R.string.plan_result_book_default),
                        ),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.plan_result_subtitle),
                        fontSize = 12.sp,
                        color = Muted,
                    )
                }
                Surface(shape = RoundedCornerShape(50), color = PlanOrangeLightBg) {
                    Text(
                        text = stringResource(R.string.plan_result_recommended),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandOrange,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            val summary = result.summary.trim()
            if (summary.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = summary,
                    fontSize = 12.5.sp,
                    lineHeight = 19.sp,
                    color = Ink.copy(alpha = 0.85f),
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = PlanFormDivider, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ResultMetaChip(
                    label = stringResource(R.string.plan_result_meta_days),
                    value = stringResource(R.string.plan_result_days_value, dayCount),
                )
                ResultMetaChip(
                    label = stringResource(R.string.plan_result_meta_nodes),
                    value = stringResource(
                        R.string.plan_result_nodes_value,
                        nodes.count { it.day >= 1 },
                    ),
                )
                ResultMetaChip(
                    label = stringResource(R.string.plan_result_meta_travelers),
                    value = stringResource(R.string.plan_result_travelers_value, request?.travelers ?: 0),
                )
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = PlanOrangeLightBg,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .minimumInteractiveComponentSize().clickable(role = Role.Button, onClick = onOpenEditor),
                ) {
                    Text(
                        text = stringResource(R.string.plan_result_edit_route),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandOrange,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                    )
                }
            }
        }
    }

    if (result.isFallback) {
        Spacer(modifier = Modifier.height(12.dp))
        InlineBanner(
            text = stringResource(R.string.plan_result_degraded),
            bg = InfoBg,
            fg = InfoFg,
            icon = Icons.Filled.Info,
        )
    }

    // 扩展屏（平板横屏/外接屏）采用「地图 + 时间轴」双栏，避免地图和日程被拉成上下两条长带。
    val expanded = rememberAppWidthClass() == AppWidthClass.Expanded
    if (expanded) {
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                ResultMapSection(
                    nodes = nodes,
                    selectedDay = selectedDay,
                    loadBaseMap = loadBaseMap,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                ResultTimelineSection(
                    dayCount = dayCount,
                    nodes = nodes,
                    selectedDay = selectedDay,
                    onSelectDay = onSelectDay,
                    notes = notes,
                    onOpenCandidate = onOpenCandidate,
                )
            }
        }
    } else {
        Spacer(modifier = Modifier.height(20.dp))
        ResultMapSection(
            nodes = nodes,
            selectedDay = selectedDay,
            loadBaseMap = loadBaseMap,
        )
        Spacer(modifier = Modifier.height(20.dp))
        ResultTimelineSection(
            dayCount = dayCount,
            nodes = nodes,
            selectedDay = selectedDay,
            onSelectDay = onSelectDay,
            notes = notes,
            onOpenCandidate = onOpenCandidate,
        )
    }

    Spacer(modifier = Modifier.height(18.dp))
    FeedbackCard(
        done = feedbackDone,
        reason = feedbackReason,
        onReasonChange = onFeedbackReasonChange,
        busy = feedbackBusy,
        errorText = feedbackError,
        onFeedback = onFeedback,
    )
}

@Composable
private fun ResultMapSection(
    nodes: List<RouteNode>,
    selectedDay: Int?,
    loadBaseMap: suspend (center: String, zoom: Int, width: Int, height: Int) -> Bitmap?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = stringResource(R.string.plan_result_map_title), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text(
            text = stringResource(
                if (nodes.any { it.hasCoordinate() }) {
                    R.string.plan_result_map_real
                } else {
                    R.string.plan_result_map_placeholder
                },
            ),
            fontSize = 12.sp,
            color = Muted,
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = PlanCardBg,
        border = BorderStroke(1.dp, PlanSubtleBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        RouteMapView(
            nodes = nodes.filter { it.day >= 1 },
            highlightDay = selectedDay,
            loadBaseMap = loadBaseMap,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .padding(8.dp),
            corner = 12.dp,
        )
    }
}

@Composable
private fun ResultTimelineSection(
    dayCount: Int,
    nodes: List<RouteNode>,
    selectedDay: Int?,
    onSelectDay: (Int?) -> Unit,
    notes: Map<String, String>,
    onOpenCandidate: (String, String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = stringResource(R.string.plan_result_timeline_title), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text(
            text = stringResource(R.string.plan_result_total_days, dayCount),
            fontSize = 12.sp,
            color = Muted,
        )
    }
    Spacer(modifier = Modifier.height(8.dp))

    DayFilterRow(
        dayCount = dayCount,
        selectedDay = selectedDay,
        onSelectDay = onSelectDay,
    )
    Spacer(modifier = Modifier.height(10.dp))

    val visibleDays = if (selectedDay == null) 1..dayCount else selectedDay..selectedDay
    visibleDays.forEach { day ->
        val dayNodes = nodes.filter { it.day == day }.sortedBy { timelineTime(it.time) }
        DayTimeline(day = day, nodes = dayNodes, notes = notes, onOpenCandidate = onOpenCandidate)
        if (day != visibleDays.last) {
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun DayFilterRow(dayCount: Int, selectedDay: Int?, onSelectDay: (Int?) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DayChip(
            text = stringResource(R.string.plan_result_all_days),
            selected = selectedDay == null,
            onClick = { onSelectDay(null) },
        )
        for (day in 1..dayCount) {
            DayChip(text = "D$day", selected = selectedDay == day, onClick = { onSelectDay(day) })
        }
    }
}

@Composable
private fun DayChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) BrandOrange else PlanCardBg,
        border = BorderStroke(1.dp, if (selected) BrandOrange else PlanSubtleBorder),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .minimumInteractiveComponentSize().clickable(role = Role.Button, onClick = onClick),
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) LocalAppPalette.current.onBrand else Muted,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
        )
    }
}

private val TimelineTimePattern = Regex("""(\d{1,2}:\d{2})""")

/**
 * 后端回传的时间形如「Day 1 | 08:30 游览」，时间轴 46dp 的窄列放不下。
 * 只取 `|` 之后的 HH:mm；拿不到时间就原样返回，由调用方降级展示。
 */
private fun timelineTime(raw: String): String {
    val text = raw.trim()
    if (text.isEmpty()) return ""
    val afterPipe = text.substringAfter('|', "").trim()
    val source = afterPipe.ifEmpty { text }
    return TimelineTimePattern.find(source)?.value ?: source.take(5)
}

/** 竖直时间轴：左侧时间，中间轨道与节点，右侧内容。 */
@Composable
private fun DayTimeline(
    day: Int,
    nodes: List<RouteNode>,
    notes: Map<String, String>,
    onOpenCandidate: (String, String) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = PlanCardBg,
        border = BorderStroke(1.dp, PlanSubtleBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(BrandOrange),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "D$day",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.plan_result_day_value, day),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (nodes.isEmpty()) {
                        stringResource(R.string.plan_result_free_day)
                    } else {
                        stringResource(R.string.plan_result_item_count, nodes.size)
                    },
                    fontSize = 12.sp,
                    color = Muted,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (nodes.isEmpty()) {
                Text(
                    text = stringResource(R.string.plan_result_day_empty),
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = Muted,
                )
                return@Column
            }

            nodes.forEachIndexed { index, node ->
                TimelineNode(
                    node = node,
                    isLast = index == nodes.lastIndex,
                    note = notes[stopNoteKey(node)],
                    onClick = { onOpenCandidate(node.category, routeNodeDetailId(node)) },
                )
            }
        }
    }
}

@Composable
private fun TimelineNode(
    node: RouteNode,
    isLast: Boolean,
    note: String? = null,
    onClick: () -> Unit = {},
) {
    val time = timelineTime(node.time)
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.width(52.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = time.ifBlank { "—" },
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (time.isBlank()) Muted.copy(alpha = 0.6f) else Ink,
                maxLines = 1,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))

        // 时间轴轨道：圆点 + 段间竖线
        Column(
            modifier = Modifier.width(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(if (node.isHotel) Color(0xFF5B7C99) else BrandOrange),
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .width(1.5.dp)
                        .height(26.dp)
                        .background(Color(0xFFEDE3D6)),
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 14.dp)
                .clip(RoundedCornerShape(10.dp))
                .minimumInteractiveComponentSize().clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 4.dp, vertical = 3.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(5.dp), color = PlanOrangeLightBg) {
                    Text(
                        text = node.category,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = BrandOrange,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = node.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(modifier = Modifier.width(6.dp))
                // 数据时效直接标在节点上，避免用户把估算价当成实时价
                Surface(
                    shape = RoundedCornerShape(5.dp),
                    color = if (node.isEstimated) PlanTagBg else Color(0xFFE9F3EA),
                ) {
                    Text(
                        text = node.freshnessLabel(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (node.isEstimated) Muted else Color(0xFF3F6B47),
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    )
                }
            }

            val meta = buildList {
                if (node.transport.isNotBlank()) add(node.transport)
                if (node.costEstimate.isNotBlank()) add(node.costEstimate)
                if (node.openTime.isNotBlank()) add(node.openTime)
            }.distinct()
            if (meta.isNotEmpty()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = meta.joinToString(" · "),
                    fontSize = 12.sp,
                    color = Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (node.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = node.description,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = Muted,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (!note.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.plan_result_note, note),
                    fontSize = 12.sp,
                    color = BrandOrange,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}


@Composable
private fun ResultMetaChip(label: String, value: String) {
    Surface(shape = RoundedCornerShape(8.dp), color = PlanTagBg) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "$label ", fontSize = 12.sp, color = Muted)
            Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        }
    }
}

/** 底部常驻操作栏：按阶段切换主行动。 */
