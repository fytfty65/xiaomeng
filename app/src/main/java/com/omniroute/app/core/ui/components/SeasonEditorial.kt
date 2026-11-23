package com.omniroute.app.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniroute.app.core.ui.theme.JourneySeason
import com.omniroute.app.core.ui.theme.LocalAppPalette
import com.omniroute.app.core.ui.theme.LocalJourneySeason
import com.omniroute.app.core.ui.theme.OmniSeasonLogo
import com.omniroute.app.core.ui.theme.edition

@Composable
internal fun SeasonArtwork(
    modifier: Modifier = Modifier,
    season: JourneySeason = LocalJourneySeason.current,
    portrait: Boolean = false,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(modifier) {
        Image(
            painter = painterResource(if (portrait) season.edition.portrait else season.edition.image),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
        content()
    }
}

@Composable
internal fun SeasonGallery(
    selected: JourneySeason,
    onSelect: (JourneySeason) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalAppPalette.current
    Row(modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        JourneySeason.entries.forEach { season ->
            val shape = RoundedCornerShape(12.dp)
            val active = season == selected
            SeasonArtwork(
                modifier = Modifier.weight(1f).heightIn(min = 62.dp).clip(shape)
                    .border(if (active) 2.dp else 1.dp, if (active) palette.brand else palette.border, shape)
                    .selectable(active, role = Role.RadioButton) { onSelect(season) },
                season = season,
            ) {
                Box(Modifier.matchParentSize().background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color(0xB3192025))),
                ))
                OmniSeasonLogo(
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                    season = season,
                    diameter = 20.dp,
                    tint = if (season == JourneySeason.Winter) Color.White else season.edition.deep,
                )
                Text(
                    stringResource(season.labelRes),
                    modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 8.dp, vertical = 5.dp),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}
