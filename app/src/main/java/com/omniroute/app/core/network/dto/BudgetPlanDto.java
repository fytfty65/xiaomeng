package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

/** 行程预算：金额统一为整数分，避免浮点误差。 */
public final class BudgetPlanDto {

    @SerializedName("id")
    public String id;

    @SerializedName("trip_id")
    public String tripId;

    @SerializedName("total_budget_cents")
    public long totalBudgetCents;

    @SerializedName("currency")
    public String currency;

    @SerializedName("created_at")
    public String createdAt;
}
