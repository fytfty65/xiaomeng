package com.omniroute.app.core.ui.components

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniroute.app.R
import com.omniroute.app.core.planning.RouteNode
import com.omniroute.app.feature.auth.Ink
import com.omniroute.app.feature.auth.Muted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.tan

private val MapBackdrop = Color(0xFFFDF7F0)

/** 底图请求像素下的留白：底部要容纳图例与停靠点名称。按 640 宽定义，随请求宽度等比缩放。 */
private const val FitRefWidth = 640.0
private const val FitPadX = 30.0
private const val FitPadTop = 24.0
private const val FitPadBottom = 84.0

/** 手势缩放上限：底图是静态图，放太大只会变糊。 */
private const val MaxViewScale = 2.5f

/** 按天配色：DAY1 蓝、DAY2 橙、DAY3 绿……与参考产品的多段路线一致。 */
private val DayPalette = listOf(
    Color(0xFF4A90E2),
    Color(0xFFF5A623),
    Color(0xFF2E9E6B),
    Color(0xFFD0021B),
    Color(0xFF8E44AD),
    Color(0xFF16A085),
    Color(0xFF34495E),
)

fun dayColor(day: Int): Color = DayPalette[(max(1, day) - 1) % DayPalette.size]

/**
 * 真实底图路线图：高德静态地图做底图，折线与停靠点由客户端绘制。
 *
 * <p>支持双指缩放、放大后单指平移、点击停靠点查看信息；底图加载失败时回退到
 * [RouteMapCanvas] 的矢量示意图，并明确标注「底图暂不可用」——不把空白或错误
 * 响应当成地图。
 */
