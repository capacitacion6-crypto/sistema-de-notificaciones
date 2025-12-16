package com.example.ticketero.model.entity;

import com.example.ticketero.model.enums.MessageStatus;
import com.example.ticketero.model.enums.MessageTemplate;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad que representa un mensaje programado para envío vía Telegram.
 */
@Entity
@Table(name = "mensaje")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    @ToString.Exclude
    private Ticket ticket;

    @Enumerated(EnumType.STRING)
    @Column(name = "plantilla", nullable = false, length = 50)
    private MessageTemplate plantilla;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_envio", nullable = false, length = 20)
    private MessageStatus estadoEnvio;

    @Column(name = "fecha_programada", nullable = false)
    private LocalDateTime fechaProgramada;

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    @Column(name = "telegram_message_id", length = 50)
    private String telegramMessageId;

    @Column(name = "intentos", nullable = false)
    @Builder.Default
    private Integer intentos = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.estadoEnvio == null) {
            this.estadoEnvio = MessageStatus.PENDIENTE;
        }
    }
}