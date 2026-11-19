package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.LinkedHashMap;
import java.util.Map;

/** 后端 service.ExpenseSummary 的镜像：预算总额、已花、占比、预警等级与分类汇总。 */
public final class BudgetSummaryDto {

    @SerializedName("total_budget_cents")
    public long totalBudgetCents;

    @SerializedName("spent_cents")
    public long spentCents;

    @SerializedName("remaining_cents")
    public long remainingCents;

    /** 已用预算占比（0~1+）。 */
    @SerializedName("ratio")
    public double ratio;

    /** safe / warning / over。 */
    @SerializedName("level")
    public String level;

    @SerializedName("currency")
    public String currency;

    @SerializedName("precision")
    public int precision;

    @SerializedName("by_category_cents")
    public Map<String, Long> byCategoryCents;

    @SerializedName("category_pct")
    public Map<String, Double> categoryPct;

    @SerializedName("expense_count")
    public int expenseCount;

    public Map<String, Long> categoriesOrEmpty() {
        return byCategoryCents == null ? new LinkedHashMap<>() : byCategoryCents;
    }

    public boolean hasBudget() {
        return totalBudgetCents > 0;
    }
}
