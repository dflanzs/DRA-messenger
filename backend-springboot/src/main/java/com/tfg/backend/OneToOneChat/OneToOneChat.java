package com.tfg.backend.OneToOneChat;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import com.tfg.backend.user.User;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "one_to_one_chats")
public class OneToOneChat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User sender;

    @ManyToOne
    private User receiver;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime deletedAt;

    public OneToOneChat(Long senderId, Long receiverId) {
        this.sender = new User();
        this.sender.setId(senderId);
        this.receiver = new User();
        this.receiver.setId(receiverId);
    }

    public Long getId() {
        return id;
    }

    public Long getSenderId() {
        return sender.getId();
    }

    public Long getReceiverId() {
        return receiver.getId();
    }
}
