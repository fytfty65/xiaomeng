package com.omniroute.app.feature.trip

import androidx.compose.material3.Icon
import com.omniroute.app.core.ui.theme.FolioSerif
import com.omniroute.app.core.ui.theme.FolioDigits
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.IconButton
import com.omniroute.app.core.ui.components.CoverBack
import com.omniroute.app.core.ui.components.CoverStatusShade
import com.omniroute.app.core.ui.SystemBarIcons
import com.omniroute.app.core.ui.theme.editionRim
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.heightIn
import android.widget.Toast
import com.omniroute.app.core.ui.components.ItineraryDayPicker
import com.omniroute.app.core.ui.theme.EditionSurface
import com.omniroute.app.core.ui.theme.LocalJourneySeason
import com.omniroute.app.core.ui.theme.OmniSeasonLogo
import com.omniroute.app.core.ui.theme.edition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.FlowRow
import com.omniroute.app.core.ui.theme.seasonBackdrop
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omniroute.app.AppContainer
import com.omniroute.app.R
import com.omniroute.app.core.network.dto.PackingItemDto
import com.omniroute.app.core.ui.theme.LocalAppPalette
import com.omniroute.app.core.ui.wheelScroll
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JourneyScreen(
    container: AppContainer,
    tripId: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: JourneyViewModel = viewModel(
        key = "journey_$tripId",
        factory = JourneyViewModel.factory(container, tripId, context),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.notice) {
        if (state.notice != null) {
            delay(3_500)
            viewModel.clearNotice()
        }
    }
    LaunchedEffect(state.replanError) {
        if (state.replanError != null) {
            delay(7_000)
            viewModel.clearReplanError()
        }
    }

    val palette = LocalAppPalette.current
    val listState = rememberLazyListState()
    val days = state.nodes.map { it.day }.distinct().sorted()
    val next = state.nodes.firstOrNull { it.status == "in_progress" || it.status == "delayed" }
        ?: state.nodes.firstOrNull { it.status == "planned" }
    var dayChoice by rememberSaveable(tripId) { mutableStateOf<Int?>(null) }
    val activeDay = dayChoice?.takeIf { it in days } ?: next?.day ?: days.firstOrNull()
    var openNode by rememberSaveable(tripId) { mutableStateOf<String?>(null) }
    var packingOpen by rememberSaveable(tripId) { mutableStateOf(true) }
    SystemBarIcons(false, !androidx.compose.foundation.isSystemInDarkTheme())
    Box(Modifier.fillMaxSize()) {

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .seasonBackdrop()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .wheelScroll(listState),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                top = 0.dp,
                bottom = 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item(key = "header") {
                Row(Modifier.fillMaxWidth().background(LocalJourneySeason.current.edition.anchor)
                    .statusBarsPadding().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    CoverBack(onBack)
                    Text(stringResource(R.string.journey_title), modifier = Modifier.weight(1f).padding(start = 9.dp),
                        fontFamily = FolioSerif, color = Color.White, fontSize = 20.sp)
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Filled.Refresh, stringResource(R.string.journey_refresh), tint = Color.White)
                    }
                }
            }

            if (state.loading) {
                journalItem(key = "loading") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator(color = palette.brand)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.journey_loading),
                            fontSize = 12.5.sp,
                            color = palette.muted,
                        )
                    }
                }
                return@LazyColumn
            }

            state.loadError?.let { message ->
                journalItem(key = "load_error") {
                    NoticeBanner(
                        text = message,
                        background = palette.errorBg,
                        foreground = palette.errorFg,
                        icon = { IconWarning(palette.errorFg) },
                    )
                }
                journalItem(key = "retry") {
                    PrimaryAction(
                        text = stringResource(R.string.journey_retry),
                        enabled = true,
                        onClick = viewModel::refresh,
                    )
                }
            }

            if (next == null) journalItem(key = "progress") {
                ProgressCard(
                    phase = state.phase,
                    progress = state.progress,
                    completed = state.completed,
                    total = state.total,
                )
            }

            if (next != null) {
                item(key = "focus") {
                    val style = LocalJourneySeason.current.edition
                    val context = LocalContext.current
                    Column(Modifier.fillMaxWidth().background(style.anchor).padding(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(15.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                Text("NEXT STOP   /   第 ${next.day} 天", color = Color.White.copy(alpha = 0.87f),
                                    fontFamily = FolioDigits, fontSize = 11.sp, letterSpacing = 1.sp)
                                Text(next.name, color = Color.White, fontFamily = style.titleFont, fontWeight = FontWeight.Bold,
                                    fontSize = 27.sp, lineHeight = 34.sp)
                                Text("%02d / %02d".format(state.completed, state.total) + "   COMPLETED",
                                    color = Color.White, fontFamily = FolioDigits, fontSize = 21.sp, letterSpacing = 1.sp)
                            }
                            Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(progress = { state.progress.coerceIn(0, 100) / 100f },
                                    modifier = Modifier.fillMaxSize().padding(3.dp), strokeWidth = 4.dp,
                                    color = Color.White, trackColor = Color.White.copy(alpha = 0.23f))
                                Text("${state.progress}%", color = Color.White, fontSize = 14.sp, fontFamily = FolioDigits)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            Surface(onClick = { viewModel.updateStatus(next, "visited") }, enabled = state.updatingNodeKey == null,
                                modifier = Modifier.weight(1f), shape = RoundedCornerShape(5.dp), color = style.deep) {
                                Box(Modifier.heightIn(min = 44.dp), contentAlignment = Alignment.Center) {
                                    Text("到达并打卡", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Surface(onClick = {
                                val query = listOf(state.destination, next.name).filter { it.isNotBlank() }.distinct().joinToString(" ")
                                if (!searchMap(context, query)) Toast.makeText(context, R.string.trip_navigate_error, Toast.LENGTH_SHORT).show()
                            }, modifier = Modifier.width(50.dp), shape = RoundedCornerShape(5.dp), color = Color.White.copy(alpha = 0.12f)) {
                                Box(Modifier.heightIn(min = 44.dp), contentAlignment = Alignment.Center) { Icon(Icons.Filled.LocationOn, "导航", tint = Color.White) }
                            }
                        }
                        Box(Modifier.fillMaxWidth().height(2.dp).background(Color.White.copy(alpha = 0.19f))) {
                            Box(Modifier.fillMaxWidth(state.progress.coerceIn(0, 100) / 100f).height(2.dp).background(style.glow))
                        }
                    }
                }
            }

            if (state.nextActions.isNotEmpty()) {
                journalItem(key = "next_actions") {
                    SectionCard(title = stringResource(R.string.journey_next_actions)) {
                        state.nextActions.forEach { action ->
                            Row(
                                modifier = Modifier.padding(vertical = 5.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 6.dp)
                                        .size(5.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(palette.brand),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = action, fontSize = 13.sp, color = palette.ink)
                            }
                        }
                    }
                }
            }

            state.notice?.let { message ->
                journalItem(key = "notice") {
                    NoticeBanner(
                        text = message,
                        background = palette.brandTint,
                        foreground = palette.brand,
                        icon = { IconCheck(palette.brand) },
                    )
                }
            }

            state.replanError?.let { message ->
                journalItem(key = "replan_error") {
                    NoticeBanner(
                        text = message,
                        background = palette.errorBg,
                        foreground = palette.errorFg,
                        icon = { IconWarning(palette.errorFg) },
                    )
                }
            }

            state.proposal?.let { proposal ->
                journalItem(key = "risk") {
                    RiskCard(
                        nodeName = proposal.affectedNodeName,
                        delayMinutes = proposal.delayMinutes,
                        loading = state.candidatesLoading,
                        onViewCandidates = viewModel::loadCandidates,
                        onIgnore = viewModel::rejectProposal,
                    )
                }
            }

            if (state.candidatesLoading || state.candidates.isNotEmpty()) {
                journalItem(key = "candidates_title") {
                    SectionTitle(text = stringResource(R.string.journey_candidates_title))
                }
                if (state.candidatesLoading) {
                    journalItem(key = "candidates_loading") {
                        Text(
                            text = stringResource(R.string.journey_replan_loading),
                            fontSize = 12.5.sp,
                            color = palette.muted,
                        )
                    }
                } else {
                    items(state.candidates, key = { "candidate_${it.id}" }) { candidate ->
                        CandidateCard(
                            candidate = candidate,
                            applying = state.applyingCandidateId == candidate.id,
                            enabled = state.applyingCandidateId == null,
                            onApply = { viewModel.applyCandidate(candidate) },
                        )
                    }
                }
            }

            journalItem(key = "nodes_title") {
                SectionTitle(text = stringResource(R.string.journey_nodes))
            }
            if (state.nodes.isEmpty()) {
                journalItem(key = "nodes_empty") {
                    Text(
                        text = stringResource(R.string.journey_empty_nodes),
                        fontSize = 12.5.sp,
                        color = palette.muted,
                    )
                }
            } else {
                stickyHeader(key = "journey_day_picker") {
                    Box(Modifier.fillMaxWidth().background(palette.background)
                        .then(if (listState.firstVisibleItemIndex > 1) Modifier.statusBarsPadding() else Modifier)
                        .padding(horizontal = 20.dp, vertical = 6.dp)) {
                        ItineraryDayPicker(days, activeDay, { dayChoice = it })
                    }
                }
                state.nodes.filter { it.day == activeDay }.groupBy { it.day }.forEach { (day, nodes) ->
                    journalItem(key = "day_$day") {
                        Text(
                            text = stringResource(R.string.journey_day_label, day),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.muted,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    items(nodes, key = { "node_${it.key}" }) { node ->
                        Column(Modifier.padding(horizontal = 20.dp, vertical = 3.dp)) {
                            node.incomingLeg?.let { Text(it, color = palette.muted, fontSize = 11.sp, modifier = Modifier.padding(vertical = 6.dp)) }
                            NodeCard(
                                node = node,
                                expanded = openNode == node.key,
                                onToggle = { openNode = if (openNode == node.key) null else node.key },
                                updating = state.updatingNodeKey == node.key,
                                enabled = state.updatingNodeKey == null,
                                onStart = { viewModel.updateStatus(node, "in_progress") },
                                onVisit = { viewModel.updateStatus(node, "visited") },
                                onSkip = { viewModel.updateStatus(node, "skipped") },
                                onDelay = { viewModel.openDelayDialog(node) },
                            )
                        }
                    }
                }
            }

            journalItem(key = "packing_title") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionTitle(
                        text = stringResource(R.string.journey_packing_title),
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = stringResource(
                            R.string.journey_packing_checked,
                            state.packing.count { item ->
                                val id = item.id ?: item.name
                                id != null && id in state.checkedPackingIds
                            },
                            state.packing.size,
                        ),
                        fontSize = 12.sp,
                        color = palette.muted,
                    )
                    TextButton(onClick = { packingOpen = !packingOpen }) {
                        Text(if (packingOpen) "收起" else "展开", fontSize = 12.sp)
                    }
                }
            }
            if (packingOpen && state.packing.isEmpty()) {
                journalItem(key = "packing_empty") {
                    Text(
                        text = stringResource(R.string.journey_packing_empty),
                        fontSize = 12.5.sp,
                        color = palette.muted,
                    )
                }
            } else if (packingOpen) {
                items(state.packing, key = { "packing_${it.id ?: it.name}" }) { item ->
                    Column(Modifier.padding(horizontal = 20.dp)) {
                        PackingRow(
                            item = item,
                            checked = (item.id ?: item.name) in state.checkedPackingIds,
                            onToggle = { viewModel.togglePacking(item) },
                        )
                    }
                }
            }
        }

        CoverStatusShade()
    }

    state.delayDialogNode?.let { node ->
        DelayDialog(
            nodeName = node.name,
            onDismiss = viewModel::dismissDelayDialog,
            onConfirm = { minutes -> viewModel.updateStatus(node, "delayed", minutes) },
        )
    }
}

