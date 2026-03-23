package com.tfg.backend.Auth;

import com.tfg.backend.Auth.dto.AuthResponseDto;
import com.tfg.backend.Auth.dto.LoginDto;
import com.tfg.backend.Auth.dto.RegisterRequestDto;
import com.tfg.backend.Auth.dto.VerifyEmailDto;
import com.tfg.backend.Security.JwtUtil;
import com.tfg.backend.User.User;
import com.tfg.backend.User.dto.UserResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequestDto registerDto) {
        try {
            String message = authService.register(registerDto);
            return ResponseEntity.ok(Map.of("message", message));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@Valid @RequestBody VerifyEmailDto verifyEmailDto) {
        try {
            User user = authService.verifyEmail(verifyEmailDto.getToken());
            return ResponseEntity.ok(Map.of(
                "message", "Email verificado correctamente. Tu cuenta está pendiente de aprobación del administrador.",
                "user", UserResponseDto.fromUser(user)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
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

    @PreAuthorize("@authorizationService.isAdmin(authentication)")
    @PostMapping("/users/{userId}/approve")
    public ResponseEntity<?> approveUser(@PathVariable Long userId) {
        try {
            User user = authService.approveUser(userId);
            return ResponseEntity.ok(Map.of(
                "message", "Usuario aprobado exitosamente",
                "user", UserResponseDto.fromUser(user)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PreAuthorize("@authorizationService.isAdmin(authentication)")
    @PostMapping("/users/{userId}/reject")
    public ResponseEntity<?> rejectUser(@PathVariable Long userId, @RequestBody Map<String, String> body) {
        try {
            String reason = body.getOrDefault("reason", null);
            authService.rejectUser(userId, reason);
            return ResponseEntity.ok(Map.of("message", "Usuario rechazado exitosamente"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

