package com.omniroute.app.feature.plan

import androidx.compose.material3.minimumInteractiveComponentSize

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniroute.app.R
import com.omniroute.app.core.planning.RouteNode
import com.omniroute.app.core.ui.components.RouteMapView
import com.omniroute.app.core.ui.components.dayColor
import com.omniroute.app.core.ui.components.routeLegMeters
import com.omniroute.app.core.ui.components.routeLegText
import com.omniroute.app.core.ui.components.routeTimeText
import com.omniroute.app.feature.auth.BrandOrange
import com.omniroute.app.feature.auth.CreamBg
import com.omniroute.app.feature.auth.Ink
import com.omniroute.app.feature.auth.Muted
import java.util.Locale
import com.omniroute.app.core.ui.theme.LocalAppPalette
import com.omniroute.app.core.ui.wheelScroll

import androidx.compose.ui.graphics.Color

private val EditorCard: Color
    @Composable get() = LocalAppPalette.current.surface
private val EditorBorder: Color
    @Composable get() = LocalAppPalette.current.border
private val EditorTagBg: Color
    @Composable get() = LocalAppPalette.current.tagBg

/** 备注按「名称 + 时间」归档，调整到别的天时不会丢。 */
fun stopNoteKey(node: RouteNode): String = node.name + "|" + node.time

/**
 * 路线编辑页：日 Tab + 停靠点排序 + 待排池 + 备注 + 路段距离耗时。
 *
 * <p>编辑只影响展示顺序与所属天，不会改写引擎给出的时间字段；
 * 时间轴仍按时间排序，地图与编辑页按这里的顺序连线。
 */
