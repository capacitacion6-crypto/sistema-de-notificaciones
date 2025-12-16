package com.example.ticketero.scheduler;

import com.example.ticketero.model.entity.Mensaje;
import com.example.ticketero.model.enums.MessageStatus;
import com.example.ticketero.repository.MensajeRepository;
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

    private final MensajeRepository mensajeRepository;
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
        
        List<Mensaje> mensajesPendientes = mensajeRepository.findPendingMessagesToSend(now);
        
        if (mensajesPendientes.isEmpty()) {
            log.debug("No pending messages to process");
            return;
        }
        
        log.info("Processing {} pending messages", mensajesPendientes.size());
        
        for (Mensaje mensaje : mensajesPendientes) {
            procesarMensaje(mensaje);
        }
    }

    /**
     * Procesa un mensaje individual con manejo de reintentos.
     */
    private void procesarMensaje(Mensaje mensaje) {
        try {
            String chatId = telegramService.extraerChatId(mensaje.getTicket().getTelefono());
            String texto = telegramService.obtenerTextoMensaje(
                mensaje.getPlantilla(), 
                mensaje.getTicket()
            );
            
            log.debug("Processing message ID: {} for ticket: {}", 
                     mensaje.getId(), mensaje.getTicket().getNumero());
            
            String telegramMessageId = telegramService.enviarMensaje(chatId, texto);
            
            if (telegramMessageId != null) {
                // Envío exitoso
                mensaje.setEstadoEnvio(MessageStatus.ENVIADO);
                mensaje.setFechaEnvio(LocalDateTime.now());
                mensaje.setTelegramMessageId(telegramMessageId);
                
                log.info("Message sent successfully for ticket: {}, telegramMessageId: {}", 
                        mensaje.getTicket().getNumero(), telegramMessageId);
            } else {
                // Envío falló - incrementar intentos
                manejarFalloEnvio(mensaje);
            }
            
            mensajeRepository.save(mensaje);
            
        } catch (Exception e) {
            log.error("Error processing message ID: {} for ticket: {}", 
                     mensaje.getId(), mensaje.getTicket().getNumero(), e);
            manejarFalloEnvio(mensaje);
            mensajeRepository.save(mensaje);
        }
    }

    /**
     * Maneja fallos de envío con reintentos y backoff exponencial.
     * RN-007: Hasta 3 reintentos antes de marcar como FALLIDO.
     * RN-008: Backoff exponencial: 30s, 60s, 120s.
     */
    private void manejarFalloEnvio(Mensaje mensaje) {
        int intentos = mensaje.getIntentos() + 1;
        mensaje.setIntentos(intentos);
        
        if (intentos >= 3) {
            // Máximo de reintentos alcanzado
            mensaje.setEstadoEnvio(MessageStatus.FALLIDO);
            log.error("Message failed permanently after {} attempts for ticket: {}", 
                     intentos, mensaje.getTicket().getNumero());
        } else {
            // Programar reintento con backoff exponencial
            LocalDateTime nextRetry = calcularProximoReintento(intentos);
            mensaje.setFechaProgramada(nextRetry);
            
            log.warn("Message failed, scheduling retry {} at {} for ticket: {}", 
                    intentos, nextRetry, mensaje.getTicket().getNumero());
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
        
        List<Mensaje> mensajesAntiguos = mensajeRepository.findByCreatedAtAfter(cutoffDate);
        
        if (!mensajesAntiguos.isEmpty()) {
            log.info("Cleaning up {} old messages", mensajesAntiguos.size());
            // TODO: Implementar limpieza si es necesario
        }
    }
}