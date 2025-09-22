package com.example.meeting.controller;

import com.example.meeting.model.Equipment;
import com.example.meeting.model.Room;
import com.example.meeting.service.RoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {
    
    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    public ResponseEntity<List<Room>> getAllRooms(
            @RequestParam(required = false) Integer minCapacity,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) List<String> equipmentTypes) {

        return ResponseEntity.ok(roomService.findRooms(minCapacity, status, equipmentTypes));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Room> getRoomById(@PathVariable Long id) {
        return roomService.getRoomById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Room> createRoom(@RequestBody Room room) {
        return ResponseEntity.ok(roomService.createRoom(room));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Room> updateRoom(@PathVariable Long id, @RequestBody Room room) {
        return ResponseEntity.ok(roomService.updateRoom(id, room));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/with-projector")
    public ResponseEntity<List<Room>> getRoomsWithProjector() {
        return ResponseEntity.ok(roomService.findRooms(null, null, List.of("PROJECTOR")));
    }

    @GetMapping("/with-whiteboard")
    public ResponseEntity<List<Room>> getRoomsWithWhiteboard() {
        return ResponseEntity.ok(roomService.findRooms(null, null, List.of("WHITEBOARD")));
    }

    @GetMapping("/with-video-conferencing")
    public ResponseEntity<List<Room>> getRoomsWithVideoConferencing() {
        return ResponseEntity.ok(roomService.findRooms(null, null, List.of("VIDEO_CONFERENCING")));
    }

    @GetMapping("/{id}/equipment")
    public ResponseEntity<Set<Equipment>> getRoomEquipment(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.getRoomEquipment(id));
    }

    @GetMapping("/available")
    public ResponseEntity<java.util.List<Room>> getAvailableRooms(
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam(required = false) String location) {
        java.time.LocalDateTime s = java.time.LocalDateTime.parse(start);
        java.time.LocalDateTime e = java.time.LocalDateTime.parse(end);
        return ResponseEntity.ok(roomService.findAvailableRooms(s, e, location));
    }

    @PostMapping("/{id}/equipment")
    public ResponseEntity<Room> addEquipmentToRoom(@PathVariable Long id, @RequestBody Equipment equipment) {
        return ResponseEntity.ok(roomService.addEquipmentToRoom(id, equipment));
    }

    @DeleteMapping("/{roomId}/equipment/{equipmentId}")
    public ResponseEntity<Room> removeEquipmentFromRoom(@PathVariable Long roomId, @PathVariable Long equipmentId) {
        return ResponseEntity.ok(roomService.removeEquipmentFromRoom(roomId, equipmentId));
    }
}
