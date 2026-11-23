package com.omniroute.app.feature.budget

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.omniroute.app.AppContainer
import com.omniroute.app.R
import com.omniroute.app.core.network.dto.BudgetSummaryDto
import com.omniroute.app.core.network.dto.ExpenseDto
import com.omniroute.app.core.ocr.ReceiptCache
import com.omniroute.app.core.ocr.ReceiptRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode

/** 分类汇总项，按金额从高到低。 */
data class BudgetCategoryUi(
    val name: String,
    val cents: Long,
    val pct: Double,
)

data class BudgetExpenseUi(
    val id: String,
    val category: String,
    val amountCents: Long,
    val currency: String,
    val note: String,
    val createdAt: String,
)

data class BudgetUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val currency: String = "CNY",
    val totalBudgetCents: Long = 0L,
    val spentCents: Long = 0L,
    val remainingCents: Long = 0L,
    val ratio: Double = 0.0,
    /** safe / warning / over。 */
    val level: String = "safe",
    val categories: List<BudgetCategoryUi> = emptyList(),
    val expenses: List<BudgetExpenseUi> = emptyList(),
    val submitting: Boolean = false,
    val recognizing: Boolean = false,
    val actionError: String? = null,
    val notice: String? = null,
    val showAddExpense: Boolean = false,
    val showSetBudget: Boolean = false,
    /** 规划阶段参考预算：目的地、天数与估算金额（元）。 */
    val destination: String = "",
    val days: Int = 0,
    val estimateYuan: Double? = null,
    val estimating: Boolean = false,
) {
    val hasBudget: Boolean get() = totalBudgetCents > 0
}

