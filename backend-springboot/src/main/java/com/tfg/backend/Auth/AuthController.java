package com.tfg.backend.Auth;

import com.tfg.backend.Auth.dto.AuthResponseDto;
import com.tfg.backend.Auth.dto.LoginDto;
import com.tfg.backend.Auth.dto.RegisterDto;
import com.tfg.backend.Security.JwtUtil;
import com.tfg.backend.User.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthService authService, JwtUtil jwtUtil) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterDto registerDto) {
        try {
            User user = authService.register(registerDto);
            String token = jwtUtil.generateToken(user.getEmail());
            
            AuthResponseDto response = new AuthResponseDto(token, user);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginDto loginDto) {
        try {
            User user = authService.login(loginDto);
            String token = jwtUtil.generateToken(user.getEmail());
            
            AuthResponseDto response = new AuthResponseDto(token, user);
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestParam Long userId) {
        try {
            authService.logout(userId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
