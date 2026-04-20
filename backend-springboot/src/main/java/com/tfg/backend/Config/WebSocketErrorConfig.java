package com.tfg.backend.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;


import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.MimeTypeUtils;

@Configuration
public class WebSocketErrorConfig {
    @Bean
    public StompSubProtocolErrorHandler stompSubProtocolErrorHandler() {
        return new StompSubProtocolErrorHandler() {
            // Custom error message for websocket errors
            @Override
            public Message<byte[]> handleClientMessageProcessingError(Message<byte[]> clientMessage, Throwable ex) {
                Throwable cause = getRootCause(ex);

                String code = "WS_ERROR";
                String message = "An error occurred while processing the message.";

                if (cause instanceof WebSocketAuthException authEx) {
                    code = authEx.getCode();
                    message = authEx.getMessage();
                }

                String body = String.format("{\"error\": {\"code\": \"%s\", \"message\": \"%s\"}}", code, message);

                StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.ERROR);
                headerAccessor.setMessage(message);
                headerAccessor.setContentType(MimeTypeUtils.APPLICATION_JSON);
                headerAccessor.setLeaveMutable(true);

                MessageHeaders headers = headerAccessor.getMessageHeaders();
                return MessageBuilder.createMessage(body.getBytes(), headers);
            }

            private Throwable getRootCause(Throwable ex) {
                Throwable cause = ex;
                while (cause.getCause() != null) {
                    cause = cause.getCause();
                }
                return cause;
            }
        };
    }
}
