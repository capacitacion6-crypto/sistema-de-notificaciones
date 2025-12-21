package com.example.ticketero.service;

import com.example.ticketero.model.entity.Message;
import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.enums.MessageType;
import com.example.ticketero.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Servicio para orquestación de notificaciones.
 * Centraliza la lógica de cuándo y cómo enviar mensajes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class NotificationService {

    private final MessageRepository messageRepository;
    private final TelegramService telegramService;

    /**
     * Programa el mensaje de pre-aviso cuando un ticket cambia a PROXIMO.
     * Se llama desde QueueProcessorScheduler cuando posición <= 3.
     */
    @Transactional
    public void programarMensajePreAviso(Ticket ticket) {
        // Buscar mensaje de pre-aviso existente
        ticket.getMessages().stream()
            .filter(m -> m.getMessageType() == MessageType.PRE_NOTICE)
            .findFirst()
            .ifPresent(mensaje -> {
                // Actualizar fecha para envío inmediato
                mensaje.setSentAt(LocalDateTime.now());
                messageRepository.save(mensaje);
                
                log.info("Pre-aviso message scheduled for immediate sending for ticket: {}", 
                        ticket.getTicketNumber());
            });
    }

    /**
     * Programa el mensaje de turno activo cuando un ticket es asignado a asesor.
     * Se llama desde QueueManagementService al asignar ticket.
     */
    @Transactional
    public void programarMensajeTurnoActivo(Ticket ticket) {
        // Buscar mensaje de turno activo existente
        ticket.getMessages().stream()
            .filter(m -> m.getMessageType() == MessageType.TURN_ACTIVE)
            .findFirst()
            .ifPresent(mensaje -> {
                // Actualizar fecha para envío inmediato
                mensaje.setSentAt(LocalDateTime.now());
                messageRepository.save(mensaje);
                
                log.info("Active turn message scheduled for immediate sending for ticket: {}", 
                        ticket.getTicketNumber());
            });
    }

    /**
     * Envía un mensaje inmediato (sin programar).
     * Útil para notificaciones urgentes o de prueba.
     */
    public boolean enviarMensajeInmediato(Ticket ticket, MessageType plantilla) {
        try {
            String chatId = telegramService.extractChatId(ticket.getCustomerPhone());
            String texto = telegramService.getMessageText(plantilla, ticket);
            
            String telegramMessageId = telegramService.sendMessage(chatId, texto);
            
            if (telegramMessageId != null) {
                log.info("Immediate message sent successfully for ticket: {}, template: {}", 
                        ticket.getTicketNumber(), plantilla);
                return true;
            } else {
                log.error("Failed to send immediate message for ticket: {}, template: {}", 
                         ticket.getTicketNumber(), plantilla);
                return false;
            }
        } catch (Exception e) {
            log.error("Error sending immediate message for ticket: {}, template: {}", 
                     ticket.getTicketNumber(), plantilla, e);
            return false;
        }
    }
}