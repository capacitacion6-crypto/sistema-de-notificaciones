package com.example.ticketero.model.dto.request;

import com.example.ticketero.model.enums.QueueType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
    @NotBlank(message = "RUT is required")
    @Pattern(regexp = "^\\d{7,8}-[\\dkK]$", message = "Invalid RUT format")
    String customerRut,

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone format")
    @Size(max = 15, message = "Phone number too long")
    String customerPhone,

    @NotNull(message = "Queue type is required")
    QueueType queueType
) {}