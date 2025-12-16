package com.example.ticketero;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Clase principal de la aplicación Ticketero
 * Sistema de gestión de tickets con notificaciones en tiempo real
 */
@SpringBootApplication
@EnableScheduling
public class TicketeroApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketeroApplication.class, args);
    }
}