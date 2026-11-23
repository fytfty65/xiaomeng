package com.omniroute.app.feature.auth

import android.graphics.BitmapFactory
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniroute.app.R
import com.omniroute.app.core.ui.components.OmniLogoMark
import com.omniroute.app.core.ui.theme.JourneySeason
import com.omniroute.app.core.ui.theme.LocalAppPalette
import java.util.Calendar
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

// 品牌色语义由 AppPalette 统一提供：浅色为暖橙 + 奶油纸底，深色自动切换为亮橙 + 深底。
internal val BrandOrange: Color
    @Composable get() = LocalAppPalette.current.brand
internal val BrandOrangeDark: Color
    @Composable get() = LocalAppPalette.current.brandDark
internal val BrandOrangeLight: Color
    @Composable get() = LocalAppPalette.current.brandLight
internal val Ink: Color
    @Composable get() = LocalAppPalette.current.ink
internal val Muted: Color
    @Composable get() = LocalAppPalette.current.muted
internal val CreamBg: Color
    @Composable get() = LocalAppPalette.current.background
internal val CardBorder: Color
    @Composable get() = LocalAppPalette.current.border
internal val FieldBg: Color
    @Composable get() = LocalAppPalette.current.fieldBg
internal val TabTrack: Color
    @Composable get() = LocalAppPalette.current.tabTrack
internal val ErrorBg: Color
    @Composable get() = LocalAppPalette.current.errorBg
internal val ErrorFg: Color
    @Composable get() = LocalAppPalette.current.errorFg
internal val InfoBg: Color
    @Composable get() = LocalAppPalette.current.infoBg
internal val InfoFg: Color
    @Composable get() = LocalAppPalette.current.infoFg

/** 统一卡片修饰：主题面底 + 细边框 + 适度圆角，用边框而非阴影分层。 */
@Composable
internal fun Modifier.paperCard(shape: RoundedCornerShape = RoundedCornerShape(14.dp)): Modifier {
    val palette = LocalAppPalette.current
    return clip(shape)
        .background(palette.surface)
        .border(1.dp, palette.border, shape)
}

/** 认证页统一骨架：奶油纸底 + 可滚动内容区。 */
@Composable
internal fun AuthScaffold(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            content = content,
        )
    }
}

@Composable
internal fun BrandHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(BrandOrange),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "OmniRoute",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Ink,
                letterSpacing = 0.2.sp,
            )
            Text(
                text = stringResource(R.string.auth_slogan),
                fontSize = 12.sp,
                color = Muted,
            )
        }
    }
}

@Composable
internal fun AuthTabs(
    registerSelected: Boolean,
    onLogin: () -> Unit,
    onRegister: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(TabTrack)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TabItem(
            text = stringResource(R.string.auth_login_tab),
            selected = !registerSelected,
            modifier = Modifier.weight(1f),
            onClick = onLogin,
        )
        TabItem(
            text = stringResource(R.string.auth_register_tab),
            selected = registerSelected,
            modifier = Modifier.weight(1f),
            onClick = onRegister,
        )
    }
}

@Composable
private fun TabItem(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) LocalAppPalette.current.surface else Color.Transparent)
            .then(
                if (selected) {
                    Modifier.border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                } else {
                    Modifier
                },
            )
            .clickable(enabled = !selected, role = Role.Tab, onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) Ink else Muted,
        )
    }
}

@Composable
internal fun FieldLabel(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = Muted,
    )
}