@Composable
fun RouteMapView(
    nodes: List<RouteNode>,
    modifier: Modifier = Modifier,
    highlightDay: Int? = null,
    corner: Dp = 14.dp,
    loadBaseMap: suspend (center: String, zoom: Int, width: Int, height: Int) -> Bitmap?,
) {
    val located = nodes.filter { it.hasCoordinate() }
    // Canvas 点位本身无法逐个暴露语义，给整张地图一份可读的路线摘要，供读屏按顺序朗读。
    val summary = routeMapSummary(nodes)
    if (located.size < 2) {
        RouteMapCanvas(
            nodes = nodes,
            modifier = modifier,
            highlightDay = highlightDay,
            corner = corner,
            contentDescription = summary,
        )
        return
    }

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .background(MapBackdrop),
    ) {
        val density = LocalDensity.current
        val measuredWidth = with(density) { maxWidth.toPx() }
        val measuredHeight = with(density) { maxHeight.toPx() }
        val boxWidth = if (measuredWidth > 0f) measuredWidth else 640f
        val boxHeight = if (measuredHeight > 0f) measuredHeight else 420f
        // 请求分辨率跟随控件尺寸（高德静态地图单边上限 1024），放大时不至于太糊。
        val requestWidth = boxWidth.roundToInt().coerceIn(360, 1024)
        val requestHeight = (requestWidth * (boxHeight / boxWidth)).roundToInt().coerceIn(200, 1024)
        // 高德静态地图的 zoom 比 Web 墨卡托层级多一级（实测 Z 约等于 Z+1 的瓦片比例），
        // 因此底图请求用 zoom - 1，叠加层仍按 Web 墨卡托 zoom 计算像素位置。
        val zoom = remember(located, requestWidth, requestHeight) {
            fitZoom(located, requestWidth, requestHeight)
        }
        // 路线中心必须**同时**用于底图请求和叠加层投影：上下留白不等时，
        // 把整幅底图的中心向南移，路线才会整体上移避让底部图例。
        // 只给叠加层加 offsetY 会让标记与底图上的 POI 标签错开（实测约 30px）。
        val center = remember(located, zoom, requestWidth) {
            routeCenter(located, zoom, requestWidth)
        }
        val baseZoom = (zoom - 1).coerceIn(3, 17)
        val centerLng = center.substringBefore(',').toDoubleOrNull() ?: 0.0
        val centerLat = center.substringAfter(',').toDoubleOrNull() ?: 0.0

        var bitmap by remember(center, baseZoom, requestWidth, requestHeight) {
            mutableStateOf<Bitmap?>(null)
        }
        var loading by remember(center, baseZoom, requestWidth, requestHeight) {
            mutableStateOf(true)
        }
        var failed by remember(center, baseZoom, requestWidth, requestHeight) {
            mutableStateOf(false)
        }

        // 视图缩放/平移只在同一路线内保持，重新规划后回到全览。
        var viewScale by remember(center) { mutableStateOf(1f) }
        var pan by remember(center) { mutableStateOf(Offset.Zero) }
        var selectedKey by remember(nodes) { mutableStateOf<String?>(null) }

        val boxCenter = Offset(boxWidth / 2f, boxHeight / 2f)

        fun projectOnBase(node: RouteNode): Offset = projectNode(
            node = node,
            zoom = zoom,
            centerLng = centerLng,
            centerLat = centerLat,
            requestWidth = requestWidth,
            requestHeight = requestHeight,
            canvasWidth = boxWidth,
            canvasHeight = boxHeight,
        )

        // 市区密集点位的坐标往往只差几十像素，编号会完全盖住；
        // 标记位置按环形轻微散开（折线仍走真实坐标），保证站数都能看见。
        val markerGap = with(density) { 20.dp.toPx() }
        val markers = remember(located, center, zoom, requestWidth, requestHeight, boxWidth, boxHeight) {
            arrangeMarkers(located, { node -> projectOnBase(node) }, markerGap)
        }

        fun screenOf(base: Offset): Offset = Offset(
            base.x * viewScale + boxCenter.x * (1f - viewScale) + pan.x,
            base.y * viewScale + boxCenter.y * (1f - viewScale) + pan.y,
        )

        fun projectOnScreen(node: RouteNode): Offset {
            val base = markers[node.routeKey()] ?: projectOnBase(node)
            return screenOf(base)
        }

        fun clampPan(value: Offset, scale: Float): Offset {
            val maxX = (boxWidth * (scale - 1f) / 2f).coerceAtLeast(0f)
            val maxY = (boxHeight * (scale - 1f) / 2f).coerceAtLeast(0f)
            return Offset(value.x.coerceIn(-maxX, maxX), value.y.coerceIn(-maxY, maxY))
        }

        fun setScale(target: Float, focus: Offset = boxCenter) {
            val next = target.coerceIn(1f, MaxViewScale)
            val ratio = if (viewScale <= 0f) 1f else next / viewScale
            pan = clampPan(
                Offset(
                    (focus.x - boxCenter.x) * (1f - ratio) + pan.x * ratio,
                    (focus.y - boxCenter.y) * (1f - ratio) + pan.y * ratio,
                ),
                next,
            )
            viewScale = next
        }

        LaunchedEffect(center, baseZoom, requestWidth, requestHeight) {
            loading = true
            failed = false
            val loaded = withContext(Dispatchers.IO) {
                runCatching { loadBaseMap(center, baseZoom, requestWidth, requestHeight) }.getOrNull()
            }
            bitmap = loaded
            failed = loaded == null
            loading = false
        }

        val base = bitmap
        if (base == null) {
            RouteMapCanvas(
                nodes = nodes,
                modifier = Modifier.fillMaxSize(),
                highlightDay = highlightDay,
                corner = 0.dp,
                contentDescription = summary,
            )
        } else {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = summary }
                    .focusable()
                    .pointerInput(center, boxWidth, boxHeight) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            var transforming = false
                            while (true) {
                                val event = awaitPointerEvent()
                                val pressed = event.changes.count { it.pressed }
                                if (pressed == 0) {
                                    break
                                }
                                // 未放大时把单指拖动让给父级滚动，避免地图吃掉整页滑动。
                                if (pressed == 1 && viewScale <= 1.001f) {
                                    if (transforming) {
                                        break
                                    }
                                    continue
                                }
                                val zoomChange = event.calculateZoom()
                                    .let { if (it.isFinite() && it > 0f) it else 1f }
                                val panChange = event.calculatePan()
                                val nextScale = (viewScale * zoomChange).coerceIn(1f, MaxViewScale)
                                val applied = if (viewScale <= 0f) 1f else nextScale / viewScale
                                val focus = event.calculateCentroid(useCurrent = true)
                                pan = clampPan(
                                    Offset(
                                        (focus.x - boxCenter.x) * (1f - applied) + pan.x * applied + panChange.x,
                                        (focus.y - boxCenter.y) * (1f - applied) + pan.y * applied + panChange.y,
                                    ),
                                    nextScale,
                                )
                                viewScale = nextScale
                                transforming = true
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                    .pointerInput(nodes, center, boxWidth, boxHeight) {
                        detectTapGestures { tap ->
                            val threshold = 24.dp.toPx()
                            var nearest: RouteNode? = null
                            var nearestDistance = Float.MAX_VALUE
                            located.forEach { node ->
                                val distance = (projectOnScreen(node) - tap).getDistance()
                                if (distance < nearestDistance) {
                                    nearestDistance = distance
                                    nearest = node
                                }
                            }
                            selectedKey = if (nearestDistance <= threshold) nearest?.routeKey() else null
                        }
                    },
            ) {
                withTransform({
                    translate(
                        boxCenter.x * (1f - viewScale) + pan.x,
                        boxCenter.y * (1f - viewScale) + pan.y,
                    )
                    scale(viewScale, viewScale, pivot = Offset.Zero)
                }) {
                    drawImage(
                        image = base.asImageBitmap(),
                        dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                    )
                    drawRouteOverlay(
                        nodes = nodes,
                        highlightDay = highlightDay,
                        selectedKey = selectedKey,
                        // 放大后名称会跟着底图一起放大并互相压住，只在全览时展示名称。
                        showNames = viewScale < 1.3f,
                        positions = markers,
                        project = { node -> projectOnBase(node) },
                    )
                }
            }
        }

        val selectedNode = located.firstOrNull { it.routeKey() == selectedKey }

        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(18.dp),
                strokeWidth = 1.5.dp,
                color = Color(0xFF4A90E2),
            )
        }
        if (failed) {
            Text(
                text = stringResource(R.string.map_base_unavailable),
                fontSize = 12.sp,
                color = Muted,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            )
        }

        if (base != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                MapControlButton(
                    text = "+",
                    description = stringResource(R.string.map_zoom_in),
                    onClick = { setScale(viewScale * 1.6f) },
                )
                MapControlButton(
                    text = "−",
                    description = stringResource(R.string.map_zoom_out),
                    onClick = { setScale(viewScale / 1.6f) },
                )
                MapControlButton(
                    description = stringResource(R.string.map_overview),
                    onClick = { setScale(1f) },
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        tint = Ink,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (selectedNode != null) {
                val stopNumber = located
                    .filter { it.day == selectedNode.day }
                    .indexOfFirst { it.routeKey() == selectedNode.routeKey() } + 1
                RoutePoiCard(
                    node = selectedNode,
                    stopNumber = stopNumber,
                    onClose = { selectedKey = null },
                    modifier = Modifier.fillMaxWidth(0.78f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                nodes.groupBy { it.day }.toSortedMap().forEach { (day, dayNodes) ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.92f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(dayColor(day), CircleShape),
                            )
                            Text(
                                text = stringResource(
                                    R.string.map_legend_day,
                                    day,
                                    dayNodes.count { it.hasCoordinate() },
                                ),
                                fontSize = 12.sp,
                                color = Color(0xFF1B1712),
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = stringResource(R.string.map_copyright),
            fontSize = 12.sp,
            color = Muted.copy(alpha = 0.8f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp),
        )
    }
}

/**
 * 地图右侧的圆形控制按钮。
 *
 * <p>视觉直径保持 32dp，但外层点击区域是 48dp（Material 最小触控目标），
 * 并补齐 contentDescription，避免读屏只听到「按钮」。
 */
@Composable
private fun MapControlButton(
    description: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.96f),
            shadowElevation = 2.dp,
            modifier = Modifier.size(32.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = description },
                contentAlignment = Alignment.Center,
            ) {
                content()
            }
        }
    }
}

