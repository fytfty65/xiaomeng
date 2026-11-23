package com.omniroute.app.feature.budget

import androidx.compose.ui.graphics.lerp
import com.omniroute.app.core.ui.theme.EditionSurface
import com.omniroute.app.core.ui.theme.OmniSeasonLogo
import com.omniroute.app.core.ui.theme.LocalJourneySeason
import com.omniroute.app.core.ui.theme.edition
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import com.omniroute.app.core.ui.theme.seasonBackdrop
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omniroute.app.AppContainer
import com.omniroute.app.R
import com.omniroute.app.core.ocr.ReceiptCache
import com.omniroute.app.core.ui.components.CircleIconButton
import com.omniroute.app.core.ui.theme.LocalAppPalette
import com.omniroute.app.core.ui.wheelScroll
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private val EXPENSE_CATEGORIES = listOf("餐饮", "住宿", "交通", "门票", "购物", "其他")
private val SUPPORTED_CURRENCIES = listOf("CNY", "USD", "EUR", "JPY", "HKD", "THB")

/** 预算管家：设置预算、逐笔记账、超支预警与分类复盘。 */
@Composable
fun BudgetScreen(
    container: AppContainer,
    tripId: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: BudgetViewModel = viewModel(
        factory = BudgetViewModel.factory(container, tripId, context),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val palette = LocalAppPalette.current

    LaunchedEffect(state.notice) {
        if (state.notice != null) {
            delay(3_000)
            viewModel.clearNotice()
        }
    }

    val receiptPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) viewModel.submitReceipt(uri) }

    // 相机会把照片写进 FileProvider 暴露的 cache 目录，回调用同一个 uri 读取识别。
    // 用 rememberSaveable 存字符串：拍照期间 Activity 可能被系统回收重建。
    var pendingCaptureUri by rememberSaveable { mutableStateOf<String?>(null) }
    var showReceiptSource by rememberSaveable { mutableStateOf(false) }
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = pendingCaptureUri?.let { Uri.parse(it) }
        pendingCaptureUri = null
        when {
            success && uri != null -> viewModel.submitReceipt(uri)
            // 取消拍照：相机可能留下 0 字节文件，交给 ViewModel 清掉。
            uri != null -> viewModel.discardReceipt(uri)
        }
    }

    val launchCamera = {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            ReceiptCache.newFile(context),
        )
        pendingCaptureUri = uri.toString()
        takePictureLauncher.launch(uri)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .seasonBackdrop()
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Row(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.common_back),
                onClick = onBack,
                background = palette.surface,
                borderColor = palette.border,
                tint = palette.ink,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.budget_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.ink,
                )
                Text(
                    text = stringResource(R.string.budget_sub),
                    fontSize = 12.sp,
                    color = palette.muted,
                )
            }
            if (state.hasBudget) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(palette.brand)
                        .clickable(role = Role.Button, onClick = viewModel::openAddExpense)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = palette.onBrand,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.budget_add_expense),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.onBrand,
                    )
                }
            }
        }

        state.notice?.let { message ->
            Banner(
                text = message,
                background = palette.brandTint,
                foreground = palette.brand,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }

        state.error?.let { message ->
            Banner(
                text = message,
                background = palette.errorBg,
                foreground = palette.errorFg,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            TextButton(onClick = viewModel::refresh, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.journey_retry), color = palette.brand)
            }
        }

        val listState = rememberLazyListState()

        when {
            state.loading && !state.hasBudget && state.expenses.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = palette.brand)
                }
            }

            else -> LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .wheelScroll(listState),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 6.dp,
                    bottom = 30.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "overview") {
                    LedgerCard(
                        state = state,
                        onEditBudget = viewModel::openSetBudget,
                        onUseEstimate = { estimate -> viewModel.submitBudget(estimate.toString(), "CNY") },
                    )
                }
                if (state.hasBudget) {
                    if (state.categories.isNotEmpty()) {
                        item(key = "categories") { CategoryCard(state = state) }
                    }
                    item(key = "advice") { AdviceCard(level = state.level) }
                }

                item(key = "ocr") {
                    OcrEntryCard(
                        enabled = !state.submitting && !state.recognizing,
                        onClick = { showReceiptSource = true },
                    )
                }

                item(key = "manual_entry") {
                    TextButton(
                        onClick = viewModel::openAddExpense,
                        enabled = !state.submitting && !state.recognizing,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("手动记一笔", fontSize = 12.sp)
                    }
                }
                if (state.expenses.isNotEmpty()) {
                    item(key = "expenses_header") {
                        Text(
                            text = stringResource(R.string.budget_expenses_title, state.expenses.size),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.muted,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    items(state.expenses, key = { it.id }) { expense ->
                        ExpenseRow(expense = expense)
                    }
                } else if (state.hasBudget) {
                    item(key = "expenses_empty") {
                        Text(
                            text = stringResource(R.string.budget_expenses_empty),
                            fontSize = 12.sp,
                            color = palette.muted,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
    }

    if (showReceiptSource) {
        AlertDialog(
            onDismissRequest = { showReceiptSource = false },
            title = {
                Text(
                    text = stringResource(R.string.budget_receipt_source_title),
                    fontSize = 16.sp,
                )
            },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            showReceiptSource = false
                            launchCamera()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.budget_receipt_camera),
                            color = palette.ink,
                            fontSize = 14.sp,
                        )
                    }
                    TextButton(
                        onClick = {
                            showReceiptSource = false
                            receiptPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.budget_receipt_gallery),
                            color = palette.ink,
                            fontSize = 14.sp,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReceiptSource = false }) {
                    Text(text = stringResource(R.string.common_cancel))
                }
            },
        )
    }

    if (state.recognizing) {
        RecognitionDialog()
    }

    if (state.showAddExpense) {
        ExpenseDialog(
            currency = state.currency,
            submitting = state.submitting,
            error = state.actionError,
            onDismiss = viewModel::dismissAddExpense,
            onConfirm = viewModel::submitExpense,
        )
    }

    if (state.showSetBudget) {
        BudgetDialog(
            initialCurrency = state.currency,
            estimate = state.estimateYuan,
            estimating = state.estimating,
            destination = state.destination,
            days = state.days,
            submitting = state.submitting,
            error = state.actionError,
            onDismiss = viewModel::dismissSetBudget,
            onConfirm = viewModel::submitBudget,
        )
    }
}

