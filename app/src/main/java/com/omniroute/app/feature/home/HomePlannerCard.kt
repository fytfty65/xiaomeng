package com.omniroute.app.feature.home

import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.omniroute.app.core.ui.theme.portalArt

import com.omniroute.app.core.ui.theme.spatialIsland
import com.omniroute.app.core.ui.theme.FolioSerif
import androidx.compose.foundation.border
import androidx.compose.animation.*

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniroute.app.R
import com.omniroute.app.core.ui.theme.JourneySeason
import com.omniroute.app.core.ui.theme.LocalAppPalette
import kotlin.math.abs

private val HeroInk = Color(0xFF111D20)
private val RouteSteps = listOf(R.string.home_intent, R.string.home_gaming, R.string.home_route)

@Composable
internal fun RoutePlannerHeroCard(
    season: JourneySeason,
    onSeasonChange: (JourneySeason) -> Unit,
    onOpenSeason: () -> Unit,
    onQuickPlan: (String, Int) -> Unit,
    onOpenPlanner: () -> Unit,
    onInteract: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SeasonalHero(season, onSeasonChange, onOpenSeason, onInteract)
        Box(Modifier.fillMaxWidth().spatialIsland(24.dp)) {
            QuickPlanForm(season, onQuickPlan, onOpenPlanner, onInteract)
        }
    }
}

