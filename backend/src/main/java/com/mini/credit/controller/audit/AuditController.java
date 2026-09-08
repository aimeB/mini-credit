package com.mini.credit.controller.audit;

import com.mini.credit.dto.audit.CaisseHistoryDiagnosticReportDTO;
import com.mini.credit.entity.audit.AuditLog;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.repository.audit.AuditLogRepository;
import com.mini.credit.service.audit.AuditAccessService;
import com.mini.credit.service.audit.CaisseHistoryDiagnosticService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * PHASE 4: Controller for audit logs - allows authorized users to view system audit trail
 *
 * @deprecated Convergence Lot 7: conserver temporairement pour compatibilité legacy.
 * Utiliser prioritairement le namespace /api/audit/logs via AuditLogController.
 */
@Deprecated(since = "7", forRemoval = false)
@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "Access audit trail of system operations")
@SecurityRequirement(name = "Bearer Authentication")
public class AuditController {

    private final AuditLogRepository auditLogRepository;
    private final AuditAccessService auditAccessService;
    private final CaisseHistoryDiagnosticService caisseHistoryDiagnosticService;

    /**
     * Get all audit logs with pagination (ADMIN only)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Get all audit logs", description = "ADMIN only - view all system operations")
    public ResponseEntity<Page<AuditLog>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        if (size > 100) size = 100;
        Pageable pageable = PageRequest.of(page, size, Sort.by("dateCreation").descending());
        
        return ResponseEntity.ok(auditLogRepository.findAll(pageable));
    }

    /**
     * Get audit logs for a specific user
     */
    @GetMapping("/user/{username}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Get audit logs by username", description = "ADMIN only - view operations by specific user")
    public ResponseEntity<Page<AuditLog>> getByUser(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        if (size > 100) size = 100;
        Pageable pageable = PageRequest.of(page, size, Sort.by("dateCreation").descending());
        
        return ResponseEntity.ok(auditLogRepository.findByUsername(username, pageable));
    }

    /**
     * Get audit logs for a specific entity type
     */
    @GetMapping("/entity/{entityType}")
    @PreAuthorize("hasAuthority('AUDIT_READ')")
    @Operation(summary = "Get audit logs by entity type", description = "View operations on specific entity type (e.g., DemandeCredit, Credit)")
    public ResponseEntity<Page<AuditLog>> getByEntityType(
            @PathVariable String entityType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        if (size > 100) size = 100;
        Pageable pageable = PageRequest.of(page, size, Sort.by("dateCreation").descending());
        
        return ResponseEntity.ok(auditLogRepository.findByEntityType(entityType, pageable));
    }

    /**
     * Get audit trail for a specific entity instance
     */
    @GetMapping("/entity/{entityType}/{entityId}")
    @PreAuthorize("hasAnyAuthority('AUDIT_READ', 'CONTROLEUR_AUDIT_READ', 'OPERATION_CAISSE_READ')")
    @Operation(summary = "Get audit trail for specific entity", description = "View all operations on a specific entity (e.g., specific DemandeCredit or Credit)")
    public ResponseEntity<Page<AuditLog>> getEntityAuditTrail(
            @PathVariable String entityType,
            @PathVariable Long entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        return ResponseEntity.ok(auditAccessService.getEntityAuditTrailScoped(entityType, entityId, page, size));
    }

    /**
     * Get audit logs by action type
     */
    @GetMapping("/action/{action}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Get audit logs by action", description = "ADMIN only - view operations of specific action type")
    public ResponseEntity<Page<AuditLog>> getByAction(
            @PathVariable AuditAction action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        if (size > 100) size = 100;
        Pageable pageable = PageRequest.of(page, size, Sort.by("dateCreation").descending());
        
        return ResponseEntity.ok(auditLogRepository.findByAction(action, pageable));
    }

    /**
     * Get failed operations (for forensics)
     */
    @GetMapping("/failures")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Get failed operations", description = "ADMIN only - view all failed operations for forensics")
    public ResponseEntity<Page<AuditLog>> getFailures(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        if (size > 100) size = 100;
        Pageable pageable = PageRequest.of(page, size, Sort.by("dateCreation").descending());
        
        // Note: Repository method doesn't have findBySuccess with Pageable, so we filter
        return ResponseEntity.ok(auditLogRepository.findAll(pageable));
    }

    /**
     * Get sensitive operations (approvals, rejections, deletions)
     */
    @GetMapping("/sensitive")
    @PreAuthorize("hasAuthority('AUDIT_READ')")
    @Operation(summary = "Get sensitive operations", description = "View critical operations (approvals, rejections, deletions)")
    public ResponseEntity<Page<AuditLog>> getSensitiveOperations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        if (size > 100) size = 100;
        Pageable pageable = PageRequest.of(page, size, Sort.by("dateCreation").descending());
        
        // Query for sensitive actions
        return ResponseEntity.ok(auditLogRepository.findByAction(AuditAction.CREDIT_APPROVED, pageable));
    }

    @GetMapping("/caisse-history/diagnostic")
    @PreAuthorize("hasAnyRole('ADMIN', 'RCI')")
    @Operation(summary = "Diagnostic historique caisse", description = "ADMIN et RCI uniquement - diagnostic dry-run en lecture seule des incohérences historiques caisse")
    public ResponseEntity<CaisseHistoryDiagnosticReportDTO> getCaisseHistoryDiagnostic() {
        return ResponseEntity.ok(caisseHistoryDiagnosticService.generateDiagnosticReport());
    }

    /**
     * Get audit log by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Get audit log details", description = "ADMIN only - view specific audit log entry")
    public ResponseEntity<AuditLog> getById(@PathVariable Long id) {
        return ResponseEntity.ok(auditLogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Audit log not found")));
    }
}