@Composable
private fun OcrEntryCard(enabled: Boolean, onClick: () -> Unit) {
    val palette = LocalAppPalette.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = EditionSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(50))
                    .background(palette.brandTint),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(19.dp)) {
                    val stroke = Stroke(1.4.dp.toPx())
                    drawRoundRect(palette.brand, cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()), style = stroke)
                    drawCircle(palette.brand, radius = 3.6.dp.toPx(), style = stroke)
                    drawLine(palette.brand, androidx.compose.ui.geometry.Offset(size.width * 0.69f, size.height * 0.23f),
                        androidx.compose.ui.geometry.Offset(size.width * 0.81f, size.height * 0.23f), strokeWidth = 1.4.dp.toPx())
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.budget_ocr_title),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.ink,
                )
                Text(
                    text = stringResource(R.string.budget_ocr_sub),
                    fontSize = 11.sp,
                    color = palette.muted,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = palette.muted,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun RecognitionDialog() {
    val palette = LocalAppPalette.current
    Dialog(onDismissRequest = {}) {
        Surface(shape = RoundedCornerShape(16.dp), color = palette.surface) {
            Row(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    color = palette.brand,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.budget_ocr_running),
                    fontSize = 13.sp,
                    color = palette.ink,
                )
            }
        }
    }
}

