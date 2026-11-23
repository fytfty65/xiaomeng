package com.omniroute.app.feature.plan

import com.omniroute.app.core.ui.theme.seasonBackdrop
import androidx.compose.material3.minimumInteractiveComponentSize

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.omniroute.app.AppContainer
import com.omniroute.app.R
import com.omniroute.app.core.ui.ContentMaxWidth
import com.omniroute.app.core.ui.wheelScroll
import com.omniroute.app.core.ui.components.CircleIconButton
import com.omniroute.app.core.ui.components.RouteMapView
import com.omniroute.app.feature.auth.BrandOrange
import com.omniroute.app.feature.auth.CreamBg
import com.omniroute.app.feature.auth.ErrorBg
import com.omniroute.app.feature.auth.ErrorFg
import com.omniroute.app.feature.auth.Ink
import com.omniroute.app.feature.auth.InlineBanner
import com.omniroute.app.feature.auth.Muted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 规划主链路：填写诉求 → WebSocket 多智能体推演（进度/异常）→ 时间轴与地图结果 → 反馈与保存。
 *
 * 房间是资源键，表单参数会被编排进 agent_negotiate 的 intent 文本。
 */
@Composable
fun PlanningScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onOpenCandidate: (String, String) -> Unit,
    onSaved: (String) -> Unit,
) {
    // 规划状态统一由 ViewModel 持有：旋转屏幕、跳转候选详情都不会丢结果，
    // 只有真正离开规划页（ViewModel 被清理）时才取消实时会话。
    val context = LocalContext.current
    val viewModel: PlanningViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                PlanningViewModel(
                    container,
                    createSavedStateHandle(),
                    context.applicationContext,
                )
            }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is PlanningEvent.Saved -> onSaved(event.tripId)
            }
        }
    }

    // 只读别名：让下方渲染逻辑保持扁平；所有写入都回到 ViewModel。
    val phase = state.phase
    val destination = state.destination
    val chatText = state.chatText
    val chatHint = state.chatHint
    val origin = state.origin
    val startDate = state.startDate
    val days = state.days
    val travelers = state.travelers
    val budget = state.budget
    val preferences = state.preferences
    val room = state.room
    val preparing = state.preparing
    val errorText = state.errorText
    val stages = state.stages
    val streamText = state.streamText
    val members = state.members
    val connected = state.connected
    val result = state.result
    val activeRequest = state.activeRequest
    val selectedDay = state.selectedDay
    val saving = state.saving
    val saveError = state.saveError
    val feedbackDone = state.feedbackDone
    val feedbackReason = state.feedbackReason
    val feedbackBusy = state.feedbackBusy
    val feedbackError = state.feedbackError
    val editorOpen = state.editorOpen
    val editedNodes = state.editedNodes
    val notes = state.notes
    val hasUnsavedEdits = state.hasUnsavedEdits
    val showDiscardPrompt = state.showDiscardPrompt

    // 底图走网关代理，失败时返回 null，由 RouteMapView 回退到矢量示意图。
    val loadBaseMap: suspend (String, Int, Int, Int) -> Bitmap? = { center, zoom, width, height ->
        withContext(Dispatchers.IO) {
            runCatching { container.staticMapRepository.load(center, zoom, width, height) }.getOrNull()
        }
    }

    // 提交前收起键盘，避免提示条被输入法挡住；推演本身由 ViewModel 负责。
    val submit: () -> Unit = {
        focusManager.clearFocus()
        viewModel.submit()
    }

    val save: () -> Unit = { viewModel.save() }


    BackHandler(enabled = phase == PlanPhase.RUNNING) { viewModel.stop() }
    // 有未保存的编辑时，返回键先弹确认，而不是直接丢掉改动
    BackHandler(enabled = phase == PlanPhase.DONE && hasUnsavedEdits) {
        viewModel.requestDiscard()
    }

    if (showDiscardPrompt) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDiscard() },
            title = {
                Text(
                    stringResource(R.string.plan_discard_title),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = { Text(stringResource(R.string.plan_discard_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissDiscard()
                    onBack()
                }) { Text(stringResource(R.string.plan_discard_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDiscard() }) {
                    Text(stringResource(R.string.plan_discard_cancel))
                }
            },
        )
    }

    if (editorOpen) {
        val editable = editedNodes ?: result?.route.orEmpty()
        RouteEditorScreen(
            nodes = editable,
            dayCount = maxOf(result?.days ?: 1, activeRequest?.days ?: 1, 1),
            notes = notes,
            onNoteChange = { key, value -> viewModel.onNoteChange(key, value) },
            loadBaseMap = loadBaseMap,
            onClose = { viewModel.closeEditor() },
            onApply = { updated -> viewModel.applyEdits(updated) },
        )
        return
    }

    val formScrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .seasonBackdrop(),
        contentAlignment = Alignment.TopCenter,
    ) {
        // 平板/扩展屏上限制正文宽度；结果页内部再按 Window Size Class 切换双栏。
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = ContentMaxWidth)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            PlanningTopBar(
                onBack = { if (hasUnsavedEdits) viewModel.requestDiscard() else onBack() },
                title = stringResource(
                    when (phase) {
                        PlanPhase.FORM, PlanPhase.FAILED -> R.string.plan_title_form
                        PlanPhase.RUNNING -> R.string.plan_title_running
                        PlanPhase.DONE -> R.string.plan_title_done
                    },
                ),
                stepHint = stringResource(
                    when (phase) {
                        PlanPhase.FORM -> R.string.plan_step_form
                        PlanPhase.RUNNING -> R.string.plan_step_running
                        PlanPhase.FAILED -> R.string.plan_step_failed
                        PlanPhase.DONE -> R.string.plan_step_done
                    },
                ),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(formScrollState)
                    .wheelScroll(formScrollState)
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(modifier = Modifier.height(14.dp))

                when (phase) {
                    PlanPhase.FORM -> FormContent(
                        chatText = chatText,
                        onChatTextChange = { viewModel.onChatTextChange(it) },
                        chatHint = chatHint,
                        chatParsing = state.chatParsing,
                        chatParsed = state.chatParsed,
                        onApplyChat = { viewModel.applyChatIdea() },
                        onConfirmChat = { viewModel.confirmChatIdea() },
                        destination = destination,
                        onDestinationChange = { viewModel.onDestinationChange(it) },
                        origin = origin,
                        onOriginChange = { viewModel.onOriginChange(it) },
                        startDate = startDate,
                        onStartDateSelected = { viewModel.onStartDateSelected(it) },
                        onStartDateClear = { viewModel.onStartDateClear() },
                        days = days,
                        onDaysChange = { viewModel.onDaysChange(it) },
                        travelers = travelers,
                        onTravelersChange = { viewModel.onTravelersChange(it) },
                        budget = budget,
                        onBudgetChange = { raw -> viewModel.onBudgetChange(raw) },
                        preferences = preferences,
                        onTogglePreference = { label -> viewModel.onTogglePreference(label) },
                        room = room,
                    )

                    PlanPhase.RUNNING -> RunningContent(
                        destination = destination,
                        request = activeRequest,
                        retrievalHint = activeRequest?.let {
                            retrievalHint(context, it.destination, it.days, it.preferences)
                        },
                        stages = stages,
                        streamText = streamText,
                        members = members,
                        connected = connected,
                    )

                    PlanPhase.FAILED -> FailureContent(
                        message = errorText ?: stringResource(R.string.plan_error_run_failed),
                        onRetry = { viewModel.retry() },
                        onEdit = { viewModel.resetToForm() },
                    )

                    PlanPhase.DONE -> {
                        val planResult = result
                        if (planResult == null) {
                            FailureContent(
                                message = stringResource(R.string.plan_error_no_result),
                                onRetry = { viewModel.retry() },
                                onEdit = { viewModel.resetToForm() },
                            )
                        } else {
                            ResultContent(
                                result = planResult,
                                request = activeRequest,
                                nodes = editedNodes ?: planResult.route,
                                notes = notes,
                                selectedDay = selectedDay,
                                onSelectDay = { viewModel.selectDay(it) },
                                onOpenEditor = { viewModel.openEditor() },
                                onOpenCandidate = onOpenCandidate,
                                loadBaseMap = loadBaseMap,
                                feedbackDone = feedbackDone,
                                feedbackReason = feedbackReason,
                                onFeedbackReasonChange = { viewModel.onFeedbackReasonChange(it) },
                                feedbackBusy = feedbackBusy,
                                feedbackError = feedbackError,
                                onFeedback = { score -> viewModel.sendFeedback(score) },
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            if (state.room != null) {
                                VariantPoolAction(
                                    saving = state.variantSaving,
                                    notice = state.variantNotice,
                                    errorText = state.variantError,
                                    onSave = { viewModel.saveVariant() },
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // 提交校验的提示必须停在按钮上方可见区：放在滚动列表末尾时，
            // 用户在底部点「启动」，提示却滚到屏幕外，表现成「点了没反应」。
            errorText?.takeIf { phase == PlanPhase.FORM }?.let { message ->
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    InlineBanner(text = message, bg = ErrorBg, fg = ErrorFg, icon = Icons.Filled.Warning)
                }
            }

            PlanningFooter(
                phase = phase,
                preparing = preparing,
                saving = saving,
                saveError = saveError,
                onStart = submit,
                onStop = { viewModel.stop() },
                onRetry = { viewModel.retry() },
                onEdit = { viewModel.resetToForm() },
                onSave = save,
            )
        }
    }
}

/** 把当前结果加入房间方案池，供同行者投票。 */
@Composable
private fun VariantPoolAction(
    saving: Boolean,
    notice: String?,
    errorText: String?,
    onSave: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = PlanCardBg,
        border = BorderStroke(1.dp, PlanSubtleBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.room_variants_title),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                    )
                    Text(
                        text = stringResource(R.string.room_variants_sub),
                        fontSize = 11.5.sp,
                        color = Muted,
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                SecondaryButton(
                    text = stringResource(
                        if (saving) R.string.plan_variant_saving else R.string.plan_variant_save,
                    ),
                    onClick = onSave,
                    enabled = !saving,
                )
            }
            notice?.let { message ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = message, fontSize = 11.5.sp, color = BrandOrange)
            }
            errorText?.let { message ->
                Spacer(modifier = Modifier.height(8.dp))
                InlineBanner(text = message, bg = ErrorBg, fg = ErrorFg, icon = Icons.Filled.Warning)
            }
        }
    }
}

/** 顶部品牌导航栏 */
@Composable
private fun PlanningTopBar(
    onBack: () -> Unit,
    title: String,
    stepHint: String,
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
                background = PlanCardBg,
                borderColor = PlanSubtleBorder,
                tint = Ink,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink)
        }

        Surface(shape = RoundedCornerShape(50), color = PlanOrangeLightBg) {
            Text(
                text = stepHint,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandOrange,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}
