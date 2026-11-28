package com.omniroute.app.feature.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextDecoration
import com.omniroute.app.core.ui.theme.*

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.omniroute.app.R
import com.omniroute.app.core.ui.theme.JourneySeason
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private val chapters = listOf("觅花", "逐浪", "拾叶", "寻雪")
private val poems = listOf("山岚初醒，赴一场花信之约", "海风有信，把盛夏装进行囊", "层林微醺，在旷野收藏金秋", "雪落无声，向远方暖灯出发")

internal fun JourneySeason.stepBy(step: Int) = JourneySeason.entries[(ordinal + step + 4) % 4]

@Composable
internal fun PortalScenery(season: JourneySeason, moving: Boolean) {
    val tilt = rememberFoilTilt(moving)
    Crossfade(season, animationSpec = tween(550), label = "landscape") { target ->
        val pan = remember(target) { Animatable(if (moving) 1f else 0f) }
        LaunchedEffect(target, moving) { pan.animateTo(0f, tween(if (moving) 550 else 0)) }
        val zoom = remember(target) { Animatable(if (moving) 1.06f else 1f) }
        LaunchedEffect(target, moving) {
            if (moving) zoom.animateTo(1.035f, tween(8000, easing = LinearEasing))
        }
        Box(Modifier.fillMaxSize().clipToBounds()) {
            Image(painterResource(target.portalArt.landscape), null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize().graphicsLayer {
                scaleX = zoom.value
                scaleY = zoom.value
                val overscan = size.width * (zoom.value - 1f) * .5f
                translationX = if (moving) (tilt.value * 6.dp.toPx() + pan.value * 18.dp.toPx()).coerceIn(-overscan, overscan) else 0f
            })
            if (moving) CoverWeather(target)
        }
    }
}

