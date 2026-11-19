package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

/** POST /api/v1/room/create 响应（扁平结构，无 Envelope）。 */
public final class CreateRoomResponse {

    @SerializedName("message")
    private String message;

    @SerializedName("invite_code")
    private String inviteCode;

    @SerializedName("room_id")
    private String roomId;

    @SerializedName("user_id")
    private String userId;

    public String getMessage() {
        return message;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getUserId() {
        return userId;
    }
}
