package com.example.ticketero.model.enums;

/**
 * Estados posibles de un asesor en el sistema.
 */
public enum AdvisorStatus {
    AVAILABLE("Disponible", true),
    BUSY("Atendiendo cliente", false),
    OFFLINE("No disponible", false);

    private final String description;
    private final boolean canReceiveAssignments;

    AdvisorStatus(String description, boolean canReceiveAssignments) {
        this.description = description;
        this.canReceiveAssignments = canReceiveAssignments;
    }

    public String getDescription() {
        return description;
    }

    public boolean canReceiveAssignments() {
        return canReceiveAssignments;
    }
}