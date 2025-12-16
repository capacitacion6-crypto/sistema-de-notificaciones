package com.example.ticketero.util;

import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Generador de números de ticket según RN-005 y RN-006.
 * Formato: [Prefijo][Número secuencial 01-99]
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TicketNumberGenerator {

    private final TicketRepository ticketRepository;

    /**
     * Genera el siguiente número de ticket para el tipo de cola especificado.
     * 
     * @param queueType Tipo de cola
     * @return Número de ticket generado (ej: "C01", "P15")
     */
    public String generateNextNumber(QueueType queueType) {
        String prefix = queueType.getPrefix();
        
        // Buscar el último número del día para este tipo de cola
        Optional<String> lastNumber = ticketRepository.findLastTicketNumberOfDay(queueType);
        
        int nextSequence = 1;
        
        if (lastNumber.isPresent()) {
            // Extraer el número secuencial del último ticket
            String lastNumberStr = lastNumber.get();
            String sequenceStr = lastNumberStr.substring(1); // Remover prefijo
            
            try {
                int lastSequence = Integer.parseInt(sequenceStr);
                nextSequence = lastSequence + 1;
                
                // Si llegamos a 99, reiniciar en 01
                if (nextSequence > 99) {
                    nextSequence = 1;
                    log.warn("Ticket sequence for {} reached 99, resetting to 01", queueType);
                }
            } catch (NumberFormatException e) {
                log.error("Error parsing last ticket number: {}", lastNumberStr, e);
                nextSequence = 1;
            }
        }
        
        String ticketNumber = String.format("%s%02d", prefix, nextSequence);
        log.debug("Generated ticket number: {} for queue type: {}", ticketNumber, queueType);
        
        return ticketNumber;
    }
}