class BudgetViewModel(
    private val container: AppContainer,
    private val tripId: String,
    private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(BudgetUiState())
    val state: StateFlow<BudgetUiState> = _state.asStateFlow()

    init {
        refresh()
        loadEstimate()
    }

    fun refresh() = load(silent = false)

    private fun load(silent: Boolean) {
        if (!silent) {
            _state.update { it.copy(loading = true, error = null) }
        }
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching {
                    val summary = container.budgetRepository.summary(tripId)
                    val expenses = container.budgetRepository.expenses(tripId)
                    summary to expenses
                }
            }
            outcome.onSuccess { (summary, expenses) ->
                _state.update { current ->
                    current.copy(
                        loading = false,
                        error = null,
                        currency = summary.currency?.takeIf { it.isNotBlank() } ?: current.currency,
                        totalBudgetCents = summary.totalBudgetCents,
                        spentCents = summary.spentCents,
                        remainingCents = summary.remainingCents,
                        ratio = summary.ratio,
                        level = summary.level?.takeIf { it.isNotBlank() } ?: "safe",
                        categories = summary.toCategories(),
                        expenses = expenses
                            .map { it.toUi() }
                            .sortedByDescending { it.createdAt },
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        loading = false,
                        error = error.message ?: context.getString(R.string.budget_error_load),
                    )
                }
            }
        }
    }

    fun openAddExpense() = _state.update { it.copy(showAddExpense = true, actionError = null) }

    fun dismissAddExpense() {
        if (_state.value.submitting) return
        _state.update { it.copy(showAddExpense = false, actionError = null) }
    }

    fun openSetBudget() {
        _state.update { it.copy(showSetBudget = true, actionError = null) }
        loadEstimate()
    }

    /**
     * 打开「设置预算」时按目的地与天数取一次参考预算（规划阶段的估算口径）。
     * 估算失败一律静默，用户仍可手动填写金额。
     */
    fun loadEstimate() {
        val current = _state.value
        if (current.estimating || current.estimateYuan != null) return
        _state.update { it.copy(estimating = true) }
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching {
                    val trip = container.planningRepository.getTrip(tripId)
                    val city = trip.destCity?.takeIf { it.isNotBlank() } ?: trip.title.orEmpty()
                    val days = if (trip.days > 0) trip.days else DEFAULT_DAYS
                    Triple(city, days, container.budgetRepository.predict(city, days))
                }
            }
            outcome.onSuccess { (city, days, estimate) ->
                _state.update {
                    it.copy(
                        estimating = false,
                        destination = city,
                        days = days,
                        estimateYuan = estimate,
                    )
                }
            }.onFailure {
                _state.update { it.copy(estimating = false) }
            }
        }
    }

    fun dismissSetBudget() {
        if (_state.value.submitting) return
        _state.update { it.copy(showSetBudget = false, actionError = null) }
    }

    fun submitBudget(amountYuan: String, currency: String) {
        val cents = parseYuan(amountYuan)
        if (cents == null || cents <= 0) {
            _state.update { it.copy(actionError = context.getString(R.string.budget_error_amount)) }
            return
        }
        if (_state.value.submitting) return
        _state.update { it.copy(submitting = true, actionError = null) }
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching {
                    container.budgetRepository.setBudget(tripId, cents, currency)
                }
            }
            outcome.onSuccess {
                _state.update {
                    it.copy(
                        submitting = false,
                        showSetBudget = false,
                        currency = currency,
                        notice = context.getString(R.string.budget_saved),
                    )
                }
                load(silent = true)
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        submitting = false,
                        actionError = error.message ?: context.getString(R.string.budget_error_save),
                    )
                }
            }
        }
    }

    fun submitExpense(category: String, amountYuan: String, note: String) {
        val cents = parseYuan(amountYuan)
        if (cents == null || cents <= 0) {
            _state.update { it.copy(actionError = context.getString(R.string.budget_error_amount)) }
            return
        }
        if (_state.value.submitting) return
        _state.update { it.copy(submitting = true, actionError = null) }
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching {
                    container.budgetRepository.addExpense(
                        tripId,
                        category,
                        cents,
                        _state.value.currency,
                        note,
                    )
                }
            }
            outcome.onSuccess { summary ->
                _state.update { current ->
                    current.copy(
                        submitting = false,
                        showAddExpense = false,
                        notice = context.getString(R.string.budget_expense_saved),
                        spentCents = summary.spentCents,
                        remainingCents = summary.remainingCents,
                        ratio = summary.ratio,
                        level = summary.level?.takeIf { it.isNotBlank() } ?: current.level,
                        categories = summary.toCategories(),
                    )
                }
                load(silent = true)
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        submitting = false,
                        actionError = error.message ?: context.getString(R.string.budget_error_add),
                    )
                }
            }
        }
    }

    /**
     * 小票记账：端上 OCR 出文本 → 网关解析金额分类并落账。
     * 识别或解析失败时自动打开手动记账弹窗，避免用户白选一张图。
     */
    fun submitReceipt(uri: Uri) {
        if (_state.value.recognizing || _state.value.submitting) return
        _state.update { it.copy(recognizing = true, actionError = null) }
        viewModelScope.launch {
            try {
                val text = ReceiptRecognizer.recognize(context, uri)
                if (text.isNullOrBlank()) {
                    _state.update {
                        it.copy(
                            recognizing = false,
                            actionError = context.getString(R.string.budget_ocr_fail),
                            showAddExpense = true,
                        )
                    }
                    return@launch
                }
                val outcome = withContext(Dispatchers.IO) {
                    runCatching {
                        container.budgetRepository.ocrExpense(tripId, text, _state.value.currency)
                    }
                }
                outcome.onSuccess { response ->
                    val summary = response.summary
                    if (summary == null) {
                        _state.update {
                            it.copy(recognizing = false, actionError = context.getString(R.string.budget_ocr_fail))
                        }
                        return@onSuccess
                    }
                    _state.update { current ->
                        current.copy(
                            recognizing = false,
                            notice = response.message?.takeIf { it.isNotBlank() }
                                ?: context.getString(R.string.budget_ocr_saved),
                            spentCents = summary.spentCents,
                            remainingCents = summary.remainingCents,
                            ratio = summary.ratio,
                            level = summary.level?.takeIf { it.isNotBlank() } ?: current.level,
                            categories = summary.toCategories(),
                        )
                    }
                    load(silent = true)
                }.onFailure { error ->
                    _state.update {
                        it.copy(
                            recognizing = false,
                            showAddExpense = true,
                            actionError = error.message ?: context.getString(R.string.budget_ocr_fail),
                        )
                    }
                }
            } finally {
                // 拍照产生的临时文件在识别/落账结束后立即删除，成功与失败都不残留。
                ReceiptCache.delete(context, uri)
            }
        }
    }

    /** 用户取消拍照时，相机可能已创建空文件，这里一并清掉。 */
    fun discardReceipt(uri: Uri) = ReceiptCache.delete(context, uri)

    fun clearNotice() = _state.update { it.copy(notice = null) }

    fun clearActionError() = _state.update { it.copy(actionError = null) }

    /** 元 → 分，四舍五入，拒绝负数与非法输入。 */
    private fun parseYuan(text: String): Long? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        return runCatching {
            BigDecimal(trimmed)
                .movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP)
                .toLong()
        }.getOrNull()
    }

    companion object {
        private const val DEFAULT_DAYS = 3

        fun factory(container: AppContainer, tripId: String, context: Context): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    BudgetViewModel(container, tripId, context.applicationContext)
                }
            }
    }
}

private fun BudgetSummaryDto.toCategories(): List<BudgetCategoryUi> {
    return categoriesOrEmpty()
        .map { (name, cents) ->
            BudgetCategoryUi(
                name = name,
                cents = cents ?: 0L,
                pct = categoryPct?.get(name) ?: 0.0,
            )
        }
        .sortedByDescending { it.cents }
}

private fun ExpenseDto.toUi(): BudgetExpenseUi {
    return BudgetExpenseUi(
        id = id.orEmpty(),
        category = category?.takeIf { it.isNotBlank() } ?: "",
        amountCents = amountCents,
        currency = currency.orEmpty(),
        note = note.orEmpty(),
        createdAt = createdAt.orEmpty(),
    )
}
