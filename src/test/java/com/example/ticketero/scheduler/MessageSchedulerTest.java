package com.example.ticketero.scheduler;

import com.example.ticketero.model.entity.Mensaje;
import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.enums.MessageStatus;
import com.example.ticketero.model.enums.MessageTemplate;
import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.repository.MensajeRepository;
import com.example.ticketero.service.TelegramService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageSchedulerTest {

    @Mock
    private MensajeRepository mensajeRepository;

    @Mock
    private TelegramService telegramService;

    @InjectMocks
    private MessageScheduler messageScheduler;

    @Test
    void shouldProcessPendingMessagesSuccessfully() {
        // Given
        Ticket ticket = Ticket.builder()
                .numero("C01")
                .telefono("+56912345678")
                .queueType(QueueType.CAJA)
                .build();

        Mensaje mensaje = Mensaje.builder()
                .id(1L)
                .ticket(ticket)
                .plantilla(MessageTemplate.TOTEM_TICKET_CREADO)
                .estadoEnvio(MessageStatus.PENDIENTE)
                .intentos(0)
                .build();

        when(mensajeRepository.findPendingMessagesToSend(any(LocalDateTime.class)))
                .thenReturn(List.of(mensaje));
        when(telegramService.extraerChatId(anyString()))
                .thenReturn("56912345678");
        when(telegramService.obtenerTextoMensaje(any(MessageTemplate.class), any(Ticket.class)))
                .thenReturn("Test message");
        when(telegramService.enviarMensaje(anyString(), anyString()))
                .thenReturn("123");

        // When
        messageScheduler.procesarMensajesPendientes();

        // Then
        verify(mensajeRepository).findPendingMessagesToSend(any(LocalDateTime.class));
        verify(telegramService).enviarMensaje("56912345678", "Test message");
        verify(mensajeRepository).save(argThat(m -> 
            m.getEstadoEnvio() == MessageStatus.ENVIADO && 
            "123".equals(m.getTelegramMessageId())
        ));
    }

    @Test
    void shouldHandleFailedMessageWithRetry() {
        // Given
        Ticket ticket = Ticket.builder()
                .numero("C01")
                .telefono("+56912345678")
                .queueType(QueueType.CAJA)
                .build();

        Mensaje mensaje = Mensaje.builder()
                .id(1L)
                .ticket(ticket)
                .plantilla(MessageTemplate.TOTEM_TICKET_CREADO)
                .estadoEnvio(MessageStatus.PENDIENTE)
                .intentos(0)
                .build();

        when(mensajeRepository.findPendingMessagesToSend(any(LocalDateTime.class)))
                .thenReturn(List.of(mensaje));
        when(telegramService.extraerChatId(anyString()))
                .thenReturn("56912345678");
        when(telegramService.obtenerTextoMensaje(any(MessageTemplate.class), any(Ticket.class)))
                .thenReturn("Test message");
        when(telegramService.enviarMensaje(anyString(), anyString()))
                .thenReturn(null); // Simular fallo

        // When
        messageScheduler.procesarMensajesPendientes();

        // Then
        verify(mensajeRepository).save(argThat(m -> 
            m.getIntentos() == 1 && 
            m.getEstadoEnvio() == MessageStatus.PENDIENTE &&
            m.getFechaProgramada().isAfter(LocalDateTime.now())
        ));
    }

    @Test
    void shouldMarkMessageAsFailedAfterMaxRetries() {
        // Given
        Ticket ticket = Ticket.builder()
                .numero("C01")
                .telefono("+56912345678")
                .queueType(QueueType.CAJA)
                .build();

        Mensaje mensaje = Mensaje.builder()
                .id(1L)
                .ticket(ticket)
                .plantilla(MessageTemplate.TOTEM_TICKET_CREADO)
                .estadoEnvio(MessageStatus.PENDIENTE)
                .intentos(2) // Ya tiene 2 intentos
                .build();

        when(mensajeRepository.findPendingMessagesToSend(any(LocalDateTime.class)))
                .thenReturn(List.of(mensaje));
        when(telegramService.extraerChatId(anyString()))
                .thenReturn("56912345678");
        when(telegramService.obtenerTextoMensaje(any(MessageTemplate.class), any(Ticket.class)))
                .thenReturn("Test message");
        when(telegramService.enviarMensaje(anyString(), anyString()))
                .thenReturn(null); // Simular fallo

        // When
        messageScheduler.procesarMensajesPendientes();

        // Then
        verify(mensajeRepository).save(argThat(m -> 
            m.getIntentos() == 3 && 
            m.getEstadoEnvio() == MessageStatus.FALLIDO
        ));
    }
}