@Composable
internal fun SoftField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    trailing: (@Composable () -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        textStyle = TextStyle(fontSize = 14.sp, color = Ink),
        visualTransformation = if (isPassword && !passwordVisible) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text,
            imeAction = imeAction,
        ),
        keyboardActions = KeyboardActions(onDone = { onImeAction() }, onGo = { onImeAction() }),
        leadingIcon = {
            Icon(icon, contentDescription = null, tint = Muted, modifier = Modifier.size(18.dp))
        },
        trailingIcon = trailing,
        placeholder = { Text(placeholder, fontSize = 14.sp, color = Muted.copy(alpha = 0.8f)) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = FieldBg,
            unfocusedContainerColor = FieldBg,
            disabledContainerColor = FieldBg,
            focusedBorderColor = BrandOrange,
            unfocusedBorderColor = CardBorder,
            disabledBorderColor = CardBorder,
            cursorColor = BrandOrange,
            focusedTextColor = Ink,
            unfocusedTextColor = Ink,
            focusedPlaceholderColor = Muted,
            unfocusedPlaceholderColor = Muted,
        ),
    )
}

/** 密码显示/隐藏切换，用文字替代图标，避免额外图标依赖。 */
@Composable
internal fun PasswordToggle(visible: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    Text(
        text = stringResource(if (visible) R.string.auth_hide else R.string.auth_show),
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = BrandOrange,
        modifier = Modifier
            .clickable(enabled = enabled, role = Role.Button, onClick = onToggle)
            .padding(horizontal = 4.dp, vertical = 6.dp),
    )
}

@Composable
internal fun InlineBanner(text: String, bg: Color, fg: Color, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontSize = 12.5.sp, color = fg, lineHeight = 18.sp)
    }
}

@Composable
internal fun GradientButton(
    text: String,
    loading: Boolean,
    onClick: () -> Unit,
    loadingText: String? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (loading) BrandOrangeDark else BrandOrange)
            .clickable(enabled = !loading, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    loadingText ?: stringResource(R.string.common_processing),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            } else {
                Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/** 功能卖点卡片，登录/注册页共用。 */
@Composable
internal fun FeatureRow(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .paperCard(RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(LocalAppPalette.current.brandTint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = BrandOrange, modifier = Modifier.size(17.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, fontSize = 12.sp, color = Muted)
        }
    }
}

// —— 四季封面（对齐 Web 端 /hero/v5/*）——
// Web 端每个季节一张 960×540 图，切换时 2s 淡入；再叠 sunlight（18s 明暗呼吸）
// 与 fogDrift（28s 缓慢漂移）两层动效。这里用 Crossfade + 无限动画复刻同一节奏。
internal enum class HeroSeason(
    val key: String,
    val imageRes: Int,
    val swingRes: Int,
    val labelRes: Int,
) {
    Spring("spring", R.drawable.hero_spring, R.drawable.swing_spring, R.string.auth_season_spring),
    Summer("summer", R.drawable.hero_summer, R.drawable.swing_summer, R.string.auth_season_summer),
    Autumn("autumn", R.drawable.hero_autumn, R.drawable.swing_autumn, R.string.auth_season_autumn),
    Winter("winter", R.drawable.hero_winter, R.drawable.swing_winter, R.string.auth_season_winter);

    companion object {
        /** Web 端「随日期变换」：按月份落到当季，3–5 春、6–8 夏、9–11 秋、12–2 冬。 */
        fun ofCurrentMonth(): HeroSeason = when (Calendar.getInstance().get(Calendar.MONTH) + 1) {
            3, 4, 5 -> Spring
            6, 7, 8 -> Summer
            12, 1, 2 -> Winter
            else -> Autumn
        }

        fun fromKey(key: String?): HeroSeason =
            values().firstOrNull { it.key == key } ?: ofCurrentMonth()
    }
}

/** 季节主题 -> 封面季别，供登录后的页面复用同一套四季插画与动效。 */
internal fun JourneySeason.heroSeason(): HeroSeason = HeroSeason.fromKey(key)

// 封面原图尺寸与秋千裁剪框；数字与 drawable 里 swing_*.webp 的导出参数一一对应。
private const val HeroSourceWidth = 960f
private const val HeroSourceHeight = 540f
private const val SwingCropLeft = 388f
private const val SwingCropTop = 76f
private const val SwingCropWidth = 112f
private const val SwingCropHeight = 210f

/** 秋千吊点（原图像素）：旋转围绕这里发生，绳子上端几乎不动。 */
private const val SwingPivotX = 438f
private const val SwingPivotY = 78f

@Composable
internal fun SeasonHeroImage(season: HeroSeason) {
    val motion = rememberInfiniteTransition(label = "hero-motion")
    val drift by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 28_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "drift",
    )
    val light by motion.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.56f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "light",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Crossfade(
            targetState = season,
            animationSpec = tween(durationMillis = 2_000),
            label = "season-crossfade",
        ) { target ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // 缓慢推近 + 轻微平移，替代 Web 端的鼠标视差（触屏没有指针）。
                        val scale = 1.06f + drift * 0.06f
                        scaleX = scale
                        scaleY = scale
                        translationX = (drift - 0.5f) * 18f
                        translationY = (drift - 0.5f) * -12f
                    },
            ) {
                Image(
                    painter = painterResource(target.imageRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                // 秋千随风格外一层：图像本身仍只做推近，秋千绕吊点轻摆。
                SwingBreeze(season = target)
            }
        }
        // 落花 / 萤火 / 落叶 / 雪：节奏按 Web 端 season-fall 的粒子参数复刻。
        SeasonAmbience(season = season)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFE6B1).copy(alpha = light * 0.42f),
                                Color.Transparent,
                            ),
                            center = Offset(size.width * 0.82f, size.height * 0.04f),
                            radius = size.maxDimension * 0.8f,
                        ),
                    )
                },
        )
    }
}

