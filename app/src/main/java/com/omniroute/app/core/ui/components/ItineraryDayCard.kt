package com.omniroute.app.core.ui.components

import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import com.omniroute.app.core.ui.theme.EditionSurface
import androidx.compose.foundation.layout.FlowRow
import com.omniroute.app.core.ui.theme.LocalAppPalette

import android.content.Context

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniroute.app.R
import com.omniroute.app.core.planning.TripItinerary
import com.omniroute.app.feature.auth.BrandOrange
import com.omniroute.app.feature.auth.CardBorder
import com.omniroute.app.feature.auth.Ink
import com.omniroute.app.feature.auth.Muted

/** 一天日程卡片：结果页预览与行程详情共用同一套渲染；传入 onItemClick 时条目可点击跳详情。 */
@Composable
fun ItineraryDayCard(
    day: TripItinerary.Day,
    modifier: Modifier = Modifier,
    isBooked: (TripItinerary.Item) -> Boolean = { false },
    showEstimatedNote: Boolean = true,
    onItemClick: ((TripItinerary.Item) -> Unit)? = null,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        // 跟随主题语义面底，深色模式下不再出现刺眼的白卡。
        color = EditionSurface,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (day.items.isEmpty()) LocalAppPalette.current.fieldBg else BrandOrange),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "D" + day.dayNumber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (day.items.isEmpty()) Muted else LocalAppPalette.current.onBrand,
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.itinerary_day_title, day.dayNumber),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (day.items.isEmpty()) {
                        stringResource(R.string.itinerary_free)
                    } else {
                        stringResource(R.string.itinerary_item_count, day.items.size)
                    },
                    fontSize = 12.sp,
                    color = Muted,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End,
                )
            }

            if (day.items.isEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.itinerary_empty),
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = Muted,
                )
                return@Column
            }

            if (showEstimatedNote && day.items.any { it.isEstimated && it.priceLabel != null }) {
                Text(
                    text = stringResource(R.string.itinerary_estimated),
                    fontSize = 11.sp,
                    color = Muted,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            val track = BrandOrange
            Column(Modifier.fillMaxWidth().padding(top = 8.dp).drawBehind {
                drawLine(
                    track.copy(alpha = 0.28f), Offset(5.dp.toPx(), 10.dp.toPx()),
                    Offset(5.dp.toPx(), size.height - 9.dp.toPx()), 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())),
                )
            }) {
                day.items.forEach { item ->
                    Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
                        Box(Modifier.padding(start = 2.dp, top = 8.dp, end = 8.dp).size(6.dp)
                            .clip(CircleShape).background(track))
                        ItineraryItemRow(item, isBooked(item), onItemClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun ItineraryItemRow(
    item: TripItinerary.Item,
    isBooked: Boolean,
    onItemClick: ((TripItinerary.Item) -> Unit)?,
) {
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onItemClick != null) {
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(role = Role.Button) { onItemClick(item) }
                        .padding(vertical = 2.dp)
                } else {
                    Modifier
                },
            ),
    ) {
        val type = item.type?.takeIf { it.isNotBlank() }
        if (type != null) {
            Text(
                text = type,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = BrandOrange,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(LocalAppPalette.current.brandTint)
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name.orEmpty(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 19.sp,
                color = Ink,
            )
            if (isBooked) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.itinerary_booked),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1B8755),
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFFE8F7EE))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            val meta = buildItemMeta(context, item)
            if (meta.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    meta.forEach { part ->
                        Text(text = part, fontSize = 12.sp, color = Muted)
                    }
                }
            }
            // 保存时备注会并入 desc（"备注：..."），在这里回显，证明编辑内容确实落到行程详情
            val note = item.description
                ?.lineSequence()
                ?.map { it.trim() }
                ?.firstOrNull { it.startsWith("备注：") && it.length > 3 }
            if (note != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = note,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = BrandOrange,
                )
            }
        }
        if (onItemClick != null) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "›", fontSize = 16.sp, color = Muted)
        }
    }
}

private fun buildItemMeta(context: Context, item: TripItinerary.Item): List<String> {
    val parts = mutableListOf<String>()
    if (item.durationMinutes > 0) {
        parts.add(formatDuration(context, item.durationMinutes))
    }
    val priceLabel = item.priceLabel
    if (priceLabel != null) {
        parts.add(priceLabel)
    }
    return parts
}

private fun formatDuration(context: Context, minutes: Int): String {
    if (minutes < 60) {
        return context.getString(R.string.itinerary_minutes, minutes)
    }
    val hours = minutes / 60
    val rest = minutes % 60
    return if (rest == 0) {
        context.getString(R.string.itinerary_hours, hours)
    } else {
        context.getString(R.string.itinerary_hours_minutes, hours, rest)
    }
}
