package com.tfg.backend.SignalEnvelope.dto;

import jakarta.validation.constraints.NotNull;

public class SignalGroupMessageRequestDto {
    @NotNull
    private  Long recipientUserId;

    // Equivalent to OneToOneChat
    @NotNull
    private  Long groupChatId;

    @NotNull
    private  Short cypherTextType;

    @NotNull
    private String cypherTextB64;

    public SignalGroupMessageRequestDto() {
    }

    public Long getRecipientUserId() {
        return recipientUserId;
    }

    public Long getGroupChatId() {
        return groupChatId;
    }

    public Short getCypherTextType() {
        return cypherTextType;
    }

    public String getCypherTextB64() {
        return cypherTextB64;
    }
}
