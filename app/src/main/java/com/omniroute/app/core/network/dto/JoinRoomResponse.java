package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

/** 加入房间的返回体（与创建房间同样是扁平 JSON，不走 data 信封）。 */
public final class JoinRoomResponse {

    @SerializedName("message")
    private String message;

    @SerializedName("room_name")
    private String roomName;

    @SerializedName("room_id")
    private String roomId;

    @SerializedName("user_id")
    private String userId;

    public String getMessage() {
        return message;
    }

    public String getRoomName() {
        return roomName;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getUserId() {
        return userId;
    }
}
