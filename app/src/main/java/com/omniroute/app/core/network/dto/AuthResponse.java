package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

/** 移动端登录/刷新的统一响应，字段名与 Gateway 的 mobile_auth.go 一致。 */
public final class AuthResponse {

    @SerializedName("access_token")
    private String accessToken;

    @SerializedName("refresh_token")
    private String refreshToken;

    @SerializedName("expires_in")
    private long expiresIn;

    private UserDto user;

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public UserDto getUser() {
        return user;
    }
}
