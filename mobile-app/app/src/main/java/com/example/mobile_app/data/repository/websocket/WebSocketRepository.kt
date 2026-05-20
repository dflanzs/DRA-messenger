package com.example.mobile_app.data.repository.websocket

import android.util.Log
import com.example.mobile_app.data.network.websocket.WsStompClient
import com.example.mobile_app.data.model.signal.SignalMessageWSDto
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineScope
import okhttp3.OkHttpClient

/**
 * Repositorio para WebSocket STOMP
 * Centraliza la lógica de conexión y suscripción a colas Signal
 */
class WebSocketRepository(
    private val baseUrl: String,
    private val token: String,
    private val okHttpClient: OkHttpClient,
    private val moshi: Moshi,
    private val scope: CoroutineScope
) {
    private var stompClient: WsStompClient? = null
    private val TAG = "WebSocketRepository"

    /**
     * Conectar al servidor WebSocket
     */
    suspend fun connect(): Boolean {
        return try {
            Log.d(TAG, "Iniciando conexión a WebSocket con baseUrl: $baseUrl")
            stompClient = WsStompClient(baseUrl, token, okHttpClient, scope)

            // Primer chequeo HTTP rápido (actuator/health) para detectar reachability
            try {
                val httpOk = com.example.mobile_app.data.network.ConnectivityDiagnostics.testHttpConnectivity(baseUrl, okHttpClient)
                if (!httpOk) {
                    Log.e(TAG, "Health check HTTP falló para $baseUrl; abortando conexión WebSocket")
                    return false
                }
            } catch (e: Exception) {
                Log.w(TAG, "Health check lanzó excepción: ${e.message}")
            }

            // Test de transporte WebSocket (handshake TCP/WebSocket)
            val transportOk = try {
                stompClient?.testTransport(5000) ?: false
            } catch (e: Exception) {
                Log.e(TAG, "testTransport lanzó excepción: ${e.message}", e)
                false
            }

            if (!transportOk) {
                Log.e(TAG, "El transporte WebSocket no está disponible para $baseUrl (testTransport falló)")
                return false
            }

            val connected = stompClient?.connect() ?: false
            if (connected) {
                Log.d(TAG, "WebSocket conectado exitosamente")
            } else {
                Log.e(TAG, "WebSocket retornó false en connect()")
            }
            connected
        } catch (e: Exception) {
            Log.e(TAG, "Error conectando a WebSocket: ${e.message}", e)
            false
        }
    }

    /**
     * Suscribirse a mensajes Signal privados
     */
    suspend fun subscribeToSignalMessages(
        onMessage: (SignalMessageWSDto) -> Unit
    ): Boolean {
        return try {
            val result = stompClient?.subscribe(
                destination = "/user/queue/signal-messages",
                id = "signal-messages"
            ) { message ->
                try {
                    val adapter = moshi.adapter(SignalMessageWSDto::class.java)
                    val dto = adapter.fromJson(message)
                    if (dto != null) {
                        Log.d(TAG, "Mensaje Signal recibido de: ${dto.senderUserId}")
                        onMessage(dto)
                    } else {
                        Log.w(TAG, "DTO Signal parseado es null")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parseando mensaje Signal: ${e.message}", e)
                }
            } ?: false

            if (!result) {
                Log.w(TAG, "subscribe() retornó false para signal-messages")
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error suscribiendo a signal-messages: ${e.message}", e)
            false
        }
    }

    /**
     * Enviar mensaje privado
     */
    suspend fun sendPrivateMessage(messageJson: String): Boolean {
        return try {
            stompClient?.send(
                destination = "/app/private-message",
                body = messageJson
            ) ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando private-message", e)
            false
        }
    }

    /**
     * Enviar mensaje de grupo
     */
    suspend fun sendGroupMessage(messageJson: String): Boolean {
        return try {
            stompClient?.send(
                destination = "/app/group-message",
                body = messageJson
            ) ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando group-message", e)
            false
        }
    }

    /**
     * Desconectar
     */
    fun disconnect() {
        stompClient?.disconnect()
        stompClient = null
    }

    /**
     * Verificar conexión
     */
    fun isConnected(): Boolean = stompClient?.isConnected() ?: false
}




