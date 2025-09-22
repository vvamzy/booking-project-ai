package com.example.meeting.repository;

import com.example.meeting.model.BookingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingHistoryRepository extends JpaRepository<BookingHistory, Long> {
    List<BookingHistory> findByBookingId(Long bookingId);
    List<BookingHistory> findByBookingIdOrderByChangedAtDesc(Long bookingId);
}