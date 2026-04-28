package com.tfg.backend.Cypher.dto;

import java.time.LocalDateTime;

import com.tfg.backend.Cypher.Entity.SignalEnvelope;

public class SignalDirectMessageWSDto {
    private final Long envelopeId;
    private final SignalEnvelope envelope; 
    private final Long senderUserId;
    private final Long oneToOneChatId;
    private final String conversationType; 
    private final Short cypherTextType;
    private final String cypherTextB64;
    private final LocalDateTime createdAt;

    public SignalDirectMessageWSDto(
        Long envelopeId,
        SignalEnvelope envelope,
        Long senderUserId,
        Long oneToOneChatId,
        String conversationType,
        Short cypherTextType,
        String cypherTextB64,
        LocalDateTime createdAt
    ) {
        this.envelopeId = envelopeId;
        this.envelope = envelope;
        this.senderUserId = senderUserId;
        this.oneToOneChatId = oneToOneChatId;
        this.conversationType = conversationType;
        this.cypherTextType = cypherTextType;
        this.cypherTextB64 = cypherTextB64;
        this.createdAt = createdAt;
    }

    public Long getEnvelopeId() {
        return envelopeId;
    }

    public SignalEnvelope getEnvelope() {
        return envelope;
    }

    public Long getSenderUserId() {
        return senderUserId;
    }

    public Long getOneToOneChatId() {
        return oneToOneChatId;
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
