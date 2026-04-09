package com.tfg.backend.Config;

import com.tfg.backend.Security.CustomUserDetailsService;
import com.tfg.backend.Security.JwtUtil;
import com.tfg.backend.Security.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationChannelInterceptorTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private MessageChannel messageChannel;

    private JwtAuthenticationChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new JwtAuthenticationChannelInterceptor(jwtUtil, userDetailsService, tokenBlacklistService);
    }

    @Test
    void preSend_setsUser_whenConnectWithValidToken() {
        String token = "valid-token";
        String email = "user@example.com";
        UserDetails userDetails = User.withUsername(email)
            .password("pwd")
            .authorities("ROLE_USER")
            .build();
        Message<byte[]> message = buildMessage(StompCommand.CONNECT, "Bearer " + token);

        when(jwtUtil.extractEmail(token)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);
        when(jwtUtil.validateToken(token, email)).thenReturn(true);

        Message<?> result = interceptor.preSend(message, messageChannel);

        assertSame(message, result);
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertNotNull(accessor);
        assertNotNull(accessor.getUser());
        UsernamePasswordAuthenticationToken auth =
            (UsernamePasswordAuthenticationToken) accessor.getUser();
        assertEquals(email, auth.getName());
    }

    @Test
    void preSend_throwsMissingAuth_whenConnectWithoutAuthorizationHeader() {
        Message<byte[]> message = buildMessage(StompCommand.CONNECT, null);

        WebSocketAuthException ex = assertThrows(
            WebSocketAuthException.class,
            () -> interceptor.preSend(message, messageChannel)
        );

        assertEquals("MISSING_AUTH_HEADER", ex.getCode());
    }

    @Test
    void preSend_throwsInvalidAuthHeader_whenAuthorizationHeaderIsNotBearer() {
        Message<byte[]> message = buildMessage(StompCommand.CONNECT, "Basic abc123");

        WebSocketAuthException ex = assertThrows(
            WebSocketAuthException.class,
            () -> interceptor.preSend(message, messageChannel)
        );

        assertEquals("INVALID_AUTH_HEADER", ex.getCode());
    }

    @Test
    void preSend_throwsBlacklisted_whenTokenIsBlacklisted() {
        String token = "blacklisted-token";
        String email = "user@example.com";
        UserDetails userDetails = User.withUsername(email)
            .password("pwd")
            .authorities("ROLE_USER")
            .build();
        Message<byte[]> message = buildMessage(StompCommand.CONNECT, "Bearer " + token);

        when(jwtUtil.extractEmail(token)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(true);
        when(jwtUtil.validateToken(token, email)).thenReturn(true);

        WebSocketAuthException ex = assertThrows(
            WebSocketAuthException.class,
            () -> interceptor.preSend(message, messageChannel)
        );

        assertEquals("TOKEN_BLACKLISTED", ex.getCode());
    }

    @Test
    void preSend_throwsInvalidToken_whenTokenValidationFails() {
        String token = "invalid-token";
        String email = "user@example.com";
        UserDetails userDetails = User.withUsername(email)
            .password("pwd")
            .authorities("ROLE_USER")
            .build();
        Message<byte[]> message = buildMessage(StompCommand.CONNECT, "Bearer " + token);

        when(jwtUtil.extractEmail(token)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);
        when(jwtUtil.validateToken(token, email)).thenReturn(false);

        WebSocketAuthException ex = assertThrows(
            WebSocketAuthException.class,
            () -> interceptor.preSend(message, messageChannel)
        );

        assertEquals("INVALID_TOKEN", ex.getCode());
    }

    @Test
    void preSend_wrapsUnexpectedExceptions_asAuthInternalError() {
        String token = "token";
        Message<byte[]> message = buildMessage(StompCommand.CONNECT, "Bearer " + token);

        when(jwtUtil.extractEmail(token)).thenThrow(new RuntimeException("boom"));

        WebSocketAuthException ex = assertThrows(
            WebSocketAuthException.class,
            () -> interceptor.preSend(message, messageChannel)
        );

        assertEquals("AUTH_INTERNAL_ERROR", ex.getCode());
    }

    @Test
    void preSend_doesNothing_whenCommandIsNotConnect() {
        Message<byte[]> message = buildMessage(StompCommand.SEND, null);

        Message<?> result = interceptor.preSend(message, messageChannel);

        assertSame(message, result);
        verifyNoInteractions(jwtUtil, userDetailsService, tokenBlacklistService);
    }

    private Message<byte[]> buildMessage(StompCommand command, String authorizationHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (authorizationHeader != null) {
            accessor.setNativeHeader("Authorization", authorizationHeader);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
