package com.tfg.backend.Notifications;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.read = false ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId);
}
