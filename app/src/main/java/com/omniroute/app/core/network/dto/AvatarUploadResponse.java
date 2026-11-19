package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

/** POST /api/user/avatar/upload 的响应；avatar_url 是相对路径。 */
public final class AvatarUploadResponse {

    @SerializedName("message")
    private String message;

    @SerializedName("avatar_url")
    private String avatarUrl;

    public String getMessage() {
        return message;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }
}