@Composable
private fun MapControlButton(text: String, description: String, onClick: () -> Unit) {
    MapControlButton(description = description, onClick = onClick) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Ink,
        )
    }
}

/** 供读屏使用的路线摘要：按天列出停靠点顺序，代替 Canvas 里无法逐个表达的圆点。 */
@Composable
private fun routeMapSummary(nodes: List<RouteNode>): String {
    val daySummaries = nodes.groupBy { it.day }.toSortedMap().map { (day, dayNodes) ->
        stringResource(
            R.string.map_summary_day,
            day,
            dayNodes.joinToString(" → ") { it.name },
        )
    }
    return stringResource(R.string.map_summary, daySummaries.joinToString("；"))
}

private val ClockPattern = Regex("""\d{1,2}:\d{2}""")

/**
 * 停靠点时间在数据里常是「Day 1 | 08:30 游览」，展示时只保留 HH:mm：
 * 天数和序号在列表/卡片里已经单独展示过，再拼一遍会重复并把整行挤到省略号里。
 */
fun routeTimeText(node: RouteNode): String {
    val raw = node.time.trim()
    if (raw.isEmpty()) {
        return ""
    }
    return ClockPattern.find(raw)?.value ?: raw
}

/** 点击停靠点后展示的简要信息卡。 */
@Composable
private fun RoutePoiCard(
    node: RouteNode,
    stopNumber: Int,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val meta = mutableListOf(stringResource(R.string.map_day, node.day))
    if (stopNumber > 0) {
        meta.add(stringResource(R.string.map_stop_number, stopNumber))
    }
    val shownTime = routeTimeText(node)
    if (shownTime.isNotBlank()) {
        meta.add(shownTime)
    }
    meta.add(node.category)
    if (node.rating.isNotBlank()) {
        meta.add(stringResource(R.string.map_rating, node.rating))
    }
    val detail = node.address.ifBlank { node.description }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = 0.96f),
        shadowElevation = 3.dp,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, top = 8.dp, end = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 3.dp)
                    .size(8.dp)
                    .background(dayColor(node.day), CircleShape),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 7.dp, end = 2.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = node.name,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = meta.joinToString(" · "),
                    fontSize = 12.sp,
                    color = Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (detail.isNotBlank()) {
                    Text(
                        text = detail,
                        fontSize = 12.sp,
                        color = Muted.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Clear,
                    contentDescription = stringResource(R.string.map_close),
                    tint = Muted,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

/** 在已绘制好的底图上叠加按天路线与当日顺序编号。 */
private fun DrawScope.drawRouteOverlay(
    nodes: List<RouteNode>,
    highlightDay: Int?,
    selectedKey: String?,
    showNames: Boolean,
    positions: Map<String, Offset>,
    project: (RouteNode) -> Offset,
) {
    val numberPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.WHITE
        textSize = 10.sp.toPx()
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    val namePaint = android.graphics.Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.rgb(27, 23, 18)
        textSize = 9.sp.toPx()
        setShadowLayer(3f, 0f, 1f, android.graphics.Color.WHITE)
    }

    nodes.groupBy { it.day }.toSortedMap().forEach { (day, dayNodes) ->
        val ordered = dayNodes.filter { it.hasCoordinate() }
        if (ordered.isEmpty()) {
            return@forEach
        }
        val color = dayColor(day)
        val dimmed = highlightDay != null && highlightDay != day
        val alpha = if (dimmed) 0.25f else 1f
        val points = ordered.map(project)

        if (points.size >= 2) {
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (index in 1 until points.size) {
                    val previous = points[index - 1]
                    val current = points[index]
                    // 弧度过渡让折线不出现生硬折角，视觉上更接近参考产品。
                    quadraticTo(
                        previous.x,
                        previous.y,
                        (previous.x + current.x) / 2f,
                        (previous.y + current.y) / 2f,
                    )
                    if (index == points.size - 1) {
                        lineTo(current.x, current.y)
                    }
                }
            }
            drawPath(
                path = path,
                color = color.copy(alpha = 0.22f * alpha),
                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round),
            )
            drawPath(
                path = path,
                color = color.copy(alpha = alpha),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
            )
        }

        points.forEachIndexed { index, point ->
            val node = ordered[index]
            // 散开后的落点只影响标记本身，折线依旧走真实坐标。
            val markerPoint = positions[node.routeKey()] ?: point
            val radius = 11.dp.toPx()
            val fill = color.copy(alpha = alpha)
            drawCircle(color = Color.White.copy(alpha = 0.9f * alpha), radius = radius + 1.5.dp.toPx(), center = markerPoint)
            drawCircle(color = fill, radius = radius, center = markerPoint)
            if (selectedKey != null && node.routeKey() == selectedKey) {
                // 选中态：白色内圈 + 当日色外圈，避免高亮把编号盖住。
                drawCircle(
                    color = Color.White.copy(alpha = 0.95f),
                    radius = radius + 4.dp.toPx(),
                    center = markerPoint,
                    style = Stroke(width = 3.dp.toPx()),
                )
                drawCircle(
                    color = color,
                    radius = radius + 2.dp.toPx(),
                    center = markerPoint,
                    style = Stroke(width = 1.5.dp.toPx()),
                )
            }
            val baseline = markerPoint.y + numberPaint.textSize / 3f
            drawContext.canvas.nativeCanvas.drawText(
                (index + 1).toString(),
                markerPoint.x,
                baseline,
                numberPaint,
            )
            // 散开过的点已经挤在一起，名称只会互相压住，故只给未散开的点画名称。
            if (!dimmed && showNames && markerPoint == point) {
                drawContext.canvas.nativeCanvas.drawText(
                    node.name,
                    markerPoint.x + radius + 4.dp.toPx(),
                    markerPoint.y - 2.dp.toPx(),
                    namePaint,
                )
            }
        }
    }
}

