package com.example.meeting;

import com.example.meeting.controller.AdminAnalyticsController;
import com.example.meeting.model.AiInsights;
import com.example.meeting.repository.AiInsightsRepository;
import com.example.meeting.repository.BookingRepository;
import com.example.meeting.repository.RoomRepository;
import com.example.meeting.service.LlmClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AdminAnalyticsParsingTests {

    private BookingRepository bookingRepository = Mockito.mock(BookingRepository.class);
    private RoomRepository roomRepository = Mockito.mock(RoomRepository.class);
    private AiInsightsRepository aiRepo = Mockito.mock(AiInsightsRepository.class);
    private LlmClient llmClient = Mockito.mock(LlmClient.class);

    @BeforeEach
    void setup() {
    }

    @Test
    void parsesValidJsonAndPersists() {
        AdminAnalyticsController c = new AdminAnalyticsController(bookingRepository, roomRepository, aiRepo);
        Mockito.when(llmClient.isConfigured()).thenReturn(true);
        String aiJson = "{\n  \"insights\": [\"A\"], \"recommendations\": [\"B\"]\n}";
        // Inject LLM client via reflection for test simplicity
        try {
            java.lang.reflect.Field f = AdminAnalyticsController.class.getDeclaredField("llmClient");
            f.setAccessible(true);
            f.set(c, llmClient);
        } catch (Exception e) {
            fail("Reflection setup failed: " + e.getMessage());
        }
        Mockito.when(llmClient.ask(Mockito.anyString())).thenReturn(aiJson);
        Mockito.when(aiRepo.save(Mockito.any(AiInsights.class))).thenAnswer(inv -> inv.getArgument(0));

    Map<String,Object> resp = c.overview();
    assertNotNull(resp.get("aiInsightsRaw"));
    assertNotNull(resp.get("aiInsights"));
    Mockito.verify(aiRepo, Mockito.times(1)).save(Mockito.any(AiInsights.class));
    }

    @Test
    void returnsParseErrorOnInvalidJson() {
        AdminAnalyticsController c = new AdminAnalyticsController(bookingRepository, roomRepository, aiRepo);
        try {
            java.lang.reflect.Field f = AdminAnalyticsController.class.getDeclaredField("llmClient");
            f.setAccessible(true);
            f.set(c, llmClient);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        Mockito.when(llmClient.isConfigured()).thenReturn(true);
        Mockito.when(llmClient.ask(Mockito.anyString())).thenReturn("Not a json");
        Map<String,Object> resp = c.overview();
        assertNotNull(resp.get("aiInsightsRaw"));
        assertTrue(resp.containsKey("aiInsightsParseError"));
    }
}
