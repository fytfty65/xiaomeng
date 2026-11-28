package com.omniroute.app.core.ui.theme

import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.composed
import com.omniroute.app.R

internal data class WorldStyle(val illustration: Int, val frost: Int, val glass: Color, val shadow: Color,
    val light: Color, val onLight: Color, val ink: Color, val secondary: Color, val accent: Color)

private val worlds = mapOf(
    JourneySeason.Spring to WorldStyle(R.drawable.world_spring, R.drawable.world_frost_spring, Color(0xD41D3029), Color(0xFF182923),
        Color(0xFFF9EAE1), Color(0xFF1A2B24), Color(0xFFF8F7F4), Color(0xFFB8CCC3), Color(0xFFE892A2)),
    JourneySeason.Summer to WorldStyle(R.drawable.world_summer, R.drawable.world_frost_summer, Color(0xD4102A3B), Color(0xFF0D2333),
        Color(0xFFEBF8FA), Color(0xFF0E2738), Color(0xFFF5FAFC), Color(0xFFA9CAD9), Color(0xFF58D1E6)),
    JourneySeason.Autumn to WorldStyle(R.drawable.world_autumn, R.drawable.world_frost_autumn, Color(0xD62B1E18), Color(0xFF241813),
        Color(0xFFF6DDB6), Color(0xFF291B15), Color(0xFFFAF5EE), Color(0xFFD4BEB0), Color(0xFFE38549)),
    JourneySeason.Winter to WorldStyle(R.drawable.world_winter, R.drawable.world_frost_winter, Color(0xD61D2036), Color(0xFF15172B),
        Color(0xFFFAE9C8), Color(0xFF1A1B30), Color(0xFFF5F6FA), Color(0xFFB6BBD6), Color(0xFF8F7BE0)),
)
internal val JourneySeason.world get() = worlds.getValue(this)

@Composable
internal fun SeasonWorldImage(season: JourneySeason, modifier: Modifier = Modifier) {
    Image(painterResource(season.world.illustration), null, modifier, contentScale = ContentScale.Crop)
}

// Cache the blurred landscape once; each glass panel samples the matching area as it scrolls.
private data class WorldFrame(val origin: Offset, val size: IntSize, val frost: ImageBitmap, val pan: () -> Float)
private val LocalFrame = compositionLocalOf<WorldFrame?> { null }

@Composable
internal fun SeasonWorldBackdrop(season: JourneySeason, scenery: (@Composable () -> Unit)? = null, content: @Composable () -> Unit) {
    val style = season.world
    var distance by remember(season) { mutableFloatStateOf(0f) }
    val range = with(androidx.compose.ui.platform.LocalDensity.current) { 180.dp.toPx() }
    val scroll = remember(range, season) { object : NestedScrollConnection {
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            distance = (distance - consumed.y).coerceIn(0f, range)
            return Offset.Zero
        }
    } }
    var origin by remember { mutableStateOf(Offset.Zero) }
    var bounds by remember { mutableStateOf(IntSize.Zero) }
    val frost = ImageBitmap.imageResource(if (scenery == null) style.frost else season.portalArt.frost)
    val frame = remember(origin, bounds, frost, range) { WorldFrame(origin, bounds, frost) { distance / range } }
    Box(Modifier.fillMaxSize().clip(androidx.compose.ui.graphics.RectangleShape).background(style.shadow).nestedScroll(scroll)
        .onGloballyPositioned { origin = it.positionInWindow(); bounds = it.size }) {
        if (scenery != null) scenery() else SeasonWorldImage(season, Modifier.matchParentSize().graphicsLayer {
            scaleX = 1.035f
            scaleY = 1.035f
            translationY = -size.height * .014f * frame.pan()
        })
        Spacer(Modifier.matchParentSize().drawBehind {
            val extra = distance / range * 0.13f
            drawRect(Brush.verticalGradient(0f to style.shadow.copy(alpha = (if (scenery != null && season == JourneySeason.Spring) .035f else .55f) + extra),
                .22f to style.shadow.copy(alpha = .23f + extra), .52f to style.shadow.copy(alpha = .72f + extra),
                1f to style.shadow.copy(alpha = .85f + extra)))
        })
        CompositionLocalProvider(LocalFrame provides frame) { content() }
    }
}

internal fun Modifier.spatialIsland(radius: Dp = 26.dp): Modifier = composed {
    val style = LocalJourneySeason.current.world
    val shape = RoundedCornerShape(radius)
    val frame = LocalFrame.current
    var origin by remember { mutableStateOf(Offset.Zero) }
    shadow(8.dp, shape, clip = false).clip(shape).onGloballyPositioned { origin = it.positionInWindow() }
        .drawBehind {
            if (frame != null && frame.size.width > 0) {
                val img = frame.frost
                val scale = maxOf(frame.size.width.toFloat() / img.width, frame.size.height.toFloat() / img.height) * 1.035f
                val dst = IntSize((img.width * scale).roundToInt(), (img.height * scale).roundToInt())
                val x = frame.origin.x - origin.x + (frame.size.width - dst.width) / 2f
                val y = frame.origin.y - origin.y + (frame.size.height - dst.height) / 2f - frame.size.height * .014f * frame.pan()
                drawImage(img, dstOffset = IntOffset(x.roundToInt(), y.roundToInt()), dstSize = dst)
            }
            drawRect(style.glass)
        }
        .border(0.8.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = .29f),
            Color.White.copy(alpha = .055f))), shape)
}
