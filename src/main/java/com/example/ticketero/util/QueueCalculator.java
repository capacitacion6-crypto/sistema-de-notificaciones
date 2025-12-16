package com.example.ticketero.util;

import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.model.enums.TicketStatus;
import com.example.ticketero.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Calculadora de posición en cola y tiempo estimado según RN-010.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QueueCalculator {

    private final TicketRepository ticketRepository;

    /**
     * Calcula la posición actual en cola para un tipo específico.
     * 
     * @param queueType Tipo de cola
     * @return Posición en cola (1-based)
     */
    public int calculatePosition(QueueType queueType) {
        long waitingCount = ticketRepository.countByQueueTypeAndStatus(queueType, TicketStatus.EN_ESPERA);
        long proximoCount = ticketRepository.countByQueueTypeAndStatus(queueType, TicketStatus.PROXIMO);
        
        // La posición es el total de tickets en espera + próximos + 1 (el nuevo ticket)
        int position = (int) (waitingCount + proximoCount + 1);
        
        log.debug("Calculated position {} for queue type {} (waiting: {}, proximo: {})", 
                 position, queueType, waitingCount, proximoCount);
        
        return position;
    }

    /**
     * Calcula el tiempo estimado de espera según RN-010.
     * Formula: posiciónEnCola × tiempoPromedioCola
     * 
     * @param position Posición en cola
     * @param queueType Tipo de cola
     * @return Tiempo estimado en minutos
     */
    public int calculateEstimatedWaitTime(int position, QueueType queueType) {
        int averageTime = queueType.getAverageTimeMinutes();
        int estimatedTime = position * averageTime;
        
        log.debug("Calculated estimated wait time {} minutes for position {} in queue type {} (avg: {} min)", 
                 estimatedTime, position, queueType, averageTime);
        
        return estimatedTime;
    }

    /**
     * Calcula posición y tiempo estimado en una sola operación.
     * 
     * @param queueType Tipo de cola
     * @return Array con [posición, tiempoEstimado]
     */
    public int[] calculatePositionAndTime(QueueType queueType) {
        int position = calculatePosition(queueType);
        int estimatedTime = calculateEstimatedWaitTime(position, queueType);
        
        return new int[]{position, estimatedTime};
    }
}