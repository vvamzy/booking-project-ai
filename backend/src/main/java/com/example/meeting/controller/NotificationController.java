package com.example.meeting.controller;

import com.example.meeting.model.Notification;
import com.example.meeting.model.NotificationStatus;
import com.example.meeting.repository.NotificationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @GetMapping
    public ResponseEntity<List<Notification>> getNotifications(@RequestParam(required = false) Long userId) {
        if (userId == null) return ResponseEntity.ok(notificationRepository.findAll());
        return ResponseEntity.ok(notificationRepository.findByUserIdOrderByScheduledAtDesc(userId));
    }

    @PostMapping("/{id}/mark-read")
    public ResponseEntity<?> markRead(@PathVariable Long id) {
        return notificationRepository.findById(id).map(n -> {
            n.setStatus(NotificationStatus.READ);
            notificationRepository.save(n);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
