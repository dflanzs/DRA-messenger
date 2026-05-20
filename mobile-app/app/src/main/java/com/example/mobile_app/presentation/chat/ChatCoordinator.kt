package com.example.mobile_app.presentation.chat

import android.util.Log
import android.util.Base64
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.mobile_app.data.network.ChatApiService
import com.example.mobile_app.data.network.RetrofitProvider
import com.example.mobile_app.data.repository.chat.ChatRepository
import com.example.mobile_app.network.NetworkConfig
import com.example.mobile_app.security.CurrentUserManager
import com.example.mobile_app.security.TokenManager
import com.example.mobile_app.domain.usecase.websocket.WebSocketUseCases
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ChatCoordinator(
    val repository: ChatRepository,
    private val currentUserManager: CurrentUserManager,
) {
    val state: Flow<com.example.mobile_app.data.model.chat.ChatState> = repository.observeState()
    var webSocketUseCases: WebSocketUseCases? = null
        private set

    // Scope de vida de aplicación: ChatCoordinator se recuerda sobre applicationContext.
    // Los mensajes WebSocket entrantes deben persistirse aunque HomeScreen ya no esté
    // en composición (p. ej. estando dentro de ChatDetailScreen).
    private val coordinatorScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun refreshChats() {
        repository.refreshFromServer()
    }

    suspend fun createDirectChat(targetUserId: Long) = repository.createDirectChat(targetUserId)

    suspend fun createGroupChat(name: String, userIds: Set<Long>) = repository.createGroupChat(name, userIds)

    suspend fun deleteChatLocally(chatKey: String) = repository.deleteChatLocally(chatKey)

    suspend fun acceptCommunicationRequest(requestId: Long) = repository.acceptRequest(requestId)

    suspend fun rejectCommunicationRequest(requestId: Long) = repository.rejectRequest(requestId)

    suspend fun saveIncomingWebSocketMessage(
        conversationType: String,
        conversationId: Long,
        senderUserId: Long,
        cypherTextB64: String,
        createdAt: String,
    ) = repository.saveIncomingWebSocketMessage(conversationType, conversationId, senderUserId, cypherTextB64, createdAt)

    /**
     * Maneja un mensaje WebSocket entrante desde el callback de suscripción STOMP.
     * Usa el scope propio del coordinador (vida de app), no el de la pantalla, para
     * que el mensaje se guarde aunque HomeScreen haya salido de composición.
     */
    fun onIncomingWebSocketMessage(
        envelopeId: Long,
        conversationType: String,
        conversationId: Long,
        senderUserId: Long,
        cypherTextB64: String,
        createdAt: String,
    ) {
        coordinatorScope.launch {
            val saved = runCatching {
                repository.saveIncomingWebSocketMessage(
                    conversationType, conversationId, senderUserId, cypherTextB64, createdAt,
                )
            }.onFailure { Log.w("ChatCoordinator", "No se pudo guardar mensaje entrante: ${it.message}") }
                .isSuccess
            if (saved) {
                repository.ackMessageDelivered(envelopeId)
            }
        }
    }

    suspend fun consumePendingMessages() {
        val pending = runCatching { repository.fetchPendingMessages() }
            .onFailure { Log.w("ChatCoordinator", "fetch pending falló: ${it.message}") }
            .getOrNull().orEmpty()
        Log.d("ChatCoordinator", "pendientes recibidos: ${pending.size}")
        for (msg in pending) {
            val saved = runCatching {
                repository.saveIncomingWebSocketMessage(
                    conversationType = msg.conversationType,
                    conversationId = msg.conversationId,
                    senderUserId = msg.senderUserId,
                    cypherTextB64 = msg.cypherTextB64,
                    createdAt = msg.createdAt?.toString() ?: java.time.LocalDateTime.now().toString(),
                )
            }.onFailure { Log.w("ChatCoordinator", "guardar pendiente ${msg.envelopeId} falló: ${it.message}") }
                .isSuccess
            if (saved) repository.ackMessageDelivered(msg.envelopeId)
        }
    }

    suspend fun saveOutgoingMessage(
        chatKey: String,
        senderUserId: Long,
        senderName: String,
        text: String,
        createdAt: String,
    ) = repository.saveOutgoingMessage(chatKey, senderUserId, senderName, text, createdAt)

    suspend fun getChat(chatKey: String) = repository.getChat(chatKey)

    suspend fun getMessages(chatKey: String) = repository.getMessages(chatKey)

    suspend fun getMembers(chatKey: String) = repository.getMembers(chatKey)

    suspend fun sendMessage(chatKey: String, text: String): Boolean {
        val chat = repository.getChat(chatKey) ?: return false
        val currentUser = currentUserManager.getCurrentUser() ?: return false
        val useCases = webSocketUseCases ?: return false
        val payload = Base64.encodeToString(text.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

        return when (chat.type) {
            "DIRECT" -> {
                val recipientUserId = chat.memberIds.firstOrNull { it != currentUser.id } ?: return false
                val messageJson = """
                    {
                      "recipientUserId": $recipientUserId,
                      "conversationId": ${chat.chatId},
                      "cypherTextType": 1,
                      "cypherTextB64": "$payload"
                    }
                """.trimIndent()
                val sent = useCases.sendPrivateMessage(messageJson)
                if (sent) {
                    repository.saveOutgoingMessage(chatKey, currentUser.id, currentUser.name, text, java.time.LocalDateTime.now().toString())
                }
                sent
            }
            "GROUP" -> {
                val messageJson = """
                    {
                      "recipientUserId": ${currentUser.id},
                      "groupChatId": ${chat.chatId},
                      "cypherTextType": 1,
                      "cypherTextB64": "$payload"
                    }
                """.trimIndent()
                val sent = useCases.sendGroupMessage(messageJson)
                if (sent) {
                    repository.saveOutgoingMessage(chatKey, currentUser.id, currentUser.name, text, java.time.LocalDateTime.now().toString())
                }
                sent
            }
            else -> false
        }
    }

    fun setWebSocketUseCases(useCases: WebSocketUseCases) {
        webSocketUseCases = useCases
    }
}

@Composable
fun rememberChatCoordinator(
    tokenManager: TokenManager,
    currentUserManager: CurrentUserManager,
): ChatCoordinator {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        val okHttpClient = RetrofitProvider.getOkHttpClient(tokenManager)
        val moshi = RetrofitProvider.getMoshi()
        val apiService = Retrofit.Builder()
            .baseUrl(NetworkConfig.resolveBaseUrl(context))
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi.newBuilder().add(KotlinJsonAdapterFactory()).build()))
            .build()
            .create(ChatApiService::class.java)
        val repository = ChatRepository(
            context = context,
            chatApiService = apiService,
            currentUserManager = currentUserManager,
            moshi = moshi,
        )
        Log.d("ChatCoordinator", "ChatRepository inicializado")
        ChatCoordinator(repository, currentUserManager)
    }
}