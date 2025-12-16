package com.example.ticketero.repository;

import com.example.ticketero.model.entity.Advisor;
import com.example.ticketero.model.entity.AdvisorStatus;
import com.example.ticketero.model.entity.QueueType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdvisorRepository extends JpaRepository<Advisor, Long> {

    List<Advisor> findByStatusAndQueueType(AdvisorStatus status, QueueType queueType);

    Optional<Advisor> findFirstByStatusAndQueueTypeOrderByUpdatedAtAsc(AdvisorStatus status, QueueType queueType);

    long countByStatusAndQueueType(AdvisorStatus status, QueueType queueType);

    List<Advisor> findByQueueType(QueueType queueType);
}