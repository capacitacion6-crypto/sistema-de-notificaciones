package com.example.ticketero.model.enums;

/**
 * Plantillas de mensajes para Telegram.
 */
public enum MessageTemplate {
    TOTEM_TICKET_CREADO("totem_ticket_creado", "Confirmación de creación"),
    TOTEM_PROXIMO_TURNO("totem_proximo_turno", "Pre-aviso"),
    TOTEM_ES_TU_TURNO("totem_es_tu_turno", "Turno activo");

    private final String templateName;
    private final String description;

    MessageTemplate(String templateName, String description) {
        this.templateName = templateName;
        this.description = description;
    }

    public String getTemplateName() {
        return templateName;
    }

    public String getDescription() {
        return description;
    }
}