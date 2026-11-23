package com.omniroute.app.core.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import com.omniroute.app.core.ui.ContentMaxWidth
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.omniroute.app.R

internal data class SeasonEdition(
    val accent: Color,
    val secondary: Color,
    val light: Color,
    val deep: Color,
    val paper: Color,
    val image: Int,
    val portrait: Int,
    val titleFont: FontFamily,
    val titleWeight: FontWeight,
) {
    val actionGradient: Brush get() = Brush.horizontalGradient(listOf(deep, lerp(deep, accent, 0.32f)))
}

private val editionStyles = JourneySeason.entries.associateWith { season ->
    when (season) {
        JourneySeason.Spring -> SeasonEdition(
            Color(0xFFD46A84), Color(0xFF5E9E88), Color(0xFFE88DA4), Color(0xFF973B57),
            Color(0xFFFBF8F9), R.drawable.pass_window_spring, R.drawable.pass_background_spring,
            FontFamily.Serif, FontWeight.Medium,
        )
        JourneySeason.Summer -> SeasonEdition(
            Color(0xFF0E86A8), Color(0xFFFF8552), Color(0xFF22B2D4), Color(0xFF075B77),
            Color(0xFFF2F9FC), R.drawable.pass_window_summer, R.drawable.pass_background_summer,
            FontFamily.SansSerif, FontWeight.Bold,
        )
        JourneySeason.Autumn -> SeasonEdition(
            Color(0xFFB85D3B), Color(0xFFD99B43), Color(0xFFD4724A), Color(0xFF873A20),
            Color(0xFFFAF5EE), R.drawable.pass_window_autumn, R.drawable.pass_background_autumn,
            FontFamily.Serif, FontWeight.SemiBold,
        )
        JourneySeason.Winter -> SeasonEdition(
            Color(0xFF6B53A6), Color(0xFF88A6DB), Color(0xFF8268C4), Color(0xFF533B8F),
            Color(0xFFF6F5FC), R.drawable.pass_window_winter, R.drawable.pass_background_winter,
            FontFamily.SansSerif, FontWeight.Light,
        )
    }

}

internal val JourneySeason.edition: SeasonEdition get() = editionStyles.getValue(this)

@Composable
internal fun SeasonEditionTheme(season: JourneySeason, content: @Composable () -> Unit) {
    val style = season.edition
    val dark = isSystemInDarkTheme()
    val base = season.palette(dark)
    val tintedPaper = if (dark) lerp(base.background, style.deep, 0.15f) else style.paper
    val surface = if (dark) lerp(base.surface, style.deep, 0.14f) else Color(0xFFFFFEFC)
    val ink = if (dark) base.ink else lerp(Color(0xFF211E20), style.deep, 0.12f)
    val accent = if (dark) lerp(style.light, Color.White, 0.55f) else style.deep
    val palette = base.copy(
        brand = accent,
        brandDark = accent,
        brandLight = style.light,
        ink = ink,
        background = tintedPaper,
        surface = surface,
        fieldBg = lerp(tintedPaper, style.accent, if (dark) 0.13f else 0.065f),
        border = lerp(surface, style.accent, if (dark) 0.28f else 0.16f),
        divider = lerp(tintedPaper, style.accent, 0.14f),
        tagBg = lerp(surface, style.accent, 0.10f),
        brandTint = lerp(tintedPaper, style.accent, if (dark) 0.24f else 0.14f),
    )
    CompositionLocalProvider(LocalJourneySeason provides season, LocalAppPalette provides palette) {
        MaterialTheme(
            colorScheme = MaterialTheme.colorScheme.copy(
                primary = palette.brand,
                onPrimary = palette.onBrand,
                primaryContainer = palette.brandTint,
                onPrimaryContainer = palette.ink,
                background = palette.background,
                surface = palette.surface,
                onSurface = palette.ink,
                surfaceVariant = palette.fieldBg,
                onSurfaceVariant = palette.muted,
                outline = palette.border,
            ),
        ) {
            Box(Modifier.fillMaxSize().background(palette.background), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.widthIn(max = ContentMaxWidth).fillMaxSize()) { content() }
            }
        }
    }
}

