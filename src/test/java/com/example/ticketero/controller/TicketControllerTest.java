package com.example.ticketero.controller;

import com.example.ticketero.model.dto.request.CreateTicketRequest;
import com.example.ticketero.model.dto.response.QueuePositionResponse;
import com.example.ticketero.model.dto.response.TicketResponse;
import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.service.TicketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TicketController.class)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TicketService ticketService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateTicketSuccessfully() throws Exception {
        // Given
        CreateTicketRequest request = new CreateTicketRequest(
            "12345678-9",
            "+56912345678",
            QueueType.CAJA
        );

        TicketResponse response = new TicketResponse(
            UUID.randomUUID(),
            "C123456",
            "CAJA",
            "EN_ESPERA",
            1,
            5,
            null,
            null,
            LocalDateTime.now()
        );

        when(ticketService.createTicket(any(CreateTicketRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketNumber").value("C123456"))
                .andExpect(jsonPath("$.queueType").value("CAJA"))
                .andExpect(jsonPath("$.status").value("EN_ESPERA"))
                .andExpect(jsonPath("$.queuePosition").value(1))
                .andExpect(jsonPath("$.estimatedWaitMinutes").value(5));
    }

    @Test
    void shouldReturnBadRequestForInvalidTicketRequest() throws Exception {
        // Given
        CreateTicketRequest invalidRequest = new CreateTicketRequest(
            "", // RUT vacío
            "+56912345678",
            QueueType.CAJA
        );

        // When & Then
        mockMvc.perform(post("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetTicketByUuid() throws Exception {
        // Given
        UUID uuid = UUID.randomUUID();
        TicketResponse response = new TicketResponse(
            uuid,
            "C123456",
            "CAJA",
            "EN_ESPERA",
            1,
            5,
            null,
            null,
            LocalDateTime.now()
        );

        when(ticketService.findByUuid(uuid)).thenReturn(Optional.of(response));

        // When & Then
        mockMvc.perform(get("/api/tickets/{uuid}", uuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketNumber").value("C123456"));
    }

    @Test
    void shouldReturnNotFoundForNonExistentTicket() throws Exception {
        // Given
        UUID uuid = UUID.randomUUID();
        when(ticketService.findByUuid(uuid)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/tickets/{uuid}", uuid))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetQueuePosition() throws Exception {
        // Given
        String ticketNumber = "C123456";
        QueuePositionResponse response = new QueuePositionResponse(
            ticketNumber,
            3,
            15,
            "EN_ESPERA"
        );

        when(ticketService.getQueuePosition(ticketNumber)).thenReturn(Optional.of(response));

        // When & Then
        mockMvc.perform(get("/api/tickets/{ticketNumber}/position", ticketNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketNumber").value(ticketNumber))
                .andExpect(jsonPath("$.currentPosition").value(3))
                .andExpect(jsonPath("$.estimatedWaitMinutes").value(15));
    }

    @Test
    void shouldReturnNotFoundForInvalidTicketNumber() throws Exception {
        // Given
        String ticketNumber = "INVALID";
        when(ticketService.getQueuePosition(ticketNumber)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/tickets/{ticketNumber}/position", ticketNumber))
                .andExpect(status().isNotFound());
    }
}