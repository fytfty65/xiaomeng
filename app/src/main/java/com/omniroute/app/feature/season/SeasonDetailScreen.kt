package com.omniroute.app.feature.season

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniroute.app.R
import com.omniroute.app.core.ui.theme.JourneySeason
import com.omniroute.app.core.ui.theme.LocalAppPalette
import com.omniroute.app.core.ui.wheelScroll
import com.omniroute.app.feature.auth.SeasonHeroImage
import com.omniroute.app.feature.auth.heroSeason

/**
 * 季节专属页：首页动态场景点击后进入。
 *
 * <p>沿用当前季节的插画与动效，并展示 Web 端 journeySeason 里的季节手记、色彩灵感与同路人寄语；
 * 底部可直接切换到其他季节（全局配色同步变化）。
 */
@Composable
fun SeasonDetailScreen(
    season: JourneySeason,
    onSeasonChange: (JourneySeason) -> Unit,
    onStartPlanning: () -> Unit,
    onBack: () -> Unit,
) {
    val palette = LocalAppPalette.current
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(scrollState)
            .wheelScroll(scrollState),
    ) {
        // 顶部：四季动态场景（插画 + 粒子 + 秋千）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)),
        ) {
            SeasonHeroImage(season = season.heroSeason())

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawRect(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = 0.34f),
                                0.45f to Color.Black.copy(alpha = 0.08f),
                                1f to Color.Black.copy(alpha = 0.72f),
                            ),
                        )
                    },
            )

            // 返回
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.20f),
                modifier = Modifier
                    .padding(start = 16.dp, top = 16.dp)
                    .size(36.dp)
                    .clickable(role = Role.Button, onClick = onBack),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.season_page_back),
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp, end = 20.dp, bottom = 22.dp),
            ) {
                Text(
                    text = stringResource(R.string.season_page_eyebrow),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.4.sp,
                    color = Color.White.copy(alpha = 0.78f),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${stringResource(season.labelRes)} · ${stringResource(season.titleRes)}",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(season.phraseRes),
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.9f),
                )
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.18f),
                ) {
                    Text(
                        text = stringResource(season.tagRes),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.1.sp,
                        color = Color.White,
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Spacer(modifier = Modifier.height(22.dp))

            SectionLabel(text = stringResource(R.string.season_page_journal))
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(season.journalRes),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = palette.ink,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(season.noteRes),
                fontSize = 13.sp,
                color = palette.muted,
            )

            SectionDivider()

            SectionLabel(text = stringResource(R.string.season_page_palette))
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                listOf(palette.brand, palette.brandLight, palette.brandDark).forEach { color ->
                    Box(
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(color),
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(season.paletteRes),
                    fontSize = 13.sp,
                    color = palette.ink,
                )
            }

            SectionDivider()

            SectionLabel(text = stringResource(R.string.season_page_community))
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(season.communityRes),
                fontSize = 14.sp,
                color = palette.ink,
            )

            Spacer(modifier = Modifier.height(26.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = palette.brand,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clickable(role = Role.Button, onClick = onStartPlanning),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.season_page_start),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.onBrand,
                    )
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            SectionLabel(text = stringResource(R.string.season_page_switch))
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                JourneySeason.values().forEach { item ->
                    val selected = item == season
                    val shape = RoundedCornerShape(50)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(shape)
                            .background(if (selected) palette.brand else palette.tagBg)
                            .border(
                                width = 1.dp,
                                color = if (selected) Color.Transparent else palette.border,
                                shape = shape,
                            )
                            .clickable(role = Role.RadioButton) { onSeasonChange(item) }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(item.labelRes),
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) palette.onBrand else palette.ink,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
        color = LocalAppPalette.current.muted,
    )
}

@Composable
private fun SectionDivider() {
    Spacer(modifier = Modifier.height(20.dp))
    HorizontalDivider(color = LocalAppPalette.current.divider, thickness = 1.dp)
    Spacer(modifier = Modifier.height(20.dp))
}
