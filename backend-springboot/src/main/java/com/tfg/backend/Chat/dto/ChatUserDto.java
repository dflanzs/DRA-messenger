package com.tfg.backend.Chat.dto;

public class ChatUserDto {
    private final Long id;
    private final String name;
    private final String email;
    private final boolean onlineStatus;

    public ChatUserDto(Long id, String name, String email, boolean onlineStatus) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.onlineStatus = onlineStatus;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public boolean isOnlineStatus() {
        return onlineStatus;
    }
}

