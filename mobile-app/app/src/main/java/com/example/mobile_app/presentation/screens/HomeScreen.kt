package com.example.mobile_app.presentation.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mobile_app.BuildConfig
import com.example.mobile_app.presentation.auth.toAuthUserMessage
import com.example.mobile_app.presentation.signal.SignalCoordinator
import com.example.mobile_app.presentation.websocket.WebSocketCoordinator
import com.example.mobile_app.security.TokenManager
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    signalCoordinator: SignalCoordinator? = null,
    tokenManager: TokenManager? = null,
    onLogout: () -> Unit,
) {
    var isBootstrappingKeys by remember { mutableStateOf(false) }
    var bootstrapError by remember { mutableStateOf<String?>(null) }
    var bootstrapSuccess by remember { mutableStateOf(false) }
    var isConnectingWebSocket by remember { mutableStateOf(false) }
    var webSocketError by remember { mutableStateOf<String?>(null) }
    var webSocketConnected by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val TAG = "HomeScreen"

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
        if (bootstrapSuccess && tokenManager != null && !webSocketConnected && webSocketError == null) {
            isConnectingWebSocket = true
            scope.launch {
                runCatching {
                    Log.d(TAG, "Iniciando conexión a WebSocket...")
                    Log.d(TAG, "Base URL: ${BuildConfig.BACKEND_BASE_URL}")

                    val webSocketUseCases = WebSocketCoordinator.getWebSocketUseCases(
                        tokenManager = tokenManager,
                        baseUrl = BuildConfig.BACKEND_BASE_URL.trimEnd('/')
                    )

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
                        // Aquí se pueden procesar los mensajes recibidos
                    }

                    if (!subscribed) {
                        Log.w(TAG, "Suscripción retornó false, pero continuando...")
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Bienvenido",
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
            Text(
                text = "✓ Claves Signal configuradas",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "✓ WebSocket conectado",
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            Text(
                text = "La base de la app ya está lista para seguir con autenticación, cifrado y chat.",
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
    }
}
