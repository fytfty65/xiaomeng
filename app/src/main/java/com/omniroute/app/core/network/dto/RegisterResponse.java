package com.omniroute.app.core.network.dto;

public final class RegisterResponse {

    private String message;
    private UserDto user;

    public String getMessage() {
        return message;
    }

    public UserDto getUser() {
        return user;
    }
}
