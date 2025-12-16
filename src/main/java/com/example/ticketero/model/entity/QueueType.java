package com.example.ticketero.model.entity;

public enum QueueType {
    CAJA(5, 1),
    PERSONAL_BANKER(15, 2),
    EMPRESAS(20, 2),
    GERENCIA(30, 3);

    private final int averageTimeMinutes;
    private final int priority;

    QueueType(int averageTimeMinutes, int priority) {
        this.averageTimeMinutes = averageTimeMinutes;
        this.priority = priority;
    }

    public int getAverageTimeMinutes() {
        return averageTimeMinutes;
    }

    public int getPriority() {
        return priority;
    }
}