internal fun Modifier.seasonBackdrop(): Modifier = composed {
    val palette = LocalAppPalette.current
    val season = LocalJourneySeason.current
    val style = season.edition
    val atmosphere = rememberInfiniteTransition(label = "edition atmosphere")
    val drift by atmosphere.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(18_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "edition drift",
    )
    drawWithCache {
        val warm = Brush.radialGradient(
            listOf(style.accent.copy(alpha = 0.18f), Color.Transparent),
            center = Offset(size.width * 0.04f, size.height * 0.10f),
            radius = size.width.coerceAtLeast(1f) * 0.86f,
        )
        val cool = Brush.radialGradient(
            listOf(style.secondary.copy(alpha = 0.17f), Color.Transparent),
            center = Offset(size.width * 0.91f, size.height * 0.22f),
            radius = size.width.coerceAtLeast(1f) * 0.76f,
        )
        val marks = List(15) { index ->
            Offset(
                size.width * ((index * 47 + 11) % 100) / 100f,
                size.height * ((index * 29 + 7) % 70) / 100f,
            )
        }
        val radius = 2.dp.toPx()
        onDrawBehind {
            drawRect(palette.background)
            drawRect(warm)
            drawRect(cool)
            marks.forEachIndexed { index, anchor ->
                val y = if (season == JourneySeason.Autumn) 0f else drift * (14.dp.toPx() + index)
                val point = anchor + Offset(y * 0.45f, y)
                val ink = if (index % 2 == 0) style.accent else style.secondary
                when (season) {
                    JourneySeason.Spring -> drawOval(
                        ink.copy(alpha = 0.15f), point,
                        androidx.compose.ui.geometry.Size(radius, radius * 2.2f),
                    )
                    JourneySeason.Summer -> drawArc(
                        ink.copy(alpha = 0.12f), 10f, 156f, false, point,
                        androidx.compose.ui.geometry.Size(radius * 7f, radius * 2f), style = Stroke(radius * 0.3f),
                    )
                    JourneySeason.Autumn -> drawCircle(ink.copy(alpha = 0.13f), radius * 0.4f, point)
                    JourneySeason.Winter -> drawCircle(ink.copy(alpha = 0.13f), radius * (index % 3 + 1), point)
                }
            }
        }
    }
}

internal val EditionSurface: Color
    @Composable get() = LocalAppPalette.current.surface.copy(alpha = 0.86f)

@Composable
internal fun OmniSeasonLogo(
    modifier: Modifier = Modifier,
    season: JourneySeason = LocalJourneySeason.current,
    diameter: Dp = 42.dp,
    tint: Color = Color.Unspecified,
) {
    val style = season.edition
    val line = if (tint == Color.Unspecified) style.deep else tint
    val symbol = if (tint == Color.Unspecified) style.accent else tint
    Canvas(modifier.size(diameter)) {
        val unit = size.minDimension / 40
        fun p(x: Float, y: Float) = Offset(x * unit, y * unit)
        drawArc(
            brush = Brush.linearGradient(listOf(symbol, line), p(2f, 2f), p(37f, 38f)),
            startAngle = -19f,
            sweepAngle = 304f,
            useCenter = false,
            topLeft = p(5f, 5f),
            size = androidx.compose.ui.geometry.Size(30 * unit, 30 * unit),
            style = Stroke(width = 3 * unit, cap = StrokeCap.Round),
        )
        when (season) {
            JourneySeason.Spring -> {
                drawPath(Path().apply {
                    moveTo(24 * unit, 14 * unit)
                    cubicTo(25 * unit, 3 * unit, 35 * unit, 2 * unit, 37 * unit, 8 * unit)
                    cubicTo(37 * unit, 13 * unit, 30 * unit, 16 * unit, 24 * unit, 14 * unit)
                    close()
                }, symbol.copy(alpha = 0.85f))
                drawCircle(line, radius = unit * 2, center = p(18f, 22f))
            }
            JourneySeason.Summer -> {
                drawPath(Path().apply {
                    moveTo(9 * unit, 24 * unit)
                    cubicTo(16 * unit, 24 * unit, 18 * unit, 13 * unit, 25 * unit, 17 * unit)
                    quadraticTo(31 * unit, 20 * unit, 28 * unit, 24 * unit)
                }, line, style = Stroke(2 * unit, cap = StrokeCap.Round))
                drawLine(symbol, p(31f, 3f), p(31f, 13f), 1.6f * unit, StrokeCap.Round)
                drawLine(symbol, p(26f, 8f), p(36f, 8f), 1.6f * unit, StrokeCap.Round)
            }
            JourneySeason.Autumn -> {
                drawPath(Path().apply {
                    moveTo(22 * unit, 16 * unit)
                    lineTo(25 * unit, 8 * unit)
                    lineTo(29 * unit, 9 * unit)
                    lineTo(32 * unit, 4 * unit)
                    lineTo(37 * unit, 11 * unit)
                    lineTo(33 * unit, 16 * unit)
                    close()
                }, symbol)
                drawLine(line, p(17f, 26f), p(23f, 18f), 2 * unit, StrokeCap.Round)
            }
            JourneySeason.Winter -> {
                for (axis in 0..5) {
                    val angle = axis * kotlin.math.PI / 3
                    val tip = p(20f + 9f * kotlin.math.cos(angle).toFloat(), 20f + 9f * kotlin.math.sin(angle).toFloat())
                    drawLine(symbol, p(20f, 20f), tip, 1.6f * unit, StrokeCap.Round)
                }
                drawCircle(symbol, 2.2f * unit, p(32f, 8f))
            }
        }
    }
}
