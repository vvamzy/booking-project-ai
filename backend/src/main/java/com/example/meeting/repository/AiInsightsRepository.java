package com.example.meeting.repository;

import com.example.meeting.model.AiInsights;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiInsightsRepository extends JpaRepository<AiInsights, Long> {
}
