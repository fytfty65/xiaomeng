package com.omniroute.app.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

@Composable
fun OmniRouteTheme(
    season: JourneySeason = JourneySeason.ofCurrentMonth(),
    darkTheme: Boolean = isSystemInDarkTheme(),
    // 品牌色优先于系统动态取色：关闭 dynamicColor，避免不同机型把季节色替换掉。
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val palette = season.palette(darkTheme)
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        // Material 组件（Snackbar、ripple、TextField 光标等）跟随季节主色。
        darkTheme -> darkColorScheme(
            primary = palette.brand,
            onPrimary = palette.onBrand,
            primaryContainer = palette.brandTint,
            onPrimaryContainer = palette.ink,
            secondary = palette.brandDark,
            onSecondary = palette.onBrand,
            secondaryContainer = palette.tagBg,
            onSecondaryContainer = palette.ink,
            background = palette.background,
            surface = palette.surface,
            surfaceVariant = palette.fieldBg,
            onSurfaceVariant = palette.muted,
            outline = palette.border,
            error = palette.errorFg,
        )

        else -> lightColorScheme(
            primary = palette.brand,
            onPrimary = palette.onBrand,
            primaryContainer = palette.brandTint,
            onPrimaryContainer = palette.ink,
            secondary = palette.brandDark,
            onSecondary = palette.onBrand,
            secondaryContainer = palette.tagBg,
            onSecondaryContainer = palette.ink,
            background = palette.background,
            surface = palette.surface,
            surfaceVariant = palette.fieldBg,
            onSurfaceVariant = palette.muted,
            outline = palette.border,
            error = palette.errorFg,
        )
    }
    CompositionLocalProvider(
        LocalAppPalette provides palette,
        LocalJourneySeason provides season,
    ) {
        MaterialTheme(colorScheme = colorScheme, typography = AppTypography, content = content)
    }
}
