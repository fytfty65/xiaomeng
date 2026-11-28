package com.omniroute.app.feature.notify

import com.omniroute.app.core.ui.components.FloatingTitle

import androidx.compose.animation.core.*

import com.omniroute.app.core.ui.theme.spatialIsland
import com.omniroute.app.core.ui.theme.world

import com.omniroute.app.core.ui.theme.inkFoil
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import com.omniroute.app.core.ui.theme.FolioSerif
import com.omniroute.app.core.ui.theme.FolioDigits
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import com.omniroute.app.core.ui.theme.editionRim
import com.omniroute.app.core.ui.theme.EditionSurface
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.omniroute.app.AppContainer
import com.omniroute.app.R
import com.omniroute.app.core.network.dto.RiskNotificationDto
import com.omniroute.app.feature.auth.BrandOrange
import com.omniroute.app.feature.auth.CardBorder
import com.omniroute.app.feature.auth.ErrorBg
import com.omniroute.app.feature.auth.ErrorFg
import com.omniroute.app.feature.auth.Ink
import com.omniroute.app.feature.auth.InlineBanner
import com.omniroute.app.feature.auth.Muted
import com.omniroute.app.core.ui.components.CircleIconButton
import com.omniroute.app.core.ui.theme.LocalAppPalette
import com.omniroute.app.core.ui.wheelScroll

private val CardBg: Color
    @Composable get() = LocalAppPalette.current.surface
private val TagBg: Color
    @Composable get() = LocalAppPalette.current.tagBg
private val OrangeLightBg: Color
    @Composable get() = LocalAppPalette.current.brandTint

/** 风险等级配色：后端只给出 high/medium/low 文本，未知值按提示级处理。 */
private fun severityColor(severity: String?): Color {
    return when (severity?.lowercase()) {
        "high", "critical", "severe" -> Color(0xFFC63B2E)
        "medium", "moderate" -> Color(0xFFB7791F)
        else -> Color(0xFF2E7D5B)
    }
}

@Composable
private fun severityLabel(severity: String?): String {
    return stringResource(
        when (severity?.lowercase()) {
            "high", "critical", "severe" -> R.string.notify_risk_high
            "medium", "moderate" -> R.string.notify_risk_medium
            else -> R.string.notify_risk_default
        },
    )
}

/** 系统通知开关：开启后由后台巡检补发，手动「立即检查」可即时验证链路。 */
@Composable
private fun RiskPushCard(
    enabled: Boolean,
    busy: Boolean,
    cities: List<String>,
    notice: String?,
    lastCheckedAt: Long?,
    onToggle: (Boolean) -> Unit,
    onCheck: () -> Unit,
    onStartPlanning: () -> Unit,
) {
    val ivory = Color(0xFFF2F2E9)
    val signal = Color(0xFF98D6AE)
    Column(Modifier.fillMaxWidth().spatialIsland().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("目的地守护哨", Modifier.weight(1f), color = ivory, fontFamily = FolioSerif, fontSize = 22.sp)
            Switch(checked = enabled, enabled = !busy, onCheckedChange = onToggle)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(15.dp)) {
            val sweep = if (enabled) rememberInfiniteTransition(label = "radar").animateFloat(
                0f, 360f, infiniteRepeatable(tween(5200, easing = LinearEasing)), label = "sweep") else null
            Canvas(Modifier.size(68.dp)) {
                sweep?.let { drawArc(signal.copy(alpha = .19f), it.value, 60f, useCenter = true) }
                val u = size.minDimension / 2f
                listOf(0.33f, 0.65f, 0.97f).forEach { scale ->
                    drawCircle(signal.copy(alpha = 0.42f), u * scale, style = Stroke(0.8.dp.toPx()))
                }
                drawLine(signal.copy(alpha = 0.25f), Offset(0f,u), Offset(size.width,u), 1.dp.toPx())
                drawLine(signal.copy(alpha = 0.25f), Offset(u,0f), Offset(u,size.height), 1.dp.toPx())
                drawLine(signal, center, Offset(u*1.62f,u*0.31f), 1.6.dp.toPx(), StrokeCap.Round)
                drawCircle(signal, 2.dp.toPx(), Offset(u*1.38f,u*0.58f))
            }
            Column {
                Text(cities.joinToString(" · ").ifBlank { "尚未订阅城市" }, color = ivory, fontSize = 22.sp, fontFamily = FolioSerif)
                val time = lastCheckedAt?.let { android.text.format.DateFormat.getTimeFormat(LocalContext.current).format(java.util.Date(it)) }
                Text(if (busy) "正在巡检…" else time?.let { "上次巡检   $it" } ?: "等待本次巡检",
                    color = ivory.copy(alpha = 0.76f), fontSize = 12.sp)
            }
        }
        HorizontalDivider(color = signal.copy(alpha = .2f))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(if (enabled) "后台通知已开启" else "后台通知已关闭", Modifier.weight(1f), color = ivory.copy(alpha = .76f), fontSize = 12.sp)
            Text(stringResource(R.string.notify_push_check), color = ivory, fontSize = 12.sp,
                modifier = Modifier.clickable(enabled = !busy, role = Role.Button, onClick = onCheck).padding(9.dp))
        }
        if (cities.isEmpty()) Text("规划一份行程，开启守护  →", color = ivory,
            modifier = Modifier.clickable(role = Role.Button, onClick = onStartPlanning).padding(vertical = 4.dp))
        notice?.let { Text(it, color = ivory, fontSize = 12.sp) }
    }
}

