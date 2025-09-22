package com.example.meeting.controller;

import com.example.meeting.model.ApprovalLog;
import com.example.meeting.repository.ApprovalLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/approvals")
public class ApprovalLogController {

    @Autowired
    private ApprovalLogRepository approvalLogRepository;

    @GetMapping("/{bookingId}/logs")
    public ResponseEntity<List<ApprovalLog>> getLogs(@PathVariable Long bookingId) {
        List<ApprovalLog> logs = approvalLogRepository.findByBookingIdOrderByCreatedAtDesc(bookingId);
        return ResponseEntity.ok(logs);
    }
}
