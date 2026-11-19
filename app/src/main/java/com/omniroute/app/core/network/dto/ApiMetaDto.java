package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

/** 规划接口统一封套里的 meta 段。 */
public final class ApiMetaDto {

    @SerializedName("request_id")
    private String requestId;

    @SerializedName("timestamp")
    private String timestamp;

    @SerializedName("contract_version")
    private String contractVersion;

    public String getRequestId() {
        return requestId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getContractVersion() {
        return contractVersion;
    }
}
