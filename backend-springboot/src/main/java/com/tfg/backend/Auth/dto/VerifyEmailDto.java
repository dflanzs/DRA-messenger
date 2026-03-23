package com.tfg.backend.Auth.dto;

import jakarta.validation.constraints.NotBlank;

public class VerifyEmailDto {
    @NotBlank
    private String token;

    public VerifyEmailDto() {
    }

    public VerifyEmailDto(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
