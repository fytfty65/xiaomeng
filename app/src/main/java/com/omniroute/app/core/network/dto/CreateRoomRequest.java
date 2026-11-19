package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

/** POST /api/v1/room/create 请求体。 */
public final class CreateRoomRequest {

    @SerializedName("username")
    private final String username;

    @SerializedName("room_name")
    private final String roomName;

    public CreateRoomRequest(String username, String roomName) {
        this.username = username;
        this.roomName = roomName;
    }

    public String getUsername() {
        return username;
    }

    public String getRoomName() {
        return roomName;
    }
}
