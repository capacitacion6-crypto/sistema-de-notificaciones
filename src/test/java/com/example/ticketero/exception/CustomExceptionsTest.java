package com.example.ticketero.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomExceptionsTest {

    @Test
    void shouldCreateTicketNotFoundException() {
        // Given
        String ticketNumber = "C123456";

        // When
        TicketNotFoundException exception = new TicketNotFoundException(ticketNumber);

        // Then
        assertThat(exception.getMessage()).contains(ticketNumber);
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldCreateDuplicateActiveTicketException() {
        // Given
        String customerRut = "12345678-9";

        // When
        DuplicateActiveTicketException exception = new DuplicateActiveTicketException(customerRut);

        // Then
        assertThat(exception.getMessage()).contains(customerRut);
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldThrowTicketNotFoundException() {
        // When & Then
        assertThatThrownBy(() -> {
            throw new TicketNotFoundException("INVALID");
        })
        .isInstanceOf(TicketNotFoundException.class)
        .hasMessageContaining("INVALID");
    }

    @Test
    void shouldThrowDuplicateActiveTicketException() {
        // When & Then
        assertThatThrownBy(() -> {
            throw new DuplicateActiveTicketException("12345678-9");
        })
        .isInstanceOf(DuplicateActiveTicketException.class)
        .hasMessageContaining("12345678-9");
    }
}