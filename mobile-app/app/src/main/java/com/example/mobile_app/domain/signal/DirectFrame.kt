package com.example.mobile_app.domain.signal

import java.nio.ByteBuffer

/**
 * Frame de plaintext del canal 1:1. Primer byte = tipo.
 *  0x00 TEXT  -> [0x00][body...]
 *  0x01 SKDM  -> [0x01][groupId: 8 bytes BE][skdmBytes...]
 *
 * Permite multiplexar mensajes de chat y SenderKeyDistributionMessage de grupo sobre la misma sesión 1:1.
 */
sealed interface DirectFrame {
    fun encode(): ByteArray

    data class Text(val body: ByteArray) : DirectFrame {
        override fun encode(): ByteArray = ByteArray(1 + body.size).also {
            it[0] = TYPE_TEXT; body.copyInto(it, 1)
        }
    }

    data class Skdm(val groupId: Long, val skdmBytes: ByteArray) : DirectFrame {
        override fun encode(): ByteArray {
            val buf = ByteBuffer.allocate(1 + 8 + skdmBytes.size)
            buf.put(TYPE_SKDM); buf.putLong(groupId); buf.put(skdmBytes)
            return buf.array()
        }
    }

    companion object {
        private const val TYPE_TEXT: Byte = 0x00
        private const val TYPE_SKDM: Byte = 0x01

        fun text(body: ByteArray): DirectFrame = Text(body)
        fun skdm(groupId: Long, skdmBytes: ByteArray): DirectFrame = Skdm(groupId, skdmBytes)

        fun decode(bytes: ByteArray): DirectFrame {
            require(bytes.isNotEmpty()) { "frame vacío" }
            return when (bytes[0]) {
                TYPE_TEXT -> Text(bytes.copyOfRange(1, bytes.size))
                TYPE_SKDM -> {
                    val buf = ByteBuffer.wrap(bytes, 1, bytes.size - 1)
                    val groupId = buf.long
                    val skdm = ByteArray(buf.remaining()).also { buf.get(it) }
                    Skdm(groupId, skdm)
                }
                else -> error("tipo de frame desconocido: ${bytes[0]}")
            }
        }
    }
}
