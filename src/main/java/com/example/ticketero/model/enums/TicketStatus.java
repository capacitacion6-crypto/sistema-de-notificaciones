package com.example.ticketero.model.enums;

/**
 * Estados posibles de un ticket en el sistema.
 * Los estados activos son: EN_ESPERA, PROXIMO, ATENDIENDO
 */
public enum TicketStatus {
    EN_ESPERA("Esperando asignación", true),
    PROXIMO("Próximo a ser atendido", true),
    ATENDIENDO("Siendo atendido", true),
    COMPLETADO("Atención finalizada", false),
    CANCELADO("Cancelado", false),
    NO_ATENDIDO("Cliente no se presentó", false);

    private final String description;
    private final boolean isActive;

    TicketStatus(String description, boolean isActive) {
        this.description = description;
        this.isActive = isActive;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return isActive;
    }
}