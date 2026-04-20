package com.tfg.backend.Auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;
    private final String fromEmail;

    public EmailService(JavaMailSender mailSender, @Value("${app.mail.from:noreply@tfg-platform.com}") String fromEmail) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
    }

    public void sendVerificationEmail(String email, String verificationToken, String baseUrl) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Verificación de correo electrónico - TFG Platform");
            
            String verificationUrl = baseUrl + "/verify-email?token=" + verificationToken;
            String body = "Hola,\n\n" +
                    "Gracias por registrarte en nuestra plataforma. Por favor, verifica tu correo electrónico haciendo clic en el siguiente enlace:\n\n" +
                    verificationUrl + "\n\n" +
                    "Este enlace expirará en 24 horas.\n\n" +
                    "Si no solicitaste este registro, ignora este correo.\n\n" +
                    "Saludos,\nEl equipo de TFG Platform";
            
            message.setText(body);
            message.setFrom(fromEmail);
            
            mailSender.send(message);
        } catch (Exception e) {
            String causeMessage = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            log.error("Fallo SMTP enviando verificacion a {} desde {}: {}", email, fromEmail, causeMessage, e);
            throw new RuntimeException("Error enviando correo de verificación: " + causeMessage);
        }
    }

    public void sendApprovalNotificationEmail(String email, String userName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Cuenta aprobada - TFG Platform");
            
            String body = "Hola " + userName + ",\n\n" +
                    "¡Tu cuenta ha sido aprobada por un administrador!\n" +
                    "Ya puedes acceder a la plataforma con tus credenciales.\n\n" +
                    "Saludos,\nEl equipo de TFG Platform";
            
            message.setText(body);
            message.setFrom(fromEmail);
            
            mailSender.send(message);
        } catch (Exception e) {
            String causeMessage = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            log.error("Fallo SMTP enviando aprobacion a {} desde {}: {}", email, fromEmail, causeMessage, e);
            throw new RuntimeException("Error enviando correo de aprobación: " + causeMessage);
        }
    }

    public void sendRejectionNotificationEmail(String email, String userName, String reason) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Registro rechazado - TFG Platform");
            
            String body = "Hola " + userName + ",\n\n" +
                    "Lamentablemente, tu solicitud de registro ha sido rechazada.\n" +
                    "Razón: " + (reason != null ? reason : "No especificada") + "\n\n" +
                    "Si tienes preguntas, contacta al administrador.\n\n" +
                    "Saludos,\nEl equipo de TFG Platform";
            
            message.setText(body);
            message.setFrom(fromEmail);
            
            mailSender.send(message);
        } catch (Exception e) {
            String causeMessage = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            log.error("Fallo SMTP enviando rechazo a {} desde {}: {}", email, fromEmail, causeMessage, e);
            throw new RuntimeException("Error enviando correo de rechazo: " + causeMessage);
        }
    }
}
