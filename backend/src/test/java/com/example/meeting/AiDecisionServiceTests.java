package com.example.meeting;

import com.example.meeting.model.Booking;
import com.example.meeting.service.AiDecisionService;
import com.example.meeting.service.AiDecisionService.Action;
import com.example.meeting.service.AiDecisionService.Decision;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class AiDecisionServiceTests {

    private final AiDecisionService ai = new AiDecisionService();

    @Test
    void autoApproveShortInHoursNoConflicts() {
        Booking booking = new Booking();
        booking.setStartTime(LocalDateTime.of(2025, 9, 20, 10, 0));
        booking.setEndTime(LocalDateTime.of(2025, 9, 20, 11, 0));

        Decision d = ai.decide(booking, Collections.emptyList());

        assertEquals(Action.AUTO_APPROVE, d.getAction());
        assertTrue(d.getConfidence() >= 0.75);
    }

    @Test
    void autoRejectTooLongBooking() {
        Booking booking = new Booking();
        booking.setStartTime(LocalDateTime.of(2025, 9, 20, 8, 0));
        booking.setEndTime(LocalDateTime.of(2025, 9, 21, 10, 0)); // > 8 hours

        Decision d = ai.decide(booking, Collections.emptyList());

        assertEquals(Action.AUTO_REJECT, d.getAction());
        assertTrue(d.getConfidence() >= 0.8);
    }

    @Test
    void requiresReviewWhenConflict() {
        Booking booking = new Booking();
        booking.setStartTime(LocalDateTime.of(2025, 9, 20, 10, 0));
        booking.setEndTime(LocalDateTime.of(2025, 9, 20, 12, 0));

        Booking other = new Booking();
        other.setStartTime(LocalDateTime.of(2025, 9, 20, 11, 0));
        other.setEndTime(LocalDateTime.of(2025, 9, 20, 13, 0));

        Decision d = ai.decide(booking, Collections.singletonList(other));

        assertEquals(Action.REQUIRES_REVIEW, d.getAction());
        assertTrue(d.getConfidence() < 0.75 && d.getConfidence() >= 0.0);
    }
}
