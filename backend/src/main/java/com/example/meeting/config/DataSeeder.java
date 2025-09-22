package com.example.meeting.config;

import com.example.meeting.model.Room;
import com.example.meeting.model.Equipment;
import com.example.meeting.repository.RoomRepository;
import com.example.meeting.repository.EquipmentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Configuration
public class DataSeeder {

    @Bean
    public CommandLineRunner seed(
            RoomRepository roomRepository,
            EquipmentRepository equipmentRepository,
            com.example.meeting.repository.BookingRepository bookingRepository,
            com.example.meeting.repository.UserRepository userRepository,
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        return args -> {
            if (roomRepository.count() == 0) {
                Map<Long, Room> rooms = new HashMap<>();

                rooms.put(1L, createRoom(1L, "Executive Boardroom", 20, "Floor 10 West Wing", "special"));
                rooms.put(2L, createRoom(2L, "Innovation Lab", 15, "Floor 8 North Wing", "available"));
                rooms.put(3L, createRoom(3L, "Collaboration Space", 12, "Floor 6 East Wing", "available"));
                rooms.put(4L, createRoom(4L, "Focus Room Alpha", 4, "Floor 4 South Wing", "available"));
                rooms.put(5L, createRoom(5L, "Conference Room Beta", 30, "Floor 12 Central", "available"));
                rooms.put(6L, createRoom(6L, "Brainstorm Pod", 6, "Floor 7 Creative Zone", "available"));
                rooms.put(7L, createRoom(7L, "Digital Studio", 10, "Floor 9 Tech Hub", "available"));
                rooms.put(8L, createRoom(8L, "Training Room", 25, "Floor 3 Learning Center", "available"));
                // Additional seeded rooms for AI testing
                rooms.put(9L, createRoom(9L, "Small Huddle 1", 2, "Floor 4 South Wing", "available"));
                rooms.put(10L, createRoom(10L, "Small Huddle 2", 3, "Floor 4 South Wing", "available"));
                rooms.put(11L, createRoom(11L, "Auditorium A", 200, "Ground Floor Auditorium", "available"));
                rooms.put(12L, createRoom(12L, "Executive Boardroom - East", 18, "Floor 11 East Wing", "available"));
                rooms.put(13L, createRoom(13L, "Medium Conference C", 40, "Floor 12 Central", "available"));
                rooms.put(14L, createRoom(14L, "Classroom 101", 50, "Floor 3 Learning Center", "available"));
                rooms.put(15L, createRoom(15L, "Theatre Hall", 500, "Ground Floor Theatre", "available"));

                Map<Long, Equipment> equipment = new HashMap<>();

                equipment.put(1L, createEquipment(1L, "Interactive Whiteboard", "DISPLAY", "AVAILABLE"));
                equipment.put(2L, createEquipment(2L, "4K Video Conferencing", "VIDEO", "AVAILABLE"));
                equipment.put(3L, createEquipment(3L, "Surround Sound System", "AUDIO", "AVAILABLE"));
                equipment.put(4L, createEquipment(4L, "Wireless Presenter", "CONTROL", "AVAILABLE"));
                equipment.put(5L, createEquipment(5L, "360 Conference Camera", "VIDEO", "AVAILABLE"));
                equipment.put(6L, createEquipment(6L, "Digital Drawing Tablet", "INPUT", "AVAILABLE"));
                equipment.put(7L, createEquipment(7L, "Smart Display", "DISPLAY", "AVAILABLE"));
                equipment.put(8L, createEquipment(8L, "Wireless Microphone Set", "AUDIO", "AVAILABLE"));
                equipment.put(9L, createEquipment(9L, "Document Camera", "VIDEO", "AVAILABLE"));
                equipment.put(10L, createEquipment(10L, "Mobile Collaboration Cart", "FURNITURE", "AVAILABLE"));

                equipment.values().forEach(equipmentRepository::save);

                linkRoomEquipment(rooms.get(1L), equipment, new long[]{1, 2, 3, 4});
                linkRoomEquipment(rooms.get(2L), equipment, new long[]{1, 6, 7, 8});
                linkRoomEquipment(rooms.get(3L), equipment, new long[]{5, 7, 8, 10});
                linkRoomEquipment(rooms.get(4L), equipment, new long[]{7, 4});
                linkRoomEquipment(rooms.get(5L), equipment, new long[]{1, 2, 3, 4, 8});
                linkRoomEquipment(rooms.get(6L), equipment, new long[]{1, 6, 10});
                linkRoomEquipment(rooms.get(7L), equipment, new long[]{2, 5, 6, 9});
                linkRoomEquipment(rooms.get(8L), equipment, new long[]{1, 2, 3, 4, 9});
                // link additional rooms
                linkRoomEquipment(rooms.get(9L), equipment, new long[]{7});
                linkRoomEquipment(rooms.get(10L), equipment, new long[]{7});
                linkRoomEquipment(rooms.get(11L), equipment, new long[]{1,2,3,5,8});
                linkRoomEquipment(rooms.get(12L), equipment, new long[]{1,2,4});
                linkRoomEquipment(rooms.get(13L), equipment, new long[]{1,2,3});
                linkRoomEquipment(rooms.get(14L), equipment, new long[]{1,7,10});
                linkRoomEquipment(rooms.get(15L), equipment, new long[]{2,3,5});

                rooms.values().forEach(roomRepository::save);

                // generate sample bookings over next 30 days
                java.time.LocalDate today = java.time.LocalDate.now();
                java.util.Random rand = new java.util.Random(42);
                for (int i = 0; i < 100; i++) {
                    int dayOffset = rand.nextInt(30);
                    java.time.LocalDate d = today.plusDays(dayOffset);
                    int startHour = 8 + rand.nextInt(9); // 8-16
                    java.time.LocalDateTime start = d.atTime(startHour, 0);
                    java.time.LocalDateTime end = start.plusHours(1 + rand.nextInt(3));
                    long roomIndex = 1 + rand.nextInt(8);
                    com.example.meeting.model.Booking b = new com.example.meeting.model.Booking();
                    b.setRoomId(roomIndex);
                    b.setUserId(1L + rand.nextInt(3));
                    b.setStartTime(start);
                    b.setEndTime(end);
                    // special rooms require admin approval
                    if (roomIndex == 1L) {
                        b.setStatus("PENDING_APPROVAL");
                    } else {
                        b.setStatus("APPROVED");
                    }
                    b.setPurpose("Team sync on project " + (rand.nextInt(20) + 1));
                    b.setAttendeesCount(1 + rand.nextInt(20));
                    b.setPriority(1 + rand.nextInt(5));
                    bookingRepository.save(b);
                }
                // seed users
                if (userRepository.count() == 0) {
                    com.example.meeting.model.UserAccount alice = new com.example.meeting.model.UserAccount("alice", passwordEncoder.encode("alicepass"), "ROLE_EMPLOYEE");
                    com.example.meeting.model.UserAccount bob = new com.example.meeting.model.UserAccount("bob", passwordEncoder.encode("bobpass"), "ROLE_MANAGER");
                    com.example.meeting.model.UserAccount admin = new com.example.meeting.model.UserAccount("admin", passwordEncoder.encode("adminpass"), "ROLE_ADMIN");
                    userRepository.save(alice);
                    userRepository.save(bob);
                    userRepository.save(admin);
                }
            }
        };
    }

    private Room createRoom(Long id, String name, int capacity, String location, String status) {
        Room room = new Room();
        room.setId(id);
        room.setName(name);
        room.setCapacity(capacity);
        room.setLocation(location);
        // normalize to lowercase for frontend checks
        room.setStatus(status == null ? "available" : status.toLowerCase());
        room.setEquipment(new HashSet<>());
        return room;
    }

    private Equipment createEquipment(Long id, String name, String type, String status) {
        Equipment equipment = new Equipment();
        equipment.setId(id);
        equipment.setName(name);
        equipment.setType(type);
        equipment.setStatus(status);
        equipment.setRooms(new HashSet<>());
        return equipment;
    }

    private void linkRoomEquipment(Room room, Map<Long, Equipment> equipmentMap, long[] equipmentIds) {
        Set<Equipment> roomEquipment = new HashSet<>();
        for (long equipmentId : equipmentIds) {
            Equipment equipment = equipmentMap.get(equipmentId);
            if (equipment != null) {
                roomEquipment.add(equipment);
                equipment.getRooms().add(room);
            }
        }
        room.setEquipment(roomEquipment);
    }
}

