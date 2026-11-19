package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

/** GET /api/v1/budget/predict：按城市档位与天数给出的参考预算（单位：元）。 */
public final class BudgetPredictResponse {

    @SerializedName("estimated_cost")
    public double estimatedCost;

    @SerializedName("city")
    public String city;

    @SerializedName("days")
    public int days;
}
