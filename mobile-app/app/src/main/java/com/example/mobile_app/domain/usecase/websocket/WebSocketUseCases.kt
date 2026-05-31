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
     * Enviar la sender key de grupo (SKDM) a un miembro.
     */
    suspend fun sendGroupSenderKey(messageJson: String): Boolean {
        return webSocketRepository.sendGroupSenderKey(messageJson)
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

