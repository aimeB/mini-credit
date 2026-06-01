package com.mini.credit.service.audit;

import com.mini.credit.entity.audit.AuditLog;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.repository.audit.AuditLogRepository;
import com.mini.credit.service.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service d'audit des opérations sensibles du système.
 *
 * OWASP A09:2021 - Logging and Monitoring Failures
 * Journalise :
 * - Les événements de sécurité (login, accès refusé)
 * - Les opérations critiques (approbation crédit, décaissement, etc.)
 * - Avec contexte complet (utilisateur, rôle, action, résultat, erreur)
 *
 * Utilisé par :
 * - Forensique en cas d'incident
 * - Traçabilité et responsabilité
 * - Détection d'anomalies
 *
 * Étape 6/8 : Audit logging
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Enregistre une action auditée.
     *
     * @param action Action effectuée
     * @param entityType Type d'entité (ex: "DemandeCredit")
     * @param entityId ID de l'entité
     * @param success Succès de l'opération
     * @param reason Raison/contexte
     * @param referenceNumber Numéro de référence si applicable
     * @param oldValuesJson Anciennes valeurs (JSON pour audit d'avant)
     * @param newValuesJson Nouvelles valeurs (JSON pour audit d'après)
     * @param errorMessage Message d'erreur si échec
     */
    @Transactional
    public void log(
            AuditAction action,
            String entityType,
            Long entityId,
            boolean success,
            String reason,
            String referenceNumber,
            String oldValuesJson,
            String newValuesJson,
            String errorMessage
    ) {
        try {
            Utilisateur currentUser = SecurityUtils.getCurrentUser();

            AuditLog auditLog = AuditLog.builder()
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .success(success)
                    .reason(reason)
                    .referenceNumber(referenceNumber)
                    .oldValuesJson(oldValuesJson)
                    .newValuesJson(newValuesJson)
                    .errorMessage(errorMessage)
                    .build();

            if (currentUser != null) {
                auditLog.setUserId(currentUser.getId());
                auditLog.setUsername(currentUser.getUsername());
                if (currentUser.getRole() != null) {
                    auditLog.setRoleCode(currentUser.getRole().getCode());
                }
            } else {
                auditLog.setUsername("ANONYMOUS");
            }

            auditLogRepository.save(auditLog);

            log.debug("Audit log saved: {} - {} on {}.{}", action, success ? "SUCCESS" : "FAILURE", entityType, entityId);
        } catch (Exception e) {
            log.error("Erreur lors de l'enregistrement de l'audit", e);
            // Ne pas lever l'exception pour ne pas affecter l'opération métier
        }
    }

    /**
     * Surcharge simplifiée pour @Auditable AOP aspect.
     * Utilisée automatiquement pour journaliser avec oldValues/newValues.
     * Récupère automatiquement l'utilisateur de SecurityUtils.
     *
     * @param action Action
     * @param entityType Type d'entité
     * @param entityId ID d'entité
     * @param success Succès
     * @param reason Raison
     * @param oldValuesJson Anciennes valeurs (JSON)
     * @param newValuesJson Nouvelles valeurs (JSON)
     * @param errorMessage Erreur si échec
     */
    @Transactional
    public void logWithValues(
            AuditAction action,
            String entityType,
            Long entityId,
            boolean success,
            String reason,
            String oldValuesJson,
            String newValuesJson,
            String errorMessage
    ) {
        try {
            Utilisateur currentUser = SecurityUtils.getCurrentUser();

            AuditLog auditLog = AuditLog.builder()
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .success(success)
                    .reason(reason)
                    .oldValuesJson(oldValuesJson)
                    .newValuesJson(newValuesJson)
                    .errorMessage(errorMessage)
                    .build();

            if (currentUser != null) {
                auditLog.setUserId(currentUser.getId());
                auditLog.setUsername(currentUser.getUsername());
                if (currentUser.getRole() != null) {
                    auditLog.setRoleCode(currentUser.getRole().getCode());
                }
            } else {
                auditLog.setUsername("ANONYMOUS");
            }

            auditLogRepository.save(auditLog);

            log.debug("Audit log saved: {} - {} on {}.{}", action, success ? "SUCCESS" : "FAILURE", entityType, entityId);
        } catch (Exception e) {
            log.error("Erreur lors de l'enregistrement de l'audit", e);
        }
    }

    /**
     * Enregistre une opération réussie (version simplifiée)
     */
    public void logSuccess(
            AuditAction action,
            String entityType,
            Long entityId,
            String reason
    ) {
        log(action, entityType, entityId, true, reason, null, null, null, null);
    }

    /**
     * Enregistre une opération échouée (version simplifiée)
     */
    public void logFailure(
            AuditAction action,
            String entityType,
            Long entityId,
            String reason,
            String errorMessage
    ) {
        log(action, entityType, entityId, false, reason, null, null, null, errorMessage);
    }

    /**
     * Enregistre un événement de sécurité (login, accès refusé, etc.)
     */
    public void logSecurityEvent(
            AuditAction action,
            String reason,
            boolean success,
            String errorMessage
    ) {
        log(action, "SECURITY", null, success, reason, null, null, null, errorMessage);
    }

    /**
     * Enregistre une tentative d'accès refusée (permission insuffisante)
     */
    public void logAccessDenied(String operation, String reason) {
        logSecurityEvent(AuditAction.ACCESS_DENIED, "Accès refusé à " + operation + " : " + reason, false, null);
    }

    /**
     * Enregistre une opération métier invalide
     */
    public void logInvalidOperation(String entityType, Long entityId, String reason, String errorMessage) {
        log(AuditAction.INVALID_OPERATION, entityType, entityId, false, reason, null, null, null, errorMessage);
    }
}
