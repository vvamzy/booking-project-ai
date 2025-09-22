package com.example.meeting.service;

import com.example.meeting.model.Notification;
import com.example.meeting.model.NotificationStatus;
import com.example.meeting.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private org.springframework.web.client.RestTemplate restTemplate;

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private BookingService bookingService;

    public Notification scheduleNotification(Long userId, Long bookingId, String type, String method, LocalDateTime when, String payload) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setBookingId(bookingId);
        n.setType(type);
        n.setMethod(method);
        n.setScheduledAt(when);
        n.setPayload(payload);
        n.setStatus(NotificationStatus.PENDING);
        return notificationRepository.save(n);
    }

    public void sendDueNotifications() {
        List<Notification> due = notificationRepository.findByStatusAndScheduledAtBefore(NotificationStatus.PENDING, LocalDateTime.now().plusSeconds(1));
        for (Notification n : due) {
            try {
                sendNotification(n);
                n.setStatus(NotificationStatus.SENT);
                n.setSentAt(LocalDateTime.now());
                // publish to websocket topic for immediate delivery
                try {
                    if (messagingTemplate != null) {
                        messagingTemplate.convertAndSend("/topic/notifications/user/" + (n.getUserId() == null ? "all" : n.getUserId()), n);
                    }
                } catch (Exception ignored) {}
            } catch (Exception e) {
                n.setStatus(NotificationStatus.FAILED);
            }
            notificationRepository.save(n);
        }
    }

    public void sendNotification(Notification n) {
        // For email, try JavaMailSender if available. For in-app, persistence is already stored.
        if ("REMINDER".equalsIgnoreCase(n.getType())) {
            // enrich with AI suggestions if not already present
            try {
                com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode node = om.readTree(n.getPayload());
                boolean hasSuggestions = node.has("suggestions");
                if (!hasSuggestions && node.has("bookingId")) {
                    Long bookingId = node.get("bookingId").asLong();
                    // fetch booking to get room/time/capacity
                    String api = "http://localhost:8080/api/bookings/" + bookingId;
                    try {
                        com.fasterxml.jackson.databind.JsonNode booking = restTemplate.getForObject(api, com.fasterxml.jackson.databind.JsonNode.class);
                        if (booking != null && booking.has("roomId") && booking.has("startTime") && booking.has("endTime")) {
                            Long roomId = booking.get("roomId").asLong();
                            String start = booking.get("startTime").asText();
                            String end = booking.get("endTime").asText();
                            int capacity = booking.has("attendeesCount") ? booking.get("attendeesCount").asInt() : 1;
                            String suggestUrl = String.format("http://localhost:8080/api/bookings/suggest?roomId=%d&start=%s&end=%s&capacity=%d", roomId, java.net.URLEncoder.encode(start, java.nio.charset.StandardCharsets.UTF_8), java.net.URLEncoder.encode(end, java.nio.charset.StandardCharsets.UTF_8), capacity);
                            com.fasterxml.jackson.databind.JsonNode sug = restTemplate.getForObject(suggestUrl, com.fasterxml.jackson.databind.JsonNode.class);
                            if (sug != null) {
                                ((com.fasterxml.jackson.databind.node.ObjectNode) node).set("suggestions", sug.get("alternateRooms"));
                                n.setPayload(om.writeValueAsString(node));
                            }
                        }
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
        }

        if ("EMAIL".equalsIgnoreCase(n.getMethod()) && mailSender != null) {
            SimpleMailMessage msg = new SimpleMailMessage();
            // payload should be simple JSON: { toEmail, subject, body }
            // defensive parse
            String body = n.getPayload();
            String to = null;
            String subject = "Meeting reminder";
            try {
                com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body);
                to = node.has("to") ? node.get("to").asText(null) : null;
                subject = node.has("subject") ? node.get("subject").asText(subject) : subject;
                String text = node.has("body") ? node.get("body").asText("") : "";
                if (to != null) {
                    msg.setTo(to);
                    msg.setSubject(subject);
                    msg.setText(text);
                    mailSender.send(msg);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        // in-app notifications are represented by the DB record; clients poll or websocket can be used to surface them.
    }
}
