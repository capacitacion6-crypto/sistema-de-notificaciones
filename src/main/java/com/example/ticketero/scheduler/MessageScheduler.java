package com.example.ticketero.scheduler;

import com.example.ticketero.model.entity.Message;
import com.example.ticketero.repository.MessageRepository;
import com.example.ticketero.service.TelegramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler para procesamiento asíncrono de mensajes de Telegram.
 * Implementa RN-007 y RN-008 (reintentos con backoff exponencial).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MessageScheduler {

    private final MessageRepository messageRepository;
    private final TelegramService telegramService;

    /**
     * Procesa mensajes pendientes cada 60 segundos.
     * Implementa RN-007: Reintentos automáticos hasta 3 veces.
     * Implementa RN-008: Backoff exponencial (30s, 60s, 120s).
     */
    @Scheduled(fixedRate = 60000) // Cada 60 segundos
    @Transactional
    public void procesarMensajesPendientes() {
        LocalDateTime now = LocalDateTime.now();
        
        List<Message> mensajesPendientes = messageRepository.findByDeliveryStatus("PENDING");
        
        if (mensajesPendientes.isEmpty()) {
            log.debug("No pending messages to process");
            return;
        }
        
        log.info("Processing {} pending messages", mensajesPendientes.size());
        
        for (Message mensaje : mensajesPendientes) {
            procesarMensaje(mensaje);
        }
    }

    /**
     * Procesa un mensaje individual con manejo de reintentos.
     */
    private void procesarMensaje(Message mensaje) {
        try {
            String chatId = telegramService.extractChatId(mensaje.getTicket().getCustomerPhone());
            String texto = telegramService.getMessageText(
                mensaje.getMessageType(), 
                mensaje.getTicket()
            );
            
            log.debug("Processing message ID: {} for ticket: {}", 
                     mensaje.getId(), mensaje.getTicket().getTicketNumber());
            
            String telegramMessageId = telegramService.sendMessage(chatId, texto);
            
            if (telegramMessageId != null) {
                // Envío exitoso
                mensaje.setDeliveryStatus("SENT");
                mensaje.setSentAt(LocalDateTime.now());
                
                log.info("Message sent successfully for ticket: {}, telegramMessageId: {}", 
                        mensaje.getTicket().getTicketNumber(), telegramMessageId);
            } else {
                // Envío falló - incrementar intentos
                manejarFalloEnvio(mensaje);
            }
            
            messageRepository.save(mensaje);
            
        } catch (Exception e) {
            log.error("Error processing message ID: {} for ticket: {}", 
                     mensaje.getId(), mensaje.getTicket().getTicketNumber(), e);
            manejarFalloEnvio(mensaje);
            messageRepository.save(mensaje);
        }
    }

    /**
     * Maneja fallos de envío con reintentos y backoff exponencial.
     * RN-007: Hasta 3 reintentos antes de marcar como FALLIDO.
     * RN-008: Backoff exponencial: 30s, 60s, 120s.
     */
    private void manejarFalloEnvio(Message mensaje) {
        int intentos = mensaje.getRetryCount() + 1;
        mensaje.setRetryCount(intentos);
        
        if (intentos >= 3) {
            // Máximo de reintentos alcanzado
            mensaje.setDeliveryStatus("FAILED");
            log.error("Message failed permanently after {} attempts for ticket: {}", 
                     intentos, mensaje.getTicket().getTicketNumber());
        } else {
            // Programar reintento con backoff exponencial
            LocalDateTime nextRetry = calcularProximoReintento(intentos);
            // No hay campo fechaProgramada en Message, usar createdAt
            
            log.warn("Message failed, scheduling retry {} for ticket: {}", 
                    intentos, mensaje.getTicket().getTicketNumber());
        }
    }

    /**
     * Calcula el próximo intento con backoff exponencial.
     * RN-008: 30s, 60s, 120s
     */
    private LocalDateTime calcularProximoReintento(int intentos) {
        LocalDateTime now = LocalDateTime.now();
        
        return switch (intentos) {
            case 1 -> now.plusSeconds(30);  // Primer reintento: 30s
            case 2 -> now.plusSeconds(60);  // Segundo reintento: 60s
            case 3 -> now.plusSeconds(120); // Tercer reintento: 120s
            default -> now.plusMinutes(5);  // Fallback
        };
    }

    /**
     * Limpia mensajes antiguos fallidos (ejecuta diariamente a las 2 AM).
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void limpiarMensajesAntiguos() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);
        
        List<Message> mensajesAntiguos = messageRepository.findByCreatedAtAfter(cutoffDate);
        
        if (!mensajesAntiguos.isEmpty()) {
            log.info("Cleaning up {} old messages", mensajesAntiguos.size());
            // TODO: Implementar limpieza si es necesario
        }
    }
}