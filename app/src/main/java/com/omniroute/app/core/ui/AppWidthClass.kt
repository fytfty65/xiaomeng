package com.omniroute.app.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

/** 与 Material 3 Window Size Class 相同的宽度断点（600 / 840dp）。 */
internal enum class AppWidthClass { Compact, Medium, Expanded }

@Composable
internal fun rememberAppWidthClass(): AppWidthClass {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return when {
        widthDp >= 840 -> AppWidthClass.Expanded
        widthDp >= 600 -> AppWidthClass.Medium
        else -> AppWidthClass.Compact
    }
}

/** 正文最大宽度：平板上避免内容被横向拉成超长行。 */
internal val ContentMaxWidth = 840.dp
