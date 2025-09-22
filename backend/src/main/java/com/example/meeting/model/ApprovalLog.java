package com.example.meeting.model;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "approval_log")
public class ApprovalLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "actor")
    private String actor;

    @Column(name = "action")
    private String action;

    @Column(name = "confidence")
    private double confidence;

    @Column(name = "rationale")
    @Lob
    private String rationale;

    @Column(name = "source")
    private String source;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public ApprovalLog() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
