package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

/** 只关心 message 的通用响应（改密 / 重置密码等）。 */
public final class MessageResponse {

    @SerializedName("message")
    private String message;

    public String getMessage() {
        return message;
    }
}
