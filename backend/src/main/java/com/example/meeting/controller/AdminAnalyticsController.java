package com.example.meeting.controller;

import com.example.meeting.model.Booking;
import com.example.meeting.model.Room;
import com.example.meeting.repository.BookingRepository;
import com.example.meeting.repository.RoomRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import com.example.meeting.service.LlmClient;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api/admin/analytics")
public class AdminAnalyticsController {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final com.example.meeting.repository.AiInsightsRepository aiInsightsRepository;
    @Autowired
    private LlmClient llmClient;

    public AdminAnalyticsController(BookingRepository bookingRepository, RoomRepository roomRepository, com.example.meeting.repository.AiInsightsRepository aiInsightsRepository) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.aiInsightsRepository = aiInsightsRepository;
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Map<String, Object> overview() {
        Map<String, Object> resp = new HashMap<>();
        List<Room> rooms = roomRepository.findAll();
        List<Booking> bookings = bookingRepository.findAll();

        // total bookings
        resp.put("totalBookings", bookings.size());

        // bookings by room
        Map<Long, Long> counts = bookings.stream().collect(Collectors.groupingBy(Booking::getRoomId, Collectors.counting()));
        List<Map<String, Object>> roomCounts = new ArrayList<>();
        for (Room r : rooms) {
            Map<String, Object> m = new HashMap<>();
            m.put("roomId", r.getId());
            m.put("name", r.getName());
            m.put("count", counts.getOrDefault(r.getId(), 0L));
            roomCounts.add(m);
        }
        roomCounts.sort((a,b) -> Long.compare((Long)b.get("count"), (Long)a.get("count")));
        resp.put("bookingsByRoom", roomCounts);

        // top/least rooms
        List<Map<String,Object>> topRooms = roomCounts.stream().limit(5).collect(Collectors.toList());
        List<Map<String,Object>> leastRooms = roomCounts.stream().sorted(Comparator.comparingLong(m -> (Long)m.get("count"))).limit(5).collect(Collectors.toList());
        resp.put("topRooms", topRooms);
        resp.put("leastRooms", leastRooms);

        // capacity usage: average attendees per booking vs room capacity distribution
        Map<Long, List<Integer>> attendeesByRoom = new HashMap<>();
        for (Booking b : bookings) {
            attendeesByRoom.computeIfAbsent(b.getRoomId(), k -> new ArrayList<>()).add(b.getAttendeesCount() == null ? 0 : b.getAttendeesCount());
        }
        List<Map<String,Object>> capacityStats = new ArrayList<>();
        for (Room r : rooms) {
            List<Integer> arr = attendeesByRoom.getOrDefault(r.getId(), Collections.emptyList());
            double avg = arr.isEmpty() ? 0.0 : arr.stream().mapToInt(Integer::intValue).average().orElse(0.0);
            Map<String,Object> cs = new HashMap<>();
            cs.put("roomId", r.getId());
            cs.put("name", r.getName());
            cs.put("capacity", r.getCapacity());
            cs.put("avgAttendees", avg);
            capacityStats.add(cs);
        }
        resp.put("capacityStats", capacityStats);

        // utilization estimate over last 30 days: fraction of hours booked vs available (assumes 8h per day)
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        Map<Long, Long> bookedMinutes = new HashMap<>();
        for (Booking b : bookings) {
            if (b.getStartTime() == null || b.getEndTime() == null) continue;
            if (b.getEndTime().isBefore(since)) continue;
            long mins = Duration.between(b.getStartTime(), b.getEndTime()).toMinutes();
            bookedMinutes.put(b.getRoomId(), bookedMinutes.getOrDefault(b.getRoomId(), 0L) + Math.max(0, mins));
        }
        List<Map<String,Object>> utilization = new ArrayList<>();
        for (Room r : rooms) {
            long mins = bookedMinutes.getOrDefault(r.getId(), 0L);
            // available minutes = 30 days * 8 hours/day * 60
            long avail = 30L * 8L * 60L;
            double util = avail == 0 ? 0.0 : ((double)mins) / (double)avail;
            Map<String,Object> u = new HashMap<>();
            u.put("roomId", r.getId());
            u.put("name", r.getName());
            u.put("utilization", util);
            utilization.add(u);
        }
        resp.put("utilization", utilization);

        // Simple AI-driven recommendation: list rooms with low utilization but high capacity (candidates to repurpose)
        List<Map<String,Object>> recommendations = utilization.stream()
                .filter(m -> ((Double)m.get("utilization")) < 0.1)
                .map(m -> {
                    Map<String,Object> mm = new HashMap<>();
                    mm.put("roomId", m.get("roomId"));
                    mm.put("name", m.get("name"));
                    mm.put("reason", "Low utilization (<10%) — consider repurposing or merging resources");
                    return mm;
                }).limit(10).collect(Collectors.toList());
        resp.put("recommendations", recommendations);

        // Ask LLM for brief insights if available
        try {
            if (llmClient != null && llmClient.isConfigured()) {
                StringBuilder prompt = new StringBuilder();
                prompt.append("Provide a short analysis of room utilization and recommendations based on the following data:\n");
                prompt.append("Top rooms (name,count):\n");
                for (Map<String,Object> tr : topRooms) {
                    prompt.append(tr.get("name")).append(",").append(tr.get("count")).append("\n");
                }
                prompt.append("Utilization (name,utilization fraction):\n");
                for (Map<String,Object> u : utilization) {
                    prompt.append(u.get("name")).append(",").append(u.get("utilization")).append("\n");
                }
                prompt.append("Return JSON with keys: 'insights' (array of strings) and 'recommendations' (array of strings). Return ONLY JSON.");
                String aiResp = llmClient.ask(prompt.toString());
                resp.put("aiInsightsRaw", aiResp);
                // attempt to parse JSON (strip markdown code fences if present) and validate schema
                try {
                    String clean = aiResp == null ? "" : aiResp.replaceAll("(?s)```\\w*", "").replaceAll("```", "").trim();
                    com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                    com.fasterxml.jackson.databind.JsonNode root = om.readTree(clean);
                    boolean valid = false;
                    java.util.List<String> insights = new java.util.ArrayList<>();
                    java.util.List<String> recs = new java.util.ArrayList<>();
                    if (root != null && root.has("insights") && root.get("insights").isArray() && root.has("recommendations") && root.get("recommendations").isArray()) {
                        valid = true;
                        for (com.fasterxml.jackson.databind.JsonNode n : root.get("insights")) {
                            if (n.isTextual()) insights.add(n.asText());
                        }
                        for (com.fasterxml.jackson.databind.JsonNode n : root.get("recommendations")) {
                            if (n.isTextual()) recs.add(n.asText());
                        }
                    }

                    if (valid && (!insights.isEmpty() || !recs.isEmpty())) {
                        Map<String,Object> structured = new HashMap<>();
                        structured.put("insights", insights);
                        structured.put("recommendations", recs);
                        resp.put("aiInsights", structured);

                        // persist structured insights for auditing
                        try {
                            com.example.meeting.model.AiInsights ai = new com.example.meeting.model.AiInsights();
                            ai.setRawOutput(aiResp);
                            ai.setInsightsJson(om.writeValueAsString(structured));
                            aiInsightsRepository.save(ai);
                            resp.put("aiInsightsStoredId", ai.getId());
                        } catch (Exception persistEx) {
                            resp.put("aiInsightsStoreError", persistEx.getMessage());
                        }
                    } else {
                        resp.put("aiInsightsParseError", "AI returned JSON but missing required arrays 'insights' or 'recommendations'");
                    }
                } catch (Exception parseEx) {
                    // leave raw response available and expose parsing error
                    resp.put("aiInsightsParseError", parseEx.getMessage());
                }
            }
        } catch (Exception ex) {
            resp.put("aiInsightsError", ex.getMessage());
        }

        return resp;
    }
}
