package com.example.ticketero.service;

import com.example.ticketero.config.TelegramConfig;
import com.example.ticketero.model.entity.*;
import com.example.ticketero.model.enums.MessageType;
import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelegramServiceTest {

    @Mock
    private TelegramConfig telegramConfig;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private MessageRepository messageRepository;

    private TelegramService telegramService;

    @BeforeEach
    void setUp() {
        telegramService = new TelegramService(telegramConfig, restTemplate, messageRepository, "123456789");
    }

    @Test
    void shouldSendConfirmationMessage() {
        // Given
        Ticket ticket = Ticket.builder()
            .ticketNumber("C123456")
            .customerPhone("+56912345678")
            .queueType(QueueType.CAJA)
            .queuePosition(5)
            .estimatedWaitMinutes(25)
            .build();

        Message savedMessage = Message.builder()
            .id(1L)
            .ticket(ticket)
            .messageType(MessageType.CONFIRMATION)
            .content("Test message")
            .build();

        when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);
        when(telegramConfig.getBotToken()).thenReturn("");

        // When
        telegramService.sendConfirmationMessage(ticket);

        // Then
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void shouldSkipMessageWhenNoPhoneNumber() {
        // Given
        Ticket ticket = Ticket.builder()
            .ticketNumber("C123456")
            .customerPhone(null)
            .queueType(QueueType.CAJA)
            .build();

        // When
        telegramService.sendConfirmationMessage(ticket);

        // Then
        verify(messageRepository, never()).save(any(Message.class));
    }
}