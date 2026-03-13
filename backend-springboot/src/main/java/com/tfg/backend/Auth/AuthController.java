package com.tfg.backend.Auth;

import com.tfg.backend.Auth.dto.AuthResponseDto;
import com.tfg.backend.Auth.dto.LoginDto;
import com.tfg.backend.Auth.dto.RegisterDto;
import com.tfg.backend.Security.JwtUtil;
import com.tfg.backend.User.User;
import com.tfg.backend.User.dto.UserResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final com.tfg.backend.Security.TokenBlacklistService tokenBlacklistService;

    public AuthController(AuthService authService, JwtUtil jwtUtil,
                          com.tfg.backend.Security.TokenBlacklistService tokenBlacklistService) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterDto registerDto) {
        try {
            User user = authService.register(registerDto);
            String token = jwtUtil.generateToken(user);
            AuthResponseDto response = new AuthResponseDto(token, UserResponseDto.fromUser(user));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginDto loginDto) {
        try {
            User user = authService.login(loginDto);
            String token = jwtUtil.generateToken(user);
            AuthResponseDto response = new AuthResponseDto(token, UserResponseDto.fromUser(user));
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Long userId = jwtUtil.extractUserId(token);
                if (userId != null) {
                    authService.logout(userId);
                }
                tokenBlacklistService.blacklist(token);
            } catch (Exception ignored) {
                // token may be invalid; blacklist anyway if possible
            }
        }
        return ResponseEntity.ok().build();
    }
}
