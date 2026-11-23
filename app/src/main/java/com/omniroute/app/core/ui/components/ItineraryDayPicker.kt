package com.omniroute.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import com.omniroute.app.core.ui.theme.LocalAppPalette
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun ItineraryDayPicker(
    days: List<Int>,
    selected: Int?,
    onSelect: (Int?) -> Unit,
    overview: Boolean = false,
) {
    val palette = LocalAppPalette.current
    Row(Modifier.horizontalScroll(rememberScrollState()).clip(CircleShape).background(palette.brandTint)
        .selectableGroup().padding(3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        val options = if (overview) listOf(null) + days else days
        options.forEach { day ->
            val active = selected == day
            val tile = if (active) Modifier.shadow(2.dp, CircleShape).background(palette.surface, CircleShape) else Modifier
            Text(day?.let { "第 ${it} 天" } ?: "总览", fontSize = 12.sp, color = if (active) palette.brand else palette.muted,
                modifier = tile.clip(CircleShape).selectable(active, role = Role.Tab) { onSelect(day) }
                    .padding(horizontal = 13.dp, vertical = 8.dp))
        }
    }
}
