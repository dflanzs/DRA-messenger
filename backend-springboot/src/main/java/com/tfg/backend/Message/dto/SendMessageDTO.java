package com.tfg.backend.Message.dto;

import io.micrometer.common.lang.Nullable;

public class SendMessageDTO {
    private Long id;
    private Long senderId;

    @Nullable
    private Long oneToOneChatId;

    @Nullable
    private Long groupChatId;

    private String content;
    private String timestamp;
    private boolean read;

    public SendMessageDTO() {
    }

    public SendMessageDTO(Long senderId, String content, Long chatId, boolean isGroupChat) {
        this.senderId = senderId;
        this.content = content;
        if (isGroupChat) {
            this.groupChatId = chatId;
        } else {
            this.oneToOneChatId = chatId; // Reusing the same variable for simplicity
        }
        this.read = false;
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
        return oneToOneChatId;
    }

    public void setOneToOneChatId(Long oneToOneChatId) {
        this.oneToOneChatId = oneToOneChatId;
    }

    public Long getGroupChatId() {
        return groupChatId;
    }

    public void setGroupChatId(Long groupChatId) {
        this.groupChatId = groupChatId;
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
