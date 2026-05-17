package com.example.mobile_app.data.model.websocket

/**
 * Representación de una trama STOMP
 * Formato simplificado para CONNECT, SUBSCRIBE, SEND, MESSAGE, etc.
 */
data class StompFrame(
    val command: String,
    val headers: Map<String, String> = emptyMap(),
    val body: String = ""
) {
    companion object {
        const val COMMAND_CONNECT = "CONNECT"
        const val COMMAND_CONNECTED = "CONNECTED"
        const val COMMAND_SEND = "SEND"
        const val COMMAND_SUBSCRIBE = "SUBSCRIBE"
        const val COMMAND_MESSAGE = "MESSAGE"
        const val COMMAND_DISCONNECT = "DISCONNECT"
        const val COMMAND_RECEIPT = "RECEIPT"
        const val COMMAND_ERROR = "ERROR"

        /**
         * Parse una trama STOMP desde un string
         * Maneja tanto "\n" como "\r\n" como terminadores de línea
         */
        fun parse(frameString: String): StompFrame? {
            if (frameString.isBlank()) return null

            // Normalizar los terminadores de línea a "\n"
            val normalized = frameString.replace("\r\n", "\n")
            val lines = normalized.split("\n")

            if (lines.isEmpty()) return null

            val command = lines[0].trim()
            if (command.isEmpty()) return null

            val headers = mutableMapOf<String, String>()
            var bodyStartIndex = 1

            // Parsear headers hasta encontrar una línea vacía
            for (i in 1 until lines.size) {
                val line = lines[i]
                if (line.isEmpty()) {
                    bodyStartIndex = i + 1
                    break
                }
                val colonIndex = line.indexOf(':')
                if (colonIndex > 0) {
                    val key = line.substring(0, colonIndex).trim()
                    val value = line.substring(colonIndex + 1).trim()
                    headers[key] = value
                }
            }

            // Capturar el body (todo lo que queda después de la línea vacía, excluyendo el \0 final)
            val body = if (bodyStartIndex < lines.size) {
                lines.subList(bodyStartIndex, lines.size)
                    .joinToString("\n")
                    .replace("\u0000", "") // Remover el null terminator
                    .trim()
            } else {
                ""
            }

            return StompFrame(command, headers, body)
        }

        /**
         * Serializar una trama STOMP a string
         * Usa CRLF como según la especificación STOMP
         */
        fun serialize(frame: StompFrame): String {
            val sb = StringBuilder()
            sb.append(frame.command).append("\n")

            for ((key, value) in frame.headers) {
                sb.append(key).append(":").append(value).append("\n")
            }

            sb.append("\n")
            if (frame.body.isNotEmpty()) {
                sb.append(frame.body)
            }
            sb.append("\u0000") // Null terminator requerido por STOMP

            return sb.toString()
        }
    }
}



