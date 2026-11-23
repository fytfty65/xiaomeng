package com.omniroute.app.core.ui.components

import com.omniroute.app.core.ui.theme.FolioSerif
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.aspectRatio
import com.omniroute.app.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniroute.app.core.ui.theme.JourneySeason
import com.omniroute.app.core.ui.theme.LocalAppPalette
import com.omniroute.app.core.ui.theme.LocalJourneySeason
import com.omniroute.app.core.ui.theme.edition

@Composable
internal fun SeasonArtwork(
    modifier: Modifier = Modifier,
    season: JourneySeason = LocalJourneySeason.current,
    portrait: Boolean = false,
    cover: Boolean = false,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(modifier) {
        Image(
            painter = painterResource(when {
                cover -> when (season) {
                    JourneySeason.Spring -> R.drawable.pass_cover_spring
                    JourneySeason.Summer -> R.drawable.pass_cover_summer
                    JourneySeason.Autumn -> R.drawable.pass_cover_autumn
                    JourneySeason.Winter -> R.drawable.pass_cover_winter
                }
                portrait -> season.edition.portrait
                else -> season.edition.image
            }),
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
            val active = season == selected
            val edge = if (active) palette.brand else palette.divider
            Column(Modifier.weight(1f).border(if (active) 1.5.dp else 0.7.dp, edge)
                .background(palette.surface).selectable(active, role = Role.RadioButton) { onSelect(season) }.padding(3.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                SeasonArtwork(Modifier.fillMaxWidth().aspectRatio(0.80f), season = season, cover = true)
                Text(stringResource(season.labelRes), modifier = Modifier.padding(vertical = 3.dp),
                    color = if (active) palette.brand else palette.ink, fontFamily = FolioSerif, fontSize = 15.sp)
            }
        }
    }
}
