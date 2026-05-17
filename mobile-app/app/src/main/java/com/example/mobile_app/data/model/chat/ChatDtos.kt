package com.example.mobile_app.data.model.chat

import java.util.UUID

data class ChatUserDto(
    val id: Long,
    val name: String,
    val email: String,
    val onlineStatus: Boolean,
)

data class CreateDirectChatRequestDto(
    val targetUserId: Long,
)

data class CreateGroupChatRequestDto(
    val name: String,
    val userIds: Set<Long>,
)

data class DirectChatSummaryDto(
    val id: Long,
    val user1Id: Long,
    val user2Id: Long,
    val otherUserName: String,
    val createdAt: String,
)

data class GroupChatSummaryDto(
    val id: Long,
    val name: String,
    val userIds: Set<Long>,
    val createdAt: String,
)

data class CommunicationRequestDto(
    val id: Long,
    val requesterId: Long,
    val requesterName: String,
    val targetId: Long,
    val targetName: String,
    val createdAt: String,
)

data class DirectChatResultDto(
    val status: String,
    val chat: DirectChatSummaryDto? = null,
    val request: CommunicationRequestDto? = null,
)

data class LocalChatRecord(
    val chatKey: String,
    val chatId: Long? = null,
    val type: String,
    val title: String,
    val memberIds: List<Long>,
    val memberNames: List<String>,
    val createdAt: String,
    val lastMessageText: String? = null,
    val lastMessageAt: String? = null,
    val status: String = "ACTIVE",
    val requestId: Long? = null,
)

data class LocalChatMessageRecord(
    val messageId: String = UUID.randomUUID().toString(),
    val chatKey: String,
    val senderUserId: Long,
    val senderName: String,
    val text: String,
    val createdAt: String,
    val isOutgoing: Boolean,
)

data class ChatState(
    val users: List<ChatUserDto> = emptyList(),
    val chats: List<LocalChatRecord> = emptyList(),
    val messages: List<LocalChatMessageRecord> = emptyList(),
    val hiddenChatKeys: List<String> = emptyList(),
)
