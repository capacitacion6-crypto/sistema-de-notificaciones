package com.example.ticketero.controller;

import com.example.ticketero.model.dto.response.AdvisorResponse;
import com.example.ticketero.model.dto.response.DashboardResponse;
import com.example.ticketero.model.enums.AdvisorStatus;
import com.example.ticketero.service.AdvisorService;
import com.example.ticketero.service.AssignmentService;
import com.example.ticketero.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AdvisorService advisorService;
    private final AssignmentService assignmentService;
    private final DashboardService dashboardService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard() {
        log.info("GET /api/admin/dashboard - Retrieving dashboard data");
        DashboardResponse dashboard = dashboardService.getDashboard();
        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/advisors")
    public ResponseEntity<List<AdvisorResponse>> getAllAdvisors() {
        log.info("GET /api/admin/advisors - Retrieving all advisors");
        List<AdvisorResponse> advisors = advisorService.getAllAdvisors();
        return ResponseEntity.ok(advisors);
    }

    @PutMapping("/advisors/{id}/status")
    public ResponseEntity<Void> updateAdvisorStatus(
        @PathVariable Long id,
        @RequestParam AdvisorStatus status
    ) {
        log.info("PUT /api/admin/advisors/{}/status - Updating to {}", id, status);
        advisorService.updateAdvisorStatus(id, status);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/tickets/{id}/complete")
    public ResponseEntity<Void> completeTicket(@PathVariable Long id) {
        log.info("POST /api/admin/tickets/{}/complete - Completing ticket", id);
        assignmentService.completeTicket(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/assignments/process")
    public ResponseEntity<Void> processAssignments() {
        log.info("POST /api/admin/assignments/process - Processing all assignments");
        assignmentService.processAllAssignments();
        return ResponseEntity.ok().build();
    }
}