@Composable
private fun SeasonalHero(
    season: JourneySeason,
    onSeasonChange: (JourneySeason) -> Unit,
    onOpenSeason: () -> Unit,
    onInteract: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)).border(.8.dp, Color.White.copy(alpha = .28f), RoundedCornerShape(20.dp))
            .background(HeroInk)
            .pointerInput(season) {
                var drag = 0f
                detectHorizontalDragGestures(
                    onDragStart = { onInteract(); drag = 0f },
                    onDragCancel = { drag = 0f },
                    onHorizontalDrag = { _, amount -> drag += amount },
                    onDragEnd = {
                        if (abs(drag) >= 40.dp.toPx()) {
                            val seasons = JourneySeason.entries
                            val step = if (drag < 0f) 1 else -1
                            onSeasonChange(seasons[(season.ordinal + step + seasons.size) % seasons.size])
                        }
                    },
                )
            },
    ) {
        AnimatedContent(season, modifier = Modifier.matchParentSize(), transitionSpec = {
            (fadeIn(tween(450)) + scaleIn(tween(450), initialScale = 1.03f)) togetherWith fadeOut(tween(450))
        }, label = "seasonal cabin") { target ->
            Image(painterResource(target.portalArt.window), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Box(
            modifier = Modifier.matchParentSize().drawBehind {
                drawRect(
                    Brush.horizontalGradient(
                        0f to HeroInk.copy(alpha = 0.65f),
                        0.40f to HeroInk.copy(alpha = 0.24f),
                        0.80f to Color.Transparent,
                    ),
                )
                drawRect(
                    Brush.verticalGradient(
                        0f to HeroInk.copy(alpha = 0.34f),
                        0.42f to Color.Transparent,
                        0.64f to HeroInk.copy(alpha = 0.14f),
                        1f to HeroInk.copy(alpha = 0.83f),
                    ),
                )
            },
        )
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 11.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EditionMenu(season, onSeasonChange, onInteract)
                Surface(
                    onClick = onOpenSeason,
                    shape = CircleShape,
                    color = HeroInk.copy(alpha = 0.42f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
                ) {
                    Row(
                        modifier = Modifier.heightIn(min = 34.dp).padding(horizontal = 9.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.home_season_enter), color = Color.White, fontSize = 11.sp)
                        Icon(Icons.Filled.ArrowForward, null, tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            AnimatedContent(season, transitionSpec = {
                (fadeIn(tween(320, 100)) + slideInVertically { it / 7 }) togetherWith fadeOut(tween(150))
            }, label = "window caption") { page ->
                Column {
                    Text(stringResource(page.titleRes), fontFamily = FolioSerif, color = Color.White, fontSize = 24.sp, lineHeight = 30.sp)
                    Text(stringResource(page.phraseRes), color = Color.White.copy(alpha = .93f), fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(9.dp))
            HeroPath()
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                RouteSteps.forEach { label ->
                    Text(
                        stringResource(label),
                        color = Color.White.copy(alpha = 0.93f),
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EditionMenu(season: JourneySeason, onSeasonChange: (JourneySeason) -> Unit, onInteract: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    val palette = LocalAppPalette.current
    val switchLabel = stringResource(R.string.home_switch_season)
    Box {
        Surface(
            onClick = { onInteract(); open = true },
            shape = CircleShape,
            color = HeroInk.copy(alpha = 0.42f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
            modifier = Modifier.semantics { contentDescription = switchLabel },
        ) {
            Row(
                modifier = Modifier.heightIn(min = 34.dp).padding(horizontal = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(season.tagRes),
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    letterSpacing = 0.6.sp,
                )
                Icon(Icons.Filled.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = palette.surface) {
            JourneySeason.entries.forEach { choice ->
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(R.string.home_season_choice, stringResource(choice.labelRes))
                                + "  ·  " + stringResource(choice.tagRes),
                        )
                    },
                    onClick = {
                        open = false
                        onSeasonChange(choice)
                    },
                    trailingIcon = if (choice == season) {
                        { Icon(Icons.Filled.Check, null, tint = palette.brand) }
                    } else null,
                )
            }
        }
    }
}

@Composable
private fun HeroPath() {
    val trace = rememberInfiniteTransition(label = "hero_route")
    val progress by trace.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing)),
        label = "hero_route_phase",
    )
    Spacer(
        Modifier.fillMaxWidth().height(12.dp).drawWithCache {
            // 与下方三个等宽文字列共用中心坐标。
            val first = Offset(size.width / 6f, size.height * 0.62f)
            val second = Offset(size.width / 2f, size.height * 0.22f)
            val third = Offset(size.width * 5f / 6f, size.height * 0.47f)
            val points = listOf(first, second, third)
            val path = Path().apply {
                moveTo(first.x, first.y)
                cubicTo(size.width * 0.31f, first.y, size.width * 0.37f, second.y, second.x, second.y)
                cubicTo(size.width * 0.63f, second.y, size.width * 0.71f, third.y, third.x, third.y)
            }
            val dash = 4.dp.toPx()
            val intervals = floatArrayOf(dash, dash)
            onDrawBehind {
                drawPath(
                    path,
                    Color.White.copy(alpha = 0.64f),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(intervals, -progress * dash * 6f),
                    ),
                )
                points.forEach { point ->
                    drawCircle(Color.White.copy(alpha = 0.22f), 5.5.dp.toPx(), point)
                    drawCircle(Color.White, 2.6.dp.toPx(), point)
                }
            }
        },
    )
}

@Composable
private fun QuickPlanForm(
    season: JourneySeason,
    onQuickPlan: (String, Int) -> Unit,
    onOpenPlanner: () -> Unit,
    onInteract: () -> Unit,
) {
    val palette = LocalAppPalette.current
    var query by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val fieldLabel = stringResource(R.string.home_planner_input)
    val suggestions = stringArrayResource(
        when (season) {
            JourneySeason.Spring -> R.array.home_suggestions_spring
            JourneySeason.Summer -> R.array.home_suggestions_summer
            JourneySeason.Autumn -> R.array.home_suggestions_autumn
            JourneySeason.Winter -> R.array.home_suggestions_winter
        },
    )
    fun submit() {
        val plan = parseQuickPlan(query)
        error = plan == null
        if (plan != null) {
            focusManager.clearFocus()
            onQuickPlan(plan.city, plan.days)
        }
    }

    Column(Modifier.fillMaxWidth().padding(start = 13.dp, end = 13.dp, top = 4.dp, bottom = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                stringResource(R.string.home_new_trip),
                color = palette.ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = {
                focusManager.clearFocus()
                onOpenPlanner()
            }) {
                Text(stringResource(R.string.home_planner_full), fontSize = 12.sp)
            }
        }
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = palette.fieldBg,
            border = BorderStroke(
                1.dp,
                when {
                    error -> palette.errorFg
                    focused -> palette.brand
                    else -> palette.border
                },
            ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = {
                        if (it.length <= 80) {
                            query = it
                            error = false
                        }
                    },
                    modifier = Modifier.weight(1f).onFocusChanged { focused = it.isFocused; if (it.isFocused) onInteract() }
                        .semantics { contentDescription = fieldLabel },
                    textStyle = TextStyle(color = palette.ink, fontSize = 14.sp),
                    cursorBrush = SolidColor(palette.brand),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { submit() }),
                    decorationBox = { inner ->
                        Box(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (query.isEmpty()) {
                                Text(
                                    stringResource(R.string.home_planner_placeholder),
                                    color = palette.muted,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            inner()
                        }
                    },
                )
                Surface(
                    onClick = { submit() },
                    shape = RoundedCornerShape(12.dp),
                    color = palette.brand,
                    contentColor = palette.onBrand,
                ) {
                    Row(
                        modifier = Modifier.heightIn(min = 44.dp).padding(horizontal = 9.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.home_planner_go), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Filled.ArrowForward, null, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
        if (error) {
            Spacer(Modifier.height(5.dp))
            Text(
                stringResource(R.string.home_planner_input_error),
                color = palette.errorFg,
                fontSize = 12.sp,
            )
        }
        Spacer(Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier.semantics { isTraversalGroup = true },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            suggestions.forEach { city ->
                Surface(
                    onClick = {
                        error = false
                        focusManager.clearFocus()
                        onQuickPlan(city, 3)
                    },
                    shape = CircleShape,
                    color = palette.brandTint,
                ) {
                    Box(
                        modifier = Modifier.heightIn(min = 35.dp).padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(city, color = palette.brandDark, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
