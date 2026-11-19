package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * POST /api/auth/password/forgot 的响应。
 * 开发环境不配投递通道时直接返回 reset_token；生产环境只返回统一提示。
 */
public final class ForgotPasswordResponse {

    @SerializedName("message")
    private String message;

    @SerializedName("delivery")
    private String delivery;

    @SerializedName("reset_token")
    private String resetToken;

    @SerializedName("expires_in")
    private int expiresIn;

    public String getMessage() {
        return message;
    }

    public String getDelivery() {
        return delivery;
    }

    public String getResetToken() {
        return resetToken;
    }

    public int getExpiresIn() {
        return expiresIn;
    }

    /** 开发环境直返令牌时前端可以直接进入重置步骤。 */
    public boolean hasResetToken() {
        return resetToken != null && !resetToken.trim().isEmpty();
    }
}
