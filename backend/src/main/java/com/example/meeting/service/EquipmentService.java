package com.example.meeting.service;

import com.example.meeting.model.Equipment;
import com.example.meeting.repository.EquipmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;

    @Autowired
    public EquipmentService(EquipmentRepository equipmentRepository) {
        this.equipmentRepository = equipmentRepository;
    }

    public List<Equipment> getEquipmentByRoom(Long roomId) {
        return equipmentRepository.findByRoomId(roomId);
    }

    public List<Equipment> getAvailableEquipmentByRoom(Long roomId) {
        return equipmentRepository.findByRoomIdAndStatus(roomId, "AVAILABLE");
    }

    public List<Equipment> getEquipmentByStatus(String status) {
        return equipmentRepository.findByStatus(status);
    }

    @Transactional
    public Equipment addEquipment(Equipment equipment) {
        return equipmentRepository.save(equipment);
    }

    @Transactional
    public Equipment updateEquipmentStatus(Long equipmentId, String newStatus) {
        Optional<Equipment> equipment = equipmentRepository.findById(equipmentId);
        if (equipment.isPresent()) {
            Equipment eq = equipment.get();
            eq.setStatus(newStatus);
            return equipmentRepository.save(eq);
        }
        throw new RuntimeException("Equipment not found with id: " + equipmentId);
    }

    @Transactional
    public void removeEquipment(Long equipmentId) {
        equipmentRepository.deleteById(equipmentId);
    }

    public List<Equipment> getAllEquipment() {
        return equipmentRepository.findAll();
    }

    @Transactional
    public Equipment assignEquipmentToRoom(Long equipmentId, Long roomId) {
        Optional<Equipment> equipment = equipmentRepository.findById(equipmentId);
        if (equipment.isPresent()) {
            Equipment eq = equipment.get();
            // Note: Room assignment should be handled through Room entity
            return equipmentRepository.save(eq);
        }
        throw new RuntimeException("Equipment not found with id: " + equipmentId);
    }

    @Transactional
    public Equipment markEquipmentForMaintenance(Long equipmentId, String reason) {
        Optional<Equipment> equipment = equipmentRepository.findById(equipmentId);
        if (equipment.isPresent()) {
            Equipment eq = equipment.get();
            eq.setStatus("MAINTENANCE");
            // Could add maintenance tracking fields if needed
            return equipmentRepository.save(eq);
        }
        throw new RuntimeException("Equipment not found with id: " + equipmentId);
    }
}