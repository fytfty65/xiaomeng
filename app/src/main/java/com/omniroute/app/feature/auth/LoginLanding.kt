package com.omniroute.app.feature.auth

import android.animation.ValueAnimator
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.omniroute.app.R
import com.omniroute.app.core.ui.components.OmniLogoMark
import com.omniroute.app.core.ui.theme.JourneySeason
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private val CoverIvory = Color(0xFFFFFBF1)
private val CoverShade = Brush.verticalGradient(
    0f to Color(0xAB183440),
    .28f to Color.Transparent,
    .52f to Color.Transparent,
    1f to Color(0xF0152A28),
)

@Composable
internal fun LoginLanding(
    season: JourneySeason,
    ticketOpen: Boolean,
    onSeasonChange: (JourneySeason) -> Unit,
    onOpen: (register: Boolean) -> Unit,
    ticket: @Composable BoxScope.() -> Unit,
) {
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val motion = ValueAnimator.areAnimatorsEnabled()
    val active = lifecycle == Lifecycle.State.RESUMED && motion && !ticketOpen
    var autoPlay by rememberSaveable { mutableStateOf(true) }
    val selectSeason by rememberUpdatedState(onSeasonChange)

    LaunchedEffect(season, active, autoPlay) {
        if (active && autoPlay) {
            delay(9_000L)
            selectSeason(JourneySeason.entries[(season.ordinal + 1) % JourneySeason.entries.size])
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF20343A)).clipToBounds()) {
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer {
                    if (ticketOpen) {
                        scaleX = 1.025f
                        scaleY = 1.025f
                    }
                }
                .blur(if (!ticketOpen) 0.dp else if (season == JourneySeason.Summer) 6.dp else 4.dp),
        ) {
            Crossfade(
                targetState = if (ticketOpen) season.passStyle().ticketBackground else season.passStyle().cover,
                animationSpec = tween(if (!motion) 0 else if (ticketOpen) 420 else 1000),
                label = "season-art",
            ) { artwork ->
                Image(
                    painterResource(artwork),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (active) CoverWeather(season)
        }
        if (ticketOpen) {
            ticket()
        } else {
            Box(Modifier.matchParentSize().background(CoverShade))
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CoverHeader(
                    season = season,
                    autoPlay = autoPlay,
                    motion = motion,
                    onSeason = { autoPlay = false; onSeasonChange(it) },
                    onToggle = { autoPlay = !autoPlay },
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 12.dp)) {
                    Text(
                        stringResource(R.string.auth_hero_title),
                        fontFamily = FontFamily.Serif,
                        color = CoverIvory,
                        fontSize = 29.sp,
                        lineHeight = 45.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(22.dp))
                    Button(
                        onClick = { onOpen(true) },
                        modifier = Modifier.widthIn(min = 205.dp).heightIn(min = 52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CoverIvory,
                            contentColor = Color(0xFF21382D),
                        ),
                    ) {
                        Text(stringResource(R.string.pass_begin), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(onClick = { onOpen(false) }) {
                        Text(stringResource(R.string.pass_returning), color = CoverIvory)
                    }
                }
            }
        }
    }
}