@Composable
fun RouteEditorScreen(
    nodes: List<RouteNode>,
    dayCount: Int,
    notes: Map<String, String>,
    onNoteChange: (String, String) -> Unit,
    loadBaseMap: suspend (center: String, zoom: Int, width: Int, height: Int) -> Bitmap?,
    onClose: () -> Unit,
    onApply: (List<RouteNode>) -> Unit,
) {
    var stops by remember(nodes) { mutableStateOf(nodes) }
    var tab by remember { mutableIntStateOf(0) }
    var noteTarget by remember { mutableStateOf<RouteNode?>(null) }
    var dayTarget by remember { mutableStateOf<RouteNode?>(null) }
    var addDay by remember { mutableStateOf<Int?>(null) }
    var deleteTarget by remember { mutableStateOf<RouteNode?>(null) }
    val editorScrollState = rememberScrollState()

    // 返回手势与左上角返回：有改动就按当前编辑结果落回状态，避免静默丢弃
    val leaveEditor = {
        if (stops == nodes) onClose() else onApply(stops)
    }
    BackHandler { leaveEditor() }

    val poolCount = stops.count { it.day < 1 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBg)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        // 顶部：返回（保留改动） / 标题 / 完成（应用改动）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .minimumInteractiveComponentSize()
                    .clickable(role = Role.Button) { leaveEditor() }
                    .padding(horizontal = 6.dp, vertical = 6.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.common_back),
                    tint = Ink,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = stringResource(R.string.plan_editor_breadcrumb), fontSize = 13.sp, color = Muted)
            }
            Text(
                text = stringResource(R.string.plan_editor_title),
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Ink,
            )
            TextButton(onClick = { onApply(stops) }) {
                Text(
                    text = stringResource(R.string.common_done),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandOrange,
                )
            }
        }

        // 日 Tab：总览 / DAY 1..N / 待排
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EditorTab(text = stringResource(R.string.plan_editor_tab_overview), selected = tab == 0) { tab = 0 }
            for (day in 1..dayCount) {
                EditorTab(text = "DAY $day", selected = tab == day) { tab = day }
            }
            EditorTab(
                text = if (poolCount > 0) {
                    stringResource(R.string.plan_editor_tab_pool_count, poolCount)
                } else {
                    stringResource(R.string.plan_editor_tab_pool)
                },
                selected = tab == -1,
            ) { tab = -1 }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(editorScrollState)
                .wheelScroll(editorScrollState)
                .padding(horizontal = 16.dp),
        ) {
            when {
                tab == 0 -> OverviewTab(stops, dayCount, loadBaseMap)
                tab == -1 -> PoolTab(
                    stops = stops,
                    notes = notes,
                    dayCount = dayCount,
                    onPickDay = { dayTarget = it },
                    onNote = { noteTarget = it },
                    onDelete = { deleteTarget = it },
                )
                else -> DayTab(
                    stops = stops,
                    day = tab,
                    notes = notes,
                    loadBaseMap = loadBaseMap,
                    onAdd = { addDay = tab },
                    onDelete = { deleteTarget = it },
                    onMove = { node, delta ->
                        val dayNodes = stops.filter { it.day == node.day }
                        val localIndex = dayNodes.indexOf(node)
                        val other = dayNodes.getOrNull(localIndex + delta)
                        if (other != null) {
                            val current = stops.toMutableList()
                            val a = current.indexOf(node)
                            val b = current.indexOf(other)
                            current[a] = other
                            current[b] = node
                            stops = current
                        }
                    },
                    onPool = { node ->
                        stops = stops.map { if (it === node) it.withDay(0) else it }
                    },
                    onNote = { noteTarget = it },
                    onPickDay = { dayTarget = it },
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    noteTarget?.let { node ->
        var draft by remember(node) { mutableStateOf(notes[stopNoteKey(node)] ?: "") }
        AlertDialog(
            onDismissRequest = { noteTarget = null },
            title = {
                Text(
                    text = stringResource(R.string.plan_editor_note_title, node.name),
                    fontSize = 16.sp,
                    color = Ink,
                )
            },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.take(120) },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.plan_editor_note_hint),
                            fontSize = 13.sp,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onNoteChange(stopNoteKey(node), draft.trim())
                    noteTarget = null
                }) { Text(text = stringResource(R.string.common_save), color = BrandOrange) }
            },
            dismissButton = {
                TextButton(onClick = { noteTarget = null }) {
                    Text(text = stringResource(R.string.common_cancel), color = Muted)
                }
            },
        )
    }

    dayTarget?.let { node ->
        AlertDialog(
            onDismissRequest = { dayTarget = null },
            title = {
                Text(
                    text = stringResource(R.string.plan_editor_assign_title, node.name),
                    fontSize = 16.sp,
                    color = Ink,
                )
            },
            text = {
                Column {
                    for (day in 1..dayCount) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .minimumInteractiveComponentSize().clickable(role = Role.Button) {
                                    stops = stops.map { if (it === node) it.withDay(day) else it }
                                    dayTarget = null
                                    tab = day
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(dayColor(day), CircleShape),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "DAY $day", fontSize = 14.sp, color = Ink)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { dayTarget = null }) {
                    Text(text = stringResource(R.string.common_cancel), color = Muted)
                }
            },
        )
    }

    addDay?.let { day ->
        AddStopDialog(
            day = day,
            onDismiss = { addDay = null },
            onAdd = { node ->
                stops = stops + node
                addDay = null
            },
        )
    }

    deleteTarget?.let { node ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = {
                Text(
                    text = stringResource(R.string.plan_editor_remove_title, node.name),
                    fontSize = 16.sp,
                    color = Ink,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.plan_editor_remove_message),
                    fontSize = 12.sp,
                    color = Muted,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    stops = stops.filter { it !== node }
                    onNoteChange(stopNoteKey(node), "")
                    deleteTarget = null
                }) { Text(text = stringResource(R.string.common_delete), color = BrandOrange) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(text = stringResource(R.string.common_cancel), color = Muted)
                }
            },
        )
    }
}

/** 新增停靠点：名称必填，其余选填；没有坐标的点只在列表里出现。 */
@Composable
private fun AddStopDialog(
    day: Int,
    onDismiss: () -> Unit,
    onAdd: (RouteNode) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var lnglat by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("景点") }
    val canAdd = name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.plan_editor_add_title, day),
                fontSize = 16.sp,
                color = Ink,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(30) },
                    label = { Text(text = stringResource(R.string.plan_editor_add_name), fontSize = 12.sp) },
                    placeholder = {
                        Text(text = stringResource(R.string.plan_editor_add_name_hint), fontSize = 12.sp)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (option in listOf("景点", "餐饮", "住宿", "交通")) {
                        CategoryChip(
                            text = option,
                            selected = category == option,
                            onClick = { category = option },
                        )
                    }
                }
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it.take(12) },
                    label = { Text(text = stringResource(R.string.plan_editor_add_time), fontSize = 12.sp) },
                    placeholder = { Text(text = stringResource(R.string.plan_editor_add_time_hint), fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it.take(60) },
                    label = { Text(text = stringResource(R.string.plan_editor_add_address), fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = lnglat,
                    onValueChange = { lnglat = it.take(40) },
                    label = { Text(text = stringResource(R.string.plan_editor_add_coord), fontSize = 12.sp) },
                    placeholder = {
                        Text(text = stringResource(R.string.plan_editor_add_coord_hint), fontSize = 12.sp)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.plan_editor_add_coord_note),
                    fontSize = 12.sp,
                    color = Muted,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    RouteNode.create(day, name, time, category, address, lnglat)?.let(onAdd)
                },
                enabled = canAdd,
            ) { Text(text = stringResource(R.string.common_add), color = BrandOrange) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.common_cancel), color = Muted)
            }
        },
    )
}

