package com.example.ticketero.repository;

import com.example.ticketero.model.entity.Mensaje;
import com.example.ticketero.model.enums.MessageStatus;
import com.example.ticketero.model.enums.MessageTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository para la entidad Mensaje.
 */
@Repository
public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    /**
     * Busca mensajes por ticket ID.
     */
    List<Mensaje> findByTicketId(Long ticketId);

    /**
     * Busca mensajes pendientes de envío que ya deben ser procesados.
     */
    @Query("""
        SELECT m FROM Mensaje m 
        WHERE m.estadoEnvio = 'PENDIENTE' 
        AND m.fechaProgramada <= :now 
        ORDER BY m.fechaProgramada ASC
        """)
    List<Mensaje> findPendingMessagesToSend(@Param("now") LocalDateTime now);

    /**
     * Busca mensajes por estado de envío.
     */
    List<Mensaje> findByEstadoEnvio(MessageStatus estadoEnvio);

    /**
     * Busca mensajes por plantilla.
     */
    List<Mensaje> findByPlantilla(MessageTemplate plantilla);

    /**
     * Busca mensajes fallidos que pueden ser reintentados.
     */
    @Query("""
        SELECT m FROM Mensaje m 
        WHERE m.estadoEnvio = 'PENDIENTE' 
        AND m.intentos < 3 
        AND m.fechaProgramada <= :now
        """)
    List<Mensaje> findRetryableMessages(@Param("now") LocalDateTime now);

    /**
     * Cuenta mensajes por estado.
     */
    long countByEstadoEnvio(MessageStatus estadoEnvio);

    /**
     * Busca mensajes creados después de una fecha específica.
     */
    List<Mensaje> findByCreatedAtAfter(LocalDateTime date);
}