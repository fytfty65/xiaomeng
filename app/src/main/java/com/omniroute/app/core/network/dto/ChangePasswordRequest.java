package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

/** POST /api/auth/password/change 的请求体（需登录）。 */
public final class ChangePasswordRequest {

    @SerializedName("current_password")
    private final String currentPassword;

    @SerializedName("new_password")
    private final String newPassword;

    public ChangePasswordRequest(String currentPassword, String newPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
    }
}