@Composable
private fun CategoryChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) BrandOrange else EditorTagBg,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .minimumInteractiveComponentSize().clickable(role = Role.Button, onClick = onClick),
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = if (selected) androidx.compose.ui.graphics.Color.White else Ink,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun EditorTab(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) BrandOrange else EditorCard,
        border = BorderStroke(1.dp, if (selected) BrandOrange else EditorBorder),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .minimumInteractiveComponentSize().clickable(role = Role.Button, onClick = onClick),
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) androidx.compose.ui.graphics.Color.White else Muted,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun OverviewTab(
    stops: List<RouteNode>,
    dayCount: Int,
    loadBaseMap: suspend (center: String, zoom: Int, width: Int, height: Int) -> Bitmap?,
) {
    val scheduled = stops.filter { it.day >= 1 }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = EditorCard,
        border = BorderStroke(1.dp, EditorBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        RouteMapView(
            nodes = scheduled,
            loadBaseMap = loadBaseMap,
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .padding(6.dp),
        )
    }
    Spacer(modifier = Modifier.height(14.dp))
    Text(
        text = stringResource(R.string.plan_editor_daily_overview),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = Ink,
    )
    Spacer(modifier = Modifier.height(8.dp))
    for (day in 1..dayCount) {
        val dayNodes = stops.filter { it.day == day }
        val distance = dayNodes.zipWithNext { a, b -> routeLegMeters(a, b) }.sum()
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = EditorCard,
            border = BorderStroke(1.dp, EditorBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.size(9.dp).background(dayColor(day), CircleShape))
                Spacer(modifier = Modifier.width(9.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "DAY $day", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text(
                        text = stringResource(R.string.plan_editor_stop_count, dayNodes.size),
                        fontSize = 12.sp,
                        color = Muted,
                    )
                }
                Text(
                    text = if (distance > 0) {
                        String.format(Locale.US, "%.1f km", distance / 1000.0)
                    } else {
                        "—"
                    },
                    fontSize = 12.sp,
                    color = Muted,
                )
            }
        }
    }
}

