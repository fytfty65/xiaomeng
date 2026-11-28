package com.omniroute.app.core.ui.components

import com.omniroute.app.core.ui.theme.spatialIsland
import com.omniroute.app.core.ui.theme.OmniSeasonLogo
import com.omniroute.app.core.ui.theme.FolioSerif
import com.omniroute.app.core.ui.theme.FolioDigits
import androidx.compose.material3.Text
import androidx.compose.ui.unit.sp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.omniroute.app.R

@Composable
internal fun CoverBack(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Box(Modifier.size(36.dp).background(Color.Black.copy(alpha = 0.38f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.common_back), tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
internal fun CoverStatusShade() {
    Box(Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars)
        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.53f), Color.Black.copy(alpha = 0.29f)))))
}

/** Compact navigation label; an adjacent action retains its own touch target. */
@Composable
internal fun FloatingTitle(title: String, subtitle: String? = null) {
    val palette = com.omniroute.app.core.ui.theme.LocalAppPalette.current
    Row(Modifier.spatialIsland(24.dp).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        OmniSeasonLogo(diameter = 20.dp, tint = palette.brand)
        Column {
            Text(title, color = palette.ink, fontFamily = FolioSerif, fontSize = 15.sp, lineHeight = 20.sp)
            subtitle?.let { Text(it, color = palette.muted, fontFamily = FolioDigits,
                fontSize = 10.sp, lineHeight = 12.sp, letterSpacing = 1.sp) }
        }
    }
}