private fun LazyListScope.journalItem(key: String, content: @Composable () -> Unit) {
    item(key) { Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)) { content() } }
}

@Composable
private fun ProgressCard(phase: String, progress: Int, completed: Int, total: Int) {
    val palette = LocalAppPalette.current
    val style = LocalJourneySeason.current.edition
    val fraction = progress.coerceIn(0, 100) / 100f
    Surface(shape = RoundedCornerShape(19.dp), color = EditionSurface, modifier = Modifier.editionRim(RoundedCornerShape(19.dp)).fillMaxWidth()) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(59.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.matchParentSize().padding(5.dp)) {
                    val stroke = Stroke(4.dp.toPx(), cap = StrokeCap.Round)
                    drawArc(palette.brandTint, -90f, 360f, false, style = stroke)
                    if (fraction > 0f) drawArc(style.actionGradient, -90f, 360f * fraction, false, style = stroke)
                }
                Text("$progress%", fontSize = 11.sp, color = palette.brand, fontFamily = FolioDigits)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.journey_progress), color = palette.ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(phaseLabel(phase), color = palette.brand, fontSize = 12.sp)
                Text(stringResource(R.string.journey_progress_value, progress, completed, total), color = palette.muted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun RiskCard(
    nodeName: String,
    delayMinutes: Int,
    loading: Boolean,
    onViewCandidates: () -> Unit,
    onIgnore: () -> Unit,
) {
    val palette = LocalAppPalette.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = palette.warningBg,
        border = BorderStroke(1.dp, palette.warningFg.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconWarning(palette.warningFg)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.journey_risk_title),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.warningFg,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.journey_risk_body, nodeName, delayMinutes),
                fontSize = 12.5.sp,
                color = palette.ink,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionButton(
                    text = stringResource(
                        if (loading) R.string.journey_replan_loading else R.string.journey_risk_view,
                    ),
                    enabled = !loading,
                    onClick = onViewCandidates,
                    filled = true,
                )
                ActionButton(
                    text = stringResource(R.string.journey_risk_ignore),
                    enabled = !loading,
                    onClick = onIgnore,
                    filled = false,
                )
            }
        }
    }
}