@Composable
private fun CoverHeader(
    season: JourneySeason,
    autoPlay: Boolean,
    motion: Boolean,
    onSeason: (JourneySeason) -> Unit,
    onToggle: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OmniLogoMark(size = 29.dp)
        Spacer(Modifier.width(6.dp))
        Text(
            stringResource(R.string.app_name),
            modifier = Modifier.weight(1f),
            color = CoverIvory,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Row(
            modifier = Modifier.clip(CircleShape).background(Color(0x54324D53)).selectableGroup(),
        ) {
            JourneySeason.entries.forEach { choice ->
                val chosen = season == choice
                Box(
                    Modifier
                        .width(32.dp)
                        .height(44.dp)
                        .selectable(chosen, role = Role.Tab) { onSeason(choice) },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.size(28.dp).background(if (chosen) CoverIvory else Color.Transparent, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(choice.labelRes),
                            color = if (chosen) Color(0xFF21382D) else CoverIvory,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }
        if (motion) {
            val description = stringResource(if (autoPlay) R.string.auth_pause_seasons else R.string.auth_play_seasons)
            IconButton(
                onClick = onToggle,
                modifier = Modifier.size(44.dp).semantics { contentDescription = description },
            ) {
                if (autoPlay) {
                    Canvas(Modifier.size(11.dp)) {
                        val width = 2.dp.toPx()
                        drawRect(CoverIvory, size = Size(width, size.height))
                        drawRect(CoverIvory, topLeft = Offset(size.width - width, 0f), size = Size(width, size.height))
                    }
                } else {
                    Icon(Icons.Filled.PlayArrow, null, tint = CoverIvory, modifier = Modifier.size(19.dp))
                }
            }
        }
    }
}

private data class WeatherParticle(val x: Float, val y: Float, val size: Float, val speed: Int)

@Composable
private fun CoverWeather(season: JourneySeason) {
    val particles = remember(season) {
        val random = Random(17 + season.ordinal)
        List(if (season == JourneySeason.Winter) 31 else 16) {
            WeatherParticle(random.nextFloat(), random.nextFloat(), random.nextFloat(), random.nextInt(1, 3))
        }
    }
    val clock = rememberInfiniteTransition(label = "cover-weather")
    val travel by clock.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(19_000, easing = LinearEasing)),
        label = "weather-travel",
    )
    Canvas(Modifier.fillMaxSize()) {
        if (season != JourneySeason.Winter) {
            // Map the falls' coordinates in the original 512×768 image through ContentScale.Crop.
            val scale = maxOf(size.width / 512f, size.height / 768f)
            val origin = Offset((size.width - 512f * scale) / 2f, (size.height - 768f * scale) / 2f)
            val flowAlpha = if (season == JourneySeason.Summer) .48f else .24f
            repeat(11) { i ->
                val phase = (travel * 18f + i * .137f) % 1f
                val y = 403f + phase * 117f
                val x = 109f + i * 3.25f + phase * 19f
                val from = origin + Offset(x * scale, y * scale)
                val end = from + Offset((3.9f + phase * 3f) * scale, (13f + i % 4 * 4f) * scale)
                val intensity = sin(phase * PI).toFloat() * flowAlpha
                drawLine(
                    Brush.linearGradient(
                        listOf(Color.Transparent, Color(0xFFDDF3F2).copy(alpha = intensity), Color.Transparent),
                        start = from, end = end,
                    ),
                    from, end, strokeWidth = (0.95f + i % 4 * .42f) * scale, cap = StrokeCap.Round,
                )
            }
        }
        if (season == JourneySeason.Summer) return@Canvas
        particles.forEachIndexed { i, p ->
            val phase = (p.y + travel * p.speed) % 1f
            val sway = sin((phase * 7f + i) * PI).toFloat()
            val pos = Offset((p.x + sway * .047f) * size.width, (phase * 1.12f - .06f) * size.height)
            val radius = (1.4f + p.size * 2.8f).dp.toPx()
            val alpha = sin(phase * PI).toFloat() * (.25f + p.size * .38f)
            when (season) {
                JourneySeason.Spring -> rotate(phase * 350f + i * 17f, pivot = pos) {
                    drawOval(
                        Color(0xFFFFE2DF).copy(alpha = alpha),
                        pos - Offset(radius, radius * .5f), Size(radius * 2, radius),
                    )
                }
                JourneySeason.Autumn -> rotate(phase * 720f + i * 38f, pivot = pos) {
                    val leaf = Path().apply {
                        moveTo(pos.x - radius * 1.3f, pos.y)
                        quadraticTo(pos.x, pos.y - radius * 1.45f, pos.x + radius * 1.4f, pos.y)
                        quadraticTo(pos.x, pos.y + radius, pos.x - radius * 1.3f, pos.y)
                        close()
                    }
                    drawPath(leaf, (if (i % 3 == 0) Color(0xFFC46B39) else Color(0xFFE5AD62)).copy(alpha = alpha))
                }
                JourneySeason.Winter -> drawCircle(Color(0xFFF3F7FF).copy(alpha = alpha), radius * .55f, pos)
                JourneySeason.Summer -> Unit
            }
        }
    }
}
