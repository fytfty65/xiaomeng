package com.omniroute.app.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * OmniRoute 系统图标，与 Web 端 `app/icon.svg` 同一份造型：
 * 墨蓝圆角底 → 暖色 300° 航路环 → 右上角青色航点。
 * 环的缺口交给航点收尾，读作「一条走到站的路线」。
 *
 * 所有尺寸按 48×48 设计稿等比缩放，保证 16dp~48dp 不变形。
 */
@Composable
fun OmniLogoMark(size: Dp = 40.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.width / 48f
        val corner = CornerRadius(12f * s)
        val tile = Rect(Offset.Zero, Size(this.size.width, this.size.height))

        // 1. 墨蓝底：左上微亮压到右下近黑，给圆角矩形一点纵深
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF16283F), Color(0xFF0D1827), Color(0xFF080F1A)),
                start = Offset.Zero,
                end = Offset(this.size.width, this.size.height),
            ),
            cornerRadius = corner,
        )

        clipPath(Path().apply { addRoundRect(RoundRect(tile, corner)) }) {
            // 2. 左上暖光 / 右下冷光：品牌双色的余韵，不抢主体
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x42FF8E53),
                        Color(0x0DFF8E53),
                        Color(0x00FF8E53),
                    ),
                    center = Offset(9.6f * s, 4.8f * s),
                    radius = 38.4f * s,
                ),
                radius = 38.4f * s,
                center = Offset(9.6f * s, 4.8f * s),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x334FACFE), Color(0x004FACFE)),
                    center = Offset(44.6f * s, 45.6f * s),
                    radius = 36f * s,
                ),
                radius = 36f * s,
                center = Offset(44.6f * s, 45.6f * s),
            )
        }

        // 3. 内描边模拟玻璃高光
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0x38FFFFFF),
                    Color(0x12FFFFFF),
                    Color(0x08FFFFFF),
                ),
            ),
            topLeft = Offset(0.55f * s, 0.55f * s),
            size = Size(46.9f * s, 46.9f * s),
            cornerRadius = CornerRadius(11.45f * s),
            style = Stroke(width = 1.1f * s),
        )

        // 4. 航路环：300° 圆环，缺口留在右上角，交给青色航点收尾
        val ringTopLeft = Offset(11.7f * s, 11.7f * s)
        val ringSize = Size(24.6f * s, 24.6f * s)
        val ringBrush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFFFDDB6),
                Color(0xFFFFB271),
                Color(0xFFFF8A5C),
                Color(0xFFF76A63),
                Color(0xFFE85B6B),
            ),
            start = Offset(6f * s, 6f * s),
            end = Offset(44f * s, 46f * s),
        )
        // 环外侧的暖色辉光，替代 SVG 里的高斯模糊
        drawArc(
            color = Color(0x29FF8E53),
            startAngle = -15f,
            sweepAngle = 300f,
            useCenter = false,
            topLeft = ringTopLeft,
            size = ringSize,
            style = Stroke(width = 9.2f * s, cap = StrokeCap.Round),
        )
        // 落地投影，让圆环从底板上浮起来
        drawArc(
            color = Color(0x4D000000),
            startAngle = -15f,
            sweepAngle = 300f,
            useCenter = false,
            topLeft = Offset(ringTopLeft.x, ringTopLeft.y + 1f * s),
            size = ringSize,
            style = Stroke(width = 6.4f * s, cap = StrokeCap.Round),
        )
        drawArc(
            brush = ringBrush,
            startAngle = -15f,
            sweepAngle = 300f,
            useCenter = false,
            topLeft = ringTopLeft,
            size = ringSize,
            style = Stroke(width = 6.2f * s, cap = StrokeCap.Round),
        )

        // 5. 航点：青色宝石，用一圈淡光困住，避免在 16dp 下糊成一团
        val waypoint = Offset(32.7f * s, 15.3f * s)
        drawCircle(color = Color(0x475FE0EF), radius = 4.3f * s, center = waypoint)
        drawCircle(color = Color(0xFF62E3EF), radius = 3.3f * s, center = waypoint)
        drawCircle(
            color = Color(0xE0FFFFFF),
            radius = 1f * s,
            center = Offset(31.55f * s, 14.15f * s),
        )
    }
}
