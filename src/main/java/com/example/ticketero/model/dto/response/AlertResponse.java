package com.example.ticketero.model.dto.response;

import java.time.LocalDateTime;

public record AlertResponse(
    String type,
    String message,
    String severity,
    LocalDateTime timestamp
) {}