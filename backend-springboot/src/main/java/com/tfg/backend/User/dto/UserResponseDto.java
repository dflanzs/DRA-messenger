package com.tfg.backend.User.dto;

import com.tfg.backend.User.User;
import java.time.LocalDateTime;

public class UserResponseDto {
    private Long id;
    private String name;
    private String email;
    private String publicKey;
    private boolean onlineStatus;
    private LocalDateTime createdAt;

    public UserResponseDto(Long id, String name, String email, String publicKey,
                           boolean onlineStatus, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.publicKey = publicKey;
        this.onlineStatus = onlineStatus;
        this.createdAt = createdAt;
    }

    public static UserResponseDto fromUser(User user) {
        return new UserResponseDto(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getPublicKey(),
            user.isOnlineStatus(),
            user.getCreatedAt()
        );
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPublicKey() { return publicKey; }
    public boolean isOnlineStatus() { return onlineStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
