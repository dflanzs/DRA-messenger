package com.example.mobile_app.data.model.signal

/**
 * DTO para mensajes Signal recibidos via WebSocket.
 *
 * `createdAt` se modela como String: el backend serializa LocalDateTime como
 * cadena ISO-8601 ("2026-05-17T18:14:42.401976378") y Moshi no trae adapter
 * para java.time.LocalDateTime.
 */
data class SignalMessageWSDto(
    val envelopeId: Long,
    val senderUserId: Long,
    val conversationId: Long,
    val conversationType: String,
    val cypherTextType: Short,
    val cypherTextB64: String,
    val createdAt: String? = null
)
