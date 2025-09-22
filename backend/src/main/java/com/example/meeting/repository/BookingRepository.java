package com.example.meeting.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.example.meeting.model.Booking;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByStatus(String status);
    List<Booking> findByRoomId(Long roomId);
    List<Booking> findByRoomIdAndStartTimeLessThanAndEndTimeGreaterThan(Long roomId, LocalDateTime endTime, LocalDateTime startTime);
    
    List<Booking> findByRoomIdAndStatus(Long roomId, String status);
    List<Booking> findByPriorityGreaterThanEqual(Integer priority);
    List<Booking> findByUserId(Long userId);
    
    @Query("SELECT b FROM Booking b WHERE b.roomId = :roomId " +
           "AND b.status != 'CANCELLED' " +
           "AND ((b.startTime BETWEEN :start AND :end) " +
           "OR (b.endTime BETWEEN :start AND :end) " +
           "OR (b.startTime <= :start AND b.endTime >= :end))")
    List<Booking> findOverlappingBookings(Long roomId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT b FROM Booking b WHERE (b.status = 'PENDING' OR b.status = 'PENDING_APPROVAL')")
    List<Booking> findPendingBookings();
}