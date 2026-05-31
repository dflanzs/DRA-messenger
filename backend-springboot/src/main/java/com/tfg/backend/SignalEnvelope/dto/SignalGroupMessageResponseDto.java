package com.tfg.backend.SignalEnvelope.dto;

import java.time.LocalDateTime;
import java.util.List;

public class SignalGroupMessageResponseDto {
    private final List<Long> envelopesId;
    private final LocalDateTime createdAt;

    public SignalGroupMessageResponseDto(List<Long> envelopesId, LocalDateTime createdAt) {
        this.envelopesId = envelopesId;
        this.createdAt = createdAt;
    }
}
