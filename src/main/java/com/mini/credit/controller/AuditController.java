package com.mini.credit.controller;

import com.mini.credit.dto.audit.AuditLogResponse;
import com.mini.credit.dto.audit.AuditStatistiques;
import com.mini.credit.entity.audit.AuditLog;
import com.mini.credit.repository.audit.AuditLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Audit", description = "Audit logging and monitoring endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESPONSABLE') or hasRole('AGENT_BUREAU')")
    @Operation(summary = "Get all audit logs", description = "Retrieve all audit logs")
    public ResponseEntity<List<AuditLog>> getAllAuditLogs() {
        List<AuditLog> logs = auditLogRepository.findAll();
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESPONSABLE') or hasRole('AGENT_BUREAU')")
    @Operation(summary = "Get audit log by ID", description = "Retrieve a specific audit log")
    public ResponseEntity<AuditLog> getAuditLogById(@PathVariable Long id) {
        return auditLogRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/utilisateur/{username}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESPONSABLE') or hasRole('AGENT_BUREAU')")
    @Operation(summary = "Get logs by user", description = "Retrieve audit logs for a specific user")
    public ResponseEntity<Page<AuditLog>> getLogsByUtilisateur(
            @PathVariable String username,
            Pageable pageable) {
        Page<AuditLog> logs = auditLogRepository.findByUsername(username, pageable);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/action/{action}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESPONSABLE') or hasRole('AGENT_BUREAU')")
    @Operation(summary = "Get logs by action", description = "Retrieve audit logs for a specific action")
    public ResponseEntity<Page<AuditLog>> getLogsByAction(
            @PathVariable String action,
            Pageable pageable) {
        // Convert to uppercase for enum matching
        Page<AuditLog> logs = auditLogRepository.findByAction(
                com.mini.credit.enums.security.AuditAction.valueOf(action.toUpperCase()),
                pageable
        );
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/entity/{entityType}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESPONSABLE') or hasRole('AGENT_BUREAU')")
    @Operation(summary = "Get logs by entity type", description = "Retrieve audit logs for a specific entity type")
    public ResponseEntity<Page<AuditLog>> getLogsByEntityType(
            @PathVariable String entityType,
            Pageable pageable) {
        Page<AuditLog> logs = auditLogRepository.findByEntityType(entityType, pageable);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESPONSABLE') or hasRole('AGENT_BUREAU')")
    @Operation(summary = "Get logs by entity", description = "Retrieve audit logs for a specific entity")
    public ResponseEntity<List<AuditLog>> getLogsByEntity(
            @PathVariable String entityType,
            @PathVariable Long entityId) {
        List<AuditLog> logs = auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/date-range")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESPONSABLE') or hasRole('AGENT_BUREAU')")
    @Operation(summary = "Get logs by date range", description = "Retrieve audit logs within a date range")
    public ResponseEntity<List<AuditLog>> getLogsByDateRange(
            @RequestParam String dateDebut,
            @RequestParam String dateFin) {
        try {
            LocalDateTime startDate = LocalDateTime.parse(dateDebut.replace(" ", "T"));
            LocalDateTime endDate = LocalDateTime.parse(dateFin.replace(" ", "T"));
            List<AuditLog> logs = auditLogRepository.findByDateCreationBetween(startDate, endDate);
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            // Try alternate date format handling
            LocalDateTime startDate = LocalDateTime.parse(dateDebut + "T00:00:00");
            LocalDateTime endDate = LocalDateTime.parse(dateFin + "T23:59:59");
            List<AuditLog> logs = auditLogRepository.findByDateCreationBetween(startDate, endDate);
            return ResponseEntity.ok(logs);
        }
    }

    @GetMapping("/utilisateur/{username}/date-range")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESPONSABLE') or hasRole('AGENT_BUREAU')")
    @Operation(summary = "Get user logs by date range", description = "Retrieve audit logs for a user within a date range")
    public ResponseEntity<Page<AuditLog>> getUserLogsByDateRange(
            @PathVariable String username,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Pageable pageable) {
        Page<AuditLog> logs = auditLogRepository.findUserActionsBetween(username, startDate, endDate, pageable);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/failed")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESPONSABLE') or hasRole('AGENT_BUREAU')")
    @Operation(summary = "Get failed operations", description = "Retrieve failed audit logs")
    public ResponseEntity<Page<AuditLog>> getFailedOperations(Pageable pageable) {
        Page<AuditLog> logs = auditLogRepository.findFailedOperations(pageable);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESPONSABLE') or hasRole('AGENT_BUREAU')")
    @Operation(summary = "Get audit statistics", description = "Retrieve audit statistics")
    public ResponseEntity<AuditStatistiques> getStatistics() {
        List<AuditLog> allLogs = auditLogRepository.findAll();

        long successfully = allLogs.stream().filter(AuditLog::getSuccess).count();
        long failed = allLogs.stream().filter(log -> !log.getSuccess()).count();
        double successRate = allLogs.isEmpty() ? 0 : (successfully * 100.0) / allLogs.size();

        AuditStatistiques stats = AuditStatistiques.builder()
                .totalLogsCount(allLogs.size())
                .successfulActionsCount(successfully)
                .failedActionsCount(failed)
                .successRate(successRate)
                .build();

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/export/csv")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESPONSABLE') or hasRole('AGENT_BUREAU')")
    @Operation(summary = "Export audit logs", description = "Export audit logs as CSV")
    public ResponseEntity<String> exportAuditLogsCsv() {
        List<AuditLog> logs = auditLogRepository.findAll();

        StringBuilder csv = new StringBuilder();
        csv.append("ID,Action,Entity Type,Entity ID,Username,Role,Success,Reason,Created Date\n");

        for (AuditLog log : logs) {
            csv.append(String.format("%d,%s,%s,%d,%s,%s,%b,%s,%s\n",
                    log.getId(),
                    log.getAction(),
                    log.getEntityType(),
                    log.getEntityId() != null ? log.getEntityId() : "",
                    log.getUsername(),
                    log.getRoleCode() != null ? log.getRoleCode() : "",
                    log.getSuccess(),
                    log.getReason() != null ? log.getReason().replace(",", ";") : "",
                    log.getDateCreation()));
        }

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"audit_logs.csv\"")
                .body(csv.toString());
    }
}
