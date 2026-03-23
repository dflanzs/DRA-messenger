package com.tfg.backend.Notifications;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public Notification createNotification(NotificationType type, String title, String message, Long userId) {
        Notification notification = new Notification(type, title, message, userId);
        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<Notification> getAdminNotifications(Long adminId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(adminId);
    }

    @Transactional(readOnly = true)
    public List<Notification> getUnreadAdminNotifications(Long adminId) {
        return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(adminId);
    }

    @Transactional
    public Notification markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new RuntimeException("Notificación no encontrada"));
        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    @Transactional
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }
}
