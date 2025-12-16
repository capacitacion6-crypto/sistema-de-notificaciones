package com.example.ticketero.repository;

import com.example.ticketero.model.entity.QueueType;
import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.entity.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByUuid(UUID uuid);

    Optional<Ticket> findByTicketNumber(String ticketNumber);

    List<Ticket> findByStatusAndQueueTypeOrderByCreatedAtAsc(TicketStatus status, QueueType queueType);

    long countByStatusAndQueueType(TicketStatus status, QueueType queueType);

    @Query("""
        SELECT t FROM Ticket t
        WHERE t.status = :status
        AND t.queueType = :queueType
        AND t.createdAt < (SELECT t2.createdAt FROM Ticket t2 WHERE t2.id = :ticketId)
        """)
    long countTicketsAheadInQueue(
        @Param("status") TicketStatus status,
        @Param("queueType") QueueType queueType,
        @Param("ticketId") Long ticketId
    );

    List<Ticket> findByStatus(TicketStatus status);

    @Query("""
        SELECT t FROM Ticket t
        WHERE t.status = 'WAITING'
        ORDER BY t.createdAt ASC
        """)
    List<Ticket> findNextTicketToAssign();
}