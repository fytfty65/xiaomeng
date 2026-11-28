package com.omniroute.app.feature.trip

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniroute.app.core.ui.theme.*

@Composable
internal fun RouteSky(nodes: List<JourneyNodeUi>, selected: String?, onSelect: (String) -> Unit) {
    if (nodes.isEmpty()) return
    val style = LocalJourneySeason.current.world
    val scroll = rememberScrollState()
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val viewport = maxWidth
        val width = maxOf(viewport, 100.dp * nodes.size)
        val height = 216.dp
        val heights = listOf(.60f, .22f, .47f, .69f)
        Box(Modifier.horizontalScroll(scroll)) {
          Box(Modifier.width(width).height(height)) {
            Canvas(Modifier.fillMaxSize()) {
                val points = nodes.indices.map { i -> Offset(size.width * (i + .5f) / nodes.size, size.height * heights[i % 4]) }
                val trail = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    points.zipWithNext().forEach { (a, b) ->
                        val mid = (a.x + b.x) / 2
                        cubicTo(mid, a.y, mid, b.y, b.x, b.y)
                    }
                }
                drawPath(trail, style.shadow.copy(alpha = .65f), style = Stroke(3.dp.toPx()))
                drawPath(trail, style.light, style = Stroke(1.dp.toPx()))
                points.forEachIndexed { i, point ->
                    if (nodes[i].key == selected) drawCircle(style.accent.copy(alpha = .5f), 13.dp.toPx(), point)
                    drawCircle(style.shadow, 7.dp.toPx(), point)
                    drawCircle(style.light, 4.dp.toPx(), point)
                }
            }
            nodes.forEachIndexed { i, node ->
                val active = node.key == selected
                Box(Modifier.offset(x = width * i / nodes.size, y = height * heights[i % 4] - 10.dp)
                    .width(width / nodes.size).heightIn(min = 50.dp).selectable(active, role = Role.RadioButton) { onSelect(node.key) }
                    .padding(top = 21.dp), contentAlignment = Alignment.TopCenter) {
                    Text((if (node.status == "visited") "✓ " else "%02d ".format(i + 1)) + node.name,
                        Modifier.background(if (active) style.light else style.glass, CircleShape).padding(horizontal = 8.dp, vertical = 4.dp),
                        color = if (active) style.onLight else style.ink, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
  }
}