@Composable
private fun LedgerCard(
    state: BudgetUiState,
    onEditBudget: () -> Unit,
    onUseEstimate: (Int) -> Unit,
) {
    val style = LocalJourneySeason.current.edition
    val base = Color(0xFF212129)
    val white = Color(0xFFFDFBFA)
    val sheen = when (state.level) {
        "over" -> Color(0xFFFFA786)
        "warning" -> Color(0xFFF3BF75)
        else -> style.light
    }
    val background = Brush.linearGradient(listOf(style.deep, base))
    val ratio = state.ratio.coerceIn(0.0, 1.0).toFloat()
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(21.dp)).background(background).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("旅行账本", color = white.copy(alpha = 0.82f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                if (state.hasBudget) {
                    Text(moneyText(state.spentCents, state.currency), color = white, fontFamily = FontFamily.Monospace, fontSize = 26.sp)
                    Text("已支出 / ${moneyText(state.totalBudgetCents, state.currency)}", color = white.copy(alpha = 0.82f), fontSize = 12.sp)
                } else {
                    Text(stringResource(R.string.budget_empty_title), color = white, fontFamily = style.titleFont, fontSize = 23.sp)
                    Text(stringResource(R.string.budget_empty_sub), color = white.copy(alpha = 0.83f), fontSize = 12.sp, lineHeight = 17.sp)
                }
            }
            Box(Modifier.size(68.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.matchParentSize().padding(4.dp)) {
                    val stroke = Stroke(4.dp.toPx(), cap = StrokeCap.Round)
                    drawArc(white.copy(alpha = 0.23f), -90f, 360f, false, style = stroke)
                    if (ratio > 0f) drawArc(sheen, -90f, ratio * 360f, false, style = stroke)
                }
                if (state.hasBudget) {
                    Text("${(state.ratio * 100).roundToInt()}%", color = white, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                } else {
                    OmniSeasonLogo(diameter = 34.dp, tint = white.copy(alpha = 0.75f))
                }
            }
        }
        if (state.hasBudget) {
            Text(
                text = stringResource(if (state.remainingCents >= 0) R.string.budget_remaining_label else R.string.budget_overrun_label)
                    + "  " + moneyText(kotlin.math.abs(state.remainingCents), state.currency),
                color = white, fontSize = 13.sp,
            )
            if (state.remainingCents >= 0 && state.days > 0) {
                Text("按 ${state.days} 天均摊 · ${moneyText(state.remainingCents / state.days, state.currency)}/天",
                    color = white.copy(alpha = 0.84f), fontSize = 11.sp)
            }
        } else {
            state.estimateYuan?.takeIf { it.isFinite() && it >= 1.0 && it <= Int.MAX_VALUE }?.roundToInt()?.let { estimate ->
                Surface(
                    onClick = { onUseEstimate(estimate) },
                    enabled = !state.submitting,
                    shape = RoundedCornerShape(50), color = white.copy(alpha = 0.16f),
                ) {
                    Text("设为估算预算 ¥$estimate", color = white, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp))
                }
            }
        }
        Surface(onClick = onEditBudget, shape = RoundedCornerShape(50), color = white) {
            Text(
                stringResource(if (state.hasBudget) R.string.budget_edit_action else R.string.budget_set_action),
                color = style.deep, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun CategoryCard(state: BudgetUiState) {
    val palette = LocalAppPalette.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = palette.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.budget_category_title),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = palette.ink,
            )
            Spacer(modifier = Modifier.height(10.dp))
            val style = LocalJourneySeason.current.edition
            val colors = listOf(style.deep, style.secondary, style.accent, style.light)
            val segments = state.categories.filter { it.cents > 0 }
            if (segments.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(50)),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    segments.forEachIndexed { index, category ->
                        Box(Modifier.weight(category.cents.toFloat()).fillMaxHeight().background(colors[index % colors.size]))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            state.categories.forEach { category ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = category.name,
                        fontSize = 12.sp,
                        color = palette.ink,
                        modifier = Modifier.width(52.dp),
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(palette.fieldBg),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((category.pct / 100.0).coerceIn(0.0, 1.0).toFloat())
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(50))
                                .background(palette.brand),
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = moneyText(category.cents, state.currency),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.ink,
                    )
                }
            }
        }
    }
}

@Composable
private fun AdviceCard(level: String) {
    val palette = LocalAppPalette.current
    val (background, foreground, textRes) = when (level) {
        "over" -> Triple(palette.errorBg, palette.errorFg, R.string.budget_advice_over)
        "warning" -> Triple(palette.warningBg, palette.warningFg, R.string.budget_advice_warning)
        else -> Triple(palette.brandTint, palette.brand, R.string.budget_advice_safe)
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = background,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = foreground,
                modifier = Modifier.size(15.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(textRes),
                fontSize = 12.sp,
                color = foreground,
            )
        }
    }
}

@Composable
private fun ExpenseRow(expense: BudgetExpenseUi) {
    val palette = LocalAppPalette.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = palette.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = RoundedCornerShape(8.dp), color = palette.fieldBg) {
                Text(
                    text = expense.category.ifBlank { stringResource(R.string.budget_category_other) },
                    fontSize = 10.5.sp,
                    color = palette.muted,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.note.ifBlank { stringResource(R.string.budget_no_note) },
                    fontSize = 12.5.sp,
                    color = palette.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = expense.createdAt.take(10),
                    fontSize = 10.sp,
                    color = palette.muted,
                )
            }
            Text(
                text = moneyText(expense.amountCents, expense.currency.ifBlank { "CNY" }),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = palette.ink,
            )
        }
    }
}

