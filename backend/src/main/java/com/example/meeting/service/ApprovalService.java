package com.example.meeting.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.meeting.model.Booking;
import com.example.meeting.model.ApprovalLog;
import com.example.meeting.model.ApprovalAction;
import com.example.meeting.repository.BookingRepository;
import com.example.meeting.repository.ApprovalLogRepository;

import java.util.Optional;
import java.util.List;

@Service
public class ApprovalService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private AiDecisionService aiDecisionService;

    @Autowired
    private ApprovalLogRepository approvalLogRepository;

    public Optional<Booking> approveBooking(Long bookingId) {
        Optional<Booking> booking = bookingRepository.findById(bookingId);
        if (booking.isPresent()) {
            Booking approvedBooking = booking.get();
            approvedBooking.setStatus("APPROVED");
            approvedBooking.setDecisionConfidence(1.0);
            approvedBooking.setDecisionRationale("Manually approved by admin");
            bookingRepository.save(approvedBooking);

            ApprovalLog log = new ApprovalLog();
            log.setBookingId(approvedBooking.getId());
            log.setActor("admin");
            log.setAction(ApprovalAction.MANUAL_APPROVE.toString());
            log.setConfidence(1.0);
            log.setRationale("Manually approved by admin");
            log.setSource("MANUAL");
            approvalLogRepository.save(log);
            return Optional.of(approvedBooking);
        }
        return Optional.empty();
    }

    public Optional<Booking> rejectBooking(Long bookingId) {
        Optional<Booking> booking = bookingRepository.findById(bookingId);
        if (booking.isPresent()) {
            Booking rejectedBooking = booking.get();
            rejectedBooking.setStatus("REJECTED");
            rejectedBooking.setDecisionConfidence(1.0);
            rejectedBooking.setDecisionRationale("Manually rejected by admin");
            bookingRepository.save(rejectedBooking);

            ApprovalLog log = new ApprovalLog();
            log.setBookingId(rejectedBooking.getId());
            log.setActor("admin");
            log.setAction(ApprovalAction.MANUAL_REJECT.toString());
            log.setConfidence(1.0);
            log.setRationale("Manually rejected by admin");
            log.setSource("MANUAL");
            approvalLogRepository.save(log);
            return Optional.of(rejectedBooking);
        }
        return Optional.empty();
    }

    public List<Booking> getPendingApprovals() {
        List<Booking> pending = bookingRepository.findByStatus("PENDING");
        for (Booking b : pending) {
            java.util.List<Booking> overlaps = bookingRepository.findByRoomIdAndStartTimeLessThanAndEndTimeGreaterThan(
                    b.getRoomId(), b.getEndTime(), b.getStartTime());
            AiDecisionService.Decision decision = aiDecisionService.decide(b, overlaps);
            b.setDecisionConfidence(decision.getConfidence());
            b.setDecisionRationale(String.join("; ", decision.getRationale()));

            // Save a snapshot log for this AI decision when pending
            ApprovalLog log = new ApprovalLog();
            log.setBookingId(b.getId());
            log.setActor("AI");
            switch (decision.getAction()) {
                case AUTO_APPROVE:
                    log.setAction(ApprovalAction.AUTO_APPROVE.toString());
                    break;
                case AUTO_REJECT:
                    log.setAction(ApprovalAction.AUTO_REJECT.toString());
                    break;
                default:
                    log.setAction(ApprovalAction.REVIEW_REQUESTED.toString());
            }
            log.setConfidence(decision.getConfidence());
            log.setRationale(String.join("; ", decision.getRationale()));
            log.setSource(aiDecisionService.isLlmConfigured() ? "LLM" : "RULES");
            approvalLogRepository.save(log);
        }
        return pending;
    }
}