package com.example.ticketero.model.enums;

/**
 * Estados de envío de mensajes.
 */
public enum MessageStatus {
    PENDIENTE("Pendiente de envío"),
    ENVIADO("Enviado exitosamente"),
    FALLIDO("Falló después de reintentos");

    private final String description;

    MessageStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}