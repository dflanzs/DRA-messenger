package com.tfg.backend.SignalEnvelope.dto;

import java.time.LocalDateTime;

public class SignalDirectMessageResponseDto {
    private final Long envelopeId;
    private final LocalDateTime createdAt;

    public SignalDirectMessageResponseDto(Long envelopeId, LocalDateTime createdAt) {
        this.envelopeId = envelopeId;
        this.createdAt = createdAt;
    }
}
