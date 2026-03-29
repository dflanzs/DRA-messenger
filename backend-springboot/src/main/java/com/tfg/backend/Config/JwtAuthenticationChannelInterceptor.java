package com.tfg.backend.Config;

import com.tfg.backend.Security.CustomUserDetailsService;
import com.tfg.backend.Security.JwtUtil;
import com.tfg.backend.Security.TokenBlacklistService;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthenticationChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;

    public JwtAuthenticationChannelInterceptor(JwtUtil jwtUtil,
                                               CustomUserDetailsService userDetailsService,
                                               TokenBlacklistService tokenBlacklistService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader == null) {
                throw new WebSocketAuthException("Missing Authorization header", "MISSING_AUTH_HEADER");
            }

            String token = authHeader.substring(7);

            try {
                String email = jwtUtil.extractEmail(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                boolean blacklisted = tokenBlacklistService.isBlacklisted(token);
                boolean valid = jwtUtil.validateToken(token, email);

                if (valid && !blacklisted) {
                    UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                    accessor.setUser(auth);
                }
                else if (blacklisted) {
                    throw new WebSocketAuthException("Token has been blacklisted", "TOKEN_BLACKLISTED");
                }
                else if (!valid) {
                    throw new WebSocketAuthException("Invalid JWT token", "INVALID_TOKEN");
                }
            } catch (WebSocketAuthException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new WebSocketAuthException("WebSocket authentication failed", "AUTH_INTERNAL_ERROR", ex);
            }
        }
        return message;
    }
}
