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
import com.example.mobile_app.data.model.chat.CommunicationRequestDto
import com.example.mobile_app.data.model.chat.CreateDirectChatRequestDto
import com.example.mobile_app.data.model.chat.CreateGroupChatRequestDto
import com.example.mobile_app.data.model.chat.DirectChatSummaryDto
import com.example.mobile_app.data.model.chat.GroupChatSummaryDto
import com.example.mobile_app.data.model.chat.LocalChatMessageRecord
import com.example.mobile_app.data.model.chat.LocalChatRecord
import com.example.mobile_app.data.network.ChatApiService
import com.example.mobile_app.domain.signal.DirectFrame
import com.example.mobile_app.domain.signal.SignalCipherService
import com.example.mobile_app.domain.signal.payloadFromWire
import com.example.mobile_app.security.CurrentUserInfo
import com.example.mobile_app.security.CurrentUserManager
import org.signal.libsignal.protocol.DuplicateMessageException
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
    private val signalCipher: SignalCipherService,
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
        val incomingRequests = runCatching { chatApiService.listIncomingRequests() }
            .getOrElse { Log.w(TAG, "No se pudieron cargar las solicitudes recibidas: ${it.message}"); emptyList() }
        val outgoingRequests = runCatching { chatApiService.listOutgoingRequests() }
            .getOrElse { Log.w(TAG, "No se pudieron cargar las solicitudes enviadas: ${it.message}"); emptyList() }

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
                incomingRequests.forEach { request ->
                    if (requestKey(request.id) !in hidden) {
                        add(mapIncomingRequest(request))
                    }
                }
                outgoingRequests.forEach { request ->
                    if (requestKey(request.id) !in hidden) {
                        add(mapOutgoingRequest(request))
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
        val result = chatApiService.createDirectChat(CreateDirectChatRequestDto(targetUserId))
        val currentUser = currentUserManager.getCurrentUser() ?: return null
        val record = when (result.status) {
            "ACTIVE" -> {
                val chatDto = result.chat ?: return null
                mapDirectChat(chatDto, getUsersSnapshot(), currentUser)
            }
            "PENDING" -> {
                val request = result.request ?: return null
                mapOutgoingRequest(request)
            }
            else -> return null
        }
        upsertChat(record)
        return record
    }

    /**
     * Acepta una solicitud de comunicación recibida: sustituye la fila pendiente
     * por el chat activo recién creado.
     */
    suspend fun acceptRequest(requestId: Long): LocalChatRecord? {
        val chatDto = chatApiService.acceptCommunicationRequest(requestId)
        val currentUser = currentUserManager.getCurrentUser() ?: return null
        val chat = mapDirectChat(chatDto, getUsersSnapshot(), currentUser)
        context.chatDataStore.edit { prefs ->
            val state = decodeState(prefs[CHAT_STATE_JSON_KEY])
            val merged = state.chats
                .filterNot { it.chatKey == requestKey(requestId) || it.chatKey == chat.chatKey }
                .plus(chat)
            prefs[CHAT_STATE_JSON_KEY] = encodeState(
                state.copy(chats = merged.sortedByDescending { it.lastMessageAt ?: it.createdAt })
            )
        }
        return chat
    }

    /**
     * Rechaza una solicitud de comunicación recibida: elimina la fila pendiente.
     */
    suspend fun rejectRequest(requestId: Long) {
        chatApiService.rejectCommunicationRequest(requestId)
        context.chatDataStore.edit { prefs ->
            val state = decodeState(prefs[CHAT_STATE_JSON_KEY])
            prefs[CHAT_STATE_JSON_KEY] = encodeState(
                state.copy(chats = state.chats.filterNot { it.chatKey == requestKey(requestId) })
            )
        }
    }

    suspend fun createGroupChat(name: String, userIds: Set<Long>): LocalChatRecord? {
        val created = chatApiService.createGroupChat(CreateGroupChatRequestDto(name, userIds))
        val currentUser = currentUserManager.getCurrentUser() ?: return null
        val users = getUsersSnapshot()
        val chat = mapGroupChat(created, users, currentUser)
        upsertChat(chat)
        return chat
    }

    /**
     * Borra un chat en el servidor (privado o de grupo, según su tipo) y luego lo
     * elimina del estado local. Sin id de servidor degrada a borrado local.
     */
    suspend fun deleteChat(chatKey: String) {
        val chat = getChat(chatKey)
        val chatId = chat?.chatId
        if (chatId != null) {
            runCatching {
                when (chat.type) {
                    "DIRECT" -> chatApiService.deletePrivateChat(chatId)
                    "GROUP" -> chatApiService.deleteGroupChat(chatId)
                }
            }.onFailure { Log.w(TAG, "Borrado de chat $chatKey en servidor falló: ${it.message}") }
        }
        deleteChatLocally(chatKey)
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
        cypherTextType: Short,
        cypherTextB64: String,
        createdAt: String,
    ) {
        val currentUser = currentUserManager.getCurrentUser() ?: return
        val users = getUsersSnapshot()
        val senderName = users.firstOrNull { it.id == senderUserId }?.name ?: senderUserId.toString()
        // Descifrado E2E real con libsignal: DIRECT (sesión 1:1) y GROUP (sender keys).
        val text: String = when (conversationType) {
            "DIRECT" -> {
                val plain = try {
                    signalCipher.decryptDirect(senderUserId, payloadFromWire(cypherTextType, cypherTextB64))
                } catch (e: DuplicateMessageException) {
                    Log.d(TAG, "Mensaje duplicado de $senderUserId ya procesado; se omite")
                    return
                }
                when (val frame = DirectFrame.decode(plain)) {
                    is DirectFrame.Text -> String(frame.body)
                    is DirectFrame.Skdm -> {
                        // No es un mensaje visible: registra la sender key del emisor para su grupo.
                        signalCipher.processSenderKeyDistribution(senderUserId, frame.groupId, frame.skdmBytes)
                        return
                    }
                }
            }
            "GROUP" -> {
                val plain = signalCipher.decryptGroup(
                    conversationId, senderUserId, payloadFromWire(cypherTextType, cypherTextB64),
                )
                // Sin sender key del emisor todavía: no se hace ACK -> el backend reintenta la entrega.
                if (plain.isEmpty()) error("Sender key de $senderUserId no disponible aún; se reintentará")
                String(plain)
            }
            else -> decodePayload(cypherTextB64)
        }
        val computedKey = chatKey(conversationType, conversationId)
        Log.d(TAG, "saveIncomingWS: type=$conversationType id=$conversationId -> key=$computedKey ; " +
            "chats existentes=${getState().chats.map { it.chatKey }}")
        saveMessage(
            chatKey = computedKey,
            senderUserId = senderUserId,
            senderName = senderName,
            text = text,
            createdAt = utcToLocal(createdAt),
            isOutgoing = senderUserId == currentUser.id,
        )
    }

    suspend fun fetchPendingMessages(): List<com.example.mobile_app.data.model.signal.SignalMessageWSDto> =
        chatApiService.getPendingMessages()

    suspend fun ackMessageDelivered(envelopeId: Long) {
        runCatching { chatApiService.ackMessageDelivered(envelopeId) }
            .onFailure { Log.w(TAG, "ackMessageDelivered($envelopeId) falló: ${it.message}") }
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

    // Orden de llegada: la lista se conserva en orden de inserción (orden en que
    // saveMessage fue invocado). No se reordena por createdAt porque los mensajes
    // enviados llevan hora local del dispositivo y los recibidos hora UTC del backend.
    suspend fun getMessages(chatKey: String): List<LocalChatMessageRecord> =
        getState().messages.filter { it.chatKey == chatKey }

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
            // Append puro: preserva el orden de llegada (ver getMessages).
            val updatedMessages = state.messages + message
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

    private fun mapIncomingRequest(request: CommunicationRequestDto): LocalChatRecord =
        LocalChatRecord(
            chatKey = requestKey(request.id),
            chatId = null,
            type = "DIRECT",
            title = request.requesterName,
            memberIds = listOf(request.requesterId, request.targetId),
            memberNames = listOf(request.requesterName, request.targetName),
            createdAt = request.createdAt,
            status = "PENDING_INCOMING",
            requestId = request.id,
        )

    private fun mapOutgoingRequest(request: CommunicationRequestDto): LocalChatRecord =
        LocalChatRecord(
            chatKey = requestKey(request.id),
            chatId = null,
            type = "DIRECT",
            title = request.targetName,
            memberIds = listOf(request.requesterId, request.targetId),
            memberNames = listOf(request.requesterName, request.targetName),
            createdAt = request.createdAt,
            status = "PENDING_OUTGOING",
            requestId = request.id,
        )

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

    /**
     * Convierte un timestamp ISO sin zona emitido por el backend (en UTC) a la
     * hora local del dispositivo. Los mensajes salientes ya se guardan en local,
     * así solo se normalizan los entrantes y todas las burbujas son comparables.
     */
    private fun utcToLocal(isoUtc: String): String = runCatching {
        java.time.LocalDateTime.parse(isoUtc)
            .atZone(java.time.ZoneOffset.UTC)
            .withZoneSameInstant(java.time.ZoneId.systemDefault())
            .toLocalDateTime()
            .toString()
    }.getOrElse { isoUtc }

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

        fun requestKey(id: Long): String = "REQUEST:$id"
    }
}

