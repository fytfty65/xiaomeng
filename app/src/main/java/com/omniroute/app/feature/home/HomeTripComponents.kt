package com.omniroute.app.feature.home

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniroute.app.R
import com.omniroute.app.core.network.dto.TripPlanDto
import com.omniroute.app.core.ui.theme.LocalAppPalette

@Composable
internal fun ModernEmptyTripsCard(onStartPlanning: () -> Unit) {
    val palette = LocalAppPalette.current
    Surface(
        shape = RoundedCornerShape(15.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Canvas(Modifier.size(width = 44.dp, height = 21.dp)) {
                val inset = 4.dp.toPx()
                val corner = CornerRadius(4.dp.toPx())
                drawRoundRect(palette.brandTint, Offset(inset, 0f), Size(size.width - inset, size.height - inset), corner)
                drawRoundRect(palette.brandTint, Offset(0f, inset), Size(size.width - inset, size.height - inset), corner)
                drawLine(
                    palette.brandDark.copy(alpha = 0.70f),
                    Offset(size.width / 3f, inset * 1.5f),
                    Offset(size.width / 3f, size.height - inset * 0.5f),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 2.dp.toPx())),
                )
            }
            Text(
                stringResource(R.string.home_empty_title),
                color = palette.ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                stringResource(R.string.home_empty_sub),
                color = palette.muted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = onStartPlanning) {
                Text(stringResource(R.string.home_empty_action))
            }
        }
    }
}

@Composable
internal fun ModernTripCard(trip: TripPlanDto, onClick: () -> Unit) {
    val palette = LocalAppPalette.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val stubWidth = (70 * density.fontScale.coerceIn(1f, 1.4f)).dp
    val stubPx = with(density) { stubWidth.roundToPx().toFloat() }
    val notchPx = with(density) { 5.dp.toPx() }
    val cornerPx = with(density) { 14.dp.toPx() }
    val ticketShape = remember(stubPx, notchPx, cornerPx) {
        GenericShape { size, _ ->
            val right = size.width
            val bottom = size.height
            val x = stubPx.coerceAtMost(right * 0.5f)
            val r = notchPx
            val c = cornerPx
            moveTo(c, 0f)
            lineTo(x - r, 0f)
            arcTo(Rect(x - r, -r, x + r, r), 180f, -180f, false)
            lineTo(right - c, 0f)
            quadraticTo(right, 0f, right, c)
            lineTo(right, bottom - c)
            quadraticTo(right, bottom, right - c, bottom)
            lineTo(x + r, bottom)
            arcTo(Rect(x - r, bottom - r, x + r, bottom + r), 0f, -180f, false)
            lineTo(c, bottom)
            quadraticTo(0f, bottom, 0f, bottom - c)
            lineTo(0f, c)
            quadraticTo(0f, 0f, c, 0f)
            close()
        }
    }

    Surface(
        onClick = onClick,
        shape = ticketShape,
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 79.dp).drawBehind {
                val x = stubPx.coerceAtMost(size.width * 0.5f)
                drawRect(color = palette.brandTint, size = Size(x, size.height))
                val endSpace = notchPx + 3.dp.toPx()
                drawLine(
                    palette.brandDark.copy(alpha = 0.43f),
                    start = Offset(x, endSpace),
                    end = Offset(x, size.height - endSpace),
                    strokeWidth = 0.8.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.5.dp.toPx(), 3.dp.toPx())),
                )
            },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.width(stubWidth).padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        trip.days.coerceAtLeast(1).toString(),
                        color = palette.brandDark,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 24.sp,
                        lineHeight = 25.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        stringResource(if (trip.days <= 1) R.string.home_trip_day else R.string.home_trip_days),
                        color = palette.brandDark,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        letterSpacing = 1.1.sp,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 11.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    tripTitle(context, trip),
                    color = palette.ink,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                val city = trip.destCity?.trim().orEmpty()
                val date = trip.createdAt?.take(10).orEmpty()
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    if (city.isNotEmpty()) {
                        Text(city, fontSize = 11.sp, color = palette.brandDark)
                    }
                    if (trip.travelers > 0) {
                        Text(stringResource(R.string.home_trip_travelers, trip.travelers), fontSize = 11.sp, color = palette.muted)
                    }
                    if (date.length == 10) {
                        Text(date, fontSize = 11.sp, color = palette.muted, fontFamily = FontFamily.Monospace)
                    }
                }
            }
            Icon(
                Icons.Filled.ArrowForward,
                contentDescription = null,
                tint = palette.brandDark,
                modifier = Modifier.padding(end = 13.dp).size(15.dp),
            )
        }
    }
}

private fun tripTitle(context: Context, trip: TripPlanDto): String {
    val title = trip.title?.trim().orEmpty()
    if (title.isNotEmpty()) return title
    val city = trip.destCity?.trim().orEmpty()
    return if (city.isNotEmpty()) {
        context.getString(R.string.home_trip_title, city)
    } else {
        context.getString(R.string.home_trip_unnamed)
    }
}
