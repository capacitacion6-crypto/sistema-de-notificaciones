package com.example.ticketero.service;

import com.example.ticketero.model.entity.Mensaje;
import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.enums.MessageTemplate;
import com.example.ticketero.repository.MensajeRepository;
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

    private final MensajeRepository mensajeRepository;
    private final TelegramService telegramService;

    /**
     * Programa el mensaje de pre-aviso cuando un ticket cambia a PROXIMO.
     * Se llama desde QueueProcessorScheduler cuando posición <= 3.
     */
    @Transactional
    public void programarMensajePreAviso(Ticket ticket) {
        // Buscar mensaje de pre-aviso existente
        ticket.getMensajes().stream()
            .filter(m -> m.getPlantilla() == MessageTemplate.TOTEM_PROXIMO_TURNO)
            .findFirst()
            .ifPresent(mensaje -> {
                // Actualizar fecha para envío inmediato
                mensaje.setFechaProgramada(LocalDateTime.now());
                mensajeRepository.save(mensaje);
                
                log.info("Pre-aviso message scheduled for immediate sending for ticket: {}", 
                        ticket.getNumero());
            });
    }

    /**
     * Programa el mensaje de turno activo cuando un ticket es asignado a asesor.
     * Se llama desde QueueManagementService al asignar ticket.
     */
    @Transactional
    public void programarMensajeTurnoActivo(Ticket ticket) {
        // Buscar mensaje de turno activo existente
        ticket.getMensajes().stream()
            .filter(m -> m.getPlantilla() == MessageTemplate.TOTEM_ES_TU_TURNO)
            .findFirst()
            .ifPresent(mensaje -> {
                // Actualizar fecha para envío inmediato
                mensaje.setFechaProgramada(LocalDateTime.now());
                mensajeRepository.save(mensaje);
                
                log.info("Active turn message scheduled for immediate sending for ticket: {}", 
                        ticket.getNumero());
            });
    }

    /**
     * Envía un mensaje inmediato (sin programar).
     * Útil para notificaciones urgentes o de prueba.
     */
    public boolean enviarMensajeInmediato(Ticket ticket, MessageTemplate plantilla) {
        try {
            String chatId = telegramService.extraerChatId(ticket.getTelefono());
            String texto = telegramService.obtenerTextoMensaje(plantilla, ticket);
            
            String telegramMessageId = telegramService.enviarMensaje(chatId, texto);
            
            if (telegramMessageId != null) {
                log.info("Immediate message sent successfully for ticket: {}, template: {}", 
                        ticket.getNumero(), plantilla);
                return true;
            } else {
                log.error("Failed to send immediate message for ticket: {}, template: {}", 
                         ticket.getNumero(), plantilla);
                return false;
            }
        } catch (Exception e) {
            log.error("Error sending immediate message for ticket: {}, template: {}", 
                     ticket.getNumero(), plantilla, e);
            return false;
        }
    }
}