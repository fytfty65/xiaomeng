package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

/** POST /api/auth/password/reset 的请求体。 */
public final class ResetPasswordRequest {

    @SerializedName("token")
    private final String token;

    @SerializedName("new_password")
    private final String newPassword;

    public ResetPasswordRequest(String token, String newPassword) {
        this.token = token;
        this.newPassword = newPassword;
    }
}
