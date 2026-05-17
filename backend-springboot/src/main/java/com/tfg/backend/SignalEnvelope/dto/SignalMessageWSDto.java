package com.tfg.backend.SignalEnvelope.dto;

import java.time.LocalDateTime;

public class SignalMessageWSDto {
    private final Long envelopeId;
    private final Long senderUserId;
    private final Long conversationId;
    private final String conversationType;
    private final Short cypherTextType;
    private final String cypherTextB64;
    private final LocalDateTime createdAt;

    public SignalMessageWSDto(
        Long envelopeId,
        Long senderUserId,
        Long conversationId,
        String conversationType,
        Short cypherTextType,
        String cypherTextB64,
        LocalDateTime createdAt
    ) {
        this.envelopeId = envelopeId;
        this.senderUserId = senderUserId;
        this.conversationId = conversationId;
        this.conversationType = conversationType;
        this.cypherTextType = cypherTextType;
        this.cypherTextB64 = cypherTextB64;
        this.createdAt = createdAt;
    }

    public Long getEnvelopeId() {
        return envelopeId;
    }

    public Long getSenderUserId() {
        return senderUserId;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public String getConversationType() {
        return conversationType;
    }

    public Short getCypherTextType() {
        return cypherTextType;
    }

    public String getCypherTextB64() {
        return cypherTextB64;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
