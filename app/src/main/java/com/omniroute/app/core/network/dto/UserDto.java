package com.omniroute.app.core.network.dto;

/** Gateway 下发的用户资料字段，字段名与后端一致（camelCase）。 */
public final class UserDto {

    private String id = "";
    private String username = "";
    private String nickname;
    private String avatarSeed;
    private String avatarUrl;
    private String signature;
    private String createdAt;

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getNickname() {
        return nickname;
    }

    public String getAvatarSeed() {
        return avatarSeed;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getSignature() {
        return signature;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    /** 昵称为空时回退到用户名，与后端 buildAuthUser 的行为保持一致。 */
    public String getDisplayName() {
        if (nickname != null && !nickname.trim().isEmpty()) {
            return nickname;
        }
        return username;
    }
}
