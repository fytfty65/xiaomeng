package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

/** POST /api/auth/password/forgot 的请求体。 */
public final class ForgotPasswordRequest {

    @SerializedName("username")
    private final String username;

    public ForgotPasswordRequest(String username) {
        this.username = username;
    }
}
