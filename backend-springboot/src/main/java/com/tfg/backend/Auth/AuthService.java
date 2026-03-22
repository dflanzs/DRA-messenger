package com.tfg.backend.Auth;

import com.tfg.backend.Auth.dto.LoginDto;
import com.tfg.backend.Auth.dto.RegisterRequestDto;
import com.tfg.backend.Enums.UserRole;
import com.tfg.backend.Notifications.NotificationService;
import com.tfg.backend.Notifications.NotificationType;
import com.tfg.backend.User.User;
import com.tfg.backend.User.UserRepository;
import com.tfg.backend.User.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final NotificationService notificationService;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    public AuthService(UserRepository userRepository, UserService userService, PasswordEncoder passwordEncoder,
                      AuthenticationManager authenticationManager, EmailService emailService,
                      EmailVerificationTokenRepository emailVerificationTokenRepository,
                      NotificationService notificationService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.emailService = emailService;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public String register(RegisterRequestDto registerDto) {
        // Validar password
        if (!userService.validatePassword(registerDto.getPassword())) {
            throw new IllegalArgumentException("Password does not meet requirements");
        }

        // Verificar si el email ya existe (solo usuarios no eliminados)
        Optional<User> existingUser = userRepository.findByEmailAndDeletedAtIsNull(registerDto.getEmail());
        if (existingUser.isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Crear nuevo usuario sin verificar email
        User user = new User();
        user.setName(registerDto.getName());
        user.setEmail(registerDto.getEmail());
        user.setPassword(passwordEncoder.encode(registerDto.getPassword()));
        user.setPublicKey(UUID.randomUUID().toString());
        user.setOnlineStatus(false);
        user.setEmailVerified(false);
        user.setAdminApproved(false);
        user.setRole(UserRole.USER);

        // Generar token de verificación
        String verificationToken = UUID.randomUUID().toString();
        EmailVerificationToken token = new EmailVerificationToken(
            verificationToken,
            registerDto.getEmail(),
            LocalDateTime.now().plusHours(24)
        );
        emailVerificationTokenRepository.save(token);

        // Enviar email de verificación
        emailService.sendVerificationEmail(registerDto.getEmail(), verificationToken, frontendUrl);

        return "Registration successful. Please check your email to verify your account.";
    }

    @Transactional
    public User verifyEmail(String token) {
        EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByToken(token)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token inválido o expirado"));

        if (verificationToken.isExpired()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token expirado");
        }

        if (verificationToken.isUsed()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token ya ha sido utilizado");
        }

        // Obtener usuario por email
        User user = userRepository.findByEmailAndDeletedAtIsNull(verificationToken.getEmail())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        // Marcar email como verificado
        user.setEmailVerified(true);
        User verifiedUser = userRepository.save(user);

        // Marcar token como usado
        verificationToken.setUsed(true);
        emailVerificationTokenRepository.save(verificationToken);

        // Crear notificación para admins
        createNotificationForAdmins("Nuevo usuario esperando aprobación",
            "El usuario " + user.getName() + " (" + user.getEmail() + ") ha verificado su correo y espera aprobación.");

        return verifiedUser;
    }

    @Transactional
    public User approveUser(Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        user.setAdminApproved(true);
        User approvedUser = userRepository.save(user);

        // Enviar email de aprobación
        emailService.sendApprovalNotificationEmail(user.getEmail(), user.getName());

        return approvedUser;
    }

    @Transactional
    public void rejectUser(Long userId, String reason) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        // Enviar email de rechazo
        emailService.sendRejectionNotificationEmail(user.getEmail(), user.getName(), reason);

        // Eliminar usuario (soft delete)
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
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
                .orElseThrow(() -> new BadCredentialsException("Usuario no encontrado"));

            // Verificar que el usuario verificó su email y fue aprobado
            if (!user.isEmailVerified()) {
                throw new BadCredentialsException("Por favor verifica tu correo electrónico");
            }

            if (!user.isAdminApproved()) {
                throw new BadCredentialsException("Tu cuenta está pendiente de aprobación del administrador");
            }

            return userService.setOnlineStatusByEmail(email, true);

        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    public void logout(Long userId) {
        userService.setOnlineStatus(userId, false);
    }

    private void createNotificationForAdmins(String title, String message) {
        // Buscar todos los admins y crear notificación para cada uno
        userRepository.findAllByDeletedAtIsNullAndRole(UserRole.ADMIN)
            .forEach(admin -> notificationService.createNotification(
                NotificationType.USER_REGISTRATION_PENDING,
                title,
                message,
                admin.getId()
            ));
    }
}

