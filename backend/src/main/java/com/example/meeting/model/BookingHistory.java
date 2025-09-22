package com.example.meeting.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "booking_history")
public class BookingHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;
    
    @Column(nullable = false)
    private String previousStatus;
    
    @Column(nullable = false)
    private String newStatus;
    
    @Column(nullable = false)
    private LocalDateTime changedAt;
    
    @Column(nullable = false)
    private String changedBy;
    
    @Column(length = 1000)
    private String reason;
    
    private Double aiConfidence;
    
    @Column(length = 1000)
    private String aiRationale;
    
    // Constructors
    public BookingHistory() {
    }
    
    public BookingHistory(Booking booking, String previousStatus, String newStatus, 
                         String changedBy, String reason, Double aiConfidence, String aiRationale) {
        this.booking = booking;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.changedAt = LocalDateTime.now();
        this.changedBy = changedBy;
        this.reason = reason;
        this.aiConfidence = aiConfidence;
        this.aiRationale = aiRationale;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Booking getBooking() {
        return booking;
    }
    
    public void setBooking(Booking booking) {
        this.booking = booking;
    }
    
    public String getPreviousStatus() {
        return previousStatus;
    }
    
    public void setPreviousStatus(String previousStatus) {
        this.previousStatus = previousStatus;
    }
    
    public String getNewStatus() {
        return newStatus;
    }
    
    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }
    
    public LocalDateTime getChangedAt() {
        return changedAt;
    }
    
    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }
    
    public String getChangedBy() {
        return changedBy;
    }
    
    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public Double getAiConfidence() {
        return aiConfidence;
    }
    
    public void setAiConfidence(Double aiConfidence) {
        this.aiConfidence = aiConfidence;
    }
    
    public String getAiRationale() {
        return aiRationale;
    }
    
    public void setAiRationale(String aiRationale) {
        this.aiRationale = aiRationale;
    }
}