package com.tfg.backend.OneToOneChat;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import com.tfg.backend.User.User;

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
    private User user1;

    @ManyToOne
    private User user2;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    public OneToOneChat() {
    }

    public OneToOneChat(User user1, User user2) {
        this.user1 = user1;
        this.user2 = user2;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser1() {
        return user1;
    }

    public void setUser1(User user1) {
        this.user1 = user1;
    }

    public User getUser2() {
        return user2;
    }

    public void setUser2(User user2) {
        this.user2 = user2;
    }

    public Long getSenderId() {
        return user1.getId();
    }

    public Long getReceiverId() {
        return user2.getId();
    }

    public Long[] getUserIds() {
        return new Long[]{user1.getId(), user2.getId()};
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
