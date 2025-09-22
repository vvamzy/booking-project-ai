package com.example.meeting.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long roomId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false)
    private String status;
    
    @Column(nullable = false)
    private String purpose;
    
    @Column(nullable = false)
    private Integer attendeesCount;
    
    @ElementCollection
    @CollectionTable(name = "booking_required_facilities",
            joinColumns = @JoinColumn(name = "booking_id"))
    @Column(name = "facility")
    private Set<String> requiredFacilities;
    
    @Column(nullable = false)
    private Integer priority; // 1-5, where 5 is highest priority
    
    @Column(length = 1000)
    private String notes;
    
    // Approval metadata
    private Double decisionConfidence;
    private String decisionRationale;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean isApproved() {
        return "APPROVED".equals(status);
    }

    public Double getDecisionConfidence() {
        return decisionConfidence;
    }

    public void setDecisionConfidence(Double decisionConfidence) {
        this.decisionConfidence = decisionConfidence;
    }

    public String getDecisionRationale() {
        return decisionRationale;
    }

    public void setDecisionRationale(String decisionRationale) {
        this.decisionRationale = decisionRationale;
    }
    
    public String getPurpose() {
        return purpose;
    }
    
    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }
    
    public Integer getAttendeesCount() {
        return attendeesCount;
    }
    
    public void setAttendeesCount(Integer attendeesCount) {
        this.attendeesCount = attendeesCount;
    }
    
    public Set<String> getRequiredFacilities() {
        return requiredFacilities;
    }
    
    public void setRequiredFacilities(Set<String> requiredFacilities) {
        this.requiredFacilities = requiredFacilities;
    }
    
    public Integer getPriority() {
        return priority;
    }
    
    public void setPriority(Integer priority) {
        this.priority = priority;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
}