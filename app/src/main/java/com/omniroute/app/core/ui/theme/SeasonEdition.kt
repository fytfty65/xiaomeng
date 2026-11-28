package com.omniroute.app.core.ui.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.Font
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import com.omniroute.app.core.ui.ContentMaxWidth
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

internal val FolioSerif = FontFamily(Font(R.font.folio_serif, FontWeight.Medium))
internal val FolioDigits = FontFamily(Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold))

internal data class SeasonEdition(
    val accent: Color,
    val secondary: Color,
    val glow: Color,
    val light: Color,
    val deep: Color,
    val anchor: Color,
    val paper: Color,
    val image: Int,
    val portrait: Int,
    val titleFont: FontFamily,
    val titleWeight: FontWeight,
) {
    val actionGradient: Brush get() = Brush.linearGradient(listOf(deep, accent))
}

private val editionStyles = JourneySeason.entries.associateWith { season ->
    when (season) {
        JourneySeason.Spring -> SeasonEdition(
            Color(0xFFD95B7A), Color(0xFF8CE0C8), Color(0xFFFF9EB8), Color(0xFFF06A8A), Color(0xFFAC3857),
            Color(0xFF2B332E), Color(0xFFFAF8F6), R.drawable.pass_window_spring, R.drawable.pass_background_spring,
            FolioSerif, FontWeight.Medium,
        )
        JourneySeason.Summer -> SeasonEdition(
            Color(0xFF0088B2), Color(0xFFFFB978), Color(0xFF52C7EA), Color(0xFF1BA9CE), Color(0xFF0A6C8F),
            Color(0xFF102A3A), Color(0xFFF7F9FA), R.drawable.pass_window_summer, R.drawable.pass_background_summer,
            FolioSerif, FontWeight.Medium,
        )
        JourneySeason.Autumn -> SeasonEdition(
            Color(0xFFC2562D), Color(0xFFEBC773), Color(0xFFF29F67), Color(0xFFD96F43), Color(0xFFA33F1E),
            Color(0xFF2F221C), Color(0xFFF8F4EE), R.drawable.pass_window_autumn, R.drawable.pass_background_autumn,
            FolioSerif, FontWeight.Medium,
        )
        JourneySeason.Winter -> SeasonEdition(
            Color(0xFF5B48A8), Color(0xFF7AB8F5), Color(0xFF9683EC), Color(0xFF7F66C9), Color(0xFF4C358F),
            Color(0xFF1C1B2E), Color(0xFFF6F7FA), R.drawable.pass_window_winter, R.drawable.pass_background_winter,
            FolioSerif, FontWeight.Medium,
        )
    }

}

internal val JourneySeason.edition: SeasonEdition get() = editionStyles.getValue(this)

@Composable
internal fun SeasonEditionTheme(season: JourneySeason, content: @Composable () -> Unit) {
    val world = season.world
    val tile = androidx.compose.ui.graphics.lerp(world.shadow, Color.White, 0.105f)
    val palette = DarkAppPalette.copy(
        brand = world.light, brandDark = world.light, brandLight = world.accent, onBrand = world.onLight,
        ink = world.ink, muted = world.secondary, background = world.shadow, surface = world.glass,
        fieldBg = tile, tabTrack = tile, border = world.secondary.copy(alpha = .32f),
        divider = world.secondary.copy(alpha = .20f), tagBg = tile, brandTint = tile,
    )
    com.omniroute.app.core.ui.SystemBarIcons(false, false)
    CompositionLocalProvider(LocalJourneySeason provides season, LocalAppPalette provides palette) {
        MaterialTheme(
            colorScheme = androidx.compose.material3.darkColorScheme(
                primary = palette.brand,
                onPrimary = palette.onBrand,
                primaryContainer = palette.brandTint,
                onPrimaryContainer = palette.ink,
                background = palette.background,
                surface = world.glass.copy(alpha = 1f),
                onSurface = palette.ink,
                surfaceVariant = palette.fieldBg,
                onSurfaceVariant = palette.muted,
                outline = palette.border,
            ),
        ) {
            SeasonWorldBackdrop(season) {
              Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.widthIn(max = ContentMaxWidth).fillMaxSize()) { content() }
              }
            }
        }
    }
}

internal val EditionSurface: Color
    @Composable get() = LocalAppPalette.current.surface

internal fun Modifier.editionRim(shape: Shape = RoundedCornerShape(12.dp)): Modifier = composed {
    border(0.7.dp, LocalAppPalette.current.ink.copy(alpha = 0.10f), shape)
}

internal fun Modifier.inkFoil(): Modifier = composed {
    val style = LocalJourneySeason.current.edition
    drawWithCache {
        val ground = Brush.linearGradient(listOf(lerp(style.anchor, style.secondary, 0.10f), style.anchor))
        val step = 5.dp.toPx()
        onDrawBehind {
            drawRect(ground)
            var x = -size.height
            while (x < size.width) {
                drawLine(Color.White.copy(alpha = 0.025f), Offset(x, size.height), Offset(x + size.height, 0f), 0.6.dp.toPx())
                x += step
            }
        }
    }
}

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
                }, symbol.copy(alpha = symbol.alpha * 0.85f))
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
