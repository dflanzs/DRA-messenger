package com.example.mobile_app.data.repository.chat

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.mobile_app.data.model.auth.UserResponseDto
import com.example.mobile_app.data.model.chat.ChatState
import com.example.mobile_app.data.model.chat.ChatUserDto
import com.example.mobile_app.data.model.chat.CreateDirectChatRequestDto
import com.example.mobile_app.data.model.chat.CreateGroupChatRequestDto
import com.example.mobile_app.data.model.chat.DirectChatSummaryDto
import com.example.mobile_app.data.model.chat.GroupChatSummaryDto
import com.example.mobile_app.data.model.chat.LocalChatMessageRecord
import com.example.mobile_app.data.model.chat.LocalChatRecord
import com.example.mobile_app.data.network.ChatApiService
import com.example.mobile_app.security.CurrentUserInfo
import com.example.mobile_app.security.CurrentUserManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

private const val PREFS_NAME = "chat_store"
private val Context.chatDataStore by preferencesDataStore(name = PREFS_NAME)
private val CHAT_STATE_JSON_KEY = stringPreferencesKey("chat_state_json")

class ChatRepository(
    private val context: Context,
    private val chatApiService: ChatApiService,
    private val currentUserManager: CurrentUserManager,
    private val moshi: Moshi,
) {
    private val TAG = "ChatRepository"
    private val stateAdapter = moshi.newBuilder()
        .add(KotlinJsonAdapterFactory())
        .build()
        .adapter(ChatState::class.java)

    fun observeState(): Flow<ChatState> = context.chatDataStore.data.map { prefs ->
        decodeState(prefs[CHAT_STATE_JSON_KEY])
    }

    suspend fun refreshFromServer() {
        currentUserManager.getCurrentUser() ?: return
        val users = chatApiService.listActiveUsers()
        // A failure loading chats must not discard the user list: the create-chat
        // dialog depends on `users`, so chat fetches are isolated and degrade to empty.
        val directChats = runCatching { chatApiService.listPrivateChats() }
            .getOrElse { Log.w(TAG, "No se pudieron cargar los chats privados: ${it.message}"); emptyList() }
        val groupChats = runCatching { chatApiService.listGroupChats() }
            .getOrElse { Log.w(TAG, "No se pudieron cargar los chats de grupo: ${it.message}"); emptyList() }

        context.chatDataStore.edit { prefs ->
            val currentState = decodeState(prefs[CHAT_STATE_JSON_KEY])
            val currentUserSnapshot = currentUserManager.getCurrentUser()
            val hidden = currentState.hiddenChatKeys.toSet()
            val chats = buildList {
                directChats.forEach { chat ->
                    val chatKey = chatKey("DIRECT", chat.id)
                    if (chatKey !in hidden) {
                        add(mapDirectChat(chat, users, currentUserSnapshot))
                    }
                }
                groupChats.forEach { chat ->
                    val chatKey = chatKey("GROUP", chat.id)
                    if (chatKey !in hidden) {
                        add(mapGroupChat(chat, users, currentUserSnapshot))
                    }
                }
            }
            prefs[CHAT_STATE_JSON_KEY] = encodeState(
                currentState.copy(
                    users = users,
                    chats = chats.sortedByDescending { it.lastMessageAt ?: it.createdAt },
                    messages = currentState.messages.filter { it.chatKey in chats.map { chat -> chat.chatKey }.toSet() },
                )
            )
        }
    }

    suspend fun createDirectChat(targetUserId: Long): LocalChatRecord? {
        val created = chatApiService.createDirectChat(CreateDirectChatRequestDto(targetUserId))
        val currentUser = currentUserManager.getCurrentUser() ?: return null
        val users = getUsersSnapshot()
        val chat = mapDirectChat(created, users, currentUser)
        upsertChat(chat)
        return chat
    }

    suspend fun createGroupChat(name: String, userIds: Set<Long>): LocalChatRecord? {
        val created = chatApiService.createGroupChat(CreateGroupChatRequestDto(name, userIds))
        val currentUser = currentUserManager.getCurrentUser() ?: return null
        val users = getUsersSnapshot()
        val chat = mapGroupChat(created, users, currentUser)
        upsertChat(chat)
        return chat
    }

    suspend fun deleteChatLocally(chatKey: String) {
        context.chatDataStore.edit { prefs ->
            val state = decodeState(prefs[CHAT_STATE_JSON_KEY])
            val updated = state.copy(
                chats = state.chats.filterNot { it.chatKey == chatKey },
                messages = state.messages.filterNot { it.chatKey == chatKey },
                hiddenChatKeys = (state.hiddenChatKeys + chatKey).distinct(),
            )
            prefs[CHAT_STATE_JSON_KEY] = encodeState(updated)
        }
    }

    suspend fun saveIncomingWebSocketMessage(
        conversationType: String,
        conversationId: Long,
        senderUserId: Long,
        cypherTextB64: String,
        createdAt: String,
    ) {
        val currentUser = currentUserManager.getCurrentUser() ?: return
        val users = getUsersSnapshot()
        val senderName = users.firstOrNull { it.id == senderUserId }?.name ?: senderUserId.toString()
        val text = decodePayload(cypherTextB64)
        saveMessage(
            chatKey = chatKey(conversationType, conversationId),
            senderUserId = senderUserId,
            senderName = senderName,
            text = text,
            createdAt = createdAt,
            isOutgoing = senderUserId == currentUser.id,
        )
    }

    suspend fun saveOutgoingMessage(
        chatKey: String,
        senderUserId: Long,
        senderName: String,
        text: String,
        createdAt: String = LocalDateTime.now().toString(),
    ) {
        saveMessage(
            chatKey = chatKey,
            senderUserId = senderUserId,
            senderName = senderName,
            text = text,
            createdAt = createdAt,
            isOutgoing = true,
        )
    }

    suspend fun getState(): ChatState = context.chatDataStore.data.first().let { prefs ->
        decodeState(prefs[CHAT_STATE_JSON_KEY])
    }

    suspend fun getChat(chatKey: String): LocalChatRecord? = getState().chats.firstOrNull { it.chatKey == chatKey }

    suspend fun getMessages(chatKey: String): List<LocalChatMessageRecord> =
        getState().messages.filter { it.chatKey == chatKey }.sortedBy { it.createdAt }

    suspend fun getMembers(chatKey: String): List<Pair<Long, String>> {
        val state = getState()
        val chat = state.chats.firstOrNull { it.chatKey == chatKey } ?: return emptyList()
        return chat.memberIds.mapIndexed { index, id ->
            id to (chat.memberNames.getOrNull(index) ?: state.users.firstOrNull { it.id == id }?.name ?: id.toString())
        }
    }

    private suspend fun upsertChat(chat: LocalChatRecord) {
        context.chatDataStore.edit { prefs ->
            val state = decodeState(prefs[CHAT_STATE_JSON_KEY])
            val mergedChats = state.chats.filterNot { it.chatKey == chat.chatKey } + chat
            prefs[CHAT_STATE_JSON_KEY] = encodeState(state.copy(chats = mergedChats.sortedByDescending { it.lastMessageAt ?: it.createdAt }))
        }
    }

    private suspend fun saveMessage(
        chatKey: String,
        senderUserId: Long,
        senderName: String,
        text: String,
        createdAt: String,
        isOutgoing: Boolean,
    ) {
        context.chatDataStore.edit { prefs ->
            val state = decodeState(prefs[CHAT_STATE_JSON_KEY])
            val message = LocalChatMessageRecord(
                messageId = UUID.randomUUID().toString(),
                chatKey = chatKey,
                senderUserId = senderUserId,
                senderName = senderName,
                text = text,
                createdAt = createdAt,
                isOutgoing = isOutgoing,
            )
            val updatedMessages = (state.messages + message).sortedBy { it.createdAt }
            val updatedChats = state.chats.map { chat ->
                if (chat.chatKey == chatKey) {
                    chat.copy(lastMessageText = text, lastMessageAt = createdAt)
                } else {
                    chat
                }
            }
            prefs[CHAT_STATE_JSON_KEY] = encodeState(state.copy(chats = updatedChats, messages = updatedMessages))
        }
    }

    private suspend fun getUsersSnapshot(): List<ChatUserDto> {
        val state = getState()
        return state.users.ifEmpty { chatApiService.listActiveUsers() }
    }

    private fun mapDirectChat(
        chat: DirectChatSummaryDto,
        users: List<ChatUserDto>,
        currentUser: CurrentUserInfo?,
    ): LocalChatRecord {
        val memberIds = listOf(chat.user1Id, chat.user2Id)
        val memberNames = memberIds.map { id ->
            users.firstOrNull { it.id == id }?.name
                ?: if (currentUser != null && currentUser.id == id) currentUser.name else id.toString()
        }
        val title = chat.otherUserName.ifBlank {
            memberNames.firstOrNull { it != currentUser?.name } ?: chat.otherUserName
        }
        return LocalChatRecord(
            chatKey = chatKey("DIRECT", chat.id),
            chatId = chat.id,
            type = "DIRECT",
            title = title,
            memberIds = memberIds,
            memberNames = memberNames,
            createdAt = chat.createdAt,
            lastMessageText = null,
            lastMessageAt = null,
        )
    }

    private fun mapGroupChat(
        chat: GroupChatSummaryDto,
        users: List<ChatUserDto>,
        currentUser: CurrentUserInfo?,
    ): LocalChatRecord {
        val memberIds = chat.userIds.toList()
        val memberNames = memberIds.map { id ->
            users.firstOrNull { it.id == id }?.name
                ?: if (currentUser != null && currentUser.id == id) currentUser.name else id.toString()
        }
        return LocalChatRecord(
            chatKey = chatKey("GROUP", chat.id),
            chatId = chat.id,
            type = "GROUP",
            title = chat.name,
            memberIds = memberIds,
            memberNames = memberNames,
            createdAt = chat.createdAt,
            lastMessageText = null,
            lastMessageAt = null,
        )
    }

    private fun decodeState(rawJson: String?): ChatState {
        if (rawJson.isNullOrBlank()) {
            return ChatState()
        }
        return runCatching {
            stateAdapter.fromJson(rawJson) ?: ChatState()
        }.getOrElse {
            Log.w(TAG, "No se pudo parsear el estado de chats: ${it.message}")
            ChatState()
        }
    }

    private fun encodeState(state: ChatState): String {
        return stateAdapter.toJson(state)
    }

    private fun decodePayload(payloadB64: String): String {
        return runCatching {
            String(Base64.decode(payloadB64, Base64.NO_WRAP), Charsets.UTF_8)
        }.getOrElse {
            payloadB64
        }
    }

    companion object {
        private const val TAG = "ChatRepository"

        fun chatKey(type: String, id: Long): String = "$type:$id"
    }
}

