package com.example.ticketero.service;

import com.example.ticketero.model.dto.request.CreateTicketRequest;
import com.example.ticketero.model.dto.response.TicketResponse;
import com.example.ticketero.model.entity.QueueType;
import com.example.ticketero.model.entity.TicketStatus;
import com.example.ticketero.repository.AdvisorRepository;
import com.example.ticketero.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TicketServiceTest {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private AdvisorRepository advisorRepository;

    @Test
    void shouldCreateTicketSuccessfully() {
        // Given
        CreateTicketRequest request = new CreateTicketRequest(
            "12345678-9",
            "+56912345678",
            QueueType.CAJA
        );

        // When
        TicketResponse response = ticketService.createTicket(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.uuid()).isNotNull();
        assertThat(response.ticketNumber()).startsWith("C");
        assertThat(response.queueType()).isEqualTo("CAJA");
        assertThat(response.status()).isEqualTo("WAITING");
        assertThat(response.queuePosition()).isGreaterThan(0);
        assertThat(response.estimatedWaitMinutes()).isGreaterThan(0);
    }

    @Test
    void shouldCalculateQueuePositionCorrectly() {
        // Given
        CreateTicketRequest request1 = new CreateTicketRequest("11111111-1", null, QueueType.CAJA);
        CreateTicketRequest request2 = new CreateTicketRequest("22222222-2", null, QueueType.CAJA);

        // When
        TicketResponse first = ticketService.createTicket(request1);
        TicketResponse second = ticketService.createTicket(request2);

        // Then
        assertThat(first.queuePosition()).isEqualTo(1);
        assertThat(second.queuePosition()).isEqualTo(2);
    }
}