/**
 * 计算标记落点：坐标相近的多个停靠点会把编号完全盖住，
 * 这里把后放置的点按环形逐圈散开，保证每个编号都能看见。
 * 折线仍使用真实坐标，散开只影响标记本身。
 */
private fun arrangeMarkers(
    nodes: List<RouteNode>,
    project: (RouteNode) -> Offset,
    gap: Float,
): Map<String, Offset> {
    val placed = ArrayList<Offset>()
    val result = LinkedHashMap<String, Offset>()
    val angleStep = PI / 3.0
    for (node in nodes) {
        val origin = project(node)
        var point = origin
        var attempt = 0
        while (attempt < 36 && placed.any { (it - point).getDistance() < gap }) {
            val ring = attempt / 6 + 1
            val angle = (attempt % 6) * angleStep
            point = Offset(
                origin.x + (cos(angle) * gap * ring * 1.05).toFloat(),
                origin.y + (Math.sin(angle) * gap * ring * 1.05).toFloat(),
            )
            attempt++
        }
        placed.add(point)
        result[node.routeKey()] = point
    }
    return result
}

/** 路线节点在底图请求坐标系中的屏幕位置（未叠加手势缩放）。 */
private fun projectNode(
    node: RouteNode,
    zoom: Int,
    centerLng: Double,
    centerLat: Double,
    requestWidth: Int,
    requestHeight: Int,
    canvasWidth: Float,
    canvasHeight: Float,
): Offset {
    val worldSize = 256.0 * 2.0.pow(zoom.toDouble())
    val centerPoint = mercator(centerLng, centerLat, worldSize)
    val point = mercator(node.lng, node.lat, worldSize)
    val scaleX = canvasWidth / requestWidth
    val scaleY = canvasHeight / requestHeight
    // 上下留白不等造成的整体上移**已经体现在 center 里**（见 routeCenter）：
    // 这里不能再单独加 offsetY，否则叠加层相对底图整体偏移，标记与 POI 标签对不上。
    return Offset(
        (point.x - centerPoint.x) * scaleX + canvasWidth / 2f,
        (point.y - centerPoint.y) * scaleY + canvasHeight / 2f,
    )
}

