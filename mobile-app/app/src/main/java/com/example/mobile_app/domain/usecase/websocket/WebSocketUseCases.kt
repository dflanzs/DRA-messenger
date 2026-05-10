package com.example.mobile_app.domain.usecase.websocket

import com.example.mobile_app.data.model.signal.SignalMessageWSDto
import com.example.mobile_app.data.repository.websocket.WebSocketRepository

/**
 * Casos de uso para WebSocket STOMP
 */
class WebSocketUseCases(
    private val webSocketRepository: WebSocketRepository
) {
    /**
     * Conectar al servidor WebSocket
     */
    suspend fun connectWebSocket(): Boolean {
        return webSocketRepository.connect()
    }

    /**
     * Suscribirse a mensajes Signal privados
     */
    suspend fun subscribeToSignalMessages(
        onMessage: (SignalMessageWSDto) -> Unit
    ): Boolean {
        return webSocketRepository.subscribeToSignalMessages(onMessage)
    }

    /**
     * Suscribirse a mensajes generales
     */
    suspend fun subscribeToMessages(
        onMessage: (String) -> Unit
    ): Boolean {
        return webSocketRepository.subscribeToMessages(onMessage)
    }

    /**
     * Enviar mensaje privado
     */
    suspend fun sendPrivateMessage(messageJson: String): Boolean {
        return webSocketRepository.sendPrivateMessage(messageJson)
    }

    /**
     * Enviar mensaje de grupo
     */
    suspend fun sendGroupMessage(messageJson: String): Boolean {
        return webSocketRepository.sendGroupMessage(messageJson)
    }

    /**
     * Desconectar
     */
    fun disconnectWebSocket() {
        webSocketRepository.disconnect()
    }

    /**
     * Verificar si está conectado
     */
    fun isWebSocketConnected(): Boolean {
        return webSocketRepository.isConnected()
    }
}