/**
 * 风险通知中心：拉取后端 risk_notifications，支持未读过滤与逐条已读。
 *
 * 系统通知由 WorkManager 后台巡检 + 本地通知实现，不依赖 Firebase 配置。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(container: AppContainer, onBack: () -> Unit, onStartPlanning: () -> Unit) {
    val context = LocalContext.current
    // 列表、过滤与已读状态由 ViewModel 持有，旋转或重进页面不会丢失。
    val viewModel: NotificationsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                NotificationsViewModel(container, context.applicationContext)
            }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    val pushPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.setPushEnabled(true) else viewModel.onPushPermissionDenied()
    }
    val onTogglePush: (Boolean) -> Unit = { enabled ->
        val needsPermission = enabled &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            pushPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.setPushEnabled(enabled)
        }
    }

    val unreadCount = state.items.count { it.isUnread }
    val visible = if (state.unreadOnly) state.items.filter { it.isUnread } else state.items

    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircleIconButton(
                        icon = Icons.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.common_back),
                        onClick = onBack,
                        background = CardBg,
                        borderColor = CardBorder,
                        tint = Ink,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    FloatingTitle(stringResource(R.string.notify_title))
                }
                if (unreadCount > 0) {
                    Surface(shape = RoundedCornerShape(50), color = OrangeLightBg) {
                        Text(
                            text = stringResource(R.string.notify_unread_count, unreadCount),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandOrange,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.weight(1f),
            ) {
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .wheelScroll(listState),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                ) {
                    item(key = "notify_header") {
                        Column {
                            Spacer(modifier = Modifier.height(4.dp))

                            RiskPushCard(
                                enabled = state.pushEnabled,
                                busy = state.pushBusy,
                                cities = state.pushCities,
                                notice = state.pushNotice,
                                lastCheckedAt = state.lastCheckedAt,
                                onToggle = onTogglePush,
                                onCheck = { viewModel.checkNow() },
                                onStartPlanning = onStartPlanning,
                            )

                            Spacer(Modifier.height(12.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    listOf("气象" to listOf("weather", "气象", "天气"), "交通" to listOf("traffic", "transport", "路况", "交通"),
                                        "景区" to listOf("scenic", "attraction", "景区")).forEach { (label, keys) ->
                                        val count = state.items.count { item -> keys.any { item.type.orEmpty().contains(it, ignoreCase = true) } }
                                        Column(Modifier.weight(1f).spatialIsland(16.dp).padding(12.dp)) {
                                            Text(label, fontFamily = FolioSerif, fontSize = 17.sp, color = Ink)
                                            Text(if (state.errorText != null) "—" else if (count == 0) "无提醒" else count.toString(),
                                                fontFamily = FolioDigits, fontSize = 22.sp, color = Ink)
                                            Text(if (state.errorText == null) "条通知" else "待同步", fontSize = 11.sp, color = Muted)
                                        }
                                    }
                                }
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterTab(
                                        text = stringResource(R.string.notify_filter_all),
                                        selected = !state.unreadOnly,
                                    ) { viewModel.setUnreadOnly(false) }
                                    FilterTab(
                                        text = stringResource(R.string.notify_filter_unread, unreadCount),
                                        selected = state.unreadOnly,
                                    ) { viewModel.setUnreadOnly(true) }
                                }

                                val canMarkAll = unreadCount > 0 && !state.markingAllRead
                                Text(
                                    text = stringResource(
                                        if (state.markingAllRead) {
                                            R.string.notify_processing
                                        } else {
                                            R.string.notify_mark_all_read
                                        },
                                    ),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (canMarkAll) BrandOrange else Muted,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .clickable(
                                            enabled = canMarkAll,
                                            role = Role.Button,
                                        ) { viewModel.markAllRead() }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            state.errorText?.let {
                                InlineBanner(text = it, bg = ErrorBg, fg = ErrorFg, icon = Icons.Filled.Warning)
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }

                    when {
                        state.loading && state.items.isEmpty() -> item(key = "notify_loading") {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = EditionSurface,
                                modifier = Modifier.editionRim(RoundedCornerShape(16.dp)).fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 26.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = BrandOrange,
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = stringResource(R.string.notify_syncing),
                                        fontSize = 12.5.sp,
                                        color = Muted,
                                    )
                                }
                            }
                        }

                        visible.isEmpty() -> item(key = "notify_empty") {
                            Column(Modifier.fillMaxWidth().spatialIsland().padding(16.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                                Text(if (state.errorText != null) "通知记录待同步" else stringResource(if (state.unreadOnly) R.string.notify_empty_unread else R.string.notify_empty_all),
                                    fontSize = 23.sp, fontFamily = FolioSerif, color = Ink)
                                HorizontalDivider(color = CardBorder)
                                HorizontalDivider(color = CardBorder)
                                if (state.pushCities.isNotEmpty()) {
                                    Text("已订阅目的地", color = Ink, fontFamily = FolioSerif, fontSize = 18.sp)
                                    state.pushCities.distinct().forEach { city ->
                                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                            Text(city, modifier = Modifier.weight(1f), color = Ink, fontSize = 14.sp)
                                            Text(if (state.pushEnabled) "后台提醒开启" else "手动巡检", color = Muted, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        else -> items(
                            items = visible,
                            key = { item -> item.id ?: item.hashCode() },
                        ) { item ->
                            Box(modifier = Modifier.padding(bottom = 10.dp)) {
                                RiskCard(
                                    item = item,
                                    busy = state.busyId == item.id,
                                    onMarkRead = { viewModel.markRead(item) },
                                )
                            }
                        }
                    }

                    item(key = "notify_footer") { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun FilterTab(text: String, selected: Boolean, onClick: () -> Unit) {
    // 外层保证 48dp 触摸区，内层保持原胶囊视觉尺寸。
    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(50))
            .clickable(role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = if (selected) BrandOrange else CardBg,
            border = BorderStroke(1.dp, if (selected) BrandOrange else CardBorder),
        ) {
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) Color.White else Muted,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            )
        }
    }
}

@Composable
private fun RiskCard(
    item: RiskNotificationDto,
    busy: Boolean,
    onMarkRead: () -> Unit,
) {
    val accent = severityColor(item.severity)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = EditionSurface,
        border = BorderStroke(1.dp, if (item.isUnread) accent.copy(alpha = 0.35f) else CardBorder),
        modifier = Modifier.editionRim(RoundedCornerShape(16.dp)).fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (item.isUnread) accent else accent.copy(alpha = 0.35f)),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(shape = RoundedCornerShape(5.dp), color = accent.copy(alpha = 0.12f)) {
                    Text(
                        text = severityLabel(item.severity),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = item.title?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.notify_default_title),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (item.isUnread) {
                    Text(
                        text = stringResource(R.string.notify_unread),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandOrange,
                    )
                }
            }

            val detail = item.detail?.trim().orEmpty()
            if (detail.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = detail,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = Muted,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            val meta = buildList {
                item.city?.takeIf { it.isNotBlank() }?.let { add(it) }
                item.observedAt?.takeIf { it.isNotBlank() }?.let {
                    add(stringResource(R.string.notify_observed, it))
                }
                item.source?.takeIf { it.isNotBlank() }?.let { add(it) }
            }
            if (meta.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = meta.joinToString(" · "),
                    fontSize = 12.sp,
                    color = Muted.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            val proposal = item.replanProposal?.trim().orEmpty()
            if (proposal.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = LocalAppPalette.current.divider, thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.notify_suggest),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandOrange,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = proposal,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = Ink.copy(alpha = 0.8f),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (item.isUnread) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.8.dp,
                            color = BrandOrange,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.notify_processing),
                            fontSize = 12.sp,
                            color = Muted,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.notify_mark_read),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandOrange,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(role = Role.Button, onClick = onMarkRead)
                                .padding(horizontal = 2.dp, vertical = 4.dp),
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (item.isEstimated) {
                        Text(
                            text = stringResource(R.string.notify_estimated),
                            fontSize = 12.sp,
                            color = Muted,
                        )
                    }
                }
            }
        }
    }
}
