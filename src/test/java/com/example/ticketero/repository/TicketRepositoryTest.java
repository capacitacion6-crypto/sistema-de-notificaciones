package com.example.ticketero.repository;

import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.model.enums.TicketStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class TicketRepositoryTest {

    @Autowired
    private TicketRepository ticketRepository;

    @Test
    void shouldSaveAndFindTicket() {
        // Given
        Ticket ticket = Ticket.builder()
                .numero("C01")
                .nationalId("12345678-9")
                .telefono("+56912345678")
                .branchOffice("Sucursal Centro")
                .queueType(QueueType.CAJA)
                .status(TicketStatus.EN_ESPERA)
                .positionInQueue(1)
                .estimatedWaitMinutes(5)
                .build();

        // When
        Ticket saved = ticketRepository.save(ticket);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCodigoReferencia()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldFindActiveTicketsByNationalId() {
        // Given
        String nationalId = "12345678-9";
        Ticket activeTicket = createTicket(nationalId, TicketStatus.EN_ESPERA);
        Ticket completedTicket = createTicket(nationalId, TicketStatus.COMPLETADO);
        
        ticketRepository.save(activeTicket);
        ticketRepository.save(completedTicket);

        // When
        List<Ticket> activeTickets = ticketRepository.findActiveTicketsByNationalId(nationalId);

        // Then
        assertThat(activeTickets).hasSize(1);
        assertThat(activeTickets.get(0).getStatus()).isEqualTo(TicketStatus.EN_ESPERA);
    }

    @Test
    void shouldFindByNumero() {
        // Given
        Ticket ticket = createTicket("12345678-9", TicketStatus.EN_ESPERA);
        ticket.setNumero("C01");
        ticketRepository.save(ticket);

        // When
        Optional<Ticket> found = ticketRepository.findByNumero("C01");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getNumero()).isEqualTo("C01");
    }

    private Ticket createTicket(String nationalId, TicketStatus status) {
        return Ticket.builder()
                .numero("C01")
                .nationalId(nationalId)
                .telefono("+56912345678")
                .branchOffice("Sucursal Centro")
                .queueType(QueueType.CAJA)
                .status(status)
                .positionInQueue(1)
                .estimatedWaitMinutes(5)
                .build();
    }
}