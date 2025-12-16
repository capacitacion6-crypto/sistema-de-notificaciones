package com.example.ticketero.service;

import com.example.ticketero.model.dto.request.CreateTicketRequest;
import com.example.ticketero.model.dto.response.QueuePositionResponse;
import com.example.ticketero.model.dto.response.TicketResponse;
import com.example.ticketero.model.entity.*;
import com.example.ticketero.repository.AdvisorRepository;
import com.example.ticketero.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TicketService {

    private final TicketRepository ticketRepository;
    private final AdvisorRepository advisorRepository;
    private final TelegramService telegramService;
    private final AuditService auditService;

    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request) {
        log.info("Creating ticket for RUT: {}, Queue: {}", request.customerRut(), request.queueType());

        String ticketNumber = generateTicketNumber(request.queueType());
        
        Ticket ticket = Ticket.builder()
            .ticketNumber(ticketNumber)
            .customerRut(request.customerRut())
            .customerPhone(request.customerPhone())
            .queueType(request.queueType())
            .build();

        Ticket saved = ticketRepository.save(ticket);
        
        // Calculate position and estimated wait time
        updateQueuePosition(saved);
        
        // Send confirmation message (RF-002 - Message 1)
        telegramService.sendConfirmationMessage(saved);
        
        // Log audit event (RF-008)
        auditService.logTicketCreated(saved.getId(), saved.getTicketNumber(), saved.getCustomerRut());
        
        log.info("Ticket created: {} at position {}", saved.getTicketNumber(), saved.getQueuePosition());
        
        return toResponse(saved);
    }

    public Optional<TicketResponse> findByUuid(UUID uuid) {
        return ticketRepository.findByUuid(uuid)
            .map(this::toResponse);
    }

    public Optional<QueuePositionResponse> getQueuePosition(String ticketNumber) {
        return ticketRepository.findByTicketNumber(ticketNumber)
            .map(ticket -> {
                updateQueuePosition(ticket);
                return new QueuePositionResponse(
                    ticket.getTicketNumber(),
                    ticket.getQueuePosition(),
                    ticket.getEstimatedWaitMinutes(),
                    ticket.getStatus().name()
                );
            });
    }

    private void updateQueuePosition(Ticket ticket) {
        if (ticket.getStatus() != TicketStatus.WAITING) {
            return;
        }

        long position = ticketRepository.countTicketsAheadInQueue(
            TicketStatus.WAITING, 
            ticket.getQueueType(), 
            ticket.getId()
        ) + 1;

        long availableAdvisors = advisorRepository.countByStatusAndQueueType(
            AdvisorStatus.AVAILABLE, 
            ticket.getQueueType()
        );

        int estimatedWait = calculateEstimatedWait(position, ticket.getQueueType(), availableAdvisors);

        ticket.setQueuePosition((int) position);
        ticket.setEstimatedWaitMinutes(estimatedWait);
    }

    private int calculateEstimatedWait(long position, QueueType queueType, long availableAdvisors) {
        if (availableAdvisors == 0) {
            return queueType.getAverageTimeMinutes() * (int) position;
        }
        
        return (int) Math.ceil((double) position / availableAdvisors) * queueType.getAverageTimeMinutes();
    }

    private String generateTicketNumber(QueueType queueType) {
        String prefix = switch (queueType) {
            case CAJA -> "C";
            case PERSONAL_BANKER -> "PB";
            case EMPRESAS -> "E";
            case GERENCIA -> "G";
        };
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
        return prefix + timestamp;
    }

    private TicketResponse toResponse(Ticket ticket) {
        return new TicketResponse(
            ticket.getUuid(),
            ticket.getTicketNumber(),
            ticket.getQueueType().name(),
            ticket.getStatus().name(),
            ticket.getQueuePosition(),
            ticket.getEstimatedWaitMinutes(),
            ticket.getAdvisor() != null ? ticket.getAdvisor().getName() : null,
            ticket.getAdvisor() != null ? ticket.getAdvisor().getModuleNumber() : null,
            ticket.getCreatedAt()
        );
    }
}