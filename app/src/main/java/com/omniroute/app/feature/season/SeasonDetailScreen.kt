package com.omniroute.app.feature.season

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.scale
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniroute.app.R
import com.omniroute.app.core.ui.ContentMaxWidth
import com.omniroute.app.core.ui.components.SeasonGallery
import com.omniroute.app.core.ui.theme.EditionSurface
import com.omniroute.app.core.ui.theme.JourneySeason
import com.omniroute.app.core.ui.theme.LocalAppPalette
import com.omniroute.app.core.ui.theme.OmniSeasonLogo
import com.omniroute.app.core.ui.theme.SeasonEditionTheme
import com.omniroute.app.core.ui.theme.edition
import com.omniroute.app.core.ui.theme.seasonBackdrop
import com.omniroute.app.feature.auth.SeasonHeroImage
import com.omniroute.app.feature.auth.heroSeason

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
    Column(
        modifier = Modifier.fillMaxSize().seasonBackdrop()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.weight(1f).widthIn(max = ContentMaxWidth)
                .fillMaxWidth().verticalScroll(rememberScrollState()),
        ) {
            Box(Modifier.fillMaxWidth().height(335.dp).clipToBounds()) {
                SeasonHeroImage(season.heroSeason())
                Box(Modifier.matchParentSize().background(
                    Brush.verticalGradient(
                        0f to Color(0x660C1820),
                        0.40f to Color.Transparent,
                        0.66f to Color(0xC6172329),
                        0.80f to palette.background.copy(alpha = 0.92f),
                        1f to palette.background,
                    ),
                ))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(shape = CircleShape, color = Color(0x660D1921)) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, stringResource(R.string.season_page_back), tint = Color.White)
                        }
                    }
                    Surface(shape = CircleShape, color = Color(0x660D1921)) {
                        OmniSeasonLogo(Modifier.padding(7.dp), season, 34.dp, Color.White)
                    }
                }
                Column(
                    modifier = Modifier.align(Alignment.BottomStart)
                        .fillMaxWidth().background(Brush.verticalGradient(
                            listOf(Color.Transparent, palette.background.copy(alpha = 0.92f)),
                        )).padding(start = 22.dp, end = 22.dp, top = 46.dp, bottom = 7.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        stringResource(season.tagRes), color = palette.brand,
                        fontFamily = FontFamily.Monospace, fontSize = 11.sp, letterSpacing = 1.8.sp,
                    )
                    Text(
                        stringResource(season.titleRes), color = palette.ink,
                        fontFamily = style.titleFont, fontWeight = style.titleWeight,
                        fontSize = 36.sp, lineHeight = 42.sp,
                    )
                    Text(stringResource(season.phraseRes), color = palette.muted, fontSize = 14.sp)
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 15.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.width(2.dp).height(22.dp).background(style.accent))
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        ChapterLabel(R.string.season_page_journal)
                        Text(
                            stringResource(season.journalRes), color = palette.ink,
                            fontFamily = style.titleFont, fontWeight = style.titleWeight,
                            fontSize = 23.sp, lineHeight = 29.sp,
                        )
                        Text(stringResource(season.noteRes), color = palette.muted, fontSize = 13.sp, lineHeight = 21.sp)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    ChapterLabel(R.string.season_page_palette)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val names = when (season) {
                            JourneySeason.Spring -> listOf("樱白", "青瓷绿", "胭脂樱")
                            JourneySeason.Summer -> listOf("海盐白", "汽水橘", "琉璃蓝")
                            JourneySeason.Autumn -> listOf("宣纸白", "银杏琥珀", "焦糖枫")
                            JourneySeason.Winter -> listOf("初雪紫", "冰川蓝", "暮雪鸢尾")
                        }
                        listOf(style.paper, style.secondary, style.accent).forEachIndexed { index, color ->
                            val hex = "#%06X".format(color.toArgb() and 0xFFFFFF)
                            val scale by animateFloatAsState(if (index == copiedIndex) 0.96f else 1f, label = "color swatch")
                            Surface(
                                onClick = {
                                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                                    clipboard?.setPrimaryClip(ClipData.newPlainText(names[index], hex))
                                    copiedIndex = index
                                },
                                modifier = Modifier.weight(1f).scale(scale),
                                shape = RoundedCornerShape(9.dp),
                                color = EditionSurface,
                            ) {
                                Column(Modifier.padding(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Box(Modifier.fillMaxWidth().height(25.dp).clip(RoundedCornerShape(5.dp)).background(color))
                                    Text(names[index], color = palette.ink, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 3.dp))
                                    Text(
                                        hex, modifier = Modifier.padding(horizontal = 3.dp),
                                        color = palette.ink, fontFamily = FontFamily.Monospace, fontSize = 10.sp,
                                    )
                                }
                            }
                        }
                    }
                }
                Surface(shape = RoundedCornerShape(19.dp), color = EditionSurface) {
                    Row(Modifier.fillMaxWidth().padding(15.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("“", color = style.accent, fontSize = 34.sp, fontFamily = FontFamily.Serif)
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            ChapterLabel(R.string.season_page_community)
                            Text(stringResource(season.communityRes), color = palette.ink, fontSize = 14.sp, lineHeight = 21.sp)
                        }
                    }
                }
            }
        }

        Column(
            Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth().background(palette.background.copy(alpha = 0.96f))
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SeasonGallery(season, onSelect)
            Row(
                Modifier.fillMaxWidth().heightIn(min = 50.dp).clip(CircleShape)
                    .background(style.actionGradient)
                    .clickable(role = Role.Button, onClick = onStart).padding(horizontal = 17.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OmniSeasonLogo(diameter = 24.dp, tint = Color.White)
                Text(stringResource(R.string.season_page_start), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Icon(Icons.Filled.ArrowForward, null, tint = Color.White, modifier = Modifier.size(17.dp))
            }
        }
    }
}

@Composable
private fun ChapterLabel(label: Int) {
    Text(stringResource(label), color = LocalAppPalette.current.brand, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.0.sp)
}
