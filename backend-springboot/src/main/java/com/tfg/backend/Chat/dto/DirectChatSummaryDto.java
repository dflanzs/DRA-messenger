package com.tfg.backend.Chat.dto;

import java.time.LocalDateTime;

public record DirectChatSummaryDto(
    Long id,
    Long user1Id,
    Long user2Id,
    String otherUserName,
    LocalDateTime createdAt
) {
}

