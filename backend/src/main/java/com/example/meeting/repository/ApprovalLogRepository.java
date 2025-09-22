package com.example.meeting.repository;

import com.example.meeting.model.ApprovalLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalLogRepository extends JpaRepository<ApprovalLog, Long> {
	java.util.List<com.example.meeting.model.ApprovalLog> findByBookingIdOrderByCreatedAtDesc(Long bookingId);
}
