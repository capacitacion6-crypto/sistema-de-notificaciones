package com.example.ticketero.model.dto.response;

public record QueueStats(
    String queueType,
    int waiting,
    int averageWaitMinutes,
    int advisorsAvailable,
    int advisorsBusy
) {}