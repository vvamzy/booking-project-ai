package com.example.meeting.controller;

import com.example.meeting.model.Booking;
import com.example.meeting.model.BookingHistory;
import com.example.meeting.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final com.example.meeting.service.AiDecisionService aiDecisionService;
    private final com.example.meeting.repository.BookingRepository bookingRepository;
    private final com.example.meeting.repository.RoomRepository roomRepository;

    public BookingController(BookingService bookingService, com.example.meeting.service.AiDecisionService aiDecisionService,
                             com.example.meeting.repository.BookingRepository bookingRepository,
                             com.example.meeting.repository.RoomRepository roomRepository) {
        this.bookingService = bookingService;
        this.aiDecisionService = aiDecisionService;
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
    }

    @GetMapping
    public ResponseEntity<List<Booking>> getAllBookings(java.security.Principal principal) {
        // If admin, return all bookings
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return ResponseEntity.ok(bookingService.getAllBookings());
        }
        if (principal == null) return ResponseEntity.status(401).build();
        // principal name is username; map to userId via service
        java.util.Optional<com.example.meeting.model.UserAccount> uaOpt = bookingService.findUserByUsername(principal.getName());
        if (uaOpt.isEmpty()) return ResponseEntity.status(401).build();
        Long userId = uaOpt.get().getId();
        return ResponseEntity.ok(bookingService.getBookingsForUser(userId));
    }

    @PostMapping
    public ResponseEntity<Booking> createBooking(@RequestBody Booking booking, java.security.Principal principal) {
        // require authenticated principal to assign ownership
        if (principal == null) return ResponseEntity.status(401).build();
        // Map principal username -> userId via service
        java.util.Optional<com.example.meeting.model.UserAccount> uaOpt = bookingService.findUserByUsername(principal.getName());
        if (uaOpt.isEmpty()) return ResponseEntity.status(401).build();
        Long userId = uaOpt.get().getId();
        if (booking.getUserId() == null) booking.setUserId(userId);

        Booking saved = bookingService.createBooking(booking);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateBooking(@RequestBody Booking booking) {
        if (booking == null) return ResponseEntity.badRequest().body(java.util.Map.of("error", "Missing booking payload"));
        com.example.meeting.service.AiDecisionService.Decision d = aiDecisionService.decide(booking, null);
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("action", d.getAction().name());
        resp.put("confidence", d.getConfidence());
        resp.put("rationale", d.getRationale());
        // simple valid flag: AUTO_APPROVE or REQUIRES_REVIEW are considered not invalid; AUTO_REJECT marks invalid
        resp.put("valid", d.getAction() != com.example.meeting.service.AiDecisionService.Action.AUTO_REJECT);
        // If AI provided suggestions include them
        try {
            resp.put("suggestions", d.getSuggestions() == null ? java.util.List.of() : d.getSuggestions());
        } catch (Throwable ex) {
            resp.put("suggestions", java.util.List.of());
        }
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<Booking>> getPendingBookings() {
        // Only admins should access via security config, but double-check
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(bookingService.getPendingBookings());
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Booking> approveBooking(@PathVariable Long id, @RequestBody(required = false) java.util.Map<String,String> body) {
    org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
    boolean isAdmin = auth != null && auth.getAuthorities() != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    if (!isAdmin) return ResponseEntity.status(403).build();
    String changedBy = auth != null ? auth.getName() : "system";
        Booking updated = bookingService.updateBookingStatus(id, "APPROVED", changedBy, body == null ? null : body.get("comments"));
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Booking> rejectBooking(@PathVariable Long id, @RequestBody(required = false) java.util.Map<String,String> body) {
    org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
    boolean isAdmin = auth != null && auth.getAuthorities() != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    if (!isAdmin) return ResponseEntity.status(403).build();
    String changedBy = auth != null ? auth.getName() : "system";
        Booking updated = bookingService.updateBookingStatus(id, "REJECTED", changedBy, body == null ? null : body.get("comments"));
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<Booking>> getRoomBookings(
            @PathVariable Long roomId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(bookingService.getRoomBookings(roomId, status));
    }

    @GetMapping("/priority")
    public ResponseEntity<List<Booking>> getHighPriorityBookings(
            @RequestParam(defaultValue = "3") Integer minPriority) {
        return ResponseEntity.ok(bookingService.getHighPriorityBookings(minPriority));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<BookingHistory>> getBookingHistory(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getBookingHistory(id));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Booking> updateBookingStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam String changedBy,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(bookingService.updateBookingStatus(id, status, changedBy, reason));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelBooking(
            @PathVariable Long id,
            @RequestParam String cancelledBy,
            @RequestParam(required = false) String reason) {
        bookingService.cancelBooking(id, cancelledBy, reason);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/ai-decision")
    public ResponseEntity<?> getAiDecision(@PathVariable Long id) {
        return bookingService.getBookingById(id)
                .map(b -> {
                    com.example.meeting.service.AiDecisionService.Decision d = aiDecisionService.decide(b, null);
                    java.util.Map<String, Object> resp = new java.util.HashMap<>();
                    resp.put("decision", d.getAction().name());
                    resp.put("confidence", d.getConfidence());
                    resp.put("rationale", d.getRationale());
                    return ResponseEntity.ok().body(resp);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBooking(@PathVariable Long id) {
        return bookingService.getBookingById(id)
                .map(b -> ResponseEntity.ok().body(b))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/analyze")
    public ResponseEntity<?> analyzeBooking(@PathVariable Long id) {
        return bookingService.getBookingById(id)
                .map(b -> {
                    com.example.meeting.service.AiDecisionService.Decision d = aiDecisionService.decide(b, null);
                    java.util.Map<String, Object> resp = new java.util.HashMap<>();
                    resp.put("decision", d.getAction().name());
                    resp.put("confidence", d.getConfidence());
                    resp.put("rationale", d.getRationale());
                    return ResponseEntity.ok().body(resp);
                })
                .orElse(ResponseEntity.notFound().build());
    }

        @GetMapping("/availability")
        public ResponseEntity<?> checkAvailability(
                @RequestParam Long roomId,
                @RequestParam String start,
                @RequestParam String end) {
            try {
            java.time.LocalDateTime s = parseDateTime(start);
            java.time.LocalDateTime e = parseDateTime(end);
                java.util.List<Booking> overlaps = bookingService.getRoomBookings(roomId, null).stream()
                        .filter(b -> !"CANCELLED".equals(b.getStatus()) && (
                                (b.getStartTime().isBefore(e) && b.getEndTime().isAfter(s))
                        ))
                        .toList();
                boolean available = overlaps.isEmpty();
                java.util.Map<String, Object> resp = new java.util.HashMap<>();
                resp.put("available", available);
                resp.put("overlaps", overlaps);
                return ResponseEntity.ok(resp);
            } catch (Exception ex) {
                return ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
            }
        }

        @GetMapping("/suggest")
        public ResponseEntity<?> suggestAlternatives(
                @RequestParam Long roomId,
                @RequestParam String start,
                @RequestParam String end,
                @RequestParam(required = false) Integer capacity
        ) {
            try {
        java.time.LocalDateTime s = parseDateTime(start);
        java.time.LocalDateTime e = parseDateTime(end);
                // If requested room is busy, find next available slots for same room (simple incremental search)
                java.time.Duration duration = java.time.Duration.between(s, e);
                java.time.LocalDateTime cursor = s.plusMinutes(30);
                java.time.LocalDateTime limit = s.plusDays(7);
                java.util.List<java.util.Map<String, java.time.LocalDateTime>> slots = new java.util.ArrayList<>();
                while (cursor.isBefore(limit) && slots.size() < 5) {
                    java.util.List<Booking> overlaps = bookingRepository.findOverlappingBookings(roomId, cursor, cursor.plus(duration));
                    if (overlaps.isEmpty()) {
                        java.util.Map<String, java.time.LocalDateTime> slot = new java.util.HashMap<>();
                        slot.put("start", cursor);
                        slot.put("end", cursor.plus(duration));
                        slots.add(slot);
                    }
                    cursor = cursor.plusMinutes(30);
                }

                // Find alternate rooms matching capacity
                java.util.List<com.example.meeting.model.Room> alternates = roomRepository.findByCapacityGreaterThanEqual(capacity == null ? 1 : capacity);
                java.util.List<java.util.Map<String, Object>> scoredAlts = new java.util.ArrayList<>();
                // original requested room equipment set for similarity calculation
                com.example.meeting.model.Room requestedRoom = roomRepository.findById(roomId).orElse(null);
                java.util.Set<String> requestedEquipNames = new java.util.HashSet<>();
                if (requestedRoom != null && requestedRoom.getEquipment() != null) {
                    requestedRoom.getEquipment().forEach(eq -> requestedEquipNames.add(eq.getName()));
                }

                for (com.example.meeting.model.Room r : alternates) {
                    if (r.getId().equals(roomId)) continue;

                    // compute equipment similarity with equipment-type weights
                    java.util.Map<String, Double> typeWeights = new java.util.HashMap<>();
                    typeWeights.put("VIDEO", 1.0);
                    typeWeights.put("DISPLAY", 0.9);
                    typeWeights.put("AUDIO", 0.7);
                    typeWeights.put("CONTROL", 0.5);
                    typeWeights.put("INPUT", 0.6);
                    typeWeights.put("FURNITURE", 0.4);
                    typeWeights.put("OTHER", 0.5);

                    double weightedInter = 0.0;
                    double weightedUnion = 0.0;
                    java.util.Set<String> otherEquipNames = new java.util.HashSet<>();
                    if (r.getEquipment() != null) {
                        r.getEquipment().forEach(eq -> otherEquipNames.add(eq.getName()));
                    }
                    // Build maps for equipment type by name (if available)
                    java.util.Map<String, String> reqMap = new java.util.HashMap<>();
                    if (requestedRoom != null && requestedRoom.getEquipment() != null) {
                        requestedRoom.getEquipment().forEach(eq -> reqMap.put(eq.getName(), eq.getType()));
                    }
                    java.util.Map<String, String> otherMap = new java.util.HashMap<>();
                    if (r.getEquipment() != null) {
                        r.getEquipment().forEach(eq -> otherMap.put(eq.getName(), eq.getType()));
                    }

                    java.util.Set<String> unionSet = new java.util.HashSet<>();
                    unionSet.addAll(reqMap.keySet());
                    unionSet.addAll(otherMap.keySet());
                    for (String en : unionSet) {
                        String t1 = reqMap.get(en);
                        String t2 = otherMap.get(en);
                        double w1 = t1 == null ? 0.0 : typeWeights.getOrDefault(t1.toUpperCase(), 0.5);
                        double w2 = t2 == null ? 0.0 : typeWeights.getOrDefault(t2.toUpperCase(), 0.5);
                        double maxw = Math.max(w1, w2);
                        weightedUnion += maxw;
                        if (t1 != null && t2 != null) {
                            weightedInter += Math.min(w1, w2);
                        }
                    }
                    double equipScore = weightedUnion > 0 ? (weightedInter / weightedUnion) : 0.0;

                    // capacity closeness score (1.0 is perfect match or slightly larger)
                    int reqCap = capacity == null ? 1 : capacity;
                    int capDiff = Math.abs(r.getCapacity() - reqCap);
                    double capScore = 1.0 / (1 + (double) capDiff / Math.max(1, reqCap));

                    // proximity: simple similarity by location substring overlap
                    double proximity = 0.0;
                    if (requestedRoom != null && requestedRoom.getLocation() != null && r.getLocation() != null) {
                        String a = requestedRoom.getLocation().toLowerCase();
                        String b = r.getLocation().toLowerCase();
                        if (a.equals(b)) proximity = 1.0;
                        else if (a.contains(b) || b.contains(a)) proximity = 0.8;
                        else if (a.split(" ").length > 0 && b.split(" ").length > 0) {
                            // common word overlap
                            java.util.Set<String> sa = new java.util.HashSet<>(java.util.Arrays.asList(a.split(" ")));
                            java.util.Set<String> sb = new java.util.HashSet<>(java.util.Arrays.asList(b.split(" ")));
                            sa.retainAll(sb);
                            proximity = sa.size() > 0 ? Math.min(0.7, 0.2 + 0.1 * sa.size()) : 0.0;
                        }
                    }

                    // historical approval rate for this room (look at booking history for bookings of this room)
                    double approvalRate = 0.5; // default neutral
                    try {
                        java.util.List<com.example.meeting.model.Booking> roomBookings = bookingRepository.findByRoomId(r.getId());
                        int approved = 0;
                        int total = 0;
                        for (com.example.meeting.model.Booking rb : roomBookings) {
                            if (rb.getStatus() != null) {
                                total++;
                                if ("APPROVED".equals(rb.getStatus())) approved++;
                            }
                        }
                        if (total > 0) approvalRate = ((double) approved) / total;
                    } catch (Exception ignore) {}


                    // find earliest slot for this room (first available within limit)
                    java.time.LocalDateTime c = s;
                    java.time.LocalDateTime found = null;
                    while (c.isBefore(limit)) {
                        java.util.List<Booking> ov = bookingRepository.findOverlappingBookings(r.getId(), c, c.plus(duration));
                        if (ov.isEmpty()) { found = c; break; }
                        c = c.plusMinutes(30);
                    }

                    double timeScore = found == null ? 0.0 : 1.0 / (1 + java.time.Duration.between(s, found).toHours());

                    // combined score: weighted sum (equip 45%, capacity 20%, proximity 15%, approval 10%, time 10%)
                    double combined = equipScore * 0.45 + capScore * 0.20 + proximity * 0.15 + approvalRate * 0.10 + timeScore * 0.10;

                    java.util.Map<String, Object> m = new java.util.HashMap<>();
                    m.put("room", r);
                    m.put("availableFrom", found); 
                    m.put("requestedCapacity", reqCap);
                    m.put("requestedLocation", requestedRoom == null ? "" : requestedRoom.getLocation());                     
                    m.put("timeScore", timeScore);
                    m.put("score", combined);
                    // include requested context so frontend can sort and display matches
                    m.put("requestedAmenities", new java.util.ArrayList<>(requestedEquipNames));
                    scoredAlts.add(m);
                    if (scoredAlts.size() >= 50) break; // gather more and sort later
                }

                // sort alternatives by score desc and take top 5
                scoredAlts.sort((a, b) -> Double.compare((Double) b.get("score"), (Double) a.get("score")));
                java.util.List<java.util.Map<String, Object>> altList = scoredAlts.stream().limit(5).toList();

                java.util.Map<String, Object> resp = new java.util.HashMap<>();
                resp.put("nextSlots", slots);
                resp.put("alternateRooms", altList);
                return ResponseEntity.ok(resp);
            } catch (Exception ex) {
                return ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
            }
        }

    // Helper to parse ISO-8601 timestamps robustly, accepting offsets and 'Z'
    private java.time.LocalDateTime parseDateTime(String input) {
        if (input == null) return null;
        input = input.trim();
        try {
            // Try parsing as OffsetDateTime (handles trailing Z and offsets)
            java.time.OffsetDateTime odt = java.time.OffsetDateTime.parse(input);
            return odt.atZoneSameInstant(java.time.ZoneId.systemDefault()).toLocalDateTime();
        } catch (Exception ignored) {
        }
        try {
            // Try parsing as Instant
            java.time.Instant instant = java.time.Instant.parse(input);
            return java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault());
        } catch (Exception ignored) {
        }
        // Fallback to LocalDateTime parse (no offset)
        return java.time.LocalDateTime.parse(input);
    }
}