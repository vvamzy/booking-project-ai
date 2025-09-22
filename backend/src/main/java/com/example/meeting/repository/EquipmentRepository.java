package com.example.meeting.repository;

import com.example.meeting.model.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    @Query("SELECT e FROM Equipment e JOIN e.rooms r WHERE r.id = :roomId")
    List<Equipment> findByRoomId(@Param("roomId") Long roomId);

    @Query("SELECT e FROM Equipment e JOIN e.rooms r WHERE r.id = :roomId AND e.status = :status")
    List<Equipment> findByRoomIdAndStatus(@Param("roomId") Long roomId, @Param("status") String status);

    List<Equipment> findByStatus(String status);
}