/** 路线中心点，格式 lng,lat。 */
private fun routeCenter(nodes: List<RouteNode>, zoom: Int, requestWidth: Int): String {
    val minLng = nodes.minOf { it.lng }
    val maxLng = nodes.maxOf { it.lng }
    val minLat = nodes.minOf { it.lat }
    val maxLat = nodes.maxOf { it.lat }
    // 经纬度中点会让最南端停靠点离画面底部更近（墨卡托纵向非线性），
    // 这里取墨卡托 y 的中点再反解纬度，保证上下留白对称。
    val world = 256.0 * 2.0.pow(zoom.toDouble())
    // 上下留白不等（顶部 24 / 底部 84）：把**底图中心**向南移（墨卡托 y 变大），
    // 整条路线就会在画面里整体上移，底部图例不再压住最南端停靠点。
    // 偏移按"请求像素"计算（底图请求与叠加层投影共用这套坐标系，1 请求像素 = 1 世界像素），
    // 并且必须由底图中心和叠加层共用 —— 只移叠加层会造成标记与底图 POI 标签错位。
    val padScale = requestWidth / FitRefWidth
    val centerY = (mercatorY(maxLat, world) + mercatorY(minLat, world)) / 2.0 +
        (FitPadBottom - FitPadTop) / 2.0 * padScale
    val midLat = latFromMercatorY(centerY, world)
    return String.format(
        Locale.US,
        "%.6f,%.6f",
        (minLng + maxLng) / 2.0,
        midLat,
    )
}

