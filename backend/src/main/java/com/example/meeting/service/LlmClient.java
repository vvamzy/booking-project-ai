package com.example.meeting.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Service
public class LlmClient {

    @Value("${gemini.api.key:AIzaSyBCLV_pwlvg-KhoCRRQjl6U8ejO63FdEPQ}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    @Value("${gemini.api.model}")
    private String model;
    
    @Value("${gemini.api.enabled:true}")
    private boolean enabled;

    private final ObjectMapper mapper = new ObjectMapper();

    private WebClient client() {
        return WebClient.builder()
            .defaultHeader("X-goog-api-key", geminiApiKey)
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .filter(ExchangeFilterFunctions.statusError(
                (s) -> s.is5xxServerError() || s.is4xxClientError(),
                response -> new RuntimeException("Gemini AI returned error: " + response.statusCode() + 
                    (response.statusCode().is4xxClientError() ? " (Check your API key)" : ""))
            ))
            .build();
    }

    public boolean isConfigured() {
        return enabled && geminiApiKey != null && !geminiApiKey.isBlank();
    }

    private String getFallbackResponse(String prompt) {
        // Simple rule-based responses when AI is not available
        if (prompt.toLowerCase().contains("overlap")) {
            return "Due to overlapping bookings, this request requires manual review.";
        } else if (prompt.toLowerCase().contains("priority")) {
            return "High priority bookings are recommended for approval.";
        }
        return "Unable to make decision - requires manual review.";
    }

    private ObjectNode createGeminiRequest(String prompt) {
        ObjectNode request = mapper.createObjectNode();
        ObjectNode content = mapper.createObjectNode();
        ObjectNode textPart = mapper.createObjectNode();
        textPart.put("text", prompt);
        content.putArray("parts").add(textPart);
        request.putArray("contents").add(content);

        ObjectNode generationConfig = mapper.createObjectNode();
        generationConfig.put("temperature", 0.7);
        generationConfig.put("maxOutputTokens", 1024);
        request.set("generationConfig", generationConfig);

        return request;
    }
    public String ask(String prompt) {
        if (!isConfigured()) {
            // When Gemini is not enabled or configured, use fallback responses
            return getFallbackResponse(prompt);
        }

        ObjectNode requestBody = createGeminiRequest(prompt);

        try {
            JsonNode response = client()
                .post()
                .uri(URI.create(apiUrl+ "?key=" + geminiApiKey))
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(2))
                    .filter(e -> !e.getMessage().contains("Check your API key")) // Don't retry on API key errors
                    .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                        throw new RuntimeException("Failed to connect to Gemini AI after multiple attempts", retrySignal.failure());
                    }))
                .timeout(Duration.ofSeconds(30))
                .block();

            if (response != null && response.has("candidates") && response.get("candidates").size() > 0) {
                JsonNode content = response.get("candidates").get(0).get("content");
                if (content != null && content.has("parts") && content.get("parts").size() > 0) {
                    return content.get("parts").get(0).get("text").asText();
                }
            }
            throw new RuntimeException("No valid response from Gemini AI");
        } catch (Exception e) {
            if (e instanceof TimeoutException) {
                throw new RuntimeException("Gemini AI request timed out", e);
            }
            throw new RuntimeException("Error calling Gemini AI: " + e.getMessage(), e);
        }
    }
}
