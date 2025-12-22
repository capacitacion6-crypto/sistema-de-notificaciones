package com.example.ticketero.service;

import com.example.ticketero.model.dto.request.CreateTicketRequest;
import com.example.ticketero.model.dto.response.QueuePositionResponse;
import com.example.ticketero.model.dto.response.TicketResponse;
import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.model.enums.TicketStatus;
import com.example.ticketero.repository.AdvisorRepository;
import com.example.ticketero.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private AdvisorRepository advisorRepository;

    @Mock
    private TelegramService telegramService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private TicketService ticketService;

    private CreateTicketRequest request;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        request = new CreateTicketRequest(
            "12345678-9",
            "+56912345678",
            QueueType.CAJA
        );

        ticket = Ticket.builder()
            .id(1L)
            .uuid(UUID.randomUUID())
            .ticketNumber("C123456")
            .customerRut("12345678-9")
            .customerPhone("+56912345678")
            .queueType(QueueType.CAJA)
            .status(TicketStatus.EN_ESPERA)
            .queuePosition(1)
            .estimatedWaitMinutes(5)
            .createdAt(LocalDateTime.now())
            .build();
    }

    @Test
    void shouldCreateTicketSuccessfully() {
        // Given
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
        when(ticketRepository.countTicketsAheadInQueue(any(), any(), any())).thenReturn(0L);
        when(advisorRepository.countByStatusAndQueueType(any(), any())).thenReturn(2L);
        doNothing().when(telegramService).sendConfirmationMessage(any());
        doNothing().when(auditService).logTicketCreated(any(), any(), any());

        // When
        TicketResponse response = ticketService.createTicket(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.uuid()).isNotNull();
        assertThat(response.ticketNumber()).isEqualTo("C123456");
        assertThat(response.queueType()).isEqualTo("CAJA");
        assertThat(response.status()).isEqualTo("EN_ESPERA");
        assertThat(response.queuePosition()).isEqualTo(1);
        assertThat(response.estimatedWaitMinutes()).isEqualTo(5);
        
        verify(ticketRepository).save(any(Ticket.class));
        verify(telegramService).sendConfirmationMessage(any(Ticket.class));
        verify(auditService).logTicketCreated(any(), any(), any());
    }

    @Test
    void shouldFindTicketByUuid() {
        // Given
        UUID uuid = UUID.randomUUID();
        when(ticketRepository.findByUuid(uuid)).thenReturn(Optional.of(ticket));

        // When
        Optional<TicketResponse> response = ticketService.findByUuid(uuid);

        // Then
        assertThat(response).isPresent();
        assertThat(response.get().ticketNumber()).isEqualTo("C123456");
    }

    @Test
    void shouldReturnEmptyWhenTicketNotFound() {
        // Given
        UUID uuid = UUID.randomUUID();
        when(ticketRepository.findByUuid(uuid)).thenReturn(Optional.empty());

        // When
        Optional<TicketResponse> response = ticketService.findByUuid(uuid);

        // Then
        assertThat(response).isEmpty();
    }

    @Test
    void shouldGetQueuePosition() {
        // Given
        String ticketNumber = "C123456";
        when(ticketRepository.findByTicketNumber(ticketNumber)).thenReturn(Optional.of(ticket));
        when(ticketRepository.countTicketsAheadInQueue(any(), any(), any())).thenReturn(2L);
        when(advisorRepository.countByStatusAndQueueType(any(), any())).thenReturn(1L);

        // When
        Optional<QueuePositionResponse> response = ticketService.getQueuePosition(ticketNumber);

        // Then
        assertThat(response).isPresent();
        assertThat(response.get().ticketNumber()).isEqualTo("C123456");
        assertThat(response.get().currentPosition()).isEqualTo(3); // 2 ahead + 1
        assertThat(response.get().status()).isEqualTo("EN_ESPERA");
    }

    @Test
    void shouldCalculateEstimatedWaitWithNoAdvisors() {
        // Given
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
        when(ticketRepository.countTicketsAheadInQueue(any(), any(), any())).thenReturn(4L);
        when(advisorRepository.countByStatusAndQueueType(any(), any())).thenReturn(0L);
        doNothing().when(telegramService).sendConfirmationMessage(any());
        doNothing().when(auditService).logTicketCreated(any(), any(), any());

        // When
        TicketResponse response = ticketService.createTicket(request);

        // Then
        // With 5 people in queue (4 ahead + 1) and 0 advisors, wait time should be 5 * 5 = 25 minutes
        assertThat(response.estimatedWaitMinutes()).isEqualTo(25);
    }
}