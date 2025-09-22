package com.example.meeting.scheduler;

import com.example.meeting.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationScheduler {

    @Autowired
    private NotificationService notificationService;

    // Run every minute
    @Scheduled(fixedRate = 60000)
    public void dispatch() {
        try {
            notificationService.sendDueNotifications();
        } catch (Exception ignored) {}
    }
}
