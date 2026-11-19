package com.omniroute.app.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniroute.app.core.planning.RouteNode
import com.omniroute.app.feature.auth.BrandOrange
import com.omniroute.app.feature.auth.Ink
import kotlin.math.abs

private val MapBg = Color(0xFFFDF7F0)
private val GridLine = Color(0xFFEDE3D6)

/**
 * 路线示意地图：按节点 lnglat 归一化后绘制路线与途经点。
 *
 * 这里是矢量路线图而不是瓦片底图——V1 不依赖任何地图 SDK 的密钥，
 * 坐标来自后端 final_route，画出来的相对位置仍然真实。
 * 坐标缺失时只画占位虚线，绝不伪装成已定位。
 */
@Composable
fun RouteMapCanvas(
    nodes: List<RouteNode>,
    modifier: Modifier = Modifier,
    highlightDay: Int? = null,
    corner: Dp = 14.dp,
    contentDescription: String? = null,
) {
    val withCoordinates = nodes.filter { it.hasCoordinate() }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .background(MapBg)
            .then(
                if (contentDescription == null) {
                    Modifier
                } else {
                    Modifier.semantics { this.contentDescription = contentDescription }
                },
            ),
    ) {
        val brand = BrandOrange
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val pad = 20.dp.toPx()

            // 经纬网格底纹
            val step = 28.dp.toPx()
            var x = 0f
            while (x < w) {
                drawLine(GridLine, Offset(x, 0f), Offset(x, h), strokeWidth = 0.7.dp.toPx())
                x += step
            }
            var y = 0f
            while (y < h) {
                drawLine(GridLine, Offset(0f, y), Offset(w, y), strokeWidth = 0.7.dp.toPx())
                y += step
            }

            if (withCoordinates.size < 2) {
                val placeholder = Path().apply {
                    moveTo(pad, h * 0.72f)
                    cubicTo(w * 0.3f, h * 0.2f, w * 0.7f, h * 0.85f, w - pad, h * 0.3f)
                }
                drawPath(
                    path = placeholder,
                    color = brand.copy(alpha = 0.35f),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f),
                    ),
                )
                return@Canvas
            }

            val minLng = withCoordinates.minOf { it.lng }
            val maxLng = withCoordinates.maxOf { it.lng }
            val minLat = withCoordinates.minOf { it.lat }
            val maxLat = withCoordinates.maxOf { it.lat }
            val spanLng = if (abs(maxLng - minLng) < 0.0001) 0.0001 else maxLng - minLng
            val spanLat = if (abs(maxLat - minLat) < 0.0001) 0.0001 else maxLat - minLat

            fun project(node: RouteNode): Offset {
                val px = pad + ((node.lng - minLng) / spanLng).toFloat() * (w - pad * 2)
                // 纬度越大越靠北，屏幕上 y 越小
                val py = pad + ((maxLat - node.lat) / spanLat).toFloat() * (h - pad * 2)
                return Offset(px, py)
            }

            // 按天与时间顺序连线
            val ordered = withCoordinates.sortedWith(compareBy({ it.day }, { it.time }))
            val points = ordered.map { project(it) }

            if (points.size >= 2) {
                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (index in 1 until points.size) {
                        val previous = points[index - 1]
                        val current = points[index]
                        val midX = (previous.x + current.x) / 2f
                        val midY = (previous.y + current.y) / 2f
                        quadraticTo(previous.x, previous.y, midX, midY)
                        if (index == points.size - 1) {
                            lineTo(current.x, current.y)
                        }
                    }
                }
                drawPath(
                    path = path,
                    color = brand.copy(alpha = 0.16f),
                    style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round),
                )
                drawPath(
                    path = path,
                    color = brand,
                    style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round),
                )
            }

            val labelPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.WHITE
                textSize = 9.sp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }

            ordered.forEachIndexed { index, node ->
                val point = points[index]
                val emphasized = highlightDay == null || node.day == highlightDay
                val radius = if (index == 0 || index == ordered.lastIndex) 8.dp.toPx() else 6.5.dp.toPx()
                val fill = if (emphasized) brand else brand.copy(alpha = 0.45f)

                if (emphasized) {
                    drawCircle(
                        color = brand.copy(alpha = 0.18f),
                        radius = radius + 5.dp.toPx(),
                        center = point,
                    )
                }
                drawCircle(color = Color.White, radius = radius, center = point)
                drawCircle(color = fill, radius = radius - 2.dp.toPx(), center = point)

                // 途经点用天序号标注，避免只看到一堆无名圆点
                val baseline = point.y + labelPaint.textSize / 3f
                drawContext.canvas.nativeCanvas.drawText(
                    node.day.toString(),
                    point.x,
                    baseline,
                    labelPaint,
                )
            }

            // 起点 / 终点补充标签
            val namePaint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.rgb(27, 23, 18)
                alpha = 150
                textSize = 9.sp.toPx()
            }
            val first = ordered.first()
            val last = ordered.last()
            drawContext.canvas.nativeCanvas.drawText(
                first.name,
                (points.first().x + 8.dp.toPx()).coerceAtMost(w - namePaint.measureText(first.name)),
                points.first().y - 8.dp.toPx(),
                namePaint,
            )
            drawContext.canvas.nativeCanvas.drawText(
                last.name,
                (points.last().x + 8.dp.toPx()).coerceAtMost(w - namePaint.measureText(last.name)),
                points.last().y - 8.dp.toPx(),
                namePaint,
            )
        }
    }
}
