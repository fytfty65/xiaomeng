package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

/** 一笔实际消费。 */
public final class ExpenseDto {

    @SerializedName("id")
    public String id;

    @SerializedName("trip_id")
    public String tripId;

    @SerializedName("category")
    public String category;

    @SerializedName("amount_cents")
    public long amountCents;

    @SerializedName("currency")
    public String currency;

    @SerializedName("note")
    public String note;

    @SerializedName("created_at")
    public String createdAt;
}
