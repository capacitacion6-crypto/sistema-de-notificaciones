package com.example.ticketero.service;

import com.example.ticketero.model.entity.*;
import com.example.ticketero.model.enums.AdvisorStatus;
import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.model.enums.TicketStatus;
import com.example.ticketero.repository.AdvisorRepository;
import com.example.ticketero.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AssignmentService {

    private final TicketRepository ticketRepository;
    private final AdvisorRepository advisorRepository;
    private final TelegramService telegramService;
    private final AuditService auditService;

    @Transactional
    public void assignNextTicket(Long advisorId) {
        Optional<Advisor> advisorOpt = advisorRepository.findById(advisorId);
        if (advisorOpt.isEmpty()) {
            log.warn("Advisor not found: {}", advisorId);
            return;
        }

        Advisor advisor = advisorOpt.get();
        if (advisor.getStatus() != AdvisorStatus.AVAILABLE) {
            log.warn("Advisor {} is not available", advisorId);
            return;
        }

        Optional<Ticket> nextTicket = findNextTicketForAdvisor(advisor);
        if (nextTicket.isEmpty()) {
            log.info("No tickets waiting for advisor {} ({})", advisor.getName(), advisor.getQueueType());
            return;
        }

        assignTicketToAdvisor(nextTicket.get(), advisor);
    }

    @Transactional
    public void processQueueUpdates() {
        // Check for pre-notice messages (position <= 3)
        List<Ticket> waitingTickets = ticketRepository.findByStatus(TicketStatus.EN_ESPERA);

        for (Ticket ticket : waitingTickets) {
            updateQueuePosition(ticket);
            
            if (ticket.getQueuePosition() != null && ticket.getQueuePosition() <= 3) {
                // Check if pre-notice already sent
                boolean preNoticeSent = ticket.getMessages().stream()
                    .anyMatch(msg -> "PRE_NOTICE".equals(msg.getMessageType()) && "SENT".equals(msg.getDeliveryStatus()));
                
                if (!preNoticeSent) {
                    telegramService.sendPreNoticeMessage(ticket);
                }
            }
        }
    }

    private Optional<Ticket> findNextTicketForAdvisor(Advisor advisor) {
        // Priority order: GERENCIA > EMPRESAS > PERSONAL_BANKER > CAJA
        List<QueueType> priorityOrder = Arrays.asList(
            QueueType.GERENCIA,
            QueueType.EMPRESAS, 
            QueueType.PERSONAL_BANKER,
            QueueType.CAJA
        );

        for (QueueType queueType : priorityOrder) {
            if (advisor.getQueueType() == queueType) {
                List<Ticket> tickets = ticketRepository.findByStatusAndQueueTypeOrderByCreatedAtAsc(
                    TicketStatus.EN_ESPERA, queueType
                );
                if (!tickets.isEmpty()) {
                    return Optional.of(tickets.get(0));
                }
            }
        }

        return Optional.empty();
    }

    private void assignTicketToAdvisor(Ticket ticket, Advisor advisor) {
        // Update ticket
        ticket.setStatus(TicketStatus.ATENDIENDO);
        ticket.setAdvisor(advisor);
        ticket.setAssignedAt(LocalDateTime.now());

        // Update advisor
        advisor.setStatus(AdvisorStatus.BUSY);
        advisor.setUpdatedAt(LocalDateTime.now());

        // Send turn active message (RF-002 - Message 3)
        telegramService.sendTurnActiveMessage(ticket);

        // Log audit event (RF-008)
        auditService.logTicketAssigned(ticket.getId(), ticket.getTicketNumber(), 
            advisor.getId(), advisor.getName());

        log.info("Ticket {} assigned to advisor {} (Module {})", 
            ticket.getTicketNumber(), advisor.getName(), advisor.getModuleNumber());
    }

    @Transactional
    public void completeTicket(Long ticketId) {
        Optional<Ticket> ticketOpt = ticketRepository.findById(ticketId);
        if (ticketOpt.isEmpty()) {
            log.warn("Ticket not found: {}", ticketId);
            return;
        }

        Ticket ticket = ticketOpt.get();
        ticket.setStatus(TicketStatus.COMPLETADO);
        ticket.setCompletedAt(LocalDateTime.now());

        if (ticket.getAdvisor() != null) {
            ticket.getAdvisor().setStatus(AdvisorStatus.AVAILABLE);
            ticket.getAdvisor().setUpdatedAt(LocalDateTime.now());
            
            // Try to assign next ticket automatically
            assignNextTicket(ticket.getAdvisor().getId());
        }

        // Log audit event (RF-008)
        auditService.logTicketCompleted(ticket.getId(), ticket.getTicketNumber());

        log.info("Ticket {} completed", ticket.getTicketNumber());
    }

    private void updateQueuePosition(Ticket ticket) {
        if (ticket.getStatus() != TicketStatus.EN_ESPERA) {
            return;
        }

        long position = ticketRepository.countTicketsAheadInQueue(
            TicketStatus.EN_ESPERA, 
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
}