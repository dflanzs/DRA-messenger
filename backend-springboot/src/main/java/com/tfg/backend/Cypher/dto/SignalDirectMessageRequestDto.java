package com.tfg.backend.Cypher.dto;

import jakarta.validation.constraints.NotNull;

public class SignalDirectMessageRequestDto {
    @NotNull
    private  Long recipientUserId;

    // Equivalent to OneToOneChat
    @NotNull
    private  Long conversationId;

    @NotNull
    private  Short cypherTextType;

    @NotNull
    private String cypherTextB64;

    public SignalDirectMessageRequestDto() {
    }

    public Long getRecipientUserId() {
        return recipientUserId;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public Short getCypherTextType() {
        return cypherTextType;
    }

    public String getCypherTextB64() {
        return cypherTextB64;
    }
}
