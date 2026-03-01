package com.tfg.backend.GroupChat;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import com.tfg.backend.User.User;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "one_to_one_chats")
public class GroupChat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToMany
    private User[] users;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime deletedAt;

    public Long getId() {
        return id;
    }

    public Long[] getUserIds() {
        Long[] userIds = new Long[users.length];
        for (int i = 0; i < users.length; i++) {
            userIds[i] = users[i].getId();
        }
        return userIds;
    }
}
