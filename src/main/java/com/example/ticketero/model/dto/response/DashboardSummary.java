package com.example.ticketero.model.dto.response;

public record DashboardSummary(
    int totalTicketsToday,
    int ticketsWaiting,
    int ticketsInProgress,
    int ticketsCompleted,
    int advisorsAvailable,
    int advisorsBusy,
    double averageWaitTime
) {}