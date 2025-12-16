package com.example.ticketero.model.dto.response;

public record AdvisorResponse(
    Long id,
    String name,
    Integer moduleNumber,
    String queueType,
    String status,
    Long currentTicketId,
    String currentTicketNumber
) {}