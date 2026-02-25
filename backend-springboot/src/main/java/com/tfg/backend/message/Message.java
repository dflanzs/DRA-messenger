package com.tfg.backend.message;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

import com.tfg.backend.GroupChat.GroupChat;
import com.tfg.backend.OneToOneChat.OneToOneChat;
import com.tfg.backend.user.User;

import io.micrometer.common.lang.Nullable;

@Entity
@Table(name = "messages")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User sender;

    @ManyToOne
    @Nullable
    private OneToOneChat oneToOneChat;


    @ManyToOne
    @Nullable
    private GroupChat groupChat;

    @NotBlank
    private String content;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    private boolean read;

    public Message() {
    }

    public Message(User sender, String content, OneToOneChat oneToOneChat) {
        this.sender = sender;
        this.content = content;
        this.oneToOneChat = oneToOneChat;
        this.createdAt = LocalDateTime.now();
        this.read = false;
    }

    public Message(User sender, String content, GroupChat groupChat) {
        this.sender = sender;
        this.content = content;
        this.groupChat = groupChat;
        this.createdAt = LocalDateTime.now();
        this.read = false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public OneToOneChat getOneToOneChat() {
        return oneToOneChat;
    }

    public void setOneToOneChat(OneToOneChat oneToOneChat) {
        this.oneToOneChat = oneToOneChat;
    }

    public GroupChat getGroupChat() {
        return groupChat;
    }

    public void setGroupChat(GroupChat groupChat) {
        this.groupChat = groupChat;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public Long getReceiverId() {
        if (oneToOneChat == null) return null;

        Long[] userIds = oneToOneChat.getUserIds();
        return userIds[0] != sender.getId() ? userIds[0] : userIds[1];
    }

    public Long[] getGroupReceiverIds() {
        if (groupChat == null) return null;

        Long[] usersIds = groupChat.getUserIds();

        Long[] receiverIds = new Long[usersIds.length - 1];
        for (int i = 0; i < usersIds.length - 1; i++) {
            receiverIds[i] = usersIds[i] != sender.getId() ? usersIds[i] : usersIds[i + 1];
        }
        return receiverIds;
    }
}
