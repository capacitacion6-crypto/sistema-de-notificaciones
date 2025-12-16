package com.example.ticketero.controller;

import com.example.ticketero.model.dto.request.CreateTicketRequest;
import com.example.ticketero.model.dto.response.QueuePositionResponse;
import com.example.ticketero.model.dto.response.TicketResponse;
import com.example.ticketero.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Slf4j
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(
        @Valid @RequestBody CreateTicketRequest request
    ) {
        log.info("POST /api/tickets - Creating ticket for RUT: {}", request.customerRut());
        TicketResponse response = ticketService.createTicket(request);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<TicketResponse> getTicket(@PathVariable UUID uuid) {
        log.info("GET /api/tickets/{} - Retrieving ticket", uuid);
        return ticketService.findByUuid(uuid)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{ticketNumber}/position")
    public ResponseEntity<QueuePositionResponse> getQueuePosition(@PathVariable String ticketNumber) {
        log.info("GET /api/tickets/{}/position - Checking queue position", ticketNumber);
        return ticketService.getQueuePosition(ticketNumber)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}