/** 秋千摆动：把原图里秋千那一小块（带背景、四边羽化）绕吊点做 ±1.2° 摆动。 */
@Composable
private fun SwingBreeze(season: HeroSeason) {
    val context = LocalContext.current
    val bitmap = remember(season.swingRes) {
        BitmapFactory.decodeResource(context.resources, season.swingRes).asImageBitmap()
    }
    val breeze = rememberInfiniteTransition(label = "swing-breeze")
    val angle by breeze.animateFloat(
        initialValue = -1.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5_600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "swing-angle",
    )

    Canvas(modifier = Modifier.fillMaxSize().clipToBounds()) {
        // 与 ContentScale.Crop 相同的映射：按比例铺满后居中裁切。
        val scale = max(size.width / HeroSourceWidth, size.height / HeroSourceHeight)
        val offsetX = (size.width - HeroSourceWidth * scale) / 2f
        val offsetY = (size.height - HeroSourceHeight * scale) / 2f
        val pivot = Offset(offsetX + SwingPivotX * scale, offsetY + SwingPivotY * scale)
        val destination = IntOffset(
            (offsetX + SwingCropLeft * scale).roundToInt(),
            (offsetY + SwingCropTop * scale).roundToInt(),
        )
        val destinationSize = IntSize(
            (SwingCropWidth * scale).roundToInt(),
            (SwingCropHeight * scale).roundToInt(),
        )
        rotate(degrees = angle, pivot = pivot) {
            drawImage(
                image = bitmap,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(bitmap.width, bitmap.height),
                dstOffset = destination,
                dstSize = destinationSize,
                filterQuality = FilterQuality.Medium,
            )
        }
    }
}

/** 四季氛围粒子，参数对齐 Web 端 SeasonParticles（18 颗、5–11px、14–29s 循环）。 */
private data class AmbientMote(
    val left: Float,
    val sizeDp: Float,
    val durationSec: Float,
    val phase: Float,
    val swayRatio: Float,
    val soft: Boolean,
)

private val ambientMotes = List(18) { index ->
    AmbientMote(
        left = (index * 37 % 100) / 100f,
        sizeDp = 5f + (index % 4) * 2f,
        durationSec = 14f + (index % 6) * 3f,
        phase = (index % 7) * 1.9f / (14f + (index % 6) * 3f),
        swayRatio = if (index % 2 == 0) 0.05f else -0.05f,
        soft = index % 3 == 0,
    )
}

