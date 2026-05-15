package com.tfg.backend.Chat.dto;

import java.util.Set;

public record CreateGroupChatRequestDto(String name, Set<Long> userIds) {
}

