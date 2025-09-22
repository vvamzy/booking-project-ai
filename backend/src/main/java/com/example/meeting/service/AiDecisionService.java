package com.example.meeting.service;

import com.example.meeting.model.Booking;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class AiDecisionService {

    @Autowired
    private LlmClient llmClient;

    @Autowired
    private RuleBasedDecisionService ruleBasedDecisionService;

    private final ObjectMapper mapper = new ObjectMapper();

    public enum Action {
        AUTO_APPROVE,
        AUTO_REJECT,
        REQUIRES_REVIEW
    }

    public static class Decision {
        private final Action action;
        private final double confidence; // 0.0 - 1.0
        private final List<String> rationale;
        private final List<String> suggestions;

        public Decision(Action action, double confidence, List<String> rationale) {
            this(action, confidence, rationale, Collections.emptyList());
        }

        public Decision(Action action, double confidence, List<String> rationale, List<String> suggestions) {
            this.action = action;
            this.confidence = confidence;
            this.rationale = rationale == null ? Collections.emptyList() : rationale;
            this.suggestions = suggestions == null ? Collections.emptyList() : suggestions;
        }

        public Action getAction() {
            return action;
        }

        public double getConfidence() {
            return confidence;
        }

        public List<String> getRationale() {
            return rationale;
        }

        public List<String> getSuggestions() {
            return suggestions;
        }
    }

    /**
     * Evaluate a booking and return a decision. This is a rules-first implementation:
     * - Reject excessively long bookings
     * - Flag conflicts for review or reject depending on severity
     * - Prefer auto-approve for short, in-hours bookings with no conflicts
     * The method accepts an optional list of overlapping bookings (conflicts) so the caller
     * can perform efficient DB queries and pass context in.
     */
    public Decision decide(Booking booking, List<Booking> overlappingBookings) {
        // Try to use LLM if configured, but fail gracefully to rules-based logic
    // flag intentionally unused in current implementation
        if (llmClient != null && llmClient.isConfigured()) {
            try {
                String prompt = buildPrompt(booking, overlappingBookings);
                String response = llmClient.ask(prompt);
                if (response != null && !response.isBlank()) {
                    try {
                        // Clean up markdown code block markers if present
                        String cleanJson = response.replaceAll("```json\\s*", "")
                                                 .replaceAll("```\\s*$", "")
                                                 .trim();
                        JsonNode root = mapper.readTree(cleanJson);
                        String action = root.has("action") ? root.get("action").asText() : null;
                        double confidence = root.has("confidence") ? root.get("confidence").asDouble() : 0.0;
                        List<String> rationale = new ArrayList<>();
                        List<String> suggestions = new ArrayList<>();
                        if (root.has("rationale") && root.get("rationale").isArray()) {
                            for (JsonNode n : root.get("rationale")) {
                                rationale.add(n.asText());
                            }
                        }
                        if (root.has("suggestions") && root.get("suggestions").isArray()) {
                            for (JsonNode n : root.get("suggestions")) {
                                suggestions.add(n.asText());
                            }
                        }
                        if (action != null) {
                            try {
                                Action act = Action.valueOf(action);
                                // If LLM rationale mentions unclear purpose, avoid auto-approving
                                boolean purposeUnclear = rationale.stream().anyMatch(s -> s.toLowerCase().contains("unclear") || s.toLowerCase().contains("insufficient") || s.toLowerCase().contains("not clear") || s.toLowerCase().contains("vague"));
                                if (purposeUnclear && act == Action.AUTO_APPROVE) {
                                    act = Action.REQUIRES_REVIEW;
                                    confidence = Math.min(confidence, 0.45);
                                }
                                return new Decision(act, confidence, rationale, suggestions);
                            } catch (IllegalArgumentException e) {
                                // Invalid action value, fall through to rules
                            }
                        }
                    } catch (IOException e) {
                        // JSON parsing failed, fall through to rules
                    }
                }
            } catch (Exception e) {
                // Any LLM error, fall through to rules-based fallback
            }
        }

        // ...existing rules-based logic follows
        if (booking == null) {
            return new Decision(Action.REQUIRES_REVIEW, 0.0, Collections.singletonList("Missing booking data"), Collections.singletonList("Please provide booking details including purpose, attendees and priority"));
        }

        List<String> reasons = new ArrayList<>();
        double score = 0.5; // baseline neutral confidence

        // Purpose clarity check: only apply when purpose is provided
        if (booking.getPurpose() != null) {
            String purpose = booking.getPurpose().trim();
            String purposeLower = purpose.toLowerCase();
            if (purpose.isEmpty() || purpose.length() < 10 || purpose.matches("[\\W_]{5,}") || purposeLower.equals("meeting") || purposeLower.equals("sync") || purposeLower.equals("call")) {
                reasons.add("Insufficient or unclear purpose");
                List<String> sugg = new ArrayList<>();
                sugg.add("Provide a short agenda or expected outcomes (2-3 sentences)");
                sugg.add("Mention key attendees and why their presence is required");
                sugg.add("If this is a client meeting, mention the client/company and meeting objective");
                return new Decision(Action.REQUIRES_REVIEW, 0.35, reasons, sugg);
            }
        }

        // Duration checks
        Duration duration = Duration.between(booking.getStartTime(), booking.getEndTime());
        long minutes = Math.max(0, duration.toMinutes());
        if (minutes <= 0) {
            reasons.add("Invalid time range");
            return new Decision(Action.AUTO_REJECT, 0.95, reasons, Collections.singletonList("Please select valid start and end times"));
        }

        if (minutes > 8 * 60) { // longer than 8 hours
            reasons.add("Booking duration exceeds 8 hours");
            return new Decision(Action.AUTO_REJECT, 0.9, reasons, Collections.singletonList("Split the booking into shorter sessions or request special approval"));
        }

        // Business hours preference (08:00 - 18:00)
        LocalTime startLocal = booking.getStartTime().toLocalTime();
        LocalTime endLocal = booking.getEndTime().toLocalTime();
        boolean withinBusinessHours = !startLocal.isBefore(LocalTime.of(8, 0)) && !endLocal.isAfter(LocalTime.of(18, 0));
        if (withinBusinessHours) {
            reasons.add("Within business hours");
            score += 0.15;
        } else {
            reasons.add("Outside business hours");
            score -= 0.1;
        }

        // Conflicts
        int conflicts = overlappingBookings == null ? 0 : overlappingBookings.size();
        if (conflicts > 0) {
            reasons.add(conflicts + " overlapping booking(s) detected");
            // single conflict -> require review; multiple conflicts -> likely reject
            if (conflicts == 1) {
                score -= 0.3;
            } else {
                score -= 0.6;
            }
        } else {
            reasons.add("No overlapping bookings");
            score += 0.2;
        }

        // Capacity/resource heuristics could go here (not implemented yet)

        // Normalize confidence
        double confidence = Math.max(0.0, Math.min(1.0, score));

        // Decide action based on score and rules
        if (confidence >= 0.75 && conflicts == 0) {
            reasons.add("High confidence and no conflicts -> auto-approve");
            return new Decision(Action.AUTO_APPROVE, confidence, reasons, Collections.emptyList());
        }

        if (confidence < 0.35) {
            reasons.add("Low confidence -> auto-reject");
            return new Decision(Action.AUTO_REJECT, confidence, reasons, Collections.singletonList("Provide a clearer purpose and expected outcomes"));
        }

        reasons.add("Moderate confidence -> requires human review");
        return new Decision(Action.REQUIRES_REVIEW, confidence, reasons, Collections.singletonList("Consider adding a short agenda, expected outcomes, and attendees list"));
    }

    /**
     * Backwards-compatible boolean API. Uses rule-based decision making when LLM is not available.
     */
    public boolean shouldApproveBooking(String bookingDetails) {
        if (!isLlmConfigured()) {
            // Use simple rule-based decision when AI is not available
            return bookingDetails.contains("priority=5") || 
                   (bookingDetails.contains("priority=4") && !bookingDetails.contains("overlap"));
        }
        try {
            JsonNode details = mapper.readTree(bookingDetails);
            return details.has("priority") && 
                   details.get("priority").asInt() >= 4 && 
                   !details.has("overlaps");
        } catch (IOException e) {
            return false; // conservative default for unparseable input
        }
    }

    public boolean isLlmConfigured() {
        return llmClient.isConfigured();
    }

    /**
     * Validate a booking purpose using LLM when configured, otherwise use simple heuristics.
     * Returns a map with keys: clear (boolean) and suggestions (List<String>)
     */
    public java.util.Map<String,Object> validatePurpose(String purpose) {
        java.util.Map<String,Object> out = new java.util.HashMap<>();
        java.util.List<String> suggestions = new java.util.ArrayList<>();
        if (purpose == null || purpose.trim().length() < 10) {
            out.put("clear", false);
            if (purpose == null) {
                suggestions.add("Purpose is missing");
            } else {
                suggestions.add("Purpose appears too brief to be clear");
            }
            out.put("suggestions", suggestions);
            return out;
        }
        if (llmClient != null && llmClient.isConfigured()) {
            try {
                String prompt = "Evaluate the clarity and relevance of this meeting purpose. Return ONLY JSON: { \"clear\": true|false, \"suggestions\": [..] }. Purpose: \"" + purpose.replaceAll("\n", " ") + "\"";
                String resp = llmClient.ask(prompt);
                if (resp != null && !resp.isBlank()) {
                    String clean = resp.replaceAll("(?s)```\\w*", "").replaceAll("```", "").trim();
                    JsonNode node = mapper.readTree(clean);
                    boolean clear = node.has("clear") && node.get("clear").asBoolean(false);
                    out.put("clear", clear);
                    if (node.has("suggestions") && node.get("suggestions").isArray()) {
                        for (JsonNode n : node.get("suggestions")) suggestions.add(n.asText());
                    }
                    out.put("suggestions", suggestions);
                    return out;
                }
            } catch (Exception e) {
                // fallback to heuristics
            }
    }
        // Simple heuristics fallback
        String p = purpose.trim();
        boolean clear = p.length() >= 15 && !p.matches("[\\W_]{5,}");
        if (!clear) {
            suggestions.add("Provide a short agenda or expected outcomes (2-3 sentences)");
        }
        out.put("clear", clear);
        out.put("suggestions", suggestions);
        return out;
    }
    
    // legacy helper removed

    private String buildPrompt(Booking booking, List<Booking> overlaps) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"booking\": {\n");
        sb.append("    \"roomId\": ").append(booking.getRoomId()).append(",\n");
        sb.append("    \"userId\": ").append(booking.getUserId()).append(",\n");
        sb.append("    \"start\": \"").append(booking.getStartTime()).append("\",\n");
        sb.append("    \"end\": \"").append(booking.getEndTime()).append("\",\n");
        sb.append("    \"purpose\": \"").append(booking.getPurpose() == null ? "" : booking.getPurpose().replaceAll("\"", "\\\"")).append("\",\n");
        sb.append("    \"attendees\": ").append(booking.getAttendeesCount() == null ? 0 : booking.getAttendeesCount()).append(",\n");
        sb.append("    \"priority\": ").append(booking.getPriority() == null ? 3 : booking.getPriority()).append(",\n");
        sb.append("    \"requiredFacilities\": ");
        if (booking.getRequiredFacilities() != null) {
            sb.append(mapper.valueToTree(booking.getRequiredFacilities()).toString()).append("\n");
        } else {
            sb.append("[]\n");
        }
        sb.append("  },\n");
        sb.append("  \"overlaps\": [\n");
        if (overlaps != null) {
            for (int i = 0; i < overlaps.size(); i++) {
                Booking o = overlaps.get(i);
                sb.append("    {\"start\":\"").append(o.getStartTime()).append("\", \"end\":\"").append(o.getEndTime()).append("\"}");
                if (i < overlaps.size() - 1) sb.append(",\n");
            }
        }
        sb.append("\n  ]\n}");
        // Ask LLM to return JSON with action/confidence/rationale
     return "Analyze this booking request (including purpose, attendees, priority and requested facilities) and provide a decision. Return ONLY a JSON object (no markdown, no explanation) with the following structure:\n" +
         "{\n" +
         "  \"action\": one of [AUTO_APPROVE, AUTO_REJECT, REQUIRES_REVIEW],\n" +
         "  \"confidence\": number between 0 and 1,\n" +
         "  \"rationale\": array of strings explaining the decision,\n" +
         "  \"suggestions\": optional array of strings suggesting clearer purpose text or remediation\n" +
         "}\n\n" +
         "Booking details:\n" + sb.toString();
    }
}