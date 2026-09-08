package com.mini.credit.service.audit;

import com.mini.credit.dto.audit.AuditLogFilterRequest;
import com.mini.credit.dto.audit.AuditStatsResponse;
import com.mini.credit.entity.audit.AuditLog;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.enums.security.AuditModule;
import com.mini.credit.enums.security.AuditSeverity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditService {

    void logInfo(AuditAction action, AuditModule module, String entityType, Long entityId, String commentaire, String referenceMetier);

    void logWarning(AuditAction action, AuditModule module, String entityType, Long entityId, String commentaire, String referenceMetier);

    void logCritical(AuditAction action, AuditModule module, String entityType, Long entityId, String commentaire, String referenceMetier, String errorMessage);

    void logExport(AuditModule module, String entityType, Long entityId, String commentaire, String referenceMetier);

    void logSecurityEvent(AuditAction action, String commentaire, boolean success, String errorMessage);

    void logBusinessEvent(AuditAction action, AuditModule module, String entityType, Long entityId, boolean success, String commentaire, String referenceMetier);

    void logAction(
            AuditAction action,
            AuditModule module,
            String entityType,
            Long entityId,
            boolean success,
            AuditSeverity severity,
            String commentaire,
            String referenceMetier,
            String oldValue,
            String newValue,
            String errorMessage,
            Long caisseId,
            Long sessionCaisseId,
            Long siteId,
            String siteLibelle
    );

    void logActionRequiresNew(
            AuditAction action,
            AuditModule module,
            String entityType,
            Long entityId,
            boolean success,
            AuditSeverity severity,
            String commentaire,
            String referenceMetier,
            String oldValue,
            String newValue,
            String errorMessage,
            Long caisseId,
            Long sessionCaisseId,
            Long siteId,
            String siteLibelle
    );

    void log(
            AuditAction action,
            String entityType,
            Long entityId,
            boolean success,
            String reason,
            String referenceNumber,
            String oldValuesJson,
            String newValuesJson,
            String errorMessage
    );

    void logWithValues(
            AuditAction action,
            String entityType,
            Long entityId,
            boolean success,
            String reason,
            String oldValuesJson,
            String newValuesJson,
            String errorMessage
    );

    void logSuccess(AuditAction action, String entityType, Long entityId, String reason);

    void logFailure(AuditAction action, String entityType, Long entityId, String reason, String errorMessage);

    void logAccessDenied(String operation, String reason);

    void logInvalidOperation(String entityType, Long entityId, String reason, String errorMessage);

    Page<AuditLog> search(AuditLogFilterRequest filter, Pageable pageable);

    Page<AuditLog> findByEntity(String entityType, Long entityId, Pageable pageable);

    Page<AuditLog> findByUser(Long userId, Pageable pageable);

    Page<AuditLog> findBySessionCaisse(Long sessionCaisseId, Pageable pageable);

        AuditLog findByIdScoped(Long id);

    AuditStatsResponse buildStats(AuditLogFilterRequest filter);

    byte[] exportCsv(AuditLogFilterRequest filter);
}
