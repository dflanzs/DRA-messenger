package com.tfg.backend.Cypher.dto;

import java.time.LocalDateTime;

public class SignalDirectMessageResponseDto {
    private final Long envelopeId;
    private final LocalDateTime createdAt;

    public SignalDirectMessageResponseDto(Long envelopeId, LocalDateTime createdAt) {
        this.envelopeId = envelopeId;
        this.createdAt = createdAt;
    }
}