@Composable
private fun CandidateCard(
    candidate: ReplanCandidateUi,
    applying: Boolean,
    enabled: Boolean,
    onApply: () -> Unit,
) {
    val palette = LocalAppPalette.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = EditionSurface,
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.editionRim(RoundedCornerShape(16.dp)).fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = candidate.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.ink,
                )
                if (candidate.meta.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = candidate.meta,
                        fontSize = 11.5.sp,
                        color = palette.muted,
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            ActionButton(
                text = stringResource(
                    if (applying) R.string.journey_candidate_checking
                    else R.string.journey_candidate_apply,
                ),
                enabled = enabled,
                onClick = onApply,
                filled = true,
            )
        }
    }
}

@Composable
private fun NodeCard(
    node: JourneyNodeUi,
    expanded: Boolean,
    onToggle: () -> Unit,
    updating: Boolean,
    enabled: Boolean,
    onStart: () -> Unit,
    onVisit: () -> Unit,
    onSkip: () -> Unit,
    onDelay: () -> Unit,
) {
    val palette = LocalAppPalette.current
    val complete = node.status == "visited" || node.status == "skipped"
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = EditionSurface,
        modifier = Modifier.editionRim(RoundedCornerShape(16.dp)).fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = if (complete) Modifier else Modifier.clickable(role = Role.Button, onClick = onToggle),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = node.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.ink.copy(alpha = if (complete) 0.60f else 1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (node.status == "visited") {
                    OmniSeasonLogo(Modifier.rotate(-8f), diameter = 18.dp)
                    Spacer(Modifier.width(4.dp))
                }
                if (node.status == "visited") {
                    Text("已打卡", color = palette.verifiedFg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.rotate(-8f).background(palette.verifiedBg, RoundedCornerShape(5.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp))
                } else StatusChip(status = node.status)
                if (!complete) {
                    Text(if (expanded) " −" else " +", color = palette.brand)
                }
            }
            node.stayMinutes?.let { Text("预计停留  $it 分钟", color = palette.muted, fontSize = 12.sp) }
            if (node.delayMinutes > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.journey_delay_recorded, node.delayMinutes),
                    fontSize = 11.5.sp,
                    color = palette.warningFg,
                )
            }
            if (!complete && expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    if (node.status == "planned") {
                        ActionButton(
                            text = stringResource(R.string.journey_action_start),
                            enabled = enabled && !updating,
                            onClick = onStart,
                            filled = true,
                        )
                    }
                    if (node.status != "visited") {
                        ActionButton(
                            text = stringResource(R.string.journey_action_visit),
                            enabled = enabled && !updating,
                            onClick = onVisit,
                            filled = node.status != "planned",
                        )
                    }
                    if (node.status != "delayed") {
                        ActionButton(
                            text = stringResource(R.string.journey_action_delay),
                            enabled = enabled && !updating,
                            onClick = onDelay,
                            filled = false,
                        )
                    }
                    ActionButton(
                        text = stringResource(R.string.journey_action_skip),
                        enabled = enabled && !updating,
                        onClick = onSkip,
                        filled = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun PackingRow(item: PackingItemDto, checked: Boolean, onToggle: () -> Unit) {
    val palette = LocalAppPalette.current
    Surface(
        shape = RoundedCornerShape(0.dp),
        color = palette.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Checkbox, onClick = onToggle),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = palette.brand),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name ?: "",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.ink,
                )
                val reason = item.reason
                if (!reason.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = reason, fontSize = 11.5.sp, color = palette.muted)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = priorityLabel(item.priority),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = when (item.priority) {
                    "must" -> palette.errorFg
                    "recommended" -> palette.brand
                    else -> palette.muted
                },
            )
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val palette = LocalAppPalette.current
    val background = when (status) {
        "visited" -> palette.verifiedBg
        "in_progress" -> palette.brandTint
        "delayed" -> palette.warningBg
        "skipped" -> palette.tagBg
        else -> palette.fieldBg
    }
    val foreground = when (status) {
        "visited" -> palette.verifiedFg
        "in_progress" -> palette.brand
        "delayed" -> palette.warningFg
        "skipped" -> palette.muted
        else -> palette.muted
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        Text(
            text = statusLabel(status),
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = foreground,
        )
    }
}

