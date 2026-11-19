package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** GET /api/v1/budget/summary */
public final class BudgetSummaryResponse {

    @SerializedName("summary")
    public BudgetSummaryDto summary;

    public BudgetSummaryDto summaryOrEmpty() {
        return summary == null ? new BudgetSummaryDto() : summary;
    }

    /** GET /api/v1/expenses */
    public static final class ExpensesResponse {
        @SerializedName("expenses")
        public List<ExpenseDto> expenses;

        public List<ExpenseDto> expensesOrEmpty() {
            return expenses == null ? new ArrayList<>() : expenses;
        }
    }

    /** POST /api/v1/budget */
    public static final class SetBudgetResponse {
        @SerializedName("message")
        public String message;

        @SerializedName("budget_plan")
        public BudgetPlanDto budgetPlan;
    }

    /** POST /api/v1/expense：记录后即返回最新汇总，用于立即刷新预警。 */
    public static final class AddExpenseResponse {
        @SerializedName("message")
        public String message;

        @SerializedName("expense")
        public ExpenseDto expense;

        @SerializedName("summary")
        public BudgetSummaryDto summary;
    }
}
