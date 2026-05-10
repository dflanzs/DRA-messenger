package com.example.mobile_app.data.network.websocket

import android.util.Log
import com.example.mobile_app.data.model.websocket.StompFrame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Cliente STOMP basado en OkHttp WebSocket
 */
class WsStompClient(
    private val baseUrl: String,
    private val token: String,
    private val okHttpClient: OkHttpClient,
    private val scope: CoroutineScope
) {
    private var webSocket: WebSocket? = null
    private val isConnected = AtomicBoolean(false)
    private val subscriptions = ConcurrentHashMap<String, (String) -> Unit>()
    private val receiptCallbacks = ConcurrentHashMap<String, () -> Unit>()
    private var messageId = 0
    private var receiptId = 0
    private var connectionContinuation: kotlinx.coroutines.CancellableContinuation<Boolean>? = null
    private var onConnectedCallback: (() -> Unit)? = null

    private val TAG = "WsStompClient"

    /**
     * Conectar al servidor STOMP
     */
    suspend fun connect(): Boolean = suspendCancellableCoroutine { continuation ->
        try {
            connectionContinuation = continuation
            onConnectedCallback = {
                Log.d(TAG, "STOMP conectado")
                isConnected.set(true)
                if (continuation.isActive) {
                    continuation.resume(true)
                }
                connectionContinuation = null
            }

            // Use SockJS websocket endpoint to ensure raw WebSocket upgrade is accepted
            val wsUrl = baseUrl.replace("http://", "ws://").replace("https://", "wss://")
                .trimEnd('/') + "/ws-chat/websocket"

            Log.d(TAG, "Conectando a: $wsUrl")

            val request = Request.Builder()
                .url(wsUrl)
                // Some servers validate Origin or require it for handshake; set to http origin
                .addHeader("Origin", baseUrl)
                .addHeader("Authorization", "Bearer $token")
                .build()

            webSocket = okHttpClient.newWebSocket(request, StompWebSocketListener(
                onOpen = {
                    Log.d(TAG, "WebSocket abierto, enviando CONNECT")
                    scope.launch {
                        sendConnectFrame()
                    }
                },
                onMessage = { message ->
                    scope.launch {
                        handleFrame(message)
                    }
                },
                onError = { exception ->
                    Log.e(TAG, "Error WebSocket", exception)
                    if (continuation.isActive) {
                        continuation.resumeWithException(exception)
                    }
                    connectionContinuation = null
                }
            ))

            // Agregar timeout de 15 segundos
            scope.launch {
                delay(15000)
                if (continuation.isActive && connectionContinuation != null) {
                    Log.e(TAG, "Timeout esperando STOMP CONNECTED")
                    continuation.resumeWithException(Exception("WebSocket connection timeout"))
                    connectionContinuation = null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando conexión", e)
            continuation.resumeWithException(e)
            connectionContinuation = null
        }
    }

    /**
     * Test rápido para verificar que el transporte WebSocket está disponible
     * Abre una conexión WebSocket y espera a onOpen u onFailure.
     */
    suspend fun testTransport(timeoutMs: Long = 5000): Boolean = suspendCancellableCoroutine { continuation ->
        try {
            // Use SockJS websocket endpoint to ensure raw WebSocket upgrade is accepted
            val wsUrl = baseUrl.replace("http://", "ws://").replace("https://", "wss://")
                .trimEnd('/') + "/ws-chat/websocket"

            Log.d(TAG, "Test transport WS a: $wsUrl")

            val request = Request.Builder()
                .url(wsUrl)
                .addHeader("Origin", baseUrl)
                .addHeader("Authorization", "Bearer $token")
                .build()

            val listener = object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                    Log.d(TAG, "testTransport: onOpen")
                    if (continuation.isActive) continuation.resume(true)
                    try { webSocket.close(1000, "test complete") } catch (_: Exception) {}
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                    Log.e(TAG, "testTransport: onFailure", t)
                    if (continuation.isActive) continuation.resume(false)
                }
            }

            val ws = okHttpClient.newWebSocket(request, listener)

            // Timeout guard
            scope.launch {
                kotlinx.coroutines.delay(timeoutMs)
                if (continuation.isActive) {
                    Log.e(TAG, "testTransport: timeout después de ${timeoutMs}ms")
                    continuation.resume(false)
                    try { ws.close(1000, "timeout") } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "testTransport: excepción", e)
            if (continuation.isActive) continuation.resume(false)
        }
    }

    /**
     * Enviar trama CONNECT
     */
    private fun sendConnectFrame() {
        val connectFrame = StompFrame(
            command = StompFrame.COMMAND_CONNECT,
            headers = mapOf(
                "accept-version" to "1.2,1.1,1.0",
                "heart-beat" to "0,0",
                "Authorization" to "Bearer $token"
            )
        )
        webSocket?.send(StompFrame.serialize(connectFrame))
    }

    /**
     * Suscribirse a un destino
     */
    suspend fun subscribe(
        destination: String,
        id: String = "sub-${++messageId}",
        onMessage: (String) -> Unit
    ): Boolean = suspendCancellableCoroutine { continuation ->
        try {
            subscriptions[id] = onMessage

            val subscribeFrame = StompFrame(
                command = StompFrame.COMMAND_SUBSCRIBE,
                headers = mapOf(
                    "id" to id,
                    "destination" to destination,
                    "ack" to "auto"
                )
            )

            Log.d(TAG, "Suscribiendo a: $destination con id: $id")
            webSocket?.send(StompFrame.serialize(subscribeFrame))
            continuation.resume(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error suscribiendo", e)
            continuation.resumeWithException(e)
        }
    }

    /**
     * Enviar un mensaje a un destino
     */
    suspend fun send(
        destination: String,
        body: String,
        receiptId: String = "receipt-${++this.receiptId}"
    ): Boolean = suspendCancellableCoroutine { continuation ->
        try {
            val sendFrame = StompFrame(
                command = StompFrame.COMMAND_SEND,
                headers = mapOf(
                    "destination" to destination,
                    "content-type" to "application/json",
                    "receipt" to receiptId
                ),
                body = body
            )

            // Registrar callback para el receipt
            receiptCallbacks[receiptId] = {
                continuation.resume(true)
            }

            Log.d(TAG, "Enviando a: $destination")
            webSocket?.send(StompFrame.serialize(sendFrame))

            // Timeout de 10 segundos
            scope.launch {
                delay(10000)
                if (continuation.isActive && receiptCallbacks.containsKey(receiptId)) {
                    receiptCallbacks.remove(receiptId)
                    continuation.resume(false)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando mensaje", e)
            continuation.resumeWithException(e)
        }
    }

    /**
     * Manejar trama recibida
     */
    private fun handleFrame(frameString: String) {
        val frame = StompFrame.parse(frameString) ?: return

        Log.d(TAG, "Frame recibido: ${frame.command}")

        when (frame.command) {
            StompFrame.COMMAND_CONNECTED -> {
                Log.d(TAG, "Servidor respondió CONNECTED")
                onConnectedCallback?.invoke()
                onConnectedCallback = null
            }
            StompFrame.COMMAND_MESSAGE -> {
                val subscriptionId = frame.headers["subscription"] ?: return
                val callback = subscriptions[subscriptionId] ?: return
                Log.d(TAG, "Mensaje recibido en $subscriptionId: ${frame.body}")
                callback(frame.body)
            }
            StompFrame.COMMAND_RECEIPT -> {
                val receiptId = frame.headers["receipt-id"]
                receiptCallbacks[receiptId]?.invoke()
                receiptCallbacks.remove(receiptId)
                Log.d(TAG, "Receipt recibido: $receiptId")
            }
            StompFrame.COMMAND_ERROR -> {
                val errorMessage = frame.body.takeIf { it.isNotEmpty() } ?: "Unknown error"
                Log.e(TAG, "Error STOMP: $errorMessage")
                // Si estamos conectando, reportar el error
                if (connectionContinuation?.isActive == true) {
                    Log.e(TAG, "Error durante handshake STOMP, resumiendo con excepción")
                    connectionContinuation?.resumeWithException(
                        Exception("STOMP connection error: $errorMessage")
                    )
                    connectionContinuation = null
                }
            }
            else -> {
                Log.d(TAG, "Frame ignorado: ${frame.command}")
            }
        }
    }

    /**
     * Desconectar
     */
    fun disconnect() {
        Log.d(TAG, "Desconectando")
        val disconnectFrame = StompFrame(
            command = StompFrame.COMMAND_DISCONNECT,
            headers = mapOf("receipt" to "disconnect-${++receiptId}")
        )
        webSocket?.send(StompFrame.serialize(disconnectFrame))
        webSocket?.close(1000, "Normal closure")
        isConnected.set(false)
    }

    /**
     * Verificar si está conectado
     */
    fun isConnected(): Boolean = isConnected.get()

    /**
     * WebSocketListener implementación
     */
    private class StompWebSocketListener(
        private val onOpen: () -> Unit,
        private val onMessage: (String) -> Unit,
        private val onError: (Exception) -> Unit
    ) : WebSocketListener() {

        private val TAG = "StompWebSocketListener"

        override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
            Log.d(TAG, "onOpen")
            onOpen()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d(TAG, "onMessage: ${text.take(100)}...")
            onMessage(text)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
            Log.e(TAG, "onFailure", t)
            onError(Exception("WebSocket failure", t))
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "onClosed: $code - $reason")
        }
    }
}













