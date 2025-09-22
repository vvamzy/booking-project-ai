package com.example.meeting.service;

import com.example.meeting.model.Equipment;
import com.example.meeting.model.Room;
import com.example.meeting.repository.EquipmentRepository;
import com.example.meeting.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class RoomService {
    
    private final RoomRepository roomRepository;
    private final EquipmentRepository equipmentRepository;
    private final EquipmentService equipmentService;
    private final com.example.meeting.repository.BookingRepository bookingRepository;

    public RoomService(RoomRepository roomRepository,
                      EquipmentRepository equipmentRepository,
                      EquipmentService equipmentService,
                      com.example.meeting.repository.BookingRepository bookingRepository) {
        this.roomRepository = roomRepository;
        this.equipmentRepository = equipmentRepository;
        this.equipmentService = equipmentService;
        this.bookingRepository = bookingRepository;
    }

    public java.util.List<Room> findAvailableRooms(java.time.LocalDateTime start, java.time.LocalDateTime end, String location) {
        java.util.List<Room> all = roomRepository.findAll();
        java.util.List<Room> res = new java.util.ArrayList<>();
        for (Room r : all) {
            if (location != null && !location.isBlank() && (r.getLocation() == null || !r.getLocation().toLowerCase().contains(location.toLowerCase()))) continue;
            var overlaps = bookingRepository.findOverlappingBookings(r.getId(), start, end);
            if (overlaps == null || overlaps.isEmpty()) {
                res.add(r);
            }
        }
        return res;
    }

    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    public Optional<Room> getRoomById(Long id) {
        return roomRepository.findById(id);
    }

    @Transactional
    public Room createRoom(Room room) {
        validateRoom(room);
        return roomRepository.save(room);
    }

    public List<Room> findRooms(Integer minCapacity, String status, List<String> equipmentTypes) {
        List<Room> rooms = roomRepository.findAll();

        return rooms.stream()
            .filter(room -> minCapacity == null || room.getCapacity() >= minCapacity)
            .filter(room -> status == null || room.getStatus().equals(status))
            .filter(room -> equipmentTypes == null || equipmentTypes.isEmpty() || 
                    room.getEquipment().stream()
                        .anyMatch(eq -> equipmentTypes.contains(eq.getType())))
            .toList();
    }

    @Transactional
    public Room updateRoom(Long id, Room roomDetails) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found with id: " + id));

        room.setName(roomDetails.getName());
        room.setLocation(roomDetails.getLocation());
        room.setCapacity(roomDetails.getCapacity());
        room.setStatus(roomDetails.getStatus());
        room.setCapacity(roomDetails.getCapacity());
        room.setStatus(roomDetails.getStatus());

        return roomRepository.save(room);
    }

    @Transactional
    public void deleteRoom(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found with id: " + id));
        
        // First detach all equipment from the room
        room.setEquipment(new HashSet<>());
        roomRepository.save(room);
        
        roomRepository.delete(room);
    }

    public List<Room> getRoomsByCapacity(int minCapacity) {
        return roomRepository.findByCapacityGreaterThanEqual(minCapacity);
    }

    @Transactional
    public Room addEquipmentToRoom(Long roomId, Equipment equipment) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found with id: " + roomId));
        
        Equipment savedEquipment = equipmentRepository.save(equipment);
        
        if (room.getEquipment() == null) {
            room.setEquipment(new HashSet<>());
        }
        room.getEquipment().add(savedEquipment);
        
        return roomRepository.save(room);
    }

    @Transactional
    public Room removeEquipmentFromRoom(Long roomId, Long equipmentId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found with id: " + roomId));
        
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new RuntimeException("Equipment not found with id: " + equipmentId));
        
        if (!room.getEquipment().contains(equipment)) {
            throw new IllegalArgumentException("Equipment does not belong to this room");
        }
        
        room.getEquipment().remove(equipment);
        return roomRepository.save(room);
    }

    public Set<Equipment> getRoomEquipment(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found with id: " + roomId));
        return room.getEquipment();
    }

    private void validateRoom(Room room) {
        if (room.getName() == null || room.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Room name is required");
        }
        if (room.getLocation() == null || room.getLocation().trim().isEmpty()) {
            throw new IllegalArgumentException("Room location is required");
        }
        if (room.getCapacity() <= 0) {
            throw new IllegalArgumentException("Room capacity must be greater than 0");
        }
    }
}