@Composable
private fun DayTab(
    stops: List<RouteNode>,
    day: Int,
    notes: Map<String, String>,
    loadBaseMap: suspend (center: String, zoom: Int, width: Int, height: Int) -> Bitmap?,
    onAdd: () -> Unit,
    onDelete: (RouteNode) -> Unit,
    onMove: (RouteNode, Int) -> Unit,
    onPool: (RouteNode) -> Unit,
    onNote: (RouteNode) -> Unit,
    onPickDay: (RouteNode) -> Unit,
) {
    val dayNodes = stops.filter { it.day == day }
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = EditorCard,
        border = BorderStroke(1.dp, EditorBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        RouteMapView(
            nodes = dayNodes,
            highlightDay = day,
            loadBaseMap = loadBaseMap,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(6.dp),
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = "DAY $day", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.plan_editor_stop_count, dayNodes.size),
                fontSize = 12.sp,
                color = Muted,
            )
            TextButton(onClick = onAdd) {
                Text(
                    text = stringResource(R.string.plan_editor_add_short),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandOrange,
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    if (dayNodes.isEmpty()) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = EditorTagBg,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.plan_editor_day_empty),
                fontSize = 12.sp,
                color = Muted,
                modifier = Modifier.padding(14.dp),
            )
        }
    }
    dayNodes.forEachIndexed { index, node ->
        val previous = dayNodes.getOrNull(index - 1)
        StopRow(
            index = index + 1,
            node = node,
            note = notes[stopNoteKey(node)],
            legText = previous?.let { routeLegText(context, it, node) },
            canMoveUp = index > 0,
            canMoveDown = index < dayNodes.lastIndex,
            onMoveUp = { onMove(node, -1) },
            onMoveDown = { onMove(node, 1) },
            onNote = { onNote(node) },
            onPool = { onPool(node) },
            onPickDay = { onPickDay(node) },
            onDelete = { onDelete(node) },
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun PoolTab(
    stops: List<RouteNode>,
    notes: Map<String, String>,
    dayCount: Int,
    onPickDay: (RouteNode) -> Unit,
    onNote: (RouteNode) -> Unit,
    onDelete: (RouteNode) -> Unit,
) {
    val pool = stops.filter { it.day < 1 }
    Text(
        text = stringResource(R.string.plan_editor_pool_desc),
        fontSize = 12.sp,
        color = Muted,
    )
    Spacer(modifier = Modifier.height(10.dp))
    if (pool.isEmpty()) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = EditorTagBg,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.plan_editor_pool_empty),
                fontSize = 12.sp,
                color = Muted,
                modifier = Modifier.padding(14.dp),
            )
        }
        return
    }
    pool.forEach { node ->
        StopRow(
            index = 0,
            node = node,
            note = notes[stopNoteKey(node)],
            legText = null,
            canMoveUp = false,
            canMoveDown = false,
            onMoveUp = {},
            onMoveDown = {},
            onNote = { onNote(node) },
            onPool = {},
            onPickDay = { onPickDay(node) },
            onDelete = { onDelete(node) },
            poolMode = true,
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = stringResource(R.string.plan_editor_days_available, dayCount),
        fontSize = 12.sp,
        color = Muted,
    )
}

@Composable
private fun StopRow(
    index: Int,
    node: RouteNode,
    note: String?,
    legText: String?,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onNote: () -> Unit,
    onPool: () -> Unit,
    onPickDay: () -> Unit,
    onDelete: () -> Unit,
    poolMode: Boolean = false,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = EditorCard,
        border = BorderStroke(1.dp, EditorBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!poolMode) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(dayColor(node.day), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = index.toString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = androidx.compose.ui.graphics.Color.White,
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = node.name,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        routeTimeText(node).takeIf { it.isNotBlank() }?.let {
                            Text(text = it, fontSize = 12.sp, color = Muted)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(text = node.category, fontSize = 12.sp, color = Muted)
                    }
                    if (!note.isNullOrBlank()) {
                        Text(
                            text = stringResource(R.string.plan_result_note, note),
                            fontSize = 12.sp,
                            color = BrandOrange,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (!poolMode) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        SmallAction(text = "↑", enabled = canMoveUp, onClick = onMoveUp)
                        SmallAction(text = "↓", enabled = canMoveDown, onClick = onMoveDown)
                    }
                }
            }

            if (!legText.isNullOrBlank()) {
                HorizontalDivider(color = EditorBorder, thickness = 1.dp)
                Text(
                    text = legText,
                    fontSize = 12.sp,
                    color = Muted,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }

            HorizontalDivider(color = EditorBorder, thickness = 1.dp)
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onNote) {
                    Text(
                        text = stringResource(
                            if (note.isNullOrBlank()) {
                                R.string.plan_editor_note_add
                            } else {
                                R.string.plan_editor_note_edit
                            },
                        ),
                        fontSize = 12.sp,
                        color = Muted,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (poolMode) {
                    TextButton(onClick = onPickDay) {
                        Text(
                            text = stringResource(R.string.plan_editor_assign),
                            fontSize = 12.sp,
                            color = BrandOrange,
                        )
                    }
                } else {
                    TextButton(onClick = onPool) {
                        Text(
                            text = stringResource(R.string.plan_editor_to_pool),
                            fontSize = 12.sp,
                            color = BrandOrange,
                        )
                    }
                }
                TextButton(onClick = onDelete) {
                    Text(text = stringResource(R.string.common_delete), fontSize = 12.sp, color = Muted)
                }
            }
        }
    }
}

@Composable
private fun SmallAction(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (enabled) EditorTagBg else androidx.compose.ui.graphics.Color.Transparent)
            .then(
                if (enabled) Modifier.minimumInteractiveComponentSize().clickable(role = Role.Button, onClick = onClick) else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            color = if (enabled) Ink else Muted.copy(alpha = 0.4f),
        )
    }
}
