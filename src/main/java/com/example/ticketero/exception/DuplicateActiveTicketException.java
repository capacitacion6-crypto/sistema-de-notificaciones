package com.example.ticketero.exception;

/**
 * Excepción lanzada cuando un cliente intenta crear un ticket 
 * teniendo ya uno activo (RN-001).
 */
public class DuplicateActiveTicketException extends RuntimeException {
    
    public DuplicateActiveTicketException(String nationalId) {
        super(String.format("Cliente con ID %s ya tiene un ticket activo", nationalId));
    }
    
    public DuplicateActiveTicketException(String nationalId, String activeTicketNumber) {
        super(String.format("Cliente con ID %s ya tiene un ticket activo: %s", nationalId, activeTicketNumber));
    }
}