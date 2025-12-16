package com.example.ticketero.exception;

import java.util.UUID;

/**
 * Excepción lanzada cuando no se encuentra un ticket.
 */
public class TicketNotFoundException extends RuntimeException {
    
    public TicketNotFoundException(UUID codigoReferencia) {
        super(String.format("Ticket no encontrado con código: %s", codigoReferencia));
    }
    
    public TicketNotFoundException(String numero) {
        super(String.format("Ticket no encontrado con número: %s", numero));
    }
    
    public TicketNotFoundException(Long id) {
        super(String.format("Ticket no encontrado con ID: %s", id));
    }
}