package com.example.meeting.controller;

import com.example.meeting.model.Booking;
import com.example.meeting.service.ApprovalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/approvals")
public class ApprovalController {

    @Autowired
    private ApprovalService approvalService;

    @GetMapping
    public ResponseEntity<List<Booking>> getPendingApprovals() {
        List<Booking> pendingBookings = approvalService.getPendingApprovals();
        return ResponseEntity.ok(pendingBookings);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<Booking>> getPendingApprovalsAlias() {
        return getPendingApprovals();
    }

    @PostMapping("/{bookingId}/approve")
    public ResponseEntity<String> approveBooking(@PathVariable Long bookingId) {
        approvalService.approveBooking(bookingId);
        return ResponseEntity.ok("Booking approved successfully.");
    }

    @PostMapping("/{bookingId}/reject")
    public ResponseEntity<String> rejectBooking(@PathVariable Long bookingId) {
        approvalService.rejectBooking(bookingId);
        return ResponseEntity.ok("Booking rejected successfully.");
    }
}