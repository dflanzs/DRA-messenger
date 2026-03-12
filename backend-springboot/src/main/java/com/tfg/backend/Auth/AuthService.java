package com.tfg.backend.Auth;

import com.tfg.backend.Auth.dto.LoginDto;
import com.tfg.backend.Auth.dto.RegisterDto;
import com.tfg.backend.User.User;
import com.tfg.backend.User.UserRepository;
import com.tfg.backend.User.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository, UserService userService, PasswordEncoder passwordEncoder,
                      AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    public User register(RegisterDto registerDto) {
        // Validar password
        if (!userService.validatePassword(registerDto.getPassword())) {
            throw new IllegalArgumentException("Password does not meet requirements");
        }

        // Verificar si el email ya existe (solo usuarios no eliminados)
        Optional<User> existingUser = userRepository.findByEmailAndDeletedAtIsNull(registerDto.getEmail());
        if (existingUser.isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Crear nuevo usuario
        User user = new User();
        user.setName(registerDto.getName());
        user.setEmail(registerDto.getEmail());
        user.setPassword(passwordEncoder.encode(registerDto.getPassword()));
        user.setPublicKey(UUID.randomUUID().toString());
        user.setOnlineStatus(false);

        return userRepository.save(user);
    }

    public User login(LoginDto loginDto) {
        try {
            // Autenticar con Spring Security
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginDto.getEmail(),
                    loginDto.getPassword()
                )
            );

            // Obtener el usuario autenticado (solo si no está eliminado)
            String email = authentication.getName();
            User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

            // Actualizar estado online
            user.setOnlineStatus(true);
            return userRepository.save(user);

        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    public void logout(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        user.setOnlineStatus(false);
        userRepository.save(user);
    }
}
