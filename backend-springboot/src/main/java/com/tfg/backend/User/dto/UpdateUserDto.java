package com.tfg.backend.User.dto;

public class UpdateUserDto {
    public static final String MODE_NAME = "name";
    public static final String MODE_EMAIL = "email";
    public static final String MODE_PASSWORD = "password";

    private String updatedValue;
    private String mode;

    public UpdateUserDto(String string, String mode) {
        this.updatedValue = string;
        if (mode == null || (!mode.equals(MODE_NAME) && !mode.equals(MODE_EMAIL) && !mode.equals(MODE_PASSWORD))) {
            throw new IllegalArgumentException("Invalid mode: " + mode);
        }
        this.mode = mode;
    }

    public String getUpdatedValue() {
        return updatedValue;
    }

    public String getMode() {
        return mode;
    }
}
