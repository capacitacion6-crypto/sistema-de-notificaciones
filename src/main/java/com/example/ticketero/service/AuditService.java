package com.example.ticketero.service;

import com.example.ticketero.model.entity.AuditEvent;
import com.example.ticketero.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    @Transactional
    public void logEvent(String eventType, String entityType, Long entityId, 
                        String actor, String oldValue, String newValue, String description) {
        
        AuditEvent event = AuditEvent.builder()
            .eventType(eventType)
            .entityType(entityType)
            .entityId(entityId)
            .actor(actor)
            .oldValue(oldValue)
            .newValue(newValue)
            .description(description)
            .build();

        auditEventRepository.save(event);
        log.debug("Audit event logged: {} for {} {}", eventType, entityType, entityId);
    }

    @Transactional
    public void logTicketCreated(Long ticketId, String ticketNumber, String customerRut) {
        logEvent("TICKET_CREATED", "TICKET", ticketId, "SYSTEM", 
                null, ticketNumber, "Ticket created for customer: " + customerRut);
    }

    @Transactional
    public void logTicketAssigned(Long ticketId, String ticketNumber, Long advisorId, String advisorName) {
        logEvent("TICKET_ASSIGNED", "TICKET", ticketId, "SYSTEM", 
                "WAITING", "ASSIGNED", "Ticket assigned to advisor: " + advisorName);
    }

    @Transactional
    public void logTicketCompleted(Long ticketId, String ticketNumber) {
        logEvent("TICKET_COMPLETED", "TICKET", ticketId, "SYSTEM", 
                "ASSIGNED", "COMPLETED", "Ticket completed");
    }

    @Transactional
    public void logAdvisorStatusChanged(Long advisorId, String advisorName, String oldStatus, String newStatus) {
        logEvent("ADVISOR_STATUS_CHANGED", "ADVISOR", advisorId, "ADMIN", 
                oldStatus, newStatus, "Advisor status changed: " + advisorName);
    }

    @Transactional
    public void logMessageSent(Long messageId, Long ticketId, String messageType, String deliveryStatus) {
        logEvent("MESSAGE_SENT", "MESSAGE", messageId, "SYSTEM", 
                "PENDING", deliveryStatus, "Message " + messageType + " for ticket ID: " + ticketId);
    }
}