@Composable
private fun DelayDialog(nodeName: String, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val palette = LocalAppPalette.current
    var selected by rememberSaveable { mutableStateOf(30) }
    var custom by remember { mutableStateOf("") }
    val quickOptions = remember { listOf(15, 30, 60, 90, 120) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.journey_delay_title),
                fontSize = 16.sp,
                color = palette.ink,
            )
        },
        text = {
            Column {
                Text(text = nodeName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = palette.ink)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.journey_delay_hint),
                    fontSize = 11.5.sp,
                    color = palette.muted,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    quickOptions.forEach { minutes ->
                        val active = custom.isBlank() && selected == minutes
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) palette.brand else palette.fieldBg)
                                .clickable(role = Role.Button) {
                                    selected = minutes
                                    custom = ""
                                }
                                .padding(horizontal = 11.dp, vertical = 7.dp),
                        ) {
                            Text(
                                text = "$minutes",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (active) palette.onBrand else palette.ink,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = custom,
                    onValueChange = { value ->
                        custom = value.filter { it.isDigit() }.take(4)
                    },
                    label = {
                        Text(text = stringResource(R.string.journey_delay_custom), fontSize = 12.sp)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val minutes = custom.toIntOrNull() ?: selected
                    onConfirm(minutes.coerceIn(1, 1440))
                },
                enabled = (custom.toIntOrNull() ?: selected) > 0,
            ) {
                Text(
                    text = stringResource(R.string.journey_delay_confirm),
                    color = palette.brand,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.common_cancel), color = palette.muted)
            }
        },
    )
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    val palette = LocalAppPalette.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = EditionSurface,
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.editionRim(RoundedCornerShape(16.dp)).fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = palette.ink,
            )
            Spacer(modifier = Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
private fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = LocalAppPalette.current.ink,
        modifier = modifier.padding(top = 6.dp),
    )
}

