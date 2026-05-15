package com.example.mobile_app.data.model.signal

import java.time.LocalDateTime

/**
 * DTO para mensajes Signal recibidos via WebSocket
 */
data class SignalMessageWSDto(
    val envelopeId: Long,
    val senderUserId: Long,
    val conversationId: Long,
    val conversationType: String,
    val cypherTextType: Short,
    val cypherTextB64: String,
    val createdAt: LocalDateTime? = null
)
