package com.example.ticketero.service;

import com.example.ticketero.config.TelegramConfig;
import com.example.ticketero.model.entity.Message;
import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.enums.MessageTemplate;
import com.example.ticketero.model.enums.MessageType;
import com.example.ticketero.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TelegramService {

    private final TelegramConfig telegramConfig;
    private final RestTemplate restTemplate;
    private final MessageRepository messageRepository;

    @Transactional
    public void sendConfirmationMessage(Ticket ticket) {
        String message = """
            ✅ Ticket confirmado
            
            📋 Número: %s
            🏦 Cola: %s
            📍 Posición: #%d
            ⏱️ Tiempo estimado: %d minutos
            
            Puedes salir de la sucursal. Te avisaremos cuando sea tu turno.
            """.formatted(
                ticket.getTicketNumber(),
                getQueueDisplayName(ticket.getQueueType().name()),
                ticket.getQueuePosition(),
                ticket.getEstimatedWaitMinutes()
            );

        sendMessage(ticket, MessageType.CONFIRMATION, message);
    }

    @Transactional
    public void sendPreNoticeMessage(Ticket ticket) {
        String message = """
            ⏰ ¡Pronto será tu turno!
            
            📋 Ticket: %s
            📍 Quedan 3 personas adelante
            
            Por favor acércate a la sucursal.
            """.formatted(ticket.getTicketNumber());

        sendMessage(ticket, MessageType.PRE_NOTICE, message);
    }

    @Transactional
    public void sendTurnActiveMessage(Ticket ticket) {
        String message = """
            🔔 ¡ES TU TURNO!
            
            📋 Ticket: %s
            👤 Asesor: %s
            🏢 Módulo: %d
            
            Preséntate en el módulo indicado.
            """.formatted(
                ticket.getTicketNumber(),
                ticket.getAdvisor().getName(),
                ticket.getAdvisor().getModuleNumber()
            );

        sendMessage(ticket, MessageType.TURN_ACTIVE, message);
    }

    private void sendMessage(Ticket ticket, MessageType messageType, String content) {
        if (ticket.getCustomerPhone() == null || ticket.getCustomerPhone().isEmpty()) {
            log.warn("No phone number for ticket {}, skipping message", ticket.getTicketNumber());
            return;
        }

        Message message = Message.builder()
            .ticket(ticket)
            .messageType(messageType)
            .content(content)
            .build();

        Message saved = messageRepository.save(message);

        try {
            sendToTelegram(ticket.getCustomerPhone(), content);
            saved.setSentAt(LocalDateTime.now());
            saved.setDeliveryStatus("SENT");
            log.info("Message sent successfully for ticket {}", ticket.getTicketNumber());
        } catch (Exception e) {
            saved.setDeliveryStatus("FAILED");
            log.error("Failed to send message for ticket {}: {}", ticket.getTicketNumber(), e.getMessage());
        }
    }

    private void sendToTelegram(String phoneNumber, String text) {
        if (telegramConfig.getBotToken() == null || telegramConfig.getBotToken().isEmpty()) {
            log.warn("Telegram bot token not configured, simulating message send");
            return;
        }

        String url = telegramConfig.getFullApiUrl() + "/sendMessage";
        
        Map<String, Object> payload = Map.of(
            "chat_id", phoneNumber,
            "text", text,
            "parse_mode", "HTML"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Telegram API error: " + response.getStatusCode());
        }
    }

    private String getQueueDisplayName(String queueType) {
        return switch (queueType) {
            case "CAJA" -> "Caja";
            case "PERSONAL_BANKER" -> "Personal Banker";
            case "EMPRESAS" -> "Empresas";
            case "GERENCIA" -> "Gerencia";
            default -> queueType;
        };
    }

    public String extractChatId(String phoneNumber) {
        // En un caso real, esto buscaría en una tabla de mapeo teléfono -> chat_id
        // Por ahora, simulamos que el teléfono es el chat_id
        return phoneNumber;
    }

    public String getMessageText(MessageType template, Ticket ticket) {
        return switch (template) {
            case CONFIRMATION -> String.format(
                "✅ Ticket confirmado\n\n📋 Número: %s\n🏦 Cola: %s\n📍 Posición: #%d\n⏱️ Tiempo estimado: %d minutos",
                ticket.getTicketNumber(),
                getQueueDisplayName(ticket.getQueueType().name()),
                ticket.getQueuePosition(),
                ticket.getEstimatedWaitMinutes()
            );
            case PRE_NOTICE -> String.format(
                "⏰ ¡Pronto será tu turno!\n\n📋 Ticket: %s\n📍 Quedan 3 personas adelante",
                ticket.getTicketNumber()
            );
            case TURN_ACTIVE -> String.format(
                "🔔 ¡ES TU TURNO!\n\n📋 Ticket: %s\n👤 Asesor: %s\n🏢 Módulo: %d",
                ticket.getTicketNumber(),
                ticket.getAdvisor().getName(),
                ticket.getAdvisor().getModuleNumber()
            );
        };
    }

    public String sendMessage(String chatId, String text) {
        try {
            sendToTelegram(chatId, text);
            return "msg_" + System.currentTimeMillis(); // Simular message ID
        } catch (Exception e) {
            log.error("Failed to send message: {}", e.getMessage());
            return null;
        }
    }
}