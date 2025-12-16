package com.example.ticketero.model.enums;

/**
 * Tipos de cola disponibles en el sistema.
 * Cada tipo tiene un tiempo promedio de atención y prioridad específica.
 */
public enum QueueType {
    CAJA("Caja", 5, 1, "C"),
    PERSONAL_BANKER("Personal Banker", 15, 2, "P"),
    EMPRESAS("Empresas", 20, 3, "E"),
    GERENCIA("Gerencia", 30, 4, "G");

    private final String displayName;
    private final int averageTimeMinutes;
    private final int priority;
    private final String prefix;

    QueueType(String displayName, int averageTimeMinutes, int priority, String prefix) {
        this.displayName = displayName;
        this.averageTimeMinutes = averageTimeMinutes;
        this.priority = priority;
        this.prefix = prefix;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getAverageTimeMinutes() {
        return averageTimeMinutes;
    }

    public int getPriority() {
        return priority;
    }

    public String getPrefix() {
        return prefix;
    }
}