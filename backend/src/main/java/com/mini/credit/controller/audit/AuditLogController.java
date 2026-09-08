package com.mini.credit.controller.audit;

import com.mini.credit.dto.audit.AuditLogFilterRequest;
import com.mini.credit.dto.audit.AuditLogView;
import com.mini.credit.dto.audit.AuditStatsResponse;
import com.mini.credit.entity.audit.AuditLog;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.enums.security.AuditModule;
import com.mini.credit.mapper.AuditLogMapper;
import com.mini.credit.service.audit.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/audit/logs")
@RequiredArgsConstructor
@Tag(name = "Audit Conformite 3N", description = "Logs audit unifies pour controle interne")
@SecurityRequirement(name = "Bearer Authentication")
public class AuditLogController {

    private final AuditService auditService;
    private final AuditLogMapper auditLogMapper;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('AUDIT_LOG_READ','AUDIT_READ','CONTROLEUR_AUDIT_READ')")
    @Operation(summary = "Lister les logs audit", description = "Liste paginee et filtree des traces d'audit")
    public ResponseEntity<Page<AuditLogView>> getLogs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(required = false) AuditModule module,
            @RequestParam(required = false) com.mini.credit.enums.security.AuditAction action,
            @RequestParam(required = false) com.mini.credit.enums.security.AuditSeverity severity,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long siteId,
            @RequestParam(required = false) Long caisseId,
            @RequestParam(required = false) Long sessionCaisseId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false) String referenceMetier,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        int normalizedSize = Math.min(Math.max(size, 1), 200);
        Pageable pageable = PageRequest.of(Math.max(page, 0), normalizedSize, Sort.by(Sort.Direction.DESC, "dateAction"));

        AuditLogFilterRequest filter = new AuditLogFilterRequest();
        filter.setDateDebut(dateDebut);
        filter.setDateFin(dateFin);
        filter.setModule(module);
        filter.setAction(action);
        filter.setSeverity(severity);
        filter.setSuccess(success);
        filter.setUserId(userId);
        filter.setSiteId(siteId);
        filter.setCaisseId(caisseId);
        filter.setSessionCaisseId(sessionCaisseId);
        filter.setEntityType(entityType);
        filter.setEntityId(entityId);
        filter.setReferenceMetier(referenceMetier);

        Page<AuditLogView> result = auditService.search(filter, pageable).map(auditLogMapper::toView);
        auditService.logBusinessEvent(
            AuditAction.RAPPORT_GENERE,
            AuditModule.CONTROLE_INTERNE,
            "AuditLog",
            null,
            true,
            "Consultation historique audit",
            null
        );
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('AUDIT_LOG_READ','AUDIT_READ','CONTROLEUR_AUDIT_READ')")
    @Operation(summary = "Detail d'un log audit", description = "Consulter une trace d'audit specifique")
    public ResponseEntity<AuditLogView> getById(@PathVariable Long id) {
        AuditLog entity = auditService.findByIdScoped(id);
        auditService.logBusinessEvent(
            AuditAction.RAPPORT_GENERE,
            AuditModule.CONTROLE_INTERNE,
            "AuditLog",
            id,
            true,
            "Consultation détail d'action critique",
            null
        );

        return ResponseEntity.ok(auditLogMapper.toView(entity));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @PreAuthorize("hasAnyAuthority('AUDIT_LOG_READ','AUDIT_READ','CONTROLEUR_AUDIT_READ','OPERATION_CAISSE_READ')")
    @Operation(summary = "Audit par entite", description = "Consulter la trace d'une entite specifique")
    public ResponseEntity<Page<AuditLogView>> getByEntity(
            @PathVariable String entityType,
            @PathVariable Long entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200), Sort.by(Sort.Direction.DESC, "dateAction"));
        return ResponseEntity.ok(auditService.findByEntity(entityType, entityId, pageable).map(auditLogMapper::toView));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyAuthority('AUDIT_LOG_READ','AUDIT_READ','CONTROLEUR_AUDIT_READ')")
    @Operation(summary = "Audit par utilisateur", description = "Consulter les traces d'un utilisateur")
    public ResponseEntity<Page<AuditLogView>> getByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200), Sort.by(Sort.Direction.DESC, "dateAction"));
        return ResponseEntity.ok(auditService.findByUser(userId, pageable).map(auditLogMapper::toView));
    }

    @GetMapping("/session-caisse/{sessionCaisseId}")
    @PreAuthorize("hasAnyAuthority('AUDIT_LOG_READ','AUDIT_READ','CONTROLEUR_AUDIT_READ','RAPPORT_CAISSE_AUDIT_READ')")
    @Operation(summary = "Audit session caisse", description = "Consulter la trace d'une session caisse")
    public ResponseEntity<Page<AuditLogView>> getBySessionCaisse(
            @PathVariable Long sessionCaisseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200), Sort.by(Sort.Direction.DESC, "dateAction"));
        return ResponseEntity.ok(auditService.findBySessionCaisse(sessionCaisseId, pageable).map(auditLogMapper::toView));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyAuthority('AUDIT_LOG_READ','AUDIT_READ','AUDIT_SECURITY_READ')")
    @Operation(summary = "Statistiques audit", description = "Stats pour supervision controle interne")
    public ResponseEntity<AuditStatsResponse> stats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(required = false) AuditModule module
    ) {
        AuditLogFilterRequest filter = new AuditLogFilterRequest();
        filter.setDateDebut(dateDebut);
        filter.setDateFin(dateFin);
        filter.setModule(module);
        auditService.logBusinessEvent(
            AuditAction.RAPPORT_GENERE,
            AuditModule.CONTROLE_INTERNE,
            "AuditStats",
            null,
            true,
            "Consultation statistiques audit",
            null
        );
        return ResponseEntity.ok(auditService.buildStats(filter));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyAuthority('AUDIT_LOG_EXPORT')")
    @Operation(summary = "Export CSV audit", description = "Exporter les logs audit filtres au format CSV")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(required = false) AuditModule module,
            @RequestParam(required = false) com.mini.credit.enums.security.AuditAction action,
            @RequestParam(required = false) com.mini.credit.enums.security.AuditSeverity severity,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long siteId,
            @RequestParam(required = false) Long caisseId,
            @RequestParam(required = false) Long sessionCaisseId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false) String referenceMetier
    ) {
        AuditLogFilterRequest filter = new AuditLogFilterRequest();
        filter.setDateDebut(dateDebut);
        filter.setDateFin(dateFin);
        filter.setModule(module);
        filter.setAction(action);
        filter.setSeverity(severity);
        filter.setSuccess(success);
        filter.setUserId(userId);
        filter.setSiteId(siteId);
        filter.setCaisseId(caisseId);
        filter.setSessionCaisseId(sessionCaisseId);
        filter.setEntityType(entityType);
        filter.setEntityId(entityId);
        filter.setReferenceMetier(referenceMetier);

        byte[] payload = auditService.exportCsv(filter);
        auditService.logExport(
            AuditModule.CONTROLE_INTERNE,
            "AuditLog",
            null,
            "Export CSV audit",
            "AUDIT-EXPORT"
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment().filename("audit-logs.csv").build());
        return ResponseEntity.ok().headers(headers).body(payload);
    }
}
