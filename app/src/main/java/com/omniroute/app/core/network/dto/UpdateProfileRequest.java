package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

/** PUT /api/user/profile 的请求体：昵称与个性签名，用户身份由服务端会话决定。 */
public final class UpdateProfileRequest {

    @SerializedName("nickname")
    private final String nickname;

    @SerializedName("signature")
    private final String signature;

    public UpdateProfileRequest(String nickname, String signature) {
        this.nickname = nickname;
        this.signature = signature;
    }
}
