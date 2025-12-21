package com.example.ticketero.model.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record DashboardResponse(
    DashboardSummary summary,
    List<QueueStats> queueStats,
    List<AdvisorResponse> advisors,
    List<AlertResponse> alerts,
    LocalDateTime lastUpdated
) {}