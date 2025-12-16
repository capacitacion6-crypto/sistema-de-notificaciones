package com.example.ticketero.service;

import com.example.ticketero.model.dto.response.DashboardResponse;
import com.example.ticketero.model.entity.*;
import com.example.ticketero.repository.AdvisorRepository;
import com.example.ticketero.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private AdvisorRepository advisorRepository;

    @Mock
    private AdvisorService advisorService;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void shouldGenerateDashboardSuccessfully() {
        // Given
        Ticket ticket1 = Ticket.builder()
            .id(1L)
            .status(TicketStatus.WAITING)
            .queueType(QueueType.CAJA)
            .estimatedWaitMinutes(15)
            .createdAt(LocalDateTime.now())
            .build();

        Advisor advisor1 = Advisor.builder()
            .id(1L)
            .name("Test Advisor")
            .status(AdvisorStatus.AVAILABLE)
            .queueType(QueueType.CAJA)
            .build();

        when(ticketRepository.findAll()).thenReturn(List.of(ticket1));
        when(advisorRepository.findAll()).thenReturn(List.of(advisor1));
        when(ticketRepository.countByStatusAndQueueType(any(), any())).thenReturn(1L);
        when(advisorRepository.countByStatusAndQueueType(any(), any())).thenReturn(1L);
        when(advisorService.getAllAdvisors()).thenReturn(List.of());

        // When
        DashboardResponse dashboard = dashboardService.getDashboard();

        // Then
        assertThat(dashboard).isNotNull();
        assertThat(dashboard.summary()).isNotNull();
        assertThat(dashboard.queueStats()).hasSize(4); // 4 queue types
        assertThat(dashboard.lastUpdated()).isNotNull();
    }
}