package com.example.ticketero.model.dto.response;

public record QueuePositionResponse(
    String ticketNumber,
    Integer currentPosition,
    Integer estimatedWaitMinutes,
    String status
) {}