package com.example.ticketero.scheduler;

import com.example.ticketero.service.AssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class QueueScheduler {

    private final AssignmentService assignmentService;

    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void processQueueUpdates() {
        log.debug("Processing queue updates and notifications");
        try {
            assignmentService.processQueueUpdates();
        } catch (Exception e) {
            log.error("Error processing queue updates", e);
        }
    }
}