@Composable
private fun ExpenseDialog(
    currency: String,
    submitting: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit,
) {
    var amount by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(EXPENSE_CATEGORIES.first()) }
    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        title = { Text(text = stringResource(R.string.budget_add_title), fontSize = 16.sp) },
        text = {
            Column {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.take(12) },
                    singleLine = true,
                    enabled = !submitting,
                    label = { Text(text = stringResource(R.string.budget_amount_label, currency)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(EXPENSE_CATEGORIES) { item ->
                        SelectChip(
                            text = item,
                            selected = item == category,
                            enabled = !submitting,
                            onClick = { category = item },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(40) },
                    singleLine = true,
                    enabled = !submitting,
                    label = { Text(text = stringResource(R.string.budget_note_label)) },
                )
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        fontSize = 12.sp,
                        color = LocalAppPalette.current.errorFg,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !submitting,
                onClick = { onConfirm(category, amount, note) },
            ) {
                Text(
                    text = stringResource(
                        if (submitting) R.string.budget_submitting else R.string.budget_add_confirm,
                    ),
                    color = LocalAppPalette.current.brand,
                )
            }
        },
        dismissButton = {
            TextButton(enabled = !submitting, onClick = onDismiss) {
                Text(text = stringResource(R.string.common_cancel))
            }
        },
    )
}

@Composable
private fun BudgetDialog(
    initialCurrency: String,
    estimate: Double?,
    estimating: Boolean,
    destination: String,
    days: Int,
    submitting: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    val palette = LocalAppPalette.current
    val placeLabel = destination.ifBlank { stringResource(R.string.budget_predict_unknown_city) }
    var amount by rememberSaveable { mutableStateOf("") }
    var currency by rememberSaveable { mutableStateOf(initialCurrency) }
    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        title = { Text(text = stringResource(R.string.budget_set_title), fontSize = 16.sp) },
        text = {
            Column {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.take(12) },
                    singleLine = true,
                    enabled = !submitting,
                    label = { Text(text = stringResource(R.string.budget_total_amount_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SUPPORTED_CURRENCIES) { item ->
                        SelectChip(
                            text = item,
                            selected = item == currency,
                            enabled = !submitting,
                            onClick = { currency = item },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                when {
                    estimating -> {
                        Text(
                            text = stringResource(R.string.budget_predict_loading),
                            fontSize = 11.5.sp,
                            color = palette.muted,
                        )
                    }

                    estimate != null && estimate > 0 -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(
                                    R.string.budget_predict_hint,
                                    placeLabel,
                                    days,
                                    moneyText((estimate * 100).roundToLong(), currency),
                                ),
                                fontSize = 11.5.sp,
                                color = palette.muted,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(
                                enabled = !submitting,
                                onClick = { amount = String.format(Locale.CHINA, "%.0f", estimate) },
                            ) {
                                Text(
                                    text = stringResource(R.string.budget_predict_fill),
                                    fontSize = 12.sp,
                                    color = palette.brand,
                                )
                            }
                        }
                    }
                }
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        fontSize = 12.sp,
                        color = LocalAppPalette.current.errorFg,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !submitting,
                onClick = { onConfirm(amount, currency) },
            ) {
                Text(
                    text = stringResource(
                        if (submitting) R.string.budget_submitting else R.string.budget_set_confirm,
                    ),
                    color = LocalAppPalette.current.brand,
                )
            }
        },
        dismissButton = {
            TextButton(enabled = !submitting, onClick = onDismiss) {
                Text(text = stringResource(R.string.common_cancel))
            }
        },
    )
}

@Composable
private fun SelectChip(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalAppPalette.current
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) palette.brand else palette.fieldBg,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    ) {
        Text(
            text = text,
            fontSize = 11.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) palette.onBrand else palette.muted,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun Banner(
    text: String,
    background: Color,
    foreground: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = background,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = foreground,
                modifier = Modifier.size(15.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = text, fontSize = 12.sp, color = foreground)
        }
    }
}

private fun moneyText(cents: Long, currency: String): String {
    val symbol = when (currency.uppercase(Locale.US)) {
        "CNY" -> "¥"
        "USD" -> "$"
        "EUR" -> "€"
        "JPY" -> "¥"
        "GBP" -> "£"
        "HKD" -> "HK$"
        "TWD" -> "NT$"
        "KRW" -> "₩"
        "THB" -> "฿"
        "SGD" -> "S$"
        else -> "${currency.uppercase(Locale.US)} "
    }
    return symbol + String.format(Locale.CHINA, "%,.2f", cents / 100.0)
}
