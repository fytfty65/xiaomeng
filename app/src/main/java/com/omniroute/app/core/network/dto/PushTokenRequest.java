package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

/** FCM 设备 token 登记请求。 */
public final class PushTokenRequest {

    @SerializedName("token")
    private final String token;

    @SerializedName("platform")
    private final String platform;

    public PushTokenRequest(String token, String platform) {
        this.token = token;
        this.platform = platform;
    }
}