@Composable
internal fun LoginLanding(
    season: JourneySeason,
    ticketOpen: Boolean,
    onOpen: (Boolean, JourneySeason) -> Unit,
    ticket: @Composable BoxScope.() -> Unit,
) {
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val motion = ValueAnimator.areAnimatorsEnabled()
    var preview by rememberSaveable(season) { mutableStateOf(season) }
    var playing by rememberSaveable { mutableStateOf(true) }
    var opening by remember { mutableStateOf<Boolean?>(null) }
    val active = lifecycle == Lifecycle.State.RESUMED && motion && !ticketOpen && opening == null
    val cycle = remember(preview) { Animatable(0f) }
    LaunchedEffect(active, playing, preview) {
        if (active && playing) {
            cycle.animateTo(1f, tween(((1f - cycle.value) * 8000).toInt(), easing = LinearEasing))
            preview = preview.stepBy(1)
        }
    }
    LaunchedEffect(opening) {
        opening?.let { register ->
            if (motion) delay(260)
            onOpen(register, preview)
            opening = null
        }
    }
    val selected = if (ticketOpen) season else preview
    val choose: (JourneySeason) -> Unit = { playing = false; preview = it }
    val light by animateColorAsState(selected.world.light, tween(550), label = "paper")
    val glass by animateColorAsState(selected.world.glass, tween(550), label = "glass")
    val skyInk = if (selected == JourneySeason.Spring) selected.world.onLight else Color.White
    com.omniroute.app.core.ui.SystemBarIcons(selected == JourneySeason.Spring && !ticketOpen, false)
    val label = stringResource(selected.labelRes)
    Box(Modifier.fillMaxSize().background(selected.world.shadow)) {
        Box(Modifier.matchParentSize().blur(if (ticketOpen) 4.dp else 0.dp)) {
            PortalScenery(selected, active)
        }
        Spacer(Modifier.matchParentSize().background(Brush.verticalGradient(
            0f to Color.Black.copy(alpha = if (selected == JourneySeason.Spring) .03f else .33f), .23f to Color.Transparent, .43f to Color.Transparent,
            .72f to selected.world.shadow.copy(alpha = .82f), 1f to selected.world.shadow)))
        if (ticketOpen) ticket()
        AnimatedVisibility(!ticketOpen && opening == null, enter = fadeIn(tween(260)), exit = fadeOut(tween(245)) + slideOutVertically(tween(260)) { it / 28 }) {
            BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                val viewport = maxHeight
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = viewport)
                    .padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        OmniSeasonLogo(season = selected, diameter = 25.dp, tint = skyInk)
                        Text("OMNIROUTE", Modifier.weight(1f).padding(start = 8.dp), color = skyInk,
                            fontSize = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.8.sp)
                        if (motion) IconButton(onClick = { playing = !playing }) {
                            val desc = stringResource(if (playing) R.string.auth_pause_seasons else R.string.auth_play_seasons)
                            Box(Modifier.size(36.dp).background(glass, CircleShape).semantics { contentDescription = desc }, contentAlignment = Alignment.Center) {
                                Canvas(Modifier.matchParentSize().padding(2.dp)) {
                                    val line = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx(), cap = StrokeCap.Round)
                                    drawCircle(Color.White.copy(alpha = .22f), style = line)
                                    drawArc(light, -90f, 360f * cycle.value, false, style = line)
                                }
                                if (playing) Text("Ⅱ", color = light, fontSize = 16.sp)
                                else Icon(Icons.Filled.PlayArrow, null, tint = light, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Spacer(Modifier.fillMaxWidth().weight(1f).heightIn(min = 32.dp).pointerInput(preview) {
                        var drag = 0f
                        detectHorizontalDragGestures(onDragStart = { drag = 0f }, onDragEnd = {
                            if (kotlin.math.abs(drag) > 35.dp.toPx()) choose(preview.stepBy(if (drag < 0f) 1 else -1))
                        }) { _, dx -> drag += dx }
                    })
                    Column {
                        Column(Modifier.padding(start = 10.dp, end = 6.dp)) {
                            AnimatedContent(selected, transitionSpec = { (fadeIn(tween(260, 80)) + slideInVertically { it / 3 }) togetherWith fadeOut(tween(150)) }, label = "chapter") { s ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("0${s.ordinal + 1} / ${s.name.uppercase()}   ·   ${stringResource(s.labelRes)} · ${chapters[s.ordinal]}",
                                        color = s.world.light, fontFamily = FontFamily.Monospace, fontSize = 11.sp, letterSpacing = 1.sp)
                                    Spacer(Modifier.padding(start = 8.dp).width(28.dp).height(1.dp).background(s.world.accent))
                                }
                            }
                            Spacer(Modifier.height(9.dp))
                            Text(stringResource(R.string.auth_hero_title), fontFamily = FolioSerif, color = Color.White, fontSize = 32.sp, lineHeight = 41.sp)
                            Spacer(Modifier.height(7.dp))
                            Crossfade(selected, animationSpec = tween(450), label = "poem") { Text(poems[it.ordinal], color = it.world.ink, fontSize = 13.sp) }
                        }
                        Spacer(Modifier.height(20.dp))
                        val shape = RoundedCornerShape(26.dp)
                        Column(Modifier.fillMaxWidth().clip(shape).background(glass).border(.8.dp, Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = .36f), Color.White.copy(alpha = .09f))), shape).padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(Modifier.fillMaxWidth().selectableGroup(),horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                JourneySeason.entries.forEach { s ->
                                    val chosen = s == selected
                                    val tile = RoundedCornerShape(14.dp)
                                    Box(Modifier.weight(1f).height(44.dp).clip(tile).selectable(chosen, role = Role.Tab) { choose(s) }
                                        .border(if (chosen) 1.6.dp else .6.dp, if (chosen) light else Color.White.copy(alpha = .23f), tile), contentAlignment = Alignment.Center) {
                                        Image(painterResource(s.portalArt.landscape), null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
                                        Spacer(Modifier.matchParentSize().background(Color.Black.copy(alpha = .39f)).border(if (chosen) 1.6.dp else .6.dp,
                                            if (chosen) light else Color.White.copy(alpha = .22f), tile))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            OmniSeasonLogo(season = s, diameter = 16.dp, tint = Color.White)
                                            Text(stringResource(s.labelRes), Modifier.padding(start = 4.dp), color = Color.White, fontFamily = FolioSerif, fontSize = 16.sp)
                                        }
                                        if (chosen && playing) Canvas(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(2.dp)) {
                                            drawRect(light, size = Size(size.width * cycle.value,size.height))
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { opening = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = light, contentColor = selected.world.onLight)) {
                                Text("开启${label}日旅程  →", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            }
                            TextButton(onClick = { opening = false }) {
                                Text("已有账号？", color = selected.world.secondary, fontSize = 13.sp)
                                Text("直接登录", color = light, fontSize = 13.sp, textDecoration = TextDecoration.Underline)
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class WeatherParticle(val x: Float, val y: Float, val size: Float, val speed: Int)

@Composable
internal fun CoverWeather(season: JourneySeason) {
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
                JourneySeason.Summer -> drawCircle(Color(0xFFFFE8A0).copy(alpha = alpha), radius * .53f, Offset(pos.x, size.height - pos.y))
            }
        }
    }
}
