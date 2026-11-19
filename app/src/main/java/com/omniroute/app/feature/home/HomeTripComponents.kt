package com.omniroute.app.feature.home

import android.content.Context
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniroute.app.R
import com.omniroute.app.core.network.dto.TripPlanDto
import com.omniroute.app.core.network.dto.UserDto
import com.omniroute.app.core.ui.theme.LocalAppPalette
import com.omniroute.app.core.ui.theme.LocalJourneySeason
import com.omniroute.app.feature.auth.BrandOrange
import com.omniroute.app.feature.auth.CardBorder
import com.omniroute.app.feature.auth.ErrorFg
import com.omniroute.app.feature.auth.Ink
import com.omniroute.app.feature.auth.Muted
import com.omniroute.app.feature.auth.SeasonHeroImage
import com.omniroute.app.feature.auth.heroSeason

/**
 * 核心规划卡片：
 * 上半部分是四季动态场景（季节插画 + 季节专属文案 + 航线流光），下半部分是纯白操作区
 */
@Composable
internal fun RoutePlannerHeroCard(
    onClick: () -> Unit,
    onOpenSeason: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        label = "hero_scale",
    )
    val season = LocalJourneySeason.current
    // 航线流光：虚线相位平移 + 沿线移动的光点，复刻 Web 端 hero 的路线动效。
    val trace = rememberInfiniteTransition(label = "route-trace")
    val traceProgress by trace.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 5_200, easing = LinearEasing)),
        label = "route-progress",
    )

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = BrandOrange.copy(alpha = 0.12f),
            )
            .clip(RoundedCornerShape(22.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 上半部：四季动态场景（插画 + 花瓣/萤火/落叶/飘雪 + 秋千摆动 + 航线流光）
            // 点击场景进入当前季节的专属页面；下方按钮仍进入规划流程。
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clipToBounds()
                    .clickable(role = Role.Button, onClick = onOpenSeason),
            ) {
                SeasonHeroImage(season = season.heroSeason())

                // 压暗蒙层：顶部托住季节标签，底部托住航线与节点文案
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            drawRect(
                                Brush.verticalGradient(
                                    0f to Color.Black.copy(alpha = 0.34f),
                                    0.42f to Color.Black.copy(alpha = 0.06f),
                                    1f to Color.Black.copy(alpha = 0.66f),
                                ),
                            )
                            drawRect(
                                Brush.horizontalGradient(
                                    0f to Color.Black.copy(alpha = 0.36f),
                                    0.72f to Color.Transparent,
                                ),
                            )
                        },
                )

                // 绘制平滑的虚线航迹与等高线背景
                val brand = Color.White
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // 装饰性浅色等高线弧
                    drawCircle(
                        color = brand.copy(alpha = 0.06f),
                        radius = h * 1.1f,
                        center = Offset(w * 0.85f, h * 0.2f),
                    )
                    drawCircle(
                        color = brand.copy(alpha = 0.08f),
                        radius = h * 0.65f,
                        center = Offset(w * 0.85f, h * 0.2f),
                    )

                    // 连接三个节点的 S 型路线曲线
                    val startX = w * 0.16f
                    val startY = h * 0.68f
                    val midX = w * 0.50f
                    val midY = h * 0.36f
                    val endX = w * 0.84f
                    val endY = h * 0.56f

                    val curvePath = Path().apply {
                        moveTo(startX, startY)
                        cubicTo(
                            w * 0.32f, startY,
                            w * 0.35f, midY,
                            midX, midY,
                        )
                        cubicTo(
                            w * 0.65f, midY,
                            w * 0.68f, endY,
                            endX, endY,
                        )
                    }

                    // 路线底层柔和投影线
                    drawPath(
                        path = curvePath,
                        color = brand.copy(alpha = 0.18f),
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
                    )

                    // 路线主虚线（相位随流光平移）
                    drawPath(
                        path = curvePath,
                        color = brand,
                        style = Stroke(
                            width = 2.2.dp.toPx(),
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(
                                intervals = floatArrayOf(12f, 10f),
                                phase = -traceProgress * 44f,
                            ),
                        ),
                    )

                    // 沿线巡游的光点，呼应 Web 端 hero 的路线动效
                    val pathMeasure = PathMeasure().apply { setPath(curvePath, false) }
                    val runner = pathMeasure.getPosition(pathMeasure.length * traceProgress)
                    drawCircle(
                        color = Color.White.copy(alpha = 0.26f),
                        radius = 8.dp.toPx(),
                        center = runner,
                    )
                    drawCircle(color = Color.White, radius = 2.6.dp.toPx(), center = runner)

                    // 节点 1：出发地圆点
                    drawCircle(color = Color.White, radius = 6.dp.toPx(), center = Offset(startX, startY))
                    drawCircle(color = Color.Black.copy(alpha = 0.45f), radius = 3.5.dp.toPx(), center = Offset(startX, startY))

                    // 节点 2：途经最优解圆点
                    drawCircle(color = Color.White, radius = 6.dp.toPx(), center = Offset(midX, midY))
                    drawCircle(color = Color.Black.copy(alpha = 0.45f), radius = 3.5.dp.toPx(), center = Offset(midX, midY))

                    // 节点 3：目的地光晕圆点
                    drawCircle(
                        color = brand.copy(alpha = 0.22f),
                        radius = 12.dp.toPx(),
                        center = Offset(endX, endY),
                    )
                    drawCircle(color = Color.White, radius = 6.5.dp.toPx(), center = Offset(endX, endY))
                    drawCircle(color = Color.Black.copy(alpha = 0.45f), radius = 4.dp.toPx(), center = Offset(endX, endY))
                }

                // 左上角：季节期号 + 季节专属文案（对齐 Web hero 的英文期号/标题/短句）
                Column(
                    modifier = Modifier
                        .padding(start = 16.dp, top = 14.dp)
                        .align(Alignment.TopStart),
                ) {
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
                    Spacer(modifier = Modifier.height(9.dp))
                    Text(
                        text = stringResource(season.titleRes),
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = stringResource(season.phraseRes),
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.88f),
                    )
                }

                // 右上角：进入季节专属页面的提示
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.18f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 14.dp, end = 14.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.home_season_enter),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }

                // 底部三个节点对应的小标签
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.home_intent),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.82f),
                    )
                    Text(
                        text = stringResource(R.string.home_gaming),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                    Text(
                        text = stringResource(R.string.home_route),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.82f),
                    )
                }
            }

            HorizontalDivider(color = SubtleLine, thickness = 1.dp)

            // 下半部：纯白底文案与主行动按钮
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.home_new_trip),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Ink,
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = stringResource(R.string.home_new_trip_sub),
                            fontSize = 12.5.sp,
                            color = Muted,
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // 右侧高对比度行动胶囊
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(BrandOrange)
                            .padding(horizontal = 16.dp, vertical = 11.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.home_start_plan),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite,
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Icon(
                                imageVector = Icons.Filled.ArrowForward,
                                contentDescription = null,
                                tint = PureWhite,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 精心排版的空状态卡片：包含层叠路书微缩插画，且绝不出现单字换行 */
@Composable
internal fun ModernEmptyTripsCard(onStartPlanning: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 拟物微缩路书卡片插画（用双层卡片叠放代替单调的图标）
            Box(
                modifier = Modifier.size(width = 72.dp, height = 52.dp),
                contentAlignment = Alignment.Center,
            ) {
                // 底层斜卡片
                Box(
                    modifier = Modifier
                        .size(width = 56.dp, height = 38.dp)
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AccentOrangeBg)
                )
                // 顶层主卡片（带模拟路线条）
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = PureWhite,
                    border = BorderStroke(1.2.dp, BrandOrange.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .size(width = 58.dp, height = 40.dp)
                        .align(Alignment.BottomStart),
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(BrandOrange),
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Box(
                                modifier = Modifier
                                    .height(4.dp)
                                    .fillMaxWidth(0.75f)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(BrandOrange.copy(alpha = 0.25f)),
                            )
                        }
                        Box(
                            modifier = Modifier
                                .height(3.5.dp)
                                .fillMaxWidth(0.5f)
                                .clip(RoundedCornerShape(2.dp))
                                .background(SubtleLine),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = stringResource(R.string.home_empty_title),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Ink,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.home_empty_sub),
                fontSize = 12.5.sp,
                color = Muted,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = AccentOrangeBg,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button, onClick = onStartPlanning),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.home_empty_action),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandOrange,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.ArrowForward,
                        contentDescription = null,
                        tint = BrandOrange,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
        }
    }
}

