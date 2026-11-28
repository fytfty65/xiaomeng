package com.omniroute.app.feature.auth

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.omniroute.app.R
import com.omniroute.app.core.ui.theme.JourneySeason
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Immutable
internal data class SeasonPassStyle(
    @DrawableRes val cover: Int,
    @DrawableRes val windowArt: Int,
    @DrawableRes val ticketBackground: Int,
    val windowBorder: Color,
    val paper: Color,
    val glassAlpha: Float,
    val ink: Color,
    val stamp: Color,
    val action: Color,
    val onAction: Color,
    val english: String,
    val code: String,
    @StringRes val material: Int,
    @StringRes val motto: Int,
) {
    val muted: Color get() = ink.copy(alpha = .76f)
    val surface: Color
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) paper.copy(alpha = glassAlpha) else paper
}

private val SpringPass = SeasonPassStyle(
    cover = R.drawable.pass_cover_spring, windowArt = R.drawable.pass_window_spring,
    ticketBackground = R.drawable.pass_background_spring, windowBorder = Color(0xFFC8C2B6),
    paper = Color(0xFFF8F6F0), glassAlpha = .88f, ink = Color(0xFF26332D), stamp = Color(0xFFAC5F68),
    action = Color(0xFF2C473C), onAction = Color.White, english = "SPRING", code = "SPR",
    material = R.string.pass_spring, motto = R.string.pass_motto_spring,
)
private val SummerPass = SeasonPassStyle(
    cover = R.drawable.pass_cover_summer, windowArt = R.drawable.pass_window_summer,
    ticketBackground = R.drawable.pass_background_summer, windowBorder = Color(0xFF9FC7CE),
    paper = Color(0xFFEDF7F9), glassAlpha = .80f, ink = Color(0xFF16333D), stamp = Color(0xFF347B8D),
    action = Color(0xFF1B3F4D), onAction = Color.White, english = "SUMMER", code = "SUM",
    material = R.string.pass_summer, motto = R.string.pass_motto_summer,
)
private val AutumnPass = SeasonPassStyle(
    cover = R.drawable.pass_cover_autumn, windowArt = R.drawable.pass_window_autumn,
    ticketBackground = R.drawable.pass_background_autumn, windowBorder = Color(0xFFCBB79E),
    paper = Color(0xFFF2EAE0), glassAlpha = .90f, ink = Color(0xFF3D281D), stamp = Color(0xFF936430),
    action = Color(0xFF8F4026), onAction = Color(0xFFFFF8EE), english = "AUTUMN", code = "AUT",
    material = R.string.pass_autumn, motto = R.string.pass_motto_autumn,
)
private val WinterPass = SeasonPassStyle(
    cover = R.drawable.pass_cover_winter, windowArt = R.drawable.pass_window_winter,
    ticketBackground = R.drawable.pass_background_winter, windowBorder = Color(0xFF4A5868),
    paper = Color(0xFF151A21), glassAlpha = .88f, ink = Color(0xFFEDF2F7), stamp = Color(0xFFADC3D8),
    action = Color(0xFFF2F5F8), onAction = Color(0xFF151A21), english = "WINTER", code = "WIN",
    material = R.string.pass_winter, motto = R.string.pass_motto_winter,
)

internal fun JourneySeason.passStyle(): SeasonPassStyle = when (this) {
    JourneySeason.Spring -> SpringPass
    JourneySeason.Summer -> SummerPass
    JourneySeason.Autumn -> AutumnPass
    JourneySeason.Winter -> WinterPass
}

private val PassGrain by lazy {
    val random = Random(41)
    val pixels = IntArray(128 * 128) {
        if (random.nextBoolean()) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
    }
    Bitmap.createBitmap(pixels, 128, 128, Bitmap.Config.ARGB_8888).asImageBitmap()
}

