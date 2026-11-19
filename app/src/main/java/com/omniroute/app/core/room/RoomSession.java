package com.omniroute.app.core.room;

/**
 * 当前协作房间的只读快照。
 * roomId 与 inviteCode 在后端是同一个标识（邀请码即房间键）。
 */
public final class RoomSession {

    private final String roomId;
    private final String roomName;
    private final String inviteCode;
    private final String role;
    private final boolean host;

    public RoomSession(String roomId, String roomName, String inviteCode, String role, boolean host) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.inviteCode = inviteCode;
        this.role = role;
        this.host = host;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public String getRole() {
        return role;
    }

    /** 是否是本机创建的房间（决定房间页的文案与角色默认值）。 */
    public boolean isHost() {
        return host;
    }
}
