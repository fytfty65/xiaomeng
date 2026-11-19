package com.omniroute.app.core.network.dto;

/** GET /api/user/profile 返回 {user, trips}，客户端暂只读取 user。 */
public final class ProfileResponse {

    private UserDto user;

    public UserDto getUser() {
        return user;
    }
}
