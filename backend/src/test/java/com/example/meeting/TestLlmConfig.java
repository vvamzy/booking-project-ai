package com.example.meeting;

import com.example.meeting.service.LlmClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Test configuration that provides a no-op/mock LlmClient to avoid external calls during tests.
 */
@Configuration
public class TestLlmConfig {

    @Bean
    public LlmClient llmClient() {
        return new LlmClient() {
            @Override
            public boolean isConfigured() {
                return false; // make sure tests use rules fallback
            }

            @Override
            public String ask(String prompt) {
                // never called because isConfigured returns false; if called, return empty JSON
                return "{}";
            }
        };
    }
}
