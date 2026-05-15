package com.tfg.backend.Chat.dto;

import java.time.LocalDateTime;
import java.util.Set;

public record GroupChatSummaryDto(
    Long id,
    String name,
    Set<Long> userIds,
    LocalDateTime createdAt
) {
}

