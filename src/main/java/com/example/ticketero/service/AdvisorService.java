package com.example.ticketero.service;

import com.example.ticketero.model.dto.response.AdvisorResponse;
import com.example.ticketero.model.entity.Advisor;
import com.example.ticketero.model.entity.AdvisorStatus;
import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.entity.TicketStatus;
import com.example.ticketero.repository.AdvisorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdvisorService {

    private final AdvisorRepository advisorRepository;
    private final AssignmentService assignmentService;
    private final AuditService auditService;

    public List<AdvisorResponse> getAllAdvisors() {
        return advisorRepository.findAll()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public void updateAdvisorStatus(Long advisorId, AdvisorStatus newStatus) {
        Optional<Advisor> advisorOpt = advisorRepository.findById(advisorId);
        if (advisorOpt.isEmpty()) {
            throw new IllegalArgumentException("Advisor not found: " + advisorId);
        }

        Advisor advisor = advisorOpt.get();
        AdvisorStatus oldStatus = advisor.getStatus();
        advisor.setStatus(newStatus);
        advisor.setUpdatedAt(LocalDateTime.now());

        // Log audit event (RF-008)
        auditService.logAdvisorStatusChanged(advisor.getId(), advisor.getName(), 
            oldStatus.name(), newStatus.name());

        log.info("Advisor {} status changed from {} to {}", 
            advisor.getName(), oldStatus, newStatus);

        // If advisor becomes available, try to assign next ticket
        if (newStatus == AdvisorStatus.AVAILABLE && oldStatus != AdvisorStatus.AVAILABLE) {
            assignmentService.assignNextTicket(advisorId);
        }
    }

    private AdvisorResponse toResponse(Advisor advisor) {
        Ticket currentTicket = advisor.getTickets()
            .stream()
            .filter(t -> t.getStatus() == TicketStatus.ASSIGNED || t.getStatus() == TicketStatus.IN_PROGRESS)
            .findFirst()
            .orElse(null);

        return new AdvisorResponse(
            advisor.getId(),
            advisor.getName(),
            advisor.getModuleNumber(),
            advisor.getQueueType().name(),
            advisor.getStatus().name(),
            currentTicket != null ? currentTicket.getId() : null,
            currentTicket != null ? currentTicket.getTicketNumber() : null
        );
    }
}