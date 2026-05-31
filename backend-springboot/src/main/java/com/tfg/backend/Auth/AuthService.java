package com.tfg.backend.Auth;

import com.tfg.backend.Audit.AuditService;
import com.tfg.backend.Auth.dto.LoginDto;
import com.tfg.backend.Auth.dto.RegisterRequestDto;
import com.tfg.backend.Enums.AuditAction;
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
    private final AuditService auditService;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    public AuthService(UserRepository userRepository, UserService userService, PasswordEncoder passwordEncoder,
                      AuthenticationManager authenticationManager, EmailService emailService,
                      EmailVerificationTokenRepository emailVerificationTokenRepository,
                      NotificationService notificationService, AuditService auditService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.emailService = emailService;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
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

        // Persistir usuario pendiente de verificacion para poder activarlo con el token
        User savedUser = userRepository.save(user);
        auditService.record(AuditAction.REGISTER_USER, savedUser.getId());

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

        auditService.record(AuditAction.VERIFY_USER_EMAIL, verifiedUser.getId());

        // Crear notificación para admins
        createAdminNotification("Nuevo usuario esperando aprobación",
            "El usuario " + user.getName() + " (" + user.getEmail() + ") ha verificado su correo y espera aprobación.");

        return verifiedUser;
    }

    @Transactional
    public User approveUser(Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        user.setAdminApproved(true);
        User approvedUser = userRepository.save(user);
        auditService.record(AuditAction.VALIDATE_USER, approvedUser.getId());

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
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginDto.getEmail(),
                    loginDto.getPassword()
                )
            );
        } catch (BadCredentialsException e) {
            // Credenciales realmente inválidas: mensaje genérico para no revelar si el correo existe.
            throw new BadCredentialsException("Invalid email or password");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
            .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        // Las credenciales ya son válidas aquí, así que estos estados de cuenta
        // pueden comunicarse al cliente sin riesgo de enumeración.
        if (!user.isEmailVerified()) {
            throw new BadCredentialsException("Por favor verifica tu correo electrónico");
        }

        if (!user.isAdminApproved()) {
            throw new BadCredentialsException("Tu cuenta está pendiente de aprobación del administrador");
        }

        return userService.setOnlineStatusByEmail(email, true);
    }

    public void logout(Long userId) {
        userService.setOnlineStatus(userId, false);
    }

    private void createAdminNotification(String title, String message) {
        // Bandeja compartida: una sola notificación para todos los admins (sin destinatario concreto).
        notificationService.createNotification(
            NotificationType.USER_REGISTRATION_PENDING,
            title,
            message
        );
    }
}

