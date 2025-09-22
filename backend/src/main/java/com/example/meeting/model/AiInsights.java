package com.example.meeting.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
public class AiInsights {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String correlationId; // optional id to tie back to an analytics run

    @Lob
    private String rawOutput;

    @Lob
    private String insightsJson; // structured JSON when parsed

    private LocalDateTime createdAt;

    public AiInsights() {
        this.correlationId = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getRawOutput() {
        return rawOutput;
    }

    public void setRawOutput(String rawOutput) {
        this.rawOutput = rawOutput;
    }

    public String getInsightsJson() {
        return insightsJson;
    }

    public void setInsightsJson(String insightsJson) {
        this.insightsJson = insightsJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
