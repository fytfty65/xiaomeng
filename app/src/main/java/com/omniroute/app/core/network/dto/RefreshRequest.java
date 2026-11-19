package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

public final class RefreshRequest {

    @SerializedName("refresh_token")
    private final String refreshToken;

    public RefreshRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}
