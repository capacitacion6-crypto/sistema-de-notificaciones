package com.example.ticketero.service;

import com.example.ticketero.model.dto.response.*;
import com.example.ticketero.model.entity.*;
import com.example.ticketero.repository.AdvisorRepository;
import com.example.ticketero.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardService {

    private final TicketRepository ticketRepository;
    private final AdvisorRepository advisorRepository;
    private final AdvisorService advisorService;

    public DashboardResponse getDashboard() {
        LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIN);
        
        // Summary statistics
        DashboardSummary summary = calculateSummary(startOfDay);
        
        // Queue statistics by type
        List<QueueStats> queueStats = calculateQueueStats();
        
        // Advisor status
        List<AdvisorResponse> advisors = advisorService.getAllAdvisors();
        
        // Alerts
        List<AlertResponse> alerts = generateAlerts();
        
        return new DashboardResponse(
            summary,
            queueStats,
            advisors,
            alerts,
            LocalDateTime.now()
        );
    }

    private DashboardSummary calculateSummary(LocalDateTime startOfDay) {
        List<Ticket> todayTickets = ticketRepository.findAll()
            .stream()
            .filter(t -> t.getCreatedAt().isAfter(startOfDay))
            .toList();

        int totalToday = todayTickets.size();
        int waiting = (int) todayTickets.stream().filter(t -> t.getStatus() == TicketStatus.WAITING).count();
        int inProgress = (int) todayTickets.stream().filter(t -> t.getStatus() == TicketStatus.ASSIGNED || t.getStatus() == TicketStatus.IN_PROGRESS).count();
        int completed = (int) todayTickets.stream().filter(t -> t.getStatus() == TicketStatus.COMPLETED).count();

        List<Advisor> allAdvisors = advisorRepository.findAll();
        int available = (int) allAdvisors.stream().filter(a -> a.getStatus() == AdvisorStatus.AVAILABLE).count();
        int busy = (int) allAdvisors.stream().filter(a -> a.getStatus() == AdvisorStatus.BUSY).count();

        double avgWaitTime = todayTickets.stream()
            .filter(t -> t.getEstimatedWaitMinutes() != null)
            .mapToInt(Ticket::getEstimatedWaitMinutes)
            .average()
            .orElse(0.0);

        return new DashboardSummary(totalToday, waiting, inProgress, completed, available, busy, avgWaitTime);
    }

    private List<QueueStats> calculateQueueStats() {
        return Arrays.stream(QueueType.values())
            .map(this::calculateQueueStat)
            .toList();
    }

    private QueueStats calculateQueueStat(QueueType queueType) {
        long waiting = ticketRepository.countByStatusAndQueueType(TicketStatus.WAITING, queueType);
        long available = advisorRepository.countByStatusAndQueueType(AdvisorStatus.AVAILABLE, queueType);
        long busy = advisorRepository.countByStatusAndQueueType(AdvisorStatus.BUSY, queueType);

        int avgWaitMinutes = available > 0 ? 
            (int) Math.ceil((double) waiting / available) * queueType.getAverageTimeMinutes() :
            queueType.getAverageTimeMinutes() * (int) waiting;

        return new QueueStats(
            queueType.name(),
            (int) waiting,
            avgWaitMinutes,
            (int) available,
            (int) busy
        );
    }

    private List<AlertResponse> generateAlerts() {
        List<AlertResponse> alerts = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // Check for critical queues (>15 waiting)
        Arrays.stream(QueueType.values()).forEach(queueType -> {
            long waiting = ticketRepository.countByStatusAndQueueType(TicketStatus.WAITING, queueType);
            if (waiting > 15) {
                alerts.add(new AlertResponse(
                    "CRITICAL_QUEUE",
                    "Cola " + queueType.name() + " tiene " + waiting + " clientes esperando",
                    "HIGH",
                    now
                ));
            }
        });

        // Check for advisors offline
        long offlineAdvisors = advisorRepository.findAll()
            .stream()
            .filter(a -> a.getStatus() == AdvisorStatus.OFFLINE)
            .count();

        if (offlineAdvisors > 0) {
            alerts.add(new AlertResponse(
                "ADVISORS_OFFLINE",
                offlineAdvisors + " asesores están desconectados",
                "MEDIUM",
                now
            ));
        }

        // Check for long wait times
        boolean longWaitTimes = Arrays.stream(QueueType.values())
            .anyMatch(qt -> {
                long waiting = ticketRepository.countByStatusAndQueueType(TicketStatus.WAITING, qt);
                long available = advisorRepository.countByStatusAndQueueType(AdvisorStatus.AVAILABLE, qt);
                return available == 0 && waiting > 5;
            });

        if (longWaitTimes) {
            alerts.add(new AlertResponse(
                "LONG_WAIT_TIMES",
                "Tiempos de espera excesivos detectados",
                "HIGH",
                now
            ));
        }

        return alerts;
    }
}