internal fun Modifier.passMaterial(season: JourneySeason, style: SeasonPassStyle, stub: Boolean = false): Modifier = drawWithCache {
    val grain = ShaderBrush(ImageShader(PassGrain, TileMode.Repeated, TileMode.Repeated))
    val contour = Path()
    val edge = when (season) {
        JourneySeason.Summer -> Brush.horizontalGradient(
            listOf(Color.Transparent, Color(0xB3A7D4D9), Color(0x95C9B5CB), Color.Transparent),
        )
        JourneySeason.Winter -> Brush.horizontalGradient(
            listOf(Color.Transparent, Color(0xFF97AABF), Color(0xC8DFE9F5), Color.Transparent),
        )
        JourneySeason.Spring, JourneySeason.Autumn -> Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
    }
    val high = size.height * .47f
    onDrawBehind {
        drawRect(style.surface)
        drawRect(grain, alpha = .032f, blendMode = BlendMode.Softlight)
        if (stub) return@onDrawBehind
        val thread = style.stamp.copy(alpha = if (season == JourneySeason.Summer) .12f else .085f)
        val thin = .55.dp.toPx()
        when (season) {
            JourneySeason.Summer -> {
                repeat(8) { i ->
                    val x = size.width * i / 7f
                    drawLine(thread, Offset(x, 0f), Offset(x + size.width * .09f, high), thin)
                    val y = high * i / 7f
                    drawLine(thread, Offset(0f, y), Offset(size.width, y - 10.dp.toPx()), thin)
                }
            }
            JourneySeason.Spring, JourneySeason.Autumn, JourneySeason.Winter -> {
                val count = if (season == JourneySeason.Autumn) 11 else 6
                repeat(count) { i ->
                    contour.reset()
                    for (step in 0..44) {
                        val ratio = step / 44f
                        val fold = if (season == JourneySeason.Autumn) {
                            sin(ratio * 17 + i * .57) * cos(ratio * 11 - i * .52)
                        } else {
                            sin(ratio * 9 + i * .92) * .7 + sin(ratio * 16 + i * .32) * .20
                        }
                        val x = ratio * size.width
                        val y = high * (.10f + i * .045f + fold.toFloat() * .09f)
                        if (step == 0) contour.moveTo(x, y) else contour.lineTo(x, y)
                    }
                    drawPath(contour, thread, style = Stroke(thin))
                }
                if (season == JourneySeason.Winter) {
                    val stars = listOf(.14f to .17f, .28f to .23f, .43f to .12f, .55f to .19f, .69f to .10f)
                        .map { (x, y) -> Offset(x * size.width, y * high) }
                    stars.zipWithNext().forEach { (a, b) -> drawLine(thread, a, b, thin) }
                    stars.forEach { drawCircle(thread.copy(alpha = .26f), 1.dp.toPx(), it) }
                }
            }
        }
        drawRect(edge, size = Size(size.width, 1.dp.toPx()))
    }
}

