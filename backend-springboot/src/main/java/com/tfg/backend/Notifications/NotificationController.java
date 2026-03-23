package com.tfg.backend.Notifications;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.tfg.backend.User.UserService;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;
    private final UserService userService;

    public NotificationController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @PreAuthorize("@authorizationService.isAdmin(authentication)")
    @GetMapping
    public ResponseEntity<List<Notification>> getNotifications(Authentication authentication) {
        Long userId = userService.getByEmail(authentication.getName()).getId();
        List<Notification> notifications = notificationService.getAdminNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    @PreAuthorize("@authorizationService.isAdmin(authentication)")
    @GetMapping("/unread")
    public ResponseEntity<List<Notification>> getUnreadNotifications(Authentication authentication) {
        Long userId = userService.getByEmail(authentication.getName()).getId();
        List<Notification> unreadNotifications = notificationService.getUnreadAdminNotifications(userId);
        return ResponseEntity.ok(unreadNotifications);
    }

    @PreAuthorize("@authorizationService.isAdmin(authentication)")
    @PutMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable Long id) {
        Notification notification = notificationService.markAsRead(id);
        return ResponseEntity.ok(notification);
    }

    @PreAuthorize("@authorizationService.isAdmin(authentication)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }
}
