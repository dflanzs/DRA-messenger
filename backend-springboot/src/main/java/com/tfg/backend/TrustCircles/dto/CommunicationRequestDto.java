package com.tfg.backend.TrustCircles.dto;

import java.time.LocalDateTime;

public record CommunicationRequestDto(
    Long id,
    Long requesterId,
    String requesterName,
    Long targetId,
    String targetName,
    LocalDateTime createdAt
) {
}
