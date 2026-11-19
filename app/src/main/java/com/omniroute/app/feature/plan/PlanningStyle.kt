package com.omniroute.app.feature.plan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.omniroute.app.core.ui.components.OmniLogoMark
import com.omniroute.app.core.ui.theme.LocalAppPalette
import com.omniroute.app.feature.auth.BrandOrange

internal val PlanCardBg: Color
    @Composable get() = LocalAppPalette.current.surface
internal val PlanFormDivider: Color
    @Composable get() = LocalAppPalette.current.divider
internal val PlanTagBg: Color
    @Composable get() = LocalAppPalette.current.tagBg
internal val PlanOrangeLightBg: Color
    @Composable get() = LocalAppPalette.current.brandTint
internal val PlanSubtleBorder: Color
    @Composable get() = LocalAppPalette.current.border
internal val PlanOnlineGreen: Color
    @Composable get() = LocalAppPalette.current.online