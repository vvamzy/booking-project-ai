package com.example.meeting.repository;

import com.example.meeting.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByCapacityGreaterThanEqual(int capacity);
    List<Room> findByStatus(String status);
    
    @Query("SELECT r FROM Room r WHERE " +
           "(:capacity IS NULL OR r.capacity >= :capacity) AND " +
           "(:status IS NULL OR r.status = :status)")
    List<Room> findRoomsMatchingCriteria(Integer capacity, String status);
}
