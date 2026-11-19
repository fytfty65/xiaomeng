package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

/** POST /api/v1/plan/variant：保存成功后的回显。 */
public final class SaveVariantResponse {

    @SerializedName("message")
    public String message;

    @SerializedName("variant")
    public PlanVariantDto variant;
}
