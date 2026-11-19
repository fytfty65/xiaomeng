package com.omniroute.app.core.room;

import com.google.gson.annotations.SerializedName;

/** 房间在线成员：由 WebSocket 的 room_members_update 推送。 */
public final class RoomMember {

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("role")
    private String role;

    @SerializedName("intent")
    private String intent;

    @SerializedName("avatarSeed")
    private String avatarSeed;

    @SerializedName("avatar_url")
    private String avatarUrl;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }

    public String getIntent() {
        return intent;
    }

    public String getAvatarSeed() {
        return avatarSeed;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    /** 头像占位字符：昵称首字，取不到时用人形占位。 */
    public String getInitial() {
        String label = name == null ? "" : name.trim();
        if (label.isEmpty()) {
            return "·";
        }
        return label.substring(0, 1).toUpperCase();
    }
}
