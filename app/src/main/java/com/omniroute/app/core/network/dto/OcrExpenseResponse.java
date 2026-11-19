package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

/** POST /api/v1/expense/ocr：网关从小票文本中解析金额与分类并直接记一笔。 */
public final class OcrExpenseResponse {

    @SerializedName("message")
    public String message;

    @SerializedName("expense")
    public ExpenseDto expense;

    @SerializedName("receipt")
    public Receipt receipt;

    @SerializedName("summary")
    public BudgetSummaryDto summary;

    /** 服务端识别结果，便于界面回显「识别为 xx 元 / 分类」。 */
    public static final class Receipt {
        @SerializedName("amount_cents")
        public long amountCents;

        @SerializedName("currency")
        public String currency;

        @SerializedName("category")
        public String category;
    }
}
