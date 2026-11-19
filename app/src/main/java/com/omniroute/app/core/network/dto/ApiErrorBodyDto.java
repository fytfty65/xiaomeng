package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

/** 规划接口 Envelope 内的 error 对象（与鉴权接口的扁平 error 不同）。 */
public final class ApiErrorBodyDto {

    @SerializedName("code")
    private String code;

    @SerializedName("message")
    private String message;

    @SerializedName("request_id")
    private String requestId;

    @SerializedName("details")
    private Map<String, String> details;

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getRequestId() {
        return requestId;
    }

    public Map<String, String> getDetails() {
        return details;
    }
}
