package com.example.mobile_app.presentation.screens

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mobile_app.BuildConfig
import com.example.mobile_app.data.model.chat.ChatState
import com.example.mobile_app.data.model.chat.LocalChatRecord
import com.example.mobile_app.presentation.auth.AuthCoordinator
import com.example.mobile_app.presentation.auth.toAuthUserMessage
import com.example.mobile_app.presentation.chat.ChatCoordinator
import com.example.mobile_app.presentation.signal.SignalCoordinator
import com.example.mobile_app.presentation.websocket.WebSocketCoordinator
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    signalCoordinator: SignalCoordinator? = null,
    authCoordinator: AuthCoordinator,
    chatCoordinator: ChatCoordinator,
    navController: NavController,
    onLogout: () -> Unit,
) {
    var isBootstrappingKeys by remember { mutableStateOf(false) }
    var bootstrapError by remember { mutableStateOf<String?>(null) }
    var bootstrapSuccess by remember { mutableStateOf(false) }
    var isConnectingWebSocket by remember { mutableStateOf(false) }
    var webSocketError by remember { mutableStateOf<String?>(null) }
    var webSocketConnected by remember { mutableStateOf(false) }
    var showCreateChatDialog by remember { mutableStateOf(false) }
    var chatsLoaded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val TAG = "HomeScreen"
    val chatState by chatCoordinator.state.collectAsState(initial = ChatState())
    val currentUser = authCoordinator.currentUserManager.getCurrentUser()

    // Paso 1: Bootstrap de claves Signal
    LaunchedEffect(signalCoordinator) {
        if (signalCoordinator != null && !bootstrapSuccess) {
            isBootstrappingKeys = true
            bootstrapError = null
            scope.launch {
                runCatching {
                    // Check if bootstrap was already completed
                    if (signalCoordinator.signalStore.isBootstrapCompleted()) {
                        bootstrapSuccess = true
                    } else {
                        signalCoordinator.bootstrapSignalKeysUseCase()
                        bootstrapSuccess = true
                    }
                }.onSuccess {
                    isBootstrappingKeys = false
                    Log.d(TAG, "Bootstrap de Signal completado")
                }.onFailure { throwable ->
                    bootstrapError = throwable.toAuthUserMessage("No se pudo hacer bootstrap de claves Signal.")
                    isBootstrappingKeys = false
                    Log.e(TAG, "Error en bootstrap", throwable)
                }
            }
        }
    }

    // Paso 2: Conectar a WebSocket después del bootstrap exitoso
    LaunchedEffect(bootstrapSuccess) {
        if (bootstrapSuccess && !webSocketConnected && webSocketError == null) {
            isConnectingWebSocket = true
            scope.launch {
                runCatching {
                    Log.d(TAG, "Iniciando conexión a WebSocket...")
                    Log.d(TAG, "Base URL: ${BuildConfig.BACKEND_BASE_URL}")

                    val webSocketUseCases = WebSocketCoordinator.getWebSocketUseCases(
                        tokenManager = authCoordinator.tokenManager,
                        baseUrl = BuildConfig.BACKEND_BASE_URL.trimEnd('/')
                    )
                    chatCoordinator.setWebSocketUseCases(webSocketUseCases)

                    // Conectar
                    Log.d(TAG, "Llamando connectWebSocket()...")
                    val connected = webSocketUseCases.connectWebSocket()
                    if (!connected) {
                        throw Exception("connectWebSocket() retornó false")
                    }
                    Log.d(TAG, "Conectado a WebSocket")

                    // Suscribirse a mensajes Signal
                    Log.d(TAG, "Suscribiendo a mensajes Signal...")
                    val subscribed = webSocketUseCases.subscribeToSignalMessages { message ->
                        Log.d(TAG, "Mensaje Signal recibido: ${message.envelopeId}")
                        // Delega en ChatCoordinator (scope de vida de app): el mensaje se
                        // guarda aunque HomeScreen ya no esté en composición.
                        chatCoordinator.onIncomingWebSocketMessage(
                            conversationType = message.conversationType,
                            conversationId = message.conversationId,
                            senderUserId = message.senderUserId,
                            cypherTextB64 = message.cypherTextB64,
                            createdAt = message.createdAt?.toString() ?: java.time.LocalDateTime.now().toString(),
                        )
                    }

                    if (!subscribed) {
                        Log.w(TAG, "Suscripción retornó false, pero continuando...")
                    }

                    if (!chatsLoaded) {
                        runCatching {
                            chatCoordinator.refreshChats()
                        }.onFailure { throwable ->
                            Log.w(TAG, "No se pudieron sincronizar los chats locales: ${throwable.message}")
                        }
                        chatsLoaded = true
                    }

                    webSocketConnected = true
                }.onSuccess {
                    isConnectingWebSocket = false
                    Log.d(TAG, "WebSocket conectado y suscrito exitosamente")
                }.onFailure { throwable ->
                    webSocketError = throwable.toAuthUserMessage("Error en websocket: no se pudo conectar a websocket")
                    isConnectingWebSocket = false
                    Log.e(TAG, "Error en WebSocket", throwable)
                }
            }
        }
    }

    // Sondeo periódico: refresca chats, grupos y solicitudes de comunicación mientras
    // HomeScreen está visible. Los eventos WebSocket solo entregan mensajes, no avisan
    // de chats/grupos/solicitudes nuevos creados por otros usuarios.
    LaunchedEffect(webSocketConnected) {
        if (webSocketConnected) {
            while (true) {
                delay(5000)
                runCatching { chatCoordinator.refreshChats() }
                    .onFailure { Log.w(TAG, "Sondeo: no se pudo refrescar: ${it.message}") }
            }
        }
    }

    // Refrescar la lista de usuarios cada vez que se abre el diálogo de creación de chat
    LaunchedEffect(showCreateChatDialog) {
        if (showCreateChatDialog) {
            runCatching { chatCoordinator.refreshChats() }
                .onFailure { Log.w(TAG, "No se pudo refrescar la lista de usuarios: ${it.message}") }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Mensajes",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (isBootstrappingKeys) {
            Text(
                text = "Inicializando claves Signal...",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        } else if (!bootstrapError.isNullOrBlank()) {
            Text(
                text = "Error en bootstrap: $bootstrapError",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        } else if (isConnectingWebSocket) {
            Text(
                text = "Conectando a WebSocket...",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        } else if (!webSocketError.isNullOrBlank()) {
            Text(
                text = "Error en WebSocket: $webSocketError",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        } else if (webSocketConnected) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = currentUser?.name ?: "Usuario",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = currentUser?.email ?: "",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Button(onClick = { showCreateChatDialog = true }) {
                    Text("Crear chat")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (chatState.chats.isEmpty()) {
                Text(
                    text = "No tienes chats activos todavía.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(chatState.chats, key = { it.chatKey }) { chat ->
                        ChatListItem(
                            chat = chat,
                            onClick = {
                                navController.navigate("chat/${Uri.encode(chat.chatKey)}")
                            },
                            onAccept = {
                                chat.requestId?.let { requestId ->
                                    scope.launch {
                                        runCatching { chatCoordinator.acceptCommunicationRequest(requestId) }
                                            .onFailure { Log.w(TAG, "No se pudo aceptar la solicitud: ${it.message}") }
                                    }
                                }
                            },
                            onReject = {
                                chat.requestId?.let { requestId ->
                                    scope.launch {
                                        runCatching { chatCoordinator.rejectCommunicationRequest(requestId) }
                                            .onFailure { Log.w(TAG, "No se pudo rechazar la solicitud: ${it.message}") }
                                    }
                                }
                            },
                        )
                    }
                }
            }
        } else {
            Text(
                text = "Preparando la pantalla de mensajes...",
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onLogout,
            enabled = !isBootstrappingKeys && !isConnectingWebSocket,
        ) {
            Text("Cerrar sesión")
        }

        if (showCreateChatDialog) {
            CreateChatDialog(
                users = chatState.users,
                currentUserId = currentUser?.id,
                onDismiss = { showCreateChatDialog = false },
                onCreateDirect = { targetUserId ->
                    scope.launch {
                        val chat = chatCoordinator.createDirectChat(targetUserId)
                        if (chat != null) {
                            showCreateChatDialog = false
                        }
                    }
                },
                onCreateGroup = { name, userIds ->
                    scope.launch {
                        val chat = chatCoordinator.createGroupChat(name, userIds)
                        if (chat != null) {
                            showCreateChatDialog = false
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun ChatListItem(
    chat: LocalChatRecord,
    onClick: () -> Unit,
    onAccept: () -> Unit,
    onReject: () -> Unit,
) {
    when (chat.status) {
        "PENDING_INCOMING" -> {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = chat.title,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Solicita comunicación. No perteneces a tus círculos de confianza",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = onAccept) {
                            Text("Aceptar")
                        }
                        TextButton(onClick = onReject) {
                            Text("Rechazar")
                        }
                    }
                }
            }
        }
        "PENDING_OUTGOING" -> {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = chat.title,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Pendiente de consentimiento",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        else -> {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = chat.title,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}
