package com.tfg.backend.User.dto;

public class UpdateOnlineStatusDto {

    private boolean online;

    public UpdateOnlineStatusDto() {
    }

    public UpdateOnlineStatusDto(boolean online) {
        this.online = online;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }
}
