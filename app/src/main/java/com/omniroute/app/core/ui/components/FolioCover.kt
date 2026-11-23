package com.omniroute.app.core.ui.components

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