@Composable
internal fun SeasonSeal(season: JourneySeason, error: Boolean, modifier: Modifier = Modifier) {
    val style = season.passStyle()
    val metal = season == JourneySeason.Autumn || season == JourneySeason.Winter
    val tilt by rememberFoilTilt(metal)
    val description = stringResource(R.string.pass_seal_description, stringResource(style.material))

    Canvas(modifier.rotate(-12f).semantics { contentDescription = description }) {
        val color = if (error) Color(0xFFC56560) else style.stamp
        val bright = if (season == JourneySeason.Winter) Color(0xFFF2FAFF) else Color(0xFFF5CD8E)
        val centerOfLight = size.width * (.50f + tilt * .34f)
        val foil = if (metal && !error) Brush.linearGradient(
            listOf(color, bright, color),
            start = Offset(centerOfLight - size.width * .24f, size.height),
            end = Offset(centerOfLight + size.width * .24f, 0f),
        ) else SolidColor(color)

        withTransform({ scale(size.width / 100f, size.height / 100f, Offset.Zero) }) {
            val stroke = Stroke(1.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val fine = Stroke(1.25f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val stampBase = if (season == JourneySeason.Winter) Color(0xFF212B36) else style.paper
            fun bordered(path: Path) {
                drawPath(path, stampBase.copy(alpha = .89f))
                drawPath(path, foil, style = stroke)
            }
            when (season) {
                JourneySeason.Spring -> {
                    drawCircle(stampBase.copy(alpha = .89f), 46f, Offset(50f, 50f))
                    drawCircle(foil, 46f, Offset(50f, 50f), style = stroke)
                    drawCircle(foil, 41f, Offset(50f, 50f), style = fine)
                    val stem = Path().apply {
                        moveTo(25f, 70f); cubicTo(43f, 65f, 53f, 53f, 68f, 31f)
                        moveTo(46f, 56f); quadraticTo(38f, 43f, 29f, 41f)
                        moveTo(55f, 49f); quadraticTo(67f, 54f, 75f, 52f)
                    }
                    drawPath(stem, foil, style = stroke)
                    listOf(Offset(31f, 37f), Offset(70f, 29f), Offset(77f, 52f)).forEach { blossom ->
                        repeat(5) { index ->
                            val angle = index * 2 * PI / 5
                            val petal = blossom + Offset((cos(angle) * 4).toFloat(), (sin(angle) * 4).toFloat())
                            drawCircle(foil, 3.2f, petal, style = fine)
                        }
                    }
                }
                JourneySeason.Summer -> {
                    val shape = Path().apply {
                        moveTo(44f, 7f); quadraticTo(50f, 2f, 56f, 7f)
                        lineTo(93f, 44f); quadraticTo(98f, 50f, 93f, 56f)
                        lineTo(56f, 93f); quadraticTo(50f, 98f, 44f, 93f)
                        lineTo(7f, 56f); quadraticTo(2f, 50f, 7f, 44f); close()
                    }
                    bordered(shape)
                    repeat(3) { i ->
                        val wave = Path().apply {
                            val y = 40f + i * 10f
                            moveTo(23f, y)
                            cubicTo(32f, y - 15f, 41f, y - 15f, 50f, y)
                            cubicTo(59f, y + 15f, 68f, y + 15f, 77f, y)
                        }
                        drawPath(wave, foil, style = stroke)
                    }
                }
                JourneySeason.Autumn -> {
                    val scallop = Path().apply {
                        repeat(144) { i ->
                            val angle = i * 2 * PI / 144
                            val edge = 1 + cos(angle * 24) * .027
                            val x = (50 + cos(angle) * 46 * edge).toFloat()
                            val y = (50 + sin(angle) * 34 * edge).toFloat()
                            if (i == 0) moveTo(x, y) else lineTo(x, y)
                        }
                        close()
                    }
                    bordered(scallop)
                    drawOval(foil, Offset(9f, 21f), Size(82f, 58f), style = fine)
                    val leaf = Path().apply {
                        moveTo(50f, 28f); lineTo(58f, 44f); lineTo(69f, 39f); lineTo(65f, 50f)
                        lineTo(74f, 54f); lineTo(56f, 64f); lineTo(44f, 64f); lineTo(26f, 54f)
                        lineTo(35f, 50f); lineTo(31f, 39f); lineTo(42f, 44f); close()
                        moveTo(50f, 40f); lineTo(50f, 71f)
                    }
                    drawPath(leaf, foil, style = stroke)
                }
                JourneySeason.Winter -> {
                    fun hexagon(radius: Double): Path = Path().apply {
                        repeat(6) { i ->
                            val a = i * PI / 3 - PI / 2
                            val x = (50 + cos(a) * radius).toFloat()
                            val y = (50 + sin(a) * radius).toFloat()
                            if (i == 0) moveTo(x, y) else lineTo(x, y)
                        }
                        close()
                    }
                    bordered(hexagon(47.0))
                    drawPath(hexagon(41.5), foil, style = fine)
                    repeat(6) { i ->
                        rotate(i * 60f, Offset(50f, 50f)) {
                            drawLine(foil, Offset(50f, 50f), Offset(50f, 22f), 1.5f)
                            drawLine(foil, Offset(50f, 36f), Offset(43f, 29f), 1.35f)
                            drawLine(foil, Offset(50f, 36f), Offset(57f, 29f), 1.35f)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun rememberFoilTilt(enabled: Boolean): State<Float> {
    val context = LocalContext.current
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val active = enabled && lifecycle == Lifecycle.State.RESUMED && ValueAnimator.areAnimatorsEnabled()
    val tilt = remember { mutableFloatStateOf(0f) }
    DisposableEffect(context, active) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = if (active) manager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) else null
        val matrix = FloatArray(9)
        val angles = FloatArray(3)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(matrix, event.values)
                SensorManager.getOrientation(matrix, angles)
                val target = (angles[2] / .65f).coerceIn(-1f, 1f)
                tilt.floatValue += (target - tilt.floatValue) * .11f
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (sensor != null) manager?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose {
            if (sensor != null) manager?.unregisterListener(listener)
        }
    }
    return tilt
}
