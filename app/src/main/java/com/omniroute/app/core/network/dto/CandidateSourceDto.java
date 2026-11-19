package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

/** 候选来源：provider 为空表示本地估算。 */
public final class CandidateSourceDto {

    @SerializedName("provider")
    private String provider;

    @SerializedName("retrieved_at")
    private String retrievedAt;

    @SerializedName("estimated")
    private boolean estimated;

    public String getProvider() {
        return provider;
    }

    public String getRetrievedAt() {
        return retrievedAt;
    }

    public boolean isEstimated() {
        return estimated;
    }
}
