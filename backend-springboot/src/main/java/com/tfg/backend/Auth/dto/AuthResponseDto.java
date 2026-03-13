package com.tfg.backend.Auth.dto;

import com.tfg.backend.User.dto.UserResponseDto;

public class AuthResponseDto {
    private String token;
    private UserResponseDto user;

    public AuthResponseDto(String token, UserResponseDto user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserResponseDto getUser() {
        return user;
    }

    public void setUser(UserResponseDto user) {
        this.user = user;
    }
}