@Composable
private fun NoticeBanner(
    text: String,
    background: Color,
    foreground: Color,
    icon: @Composable () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = background,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = text, fontSize = 12.sp, color = foreground)
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    filled: Boolean,
) {
    val palette = LocalAppPalette.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (filled) palette.brand else palette.fieldBg)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                !enabled -> palette.muted
                filled -> palette.onBrand
                else -> palette.ink
            },
        )
    }
}

@Composable
private fun PrimaryAction(text: String, enabled: Boolean, onClick: () -> Unit) {
    val palette = LocalAppPalette.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) palette.brand else palette.brandTint)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) palette.onBrand else palette.brand,
        )
    }
}

@Composable
private fun IconWarning(tint: Color) {
    androidx.compose.material3.Icon(
        imageVector = Icons.Filled.Warning,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(16.dp),
    )
}

@Composable
private fun IconCheck(tint: Color) {
    androidx.compose.material3.Icon(
        imageVector = Icons.Filled.Check,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(16.dp),
    )
}

@Composable
private fun statusLabel(status: String): String = stringResource(
    when (status) {
        "visited" -> R.string.journey_status_visited
        "in_progress" -> R.string.journey_status_in_progress
        "delayed" -> R.string.journey_status_delayed
        "skipped" -> R.string.journey_status_skipped
        else -> R.string.journey_status_planned
    },
)

@Composable
private fun phaseLabel(phase: String): String = stringResource(
    when (phase) {
        "in_progress" -> R.string.journey_phase_in_progress
        "completed" -> R.string.journey_phase_completed
        else -> R.string.journey_phase_planned
    },
)

@Composable
private fun priorityLabel(priority: String?): String = stringResource(
    when (priority) {
        "must" -> R.string.journey_packing_priority_must
        "optional" -> R.string.journey_packing_priority_optional
        else -> R.string.journey_packing_priority_recommended
    },
)
