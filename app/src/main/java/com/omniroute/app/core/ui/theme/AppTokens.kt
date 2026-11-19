package com.omniroute.app.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 统一间距标尺，页面不再各自写魔数。 */
object AppSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
}

/** 统一圆角。 */
object AppRadius {
    val sm = 8.dp
    val md = 12.dp
    val lg = 14.dp
    val xl = 18.dp
}

/** 交互控件最小触摸区域（Material 无障碍建议 48dp）。 */
object AppTouch {
    val min = 48.dp
}

/** 字号标尺：正文不小于 14sp，辅助说明不小于 12sp。 */
object AppType {
    val caption = 12.sp
    val label = 13.sp
    val body = 14.sp
    val bodyLarge = 15.sp
    val title = 18.sp
    val headline = 24.sp
}

/** Material 3 默认字体层级，未显式指定字号的文本会落到这套标尺。 */
val AppTypography = Typography(
    bodyLarge = TextStyle(fontSize = AppType.body, lineHeight = 21.sp),
    bodyMedium = TextStyle(fontSize = AppType.label, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = AppType.caption, lineHeight = 18.sp),
    labelLarge = TextStyle(fontSize = AppType.body, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = AppType.caption, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = AppType.caption),
    titleLarge = TextStyle(fontSize = AppType.title, fontWeight = FontWeight.SemiBold, lineHeight = 26.sp),
    titleMedium = TextStyle(fontSize = AppType.bodyLarge, fontWeight = FontWeight.SemiBold),
    headlineSmall = TextStyle(fontSize = AppType.headline, fontWeight = FontWeight.Bold, lineHeight = 32.sp),
)

/**
 * 全端共用的语义色板。
 *
 * <p>品牌色与暖纸质底保持一致，深色模式改用深底 + 亮橙，不再依赖系统动态取色。
 */
@Immutable
data class AppPalette(
    val brand: Color,
    val brandDark: Color,
    val brandLight: Color,
    val onBrand: Color,
    val ink: Color,
    val muted: Color,
    val background: Color,
    val surface: Color,
    val fieldBg: Color,
    val tabTrack: Color,
    val border: Color,
    val divider: Color,
    val tagBg: Color,
    val brandTint: Color,
    val errorBg: Color,
    val errorFg: Color,
    val infoBg: Color,
    val infoFg: Color,
    val online: Color,
    val verifiedBg: Color,
    val verifiedFg: Color,
    val warningBg: Color,
    val warningFg: Color,
)

val LightAppPalette = AppPalette(
    brand = Color(0xFFFF5A00),
    brandDark = Color(0xFFE14E00),
    brandLight = Color(0xFFFF8F49),
    onBrand = Color(0xFFFFFFFF),
    ink = Color(0xFF1B1712),
    muted = Color(0xFF7C736A),
    background = Color(0xFFFAF7F2),
    surface = Color(0xFFFFFFFF),
    fieldBg = Color(0xFFF5F1EB),
    tabTrack = Color(0xFFF1ECE5),
    border = Color(0xFFEAE2D8),
    divider = Color(0xFFF2ECE1),
    tagBg = Color(0xFFF6F1EA),
    brandTint = Color(0xFFFFF0E5),
    errorBg = Color(0xFFFFF0ED),
    errorFg = Color(0xFFC63B2E),
    infoBg = Color(0xFFF0F7F3),
    infoFg = Color(0xFF2E7D5B),
    online = Color(0xFF2E9E6B),
    verifiedBg = Color(0xFFE8F7EE),
    verifiedFg = Color(0xFF1B8755),
    warningBg = Color(0xFFFEF3C7),
    warningFg = Color(0xFFB45309),
)

val DarkAppPalette = AppPalette(
    brand = Color(0xFFFF8F49),
    brandDark = Color(0xFFFF6B14),
    brandLight = Color(0xFFFFB27A),
    onBrand = Color(0xFF3A1700),
    ink = Color(0xFFF3ECE4),
    muted = Color(0xFFB8ADA2),
    background = Color(0xFF17130F),
    surface = Color(0xFF221D17),
    fieldBg = Color(0xFF2A241E),
    tabTrack = Color(0xFF2A241E),
    border = Color(0xFF3A322A),
    divider = Color(0xFF2E2822),
    tagBg = Color(0xFF2A241E),
    brandTint = Color(0xFF3A2417),
    errorBg = Color(0xFF3A1E19),
    errorFg = Color(0xFFFFB4AB),
    infoBg = Color(0xFF17281F),
    infoFg = Color(0xFF7FD3A8),
    online = Color(0xFF4CC98A),
    verifiedBg = Color(0xFF17301F),
    verifiedFg = Color(0xFF7FD3A8),
    warningBg = Color(0xFF352A12),
    warningFg = Color(0xFFF2C46D),
)

val LocalAppPalette = staticCompositionLocalOf { LightAppPalette }