@Composable
private fun SeasonAmbience(season: HeroSeason) {
    val density = LocalDensity.current.density
    var seconds by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var start = 0L
        while (true) {
            withFrameNanos { frame ->
                if (start == 0L) start = frame
                seconds = (frame - start) / 1_000_000_000f
            }
        }
    }

    Canvas(modifier = Modifier.fillMaxSize().clipToBounds()) {
        val width = size.width
        val height = size.height
        ambientMotes.forEach { mote ->
            val progress = ((seconds / mote.durationSec) + mote.phase) % 1f
            val centerX = mote.left * width + mote.swayRatio * width * progress
            val radius = mote.sizeDp * density / 2f
            val softFactor = if (mote.soft) 0.65f else 1f

            if (season == HeroSeason.Summer) {
                // 夏夜萤火：贴着画面下三分之一缓缓上浮，明暗呼吸。
                val centerY = height * 0.72f + 20f * density - progress * 180f * density
                val alpha = when {
                    progress < 0.35f -> progress / 0.35f * 0.6f
                    progress < 0.65f -> 0.6f - (progress - 0.35f) / 0.3f * 0.35f
                    progress < 0.9f -> 0.25f - (progress - 0.65f) / 0.25f * 0.25f
                    else -> 0f
                } * softFactor
                if (alpha > 0.01f) {
                    val glowRadius = radius * 5f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFEFFFBD).copy(alpha = alpha * 0.55f), Color.Transparent),
                            center = Offset(centerX, centerY),
                            radius = glowRadius,
                        ),
                        radius = glowRadius,
                        center = Offset(centerX, centerY),
                    )
                    drawCircle(
                        color = Color(0xFFEFFFBD).copy(alpha = alpha),
                        radius = radius,
                        center = Offset(centerX, centerY),
                    )
                }
                return@forEach
            }

            // 春落花 / 秋落叶 / 冬雪：从 -6% 落到 106%，横向顺势偏移并旋转 310°。
            val centerY = -0.06f * height + progress * 1.12f * height
            val alpha = when {
                progress < 0.10f -> progress / 0.10f * 0.6f
                progress < 0.85f -> 0.6f - (progress - 0.10f) / 0.75f * 0.2f
                else -> 0.4f * (1f - (progress - 0.85f) / 0.15f)
            } * softFactor
            if (alpha <= 0.01f) return@forEach

            val center = Offset(centerX, centerY)
            rotate(degrees = 310f * progress, pivot = center) {
                when (season) {
                    HeroSeason.Spring -> drawOval(
                        color = Color(0xFFF0B5A6).copy(alpha = alpha),
                        topLeft = Offset(centerX - radius, centerY - radius * 0.85f),
                        size = Size(radius * 2f, radius * 1.7f),
                    )
                    HeroSeason.Autumn -> {
                        val leaf = Path().apply {
                            moveTo(centerX, centerY - radius * 1.4f)
                            cubicTo(
                                centerX + radius * 1.2f, centerY - radius * 0.4f,
                                centerX + radius * 1.1f, centerY + radius * 0.8f,
                                centerX, centerY + radius * 1.2f,
                            )
                            cubicTo(
                                centerX - radius * 1.1f, centerY + radius * 0.8f,
                                centerX - radius * 1.2f, centerY - radius * 0.4f,
                                centerX, centerY - radius * 1.4f,
                            )
                            close()
                        }
                        drawPath(
                            path = leaf,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFF1BE64).copy(alpha = alpha),
                                    Color(0xFFA84221).copy(alpha = alpha),
                                ),
                                start = Offset(centerX - radius, centerY - radius),
                                end = Offset(centerX + radius, centerY + radius),
                            ),
                        )
                    }
                    else -> {
                        // 冬雪：白色小圆点 + 一圈柔光（Web 端是 4–5px 雪粒 + 4px 光晕）。
                        val glowRadius = radius * 1.8f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White.copy(alpha = alpha * 0.35f), Color.Transparent),
                                center = center,
                                radius = glowRadius,
                            ),
                            radius = glowRadius,
                            center = center,
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = alpha),
                            radius = radius,
                            center = center,
                        )
                    }
                }
            }
        }
    }
}
