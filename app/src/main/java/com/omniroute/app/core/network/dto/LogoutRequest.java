package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

public final class LogoutRequest {

    @SerializedName("refresh_token")
    private final String refreshToken;

    public LogoutRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}
