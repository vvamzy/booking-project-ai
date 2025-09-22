package com.example.meeting.repository;

import com.example.meeting.model.Notification;
import com.example.meeting.model.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByStatusAndScheduledAtBefore(NotificationStatus status, LocalDateTime before);
    List<Notification> findByUserIdOrderByScheduledAtDesc(Long userId);
}