/** 取能完整容纳全部坐标的最大缩放级别。 */
private fun fitZoom(nodes: List<RouteNode>, width: Int, height: Int): Int {
    // 底部图例会盖住最南端停靠点，故上下留白不等；留白随请求宽度等比缩放。
    val padScale = width / FitRefWidth
    val usableWidth = width - FitPadX * 2 * padScale
    val usableHeight = height - (FitPadTop + FitPadBottom) * padScale
    val minLng = nodes.minOf { it.lng }
    val maxLng = nodes.maxOf { it.lng }
    val minLat = nodes.minOf { it.lat }
    val maxLat = nodes.maxOf { it.lat }
    for (zoom in 17 downTo 3) {
        val worldSize = 256.0 * 2.0.pow(zoom.toDouble())
        val topLeft = mercator(minLng, maxLat, worldSize)
        val bottomRight = mercator(maxLng, minLat, worldSize)
        val spanX = abs(bottomRight.x - topLeft.x)
        val spanY = abs(bottomRight.y - topLeft.y)
        if (spanX <= usableWidth && spanY <= usableHeight) {
            return zoom
        }
    }
    return 3
}

/** Web 墨卡托投影，返回世界像素坐标。 */
private fun mercator(lng: Double, lat: Double, worldSize: Double): Offset {
    return Offset(
        ((lng + 180.0) / 360.0 * worldSize).toFloat(),
        mercatorY(lat, worldSize).toFloat(),
    )
}

/** Web 墨卡托纵向世界像素。 */
private fun mercatorY(lat: Double, worldSize: Double): Double {
    val clampedLat = lat.coerceIn(-85.0511, 85.0511)
    val rad = Math.toRadians(clampedLat)
    return (1.0 - ln(tan(rad) + 1.0 / cos(rad)) / PI) / 2.0 * worldSize
}

/** 由墨卡托纵向世界像素反解纬度。 */
private fun latFromMercatorY(y: Double, worldSize: Double): Double {
    val n = PI * (1.0 - 2.0 * y / worldSize)
    val sinh = (Math.exp(n) - Math.exp(-n)) / 2.0
    return Math.toDegrees(Math.atan(sinh))
}

/** 停靠点在图上的稳定标识：天数 + 时间 + 名称 + 坐标。 */
private fun RouteNode.routeKey(): String = "$day|$time|$name|$lng,$lat"

/** 两停靠点之间的球面距离（米）。 */
fun routeLegMeters(from: RouteNode, to: RouteNode): Double {
    if (!from.hasCoordinate() || !to.hasCoordinate()) {
        return 0.0
    }
    val earthRadius = 6371000.0
    val lat1 = Math.toRadians(from.lat)
    val lat2 = Math.toRadians(to.lat)
    val deltaLat = lat2 - lat1
    val deltaLng = Math.toRadians(to.lng - from.lng)
    val a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
        Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2)
    return 2 * earthRadius * Math.asin(Math.sqrt(a.coerceIn(0.0, 1.0)))
}

/** 路段展示文案：距离 + 步行耗时估算，明确标注为估算值。 */
fun routeLegText(context: Context, from: RouteNode, to: RouteNode): String {
    val meters = routeLegMeters(from, to) * 1.25
    if (meters <= 0.0) {
        return context.getString(R.string.map_coord_missing)
    }
    val distance = if (meters >= 1000) {
        String.format(Locale.US, context.getString(R.string.map_distance_km), meters / 1000.0)
    } else {
        String.format(Locale.US, context.getString(R.string.map_distance_m), meters)
    }
    val minutes = max(2, (meters / 1000.0 / 4.5 * 60).roundToInt())
    return context.getString(R.string.map_leg_text, distance, minutes)
}
