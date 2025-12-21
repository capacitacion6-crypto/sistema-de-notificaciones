package com.example.ticketero.repository;

import com.example.ticketero.model.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByDeliveryStatusAndRetryCountLessThan(String deliveryStatus, Integer maxRetries);

    List<Message> findByTicketIdOrderByCreatedAtDesc(Long ticketId);
    
    List<Message> findByDeliveryStatus(String deliveryStatus);
    
    List<Message> findByCreatedAtAfter(LocalDateTime date);
}