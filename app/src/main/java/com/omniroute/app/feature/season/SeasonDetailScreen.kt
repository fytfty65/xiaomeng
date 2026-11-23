package com.omniroute.app.feature.season

import com.omniroute.app.core.ui.theme.FolioSerif
import com.omniroute.app.core.ui.theme.FolioDigits
import androidx.compose.foundation.layout.*
import com.omniroute.app.core.ui.components.CoverBack
import com.omniroute.app.core.ui.components.CoverStatusShade
import com.omniroute.app.core.ui.SystemBarIcons
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.platform.LocalConfiguration
import com.omniroute.app.core.ui.components.SeasonArtwork
import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniroute.app.R
import com.omniroute.app.core.ui.theme.JourneySeason
import com.omniroute.app.core.ui.theme.LocalAppPalette
import com.omniroute.app.core.ui.theme.SeasonEditionTheme
import com.omniroute.app.core.ui.theme.edition

@Composable
fun SeasonDetailScreen(
    season: JourneySeason,
    onStartPlanning: (JourneySeason) -> Unit,
    onBack: () -> Unit,
) {
    var preview by rememberSaveable(season) { mutableStateOf(season) }
    SeasonEditionTheme(preview) {
        SeasonChapter(preview, { preview = it }, { onStartPlanning(preview) }, onBack)
    }
}

@Composable
private fun SeasonChapter(
    season: JourneySeason,
    onSelect: (JourneySeason) -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit,
) {
    val palette = LocalAppPalette.current
    val style = season.edition
    val context = LocalContext.current
    var copiedIndex by rememberSaveable(season) { mutableStateOf<Int?>(null) }
    SystemBarIcons(false)
    val ivory = Color(0xFFF7F5ED)
    Box(Modifier.fillMaxSize().background(style.anchor)) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))) {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                Box(Modifier.fillMaxWidth().heightIn(min = (LocalConfiguration.current.screenHeightDp * 0.62f).dp.coerceIn(320.dp, 620.dp))) {
                    SeasonArtwork(Modifier.matchParentSize(), season = season, cover = true)
                    Box(Modifier.matchParentSize().background(Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.18f), 0.32f to Color.Transparent,
                        0.64f to style.anchor.copy(alpha = 0.24f), 1f to style.anchor)))
                    Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 25.dp, vertical = 14.dp)) {
                        val number = (season.ordinal + 1).toString().padStart(2, '0')
                        Text("${season.name.uppercase()}   /   CHAPTER   $number", fontFamily = FolioDigits,
                            fontSize = 15.sp, letterSpacing = 2.sp, color = ivory)
                        Text(stringResource(season.titleRes), fontFamily = FolioSerif, fontSize = 43.sp,
                            lineHeight = 58.sp, color = Color.White)
                        Text(stringResource(season.phraseRes), fontFamily = FolioSerif, color = ivory, fontSize = 16.sp)
                    }
                    Box(Modifier.statusBarsPadding().padding(start = 12.dp)) { CoverBack(onBack) }
                }
                Column(Modifier.padding(start = 25.dp, end = 25.dp, top = 12.dp, bottom = 25.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(season.noteRes), color = ivory.copy(alpha = 0.84f), fontSize = 14.sp, lineHeight = 22.sp)
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        listOf(style.paper, style.secondary, style.accent).forEachIndexed { i, ink ->
                            val hex = "#%06X".format(ink.toArgb() and 0xFFFFFF)
                            Column(Modifier.weight(1f).clickable(role = Role.Button) {
                                context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(hex, hex))
                                copiedIndex = i
                            }) {
                                Box(Modifier.fillMaxWidth().height(42.dp).background(ink))
                                Text((if (copiedIndex == i) "✓ " else "") + hex, color = ivory, fontFamily = FolioDigits,
                                    fontSize = 12.sp, letterSpacing = 1.sp, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                    Text(stringResource(season.communityRes), color = ivory.copy(alpha = 0.76f),
                        fontFamily = FolioSerif, fontSize = 14.sp, lineHeight = 22.sp)
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.selectableGroup()) {
                    JourneySeason.entries.forEach { option ->
                        val active = option == season
                        Text(stringResource(option.labelRes), color = ivory.copy(alpha = if (active) 1f else 0.65f),
                            fontFamily = FolioSerif, fontSize = 16.sp, modifier = Modifier.selectable(active,
                                role = Role.RadioButton) { onSelect(option) }.padding(horizontal = 8.dp, vertical = 12.dp))
                    }
                }
                Spacer(Modifier.weight(1f))
                Surface(onClick = onStart, shape = RoundedCornerShape(5.dp), color = ivory) {
                    Text(stringResource(R.string.season_page_start), color = style.anchor, fontFamily = FolioSerif, fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp))
                }
            }
        }
        CoverStatusShade()
    }
}