/** 已保存的行程卡片：左侧天数徽章 + 中间标题与胶囊标签 + 右侧箭头 */
@Composable
internal fun ModernTripCard(
    trip: TripPlanDto,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val city = trip.destCity?.trim().orEmpty()
    val days = if (trip.days > 0) "${trip.days}" else "1"

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 左侧：暖色行程天数方块
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(WarmCanvas)
                    .border(1.dp, AccentOrangeBg, RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = days,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BrandOrange,
                    )
                    Text(
                        text = "DAYS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandOrange.copy(alpha = 0.7f),
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // 中间：标题与元数据胶囊
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tripTitle(context, trip),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (city.isNotEmpty()) {
                        TripTag(text = city, isHighlight = true)
                    }
                    if (trip.travelers > 0) {
                        TripTag(text = stringResource(R.string.home_trip_travelers, trip.travelers))
                    }
                    val date = trip.createdAt?.take(10).orEmpty()
                    if (date.length == 10) {
                        TripTag(text = date)
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(SoftTagBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Muted,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
internal fun TripTag(text: String, isHighlight: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isHighlight) AccentOrangeBg else SoftTagBg,
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = if (isHighlight) FontWeight.SemiBold else FontWeight.Medium,
            color = if (isHighlight) BrandOrange else Muted,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
        )
    }
}

private fun tripTitle(context: Context, trip: TripPlanDto): String {
    val title = trip.title?.trim().orEmpty()
    if (title.isNotEmpty()) {
        return title
    }
    val city = trip.destCity?.trim().orEmpty()
    return if (city.isNotEmpty()) {
        context.getString(R.string.home_trip_title, city)
    } else {
        context.getString(R.string.home_trip_unnamed)
    }
}

