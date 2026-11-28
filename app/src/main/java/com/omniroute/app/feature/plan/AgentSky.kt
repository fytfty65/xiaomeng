package com.omniroute.app.feature.plan

import androidx.compose.animation.core.*

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniroute.app.core.ui.theme.*

/** Tracks the latest reported stage; keep the planning hub active for general route events. */
@Composable
internal fun AgentSky(stage: String) {
    val style = LocalJourneySeason.current.world
    val labels = listOf("路线规划", "实时气象", "风味寻访", "预算平衡")
    val focus = when {
        listOf("天气", "气象", "降水").any(stage::contains) -> 1
        listOf("餐饮", "美食", "风味").any(stage::contains) -> 2
        listOf("预算", "费用", "成本").any(stage::contains) -> 3
        else -> 0
    }
    val anchors = listOf(Offset(.49f,.49f), Offset(.79f,.18f), Offset(.77f,.72f), Offset(.17f,.32f))
    val phase = rememberInfiniteTransition(label = "agents").animateFloat(0f, 1f,
        infiniteRepeatable(tween(3600, easing = LinearEasing)), label = "signal")
    BoxWithConstraints(Modifier.fillMaxWidth().height(250.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val points = anchors.map { Offset(it.x * size.width, it.y * size.height) }
            val a = points[focus]
            points.forEachIndexed { i, b ->
                if (i != focus) {
                    val ctrl = Offset((a.x + b.x) / 2, (a.y + b.y) / 2 - 22.dp.toPx())
                    drawPath(Path().apply { moveTo(a.x, a.y); quadraticTo(ctrl.x, ctrl.y, b.x, b.y) },
                        style.light.copy(alpha = .55f), style = Stroke(1.dp.toPx()))
                    val t = (phase.value + .23f * i) % 1f
                    val u = 1 - t
                    drawCircle(style.light, 2.dp.toPx(), a * (u*u) + ctrl * (2*u*t) + b * (t*t))
                }
            }
            points.forEachIndexed { index, p ->
                if (focus == index) drawCircle(style.accent.copy(alpha = .35f), (11 + 5 * phase.value).dp.toPx(), p)
                drawCircle(style.shadow, 6.dp.toPx(), p)
                drawCircle(style.light, 3.dp.toPx(), p)
            }
        }
        anchors.forEachIndexed { i, p ->
            Box(Modifier.offset(x = maxWidth * p.x - 43.dp, y = maxHeight * p.y + 10.dp).width(86.dp),
                contentAlignment = Alignment.Center) {
                Text(labels[i], Modifier.background(if (i == focus) style.light else style.glass, CircleShape)
                    .padding(horizontal = 9.dp, vertical = 4.dp), color = if (i == focus) style.onLight else style.ink, fontSize = 12.sp)
            }
        }
    }
}
