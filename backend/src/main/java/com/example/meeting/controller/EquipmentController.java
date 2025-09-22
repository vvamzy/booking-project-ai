package com.example.meeting.controller;

import com.example.meeting.model.Equipment;
import com.example.meeting.service.EquipmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/equipment")
public class EquipmentController {
    
    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @GetMapping
    public ResponseEntity<List<Equipment>> getAllEquipment() {
        return ResponseEntity.ok(equipmentService.getAllEquipment());
    }

    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<Equipment>> getEquipmentByRoom(@PathVariable Long roomId) {
        return ResponseEntity.ok(equipmentService.getEquipmentByRoom(roomId));
    }

    @GetMapping("/available/room/{roomId}")
    public ResponseEntity<List<Equipment>> getAvailableEquipmentByRoom(@PathVariable Long roomId) {
        return ResponseEntity.ok(equipmentService.getAvailableEquipmentByRoom(roomId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Equipment>> getEquipmentByStatus(@PathVariable String status) {
        return ResponseEntity.ok(equipmentService.getEquipmentByStatus(status));
    }

    @PostMapping
    public ResponseEntity<Equipment> addEquipment(@RequestBody Equipment equipment) {
        return ResponseEntity.ok(equipmentService.addEquipment(equipment));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Equipment> updateEquipmentStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return ResponseEntity.ok(equipmentService.updateEquipmentStatus(id, status));
    }

    @PutMapping("/{id}/maintenance")
    public ResponseEntity<Equipment> markForMaintenance(
            @PathVariable Long id,
            @RequestParam String reason) {
        return ResponseEntity.ok(equipmentService.markEquipmentForMaintenance(id, reason));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeEquipment(@PathVariable Long id) {
        equipmentService.removeEquipment(id);
        return ResponseEntity.ok().build();
    }
}