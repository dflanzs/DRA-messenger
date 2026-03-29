package com.tfg.backend.Config;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebSocketErrorConfigTest {

    @Test
    void handleClientMessageProcessingError_returnsAuthErrorPayload_whenRootCauseIsAuthException() {
        WebSocketErrorConfig config = new WebSocketErrorConfig();
        StompSubProtocolErrorHandler handler = config.stompSubProtocolErrorHandler();

        RuntimeException ex = new RuntimeException(
            new WebSocketAuthException("Missing Authorization header", "MISSING_AUTH_HEADER")
        );

        Message<byte[]> result = handler.handleClientMessageProcessingError(null, ex);

        String payload = new String(result.getPayload(), StandardCharsets.UTF_8);
        assertEquals(
            "{\"error\": {\"code\": \"MISSING_AUTH_HEADER\", \"message\": \"Missing Authorization header\"}}",
            payload
        );

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);
        assertEquals(StompCommand.ERROR, accessor.getCommand());
        assertEquals("Missing Authorization header", accessor.getMessage());
        assertEquals(MimeTypeUtils.APPLICATION_JSON, accessor.getContentType());
    }

    @Test
    void handleClientMessageProcessingError_returnsGenericErrorPayload_whenCauseIsNotAuthException() {
        WebSocketErrorConfig config = new WebSocketErrorConfig();
        StompSubProtocolErrorHandler handler = config.stompSubProtocolErrorHandler();

        RuntimeException ex = new RuntimeException("boom");

        Message<byte[]> result = handler.handleClientMessageProcessingError(null, ex);

        String payload = new String(result.getPayload(), StandardCharsets.UTF_8);
        assertEquals(
            "{\"error\": {\"code\": \"WS_ERROR\", \"message\": \"An error occurred while processing the message.\"}}",
            payload
        );

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);
        assertEquals(StompCommand.ERROR, accessor.getCommand());
        assertEquals("An error occurred while processing the message.", accessor.getMessage());
        assertEquals(MimeTypeUtils.APPLICATION_JSON, accessor.getContentType());
    }
}
