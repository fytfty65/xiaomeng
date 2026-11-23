package com.omniroute.app.core.ui.components

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
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        if (overview) {
            FilterChip(selected == null, onClick = { onSelect(null) }, label = { Text("总览", fontSize = 12.sp) })
        }
        days.forEach { day ->
            FilterChip(selected == day, onClick = { onSelect(day) }, label = { Text("第 ${day} 天", fontSize = 12.sp) })
        }
    }
}
