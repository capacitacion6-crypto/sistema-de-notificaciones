package com.example.ticketero.model.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record TicketResponse(
    UUID uuid,
    String ticketNumber,
    String queueType,
    String status,
    Integer queuePosition,
    Integer estimatedWaitMinutes,
    String advisorName,
    Integer moduleNumber,
    LocalDateTime createdAt
) {}