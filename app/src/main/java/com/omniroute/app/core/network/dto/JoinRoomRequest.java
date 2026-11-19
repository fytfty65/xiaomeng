package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

/** 输入邀请码加入协作房间。 */
public final class JoinRoomRequest {

    @SerializedName("username")
    private final String username;

    @SerializedName("invite_code")
    private final String inviteCode;

    @SerializedName("role")
    private final String role;

    public JoinRoomRequest(String username, String inviteCode, String role) {
        this.username = username;
        this.inviteCode = inviteCode;
        this.role = role;
    }
}
