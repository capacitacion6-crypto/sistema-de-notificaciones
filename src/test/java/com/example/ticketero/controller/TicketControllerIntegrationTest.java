package com.example.ticketero.controller;

import com.example.ticketero.model.dto.request.CreateTicketRequest;
import com.example.ticketero.model.enums.QueueType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class TicketControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateTicketSuccessfully() throws Exception {
        CreateTicketRequest request = new CreateTicketRequest(
            "12345678-9",
            "+56912345678",
            QueueType.CAJA
        );

        mockMvc.perform(post("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uuid").exists())
                .andExpect(jsonPath("$.ticketNumber").exists())
                .andExpect(jsonPath("$.queueType").value("CAJA"))
                .andExpect(jsonPath("$.status").value("EN_ESPERA"))
                .andExpect(jsonPath("$.queuePosition").exists())
                .andExpect(jsonPath("$.estimatedWaitMinutes").exists());
    }

    @Test
    void shouldReturnValidationErrorForInvalidRut() throws Exception {
        CreateTicketRequest request = new CreateTicketRequest(
            "invalid-rut",
            "+56912345678",
            QueueType.CAJA
        );

        mockMvc.perform(post("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors").isArray());
    }
}