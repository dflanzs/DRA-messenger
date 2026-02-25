package com.tfg.backend.ChatGroup;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import com.tfg.backend.user.User;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "one_to_one_chats")
public class ChatGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User sender;

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

    public Long getSenderId() {
        return sender.getId();
    }

    public Long[] getUserIds() {
        Long[] userIds = new Long[users.length];
        for (int i = 0; i < users.length; i++) {
            userIds[i] = users[i].getId();
        }
        return userIds;
    }

    public Long[] getReceiverId(int index) {
        Long[] receiverIds = new Long[users.length - 1];
        for (int i = 0; i < users.length - 1; i++) {
            receiverIds[i] = users[i].getId() != sender.getId() ? users[i].getId() : users[i + 1].getId();
        }
        return receiverIds;
    }
}
