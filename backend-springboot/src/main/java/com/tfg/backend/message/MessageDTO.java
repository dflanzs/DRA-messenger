package com.tfg.backend.message;

import io.micrometer.common.lang.Nullable;

public class MessageDTO {
    private Long id;
    private Long senderId;

    @Nullable
    private Long OneToOneChatId;
    @Nullable
    private Long chatGroupId;

    private String content;
    private String timestamp;
    private boolean read;

    public MessageDTO() {
    }

    public MessageDTO(Long senderId, String content, @Nullable Long OneToOneChatId, @Nullable Long chatGroupId) {
        this.senderId = senderId;
        this.OneToOneChatId = OneToOneChatId;
        this.chatGroupId = chatGroupId;
        this.content = content;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public Long getOneToOneChatId() {
        return OneToOneChatId;
    }

    public void setOneToOneChatId(Long oneToOneChatId) {
        OneToOneChatId = oneToOneChatId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}
