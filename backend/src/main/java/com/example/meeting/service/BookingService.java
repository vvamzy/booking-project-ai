package com.example.meeting.service;

import com.example.meeting.model.Booking;
import com.example.meeting.model.BookingHistory;
import com.example.meeting.repository.BookingRepository;
import com.example.meeting.repository.BookingHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingHistoryRepository bookingHistoryRepository;
    private final AiDecisionService aiDecisionService;
    private final com.example.meeting.repository.UserRepository userRepository;
    private final com.example.meeting.repository.RoomRepository roomRepository;
    private final com.example.meeting.repository.NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    public BookingService(BookingRepository bookingRepository,
                         BookingHistoryRepository bookingHistoryRepository,
                         AiDecisionService aiDecisionService,
                         com.example.meeting.repository.UserRepository userRepository,
                         com.example.meeting.repository.RoomRepository roomRepository,
                         com.example.meeting.repository.NotificationRepository notificationRepository,
                         NotificationService notificationService) {
        this.bookingRepository = bookingRepository;
        this.bookingHistoryRepository = bookingHistoryRepository;
        this.aiDecisionService = aiDecisionService;
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
    }


    private void validateBooking(Booking booking) {
        if (booking.getStartTime() == null || booking.getEndTime() == null) {
            throw new IllegalArgumentException("Start time and end time are required");
        }
        if (booking.getRoomId() == null) {
            throw new IllegalArgumentException("Room ID is required");
        }
        if (booking.getPurpose() == null || booking.getPurpose().trim().isEmpty()) {
            throw new IllegalArgumentException("Booking purpose is required");
        }
        if (booking.getAttendeesCount() == null || booking.getAttendeesCount() <= 0) {
            throw new IllegalArgumentException("Valid attendees count is required");
        }
        if (booking.getStartTime().isAfter(booking.getEndTime())) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
        if (booking.getStartTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Cannot book in the past");
        }
    }

    @Transactional
    public Booking createBooking(Booking booking) {
        try {
            // Validate required fields
            validateBooking(booking);
            
            // Set default priority if not provided
            if (booking.getPriority() == null) {
                booking.setPriority(3); // Default medium priority
            }
            
            // Set initial status if not provided
            if (booking.getStatus() == null) {
                booking.setStatus("NEW");
            }
            
            // Check room availability and get overlapping bookings
            List<Booking> overlaps = bookingRepository.findOverlappingBookings(
                    booking.getRoomId(), booking.getStartTime(), booking.getEndTime());

            // Get AI decision
            AiDecisionService.Decision decision;
            try {
                decision = aiDecisionService.decide(booking, overlaps);
            } catch (Exception e) {
                // If AI decision fails, set to pending for manual review
                decision = new AiDecisionService.Decision(
                    AiDecisionService.Action.REQUIRES_REVIEW,
                    0.0,
                    List.of("AI decision service error: " + e.getMessage())
                );
            }

            if (decision.getAction() == AiDecisionService.Action.AUTO_APPROVE) {
                booking.setStatus("APPROVED");
            } else if (decision.getAction() == AiDecisionService.Action.AUTO_REJECT) {
                booking.setStatus("REJECTED");
            } else {
                booking.setStatus("PENDING");
            }
            // Purpose clarity validation: if LLM/heuristics say purpose unclear, append suggestions but do not
            // force non-executive rooms to PENDING (executive override handled separately).
            try {
                java.util.Map<String,Object> pv = aiDecisionService.validatePurpose(booking.getPurpose());
                if (pv != null && pv.containsKey("clear") && Boolean.FALSE.equals(pv.get("clear"))) {
                    // attach suggestions to rationale
                    Object s = pv.get("suggestions");
                    String suggText = "";
                    if (s instanceof java.util.List) {
                        suggText = String.join("; ", (java.util.List<String>)s);
                    }
                    String prev = booking.getDecisionRationale() == null ? "" : booking.getDecisionRationale();
                    booking.setDecisionRationale((prev.isEmpty() ? "" : prev + "; ") + "Purpose unclear: " + suggText);
                    booking.setDecisionConfidence(Math.min(booking.getDecisionConfidence() == null ? 0.5 : booking.getDecisionConfidence(), 0.5));
                }
            } catch (Exception ignored) {}
            // Enforce executive rooms always require manual admin approval
            try {
                if (booking.getRoomId() != null) {
                    com.example.meeting.model.Room room = roomRepository.findById(booking.getRoomId()).orElse(null);
                    if (room != null) {
                        String name = room.getName() == null ? "" : room.getName().toLowerCase();
                        String status = room.getStatus() == null ? "" : room.getStatus().toLowerCase();
                        if (name.contains("executive") || name.contains("executive boardroom") || status.equals("special")) {
                            // override any AUTO_APPROVE into PENDING
                            if ("APPROVED".equals(booking.getStatus())) {
                                booking.setStatus("PENDING");
                                // prepend rationale for why it was forced to pending
                                String prevRationale = booking.getDecisionRationale() == null ? "" : booking.getDecisionRationale();
                                booking.setDecisionRationale((prevRationale.isEmpty() ? "" : prevRationale + "; ") + "Executive room requires admin approval");
                                booking.setDecisionConfidence(Math.min(booking.getDecisionConfidence() == null ? 0.0 : booking.getDecisionConfidence(), 0.6));
                            }
                        }
                    }
                }
            } catch (Exception ignored) { }
            booking.setDecisionConfidence(decision.getConfidence());
            booking.setDecisionRationale(String.join("; ", decision.getRationale()));

            // If userId not provided, infer from authenticated principal
            if (booking.getUserId() == null) {
                org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                    String username = auth.getName();
                    java.util.Optional<com.example.meeting.model.UserAccount> ua = userRepository.findByUsername(username);
                    ua.ifPresent(user -> booking.setUserId(user.getId()));
                }
            }

            Booking saved = bookingRepository.save(booking);

            // Schedule reminders for the user based on configured offsets (default 30m,60m,1440m)
            try {
                java.util.List<Integer> offsets = java.util.Arrays.asList(30, 60, 1440);
                
                java.time.LocalDateTime start = saved.getStartTime();
                if (start != null) {
                    for (Integer m : offsets) {
                        java.time.LocalDateTime when = start.minusMinutes(m);
                        if (when.isAfter(java.time.LocalDateTime.now())) {
                            // build payload JSON
                            com.fasterxml.jackson.databind.node.ObjectNode payload = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
                            payload.put("toUserId", saved.getUserId() == null ? 0 : saved.getUserId());
                            payload.put("subject", "Upcoming meeting reminder");
                            StringBuilder body = new StringBuilder();
                            body.append("Meeting: ").append(saved.getPurpose()).append("\n");
                            com.example.meeting.model.Room rm = roomRepository.findById(saved.getRoomId()).orElse(null);
                            if (rm != null) {
                                body.append("Room: ").append(rm.getName()).append(" (Location: ").append(rm.getLocation()).append(")\n");
                            }
                            body.append("Starts: ").append(saved.getStartTime().toString()).append("\n");
                            payload.put("body", body.toString());
                            notificationService.scheduleNotification(saved.getUserId(), saved.getId(), "REMINDER", "IN_APP", when, payload.toString());
                        }
                    }
                }
            } catch (Exception ignored) {}

            // Create booking history entry
            BookingHistory history = new BookingHistory(
                saved,
                "NEW",
                saved.getStatus(),
                "SYSTEM",
                "Initial booking creation",
                decision.getConfidence(),
                String.join("; ", decision.getRationale())
            );
            bookingHistoryRepository.save(history);

            // Notify facilities/tech team if booking requires special services
            try {
                boolean needsAv = false;
                boolean needsVideo = false;
                boolean needsCatering = false;
                if (saved.getRequiredFacilities() != null) {
                    for (String req : saved.getRequiredFacilities()) {
                        String r = req == null ? "" : req.toLowerCase();
                        if (r.contains("av") || r.contains("audio") || r.contains("microphone")) needsAv = true;
                        if (r.contains("video") || r.contains("zoom") || r.contains("conference")) needsVideo = true;
                        if (r.contains("cater")) needsCatering = true;
                    }
                }
                // also inspect room equipment
                com.example.meeting.model.Room rm = roomRepository.findById(saved.getRoomId()).orElse(null);
                if (rm != null && rm.getEquipment() != null) {
                    for (com.example.meeting.model.Equipment eq : rm.getEquipment()) {
                        String t = eq.getName() == null ? "" : eq.getName().toLowerCase();
                        if (t.contains("projector") || t.contains("microphone") || t.contains("pa")) needsAv = true;
                        if (t.contains("camera") || t.contains("video")) needsVideo = true;
                    }
                }
                if (needsAv || needsVideo || needsCatering) {
                    // schedule immediate facilities notification (1 hour before by default)
                    java.time.LocalDateTime when = saved.getStartTime().minusHours(1);
                    if (when.isBefore(java.time.LocalDateTime.now())) when = java.time.LocalDateTime.now().plusMinutes(1);
                    com.fasterxml.jackson.databind.node.ObjectNode payload = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
                    payload.put("subject", "Facilities support required for upcoming meeting");
                    StringBuilder b = new StringBuilder();
                    b.append("Booking: ").append(saved.getPurpose()).append("\nRoom: ");
                    b.append(rm == null ? saved.getRoomId() : rm.getName()).append("\nStarts: ").append(saved.getStartTime().toString()).append("\n");
                    if (needsAv) b.append("Needs: AV setup\n");
                    if (needsVideo) b.append("Needs: Video Conferencing setup\n");
                    if (needsCatering) b.append("Needs: Catering\n");
                    payload.put("body", b.toString());
                    // schedule for configured facility emails
                    String[] emails = new String[] {"facilities@company.local", "it-support@company.local"};
                    for (String e : emails) {
                        com.fasterxml.jackson.databind.node.ObjectNode p2 = payload.deepCopy();
                        p2.put("to", e);
                        notificationService.scheduleNotification(null, saved.getId(), "FACILITIES", "EMAIL", when, p2.toString());
                    }
                }
            } catch (Exception ignored) {}

            return saved;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create booking: " + e.getMessage(), e);
        }
    }

    @Transactional
    public Booking updateBookingStatus(Long bookingId, String newStatus, String changedBy, String reason) {
        Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
        if (bookingOpt.isPresent()) {
            Booking booking = bookingOpt.get();
            String oldStatus = booking.getStatus();
            booking.setStatus(newStatus);

            // Create history entry
            BookingHistory history = new BookingHistory(
                booking,
                oldStatus,
                newStatus,
                changedBy,
                reason,
                null,
                null
            );
            bookingHistoryRepository.save(history);

            return bookingRepository.save(booking);
        }
        throw new RuntimeException("Booking not found with id: " + bookingId);
    }

    public List<Booking> getPendingBookings() {
        return bookingRepository.findPendingBookings();
    }

    public List<Booking> getRoomBookings(Long roomId, String status) {
        if (status != null) {
            return bookingRepository.findByRoomIdAndStatus(roomId, status);
        }
        return bookingRepository.findByRoomId(roomId);
    }

    public List<BookingHistory> getBookingHistory(Long bookingId) {
        return bookingHistoryRepository.findByBookingIdOrderByChangedAtDesc(bookingId);
    }

    @Transactional
    public void cancelBooking(Long bookingId, String cancelledBy, String reason) {
        Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
        if (bookingOpt.isPresent()) {
            Booking booking = bookingOpt.get();
            if ("CANCELLED".equals(booking.getStatus())) {
                throw new IllegalStateException("Booking is already cancelled");
            }
            String oldStatus = booking.getStatus();
            booking.setStatus("CANCELLED");

            // Create history entry
            BookingHistory history = new BookingHistory(
                booking,
                oldStatus,
                "CANCELLED",
                cancelledBy,
                reason,
                null,
                null
            );
            bookingHistoryRepository.save(history);
            bookingRepository.save(booking);
        } else {
            throw new RuntimeException("Booking not found with id: " + bookingId);
        }
    }

    public List<Booking> getHighPriorityBookings(Integer minPriority) {
        return bookingRepository.findByPriorityGreaterThanEqual(minPriority);
    }

    public java.util.Optional<com.example.meeting.model.UserAccount> findUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public List<Booking> getBookingsForUser(Long userId) {
        return bookingRepository.findByUserId(userId);
    }

    public Optional<Booking> getBookingById(Long id) {
        return bookingRepository.findById(id);
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public void deleteBooking(Long id) {
        bookingRepository.deleteById(id);
    }

    public Booking updateBooking(Long id, Booking bookingDetails) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        booking.setRoomId(bookingDetails.getRoomId());
        booking.setStartTime(bookingDetails.getStartTime());
        booking.setEndTime(bookingDetails.getEndTime());
        booking.setUserId(bookingDetails.getUserId());
        return bookingRepository.save(booking);
    }
}