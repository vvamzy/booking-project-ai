package com.example.meeting.service;

import com.example.meeting.model.Booking;
import com.example.meeting.model.Room;
import com.example.meeting.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class RuleBasedDecisionService {

    @Autowired
    private RoomRepository roomRepository;

    public AiDecisionService.Decision decide(Booking booking, List<Booking> overlappingBookings) {
        List<String> rationale = new ArrayList<>();
        double confidence = 0.7; // Base confidence for rule-based decisions

        // Check for overlapping bookings
        if (overlappingBookings != null && !overlappingBookings.isEmpty()) {
            rationale.add("There are overlapping bookings for this time slot");
            return new AiDecisionService.Decision(
                AiDecisionService.Action.REQUIRES_REVIEW,
                confidence,
                rationale
            );
        }

        // Validate basic fields handled elsewhere; here we focus on heuristics

        // Fetch room metadata if available
        Room room = null;
        try {
            if (booking.getRoomId() != null) {
                room = roomRepository.findById(booking.getRoomId()).orElse(null);
            }
        } catch (Exception ignored) { }

        // Justification check: require a reasonably detailed purpose
        String purpose = booking.getPurpose() == null ? "" : booking.getPurpose().trim().toLowerCase();
        if (purpose.length() < 15 || purpose.equals("meeting") || purpose.equals("sync") || purpose.equals("call")) {
            rationale.add("Insufficient justification for approval");
            return new AiDecisionService.Decision(
                AiDecisionService.Action.REQUIRES_REVIEW,
                0.4,
                rationale
            );
        }

        // Capacity matching: reject if the booking significantly underutilizes the room
        if (room != null && booking.getAttendeesCount() != null) {
            int req = booking.getAttendeesCount();
            int cap = room.getCapacity();
            if (cap > 0) {
                double utilization = (double) req / (double) cap;
                // Underutilized if less than 40% occupied and absolute capacity difference >= 5
                if (utilization < 0.4 && (cap - req) >= 5) {
                    rationale.add("Requested capacity significantly underutilizes the room (" + req + " of " + cap + ")");
                    return new AiDecisionService.Decision(
                        AiDecisionService.Action.AUTO_REJECT,
                        0.9,
                        rationale
                    );
                }
                // Overcapacity -> reject
                if (req > cap) {
                    rationale.add("Requested attendees exceed room capacity");
                    return new AiDecisionService.Decision(
                        AiDecisionService.Action.AUTO_REJECT,
                        0.95,
                        rationale
                    );
                }
            }
        }

        // Purpose compatibility: infer room type by name and check purpose keywords
        if (room != null && purpose.length() > 0) {
            String name = room.getName() == null ? "" : room.getName().toLowerCase();
            boolean compatible = true;

            if (name.contains("auditor") || name.contains("auditorium") || name.contains("theatre")) {
                // auditoriums intended for presentations, all-hands, townhalls
                compatible = purpose.contains("presentation") || purpose.contains("townhall") || purpose.contains("all-hands") || purpose.contains("keynote");
            } else if (name.contains("board") || name.contains("executive")) {
                compatible = purpose.contains("board") || purpose.contains("executive") || purpose.contains("client") || purpose.contains("strategy");
            } else if (name.contains("training") || name.contains("studio")) {
                compatible = purpose.contains("training") || purpose.contains("workshop") || purpose.contains("class") || purpose.contains("session");
            } else if (name.contains("focus") || name.contains("pod") || name.contains("huddle") || name.contains("small")) {
                compatible = purpose.contains("one-on-one") || purpose.contains("huddle") || purpose.contains("sync") || purpose.contains("interview");
            } else {
                // generic rooms: be permissive but reject if purpose explicitly large-audience
                if (purpose.contains("townhall") || purpose.contains("keynote") || purpose.contains("all-hands")) compatible = false;
            }

            if (!compatible) {
                rationale.add("Purpose seems incompatible with room type: '" + room.getName() + "'");
                return new AiDecisionService.Decision(
                    AiDecisionService.Action.AUTO_REJECT,
                    0.9,
                    rationale
                );
            }
        }

        // Time-of-day preference: prefer in-hours
        LocalTime bookingTime = booking.getStartTime().toLocalTime();
        if (bookingTime.isBefore(LocalTime.of(8, 0)) || bookingTime.isAfter(LocalTime.of(18, 0))) {
            rationale.add("Booking is outside preferred business hours");
            return new AiDecisionService.Decision(
                AiDecisionService.Action.REQUIRES_REVIEW,
                0.5,
                rationale
            );
        }

        // Priority: high priority auto-approve
        if (booking.getPriority() != null && booking.getPriority() >= 4) {
            rationale.add("High priority booking (priority " + booking.getPriority() + ")");
            return new AiDecisionService.Decision(
                AiDecisionService.Action.AUTO_APPROVE,
                0.85,
                rationale
            );
        }

        // Default to require review
        rationale.add("No automatic decision rules matched");
        return new AiDecisionService.Decision(
            AiDecisionService.Action.REQUIRES_REVIEW,
            0.5,
            rationale
        );
    }
}