package com.example.ticketero.model.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record DashboardResponse(
    DashboardSummary summary,
    List<QueueStats> queueStats,
    List<AdvisorResponse> advisors,
    List<AlertResponse> alerts,
    LocalDateTime lastUpdated
) {}

record DashboardSummary(
    int totalTicketsToday,
    int ticketsWaiting,
    int ticketsInProgress,
    int ticketsCompleted,
    int advisorsAvailable,
    int advisorsBusy,
    double averageWaitTime
) {}

record QueueStats(
    String queueType,
    int waiting,
    int averageWaitMinutes,
    int advisorsAvailable,
    int advisorsBusy
) {}

record AlertResponse(
    String type,
    String message,
    String severity,
    LocalDateTime timestamp
) {}