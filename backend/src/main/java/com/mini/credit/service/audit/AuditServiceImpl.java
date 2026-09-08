package com.mini.credit.service.audit;

import com.mini.credit.dto.audit.AuditLogFilterRequest;
import com.mini.credit.dto.audit.AuditStatsResponse;
import com.mini.credit.entity.audit.AuditLog;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.enums.security.AuditModule;
import com.mini.credit.enums.security.AuditSeverity;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.mapper.AuditLogMapper;
import com.mini.credit.repository.audit.AuditLogRepository;
import com.mini.credit.service.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditServiceImpl implements AuditService {

    private static final int EXPORT_MAX_ROWS = 5000;
    private static final Pattern SENSITIVE_PATTERN = Pattern.compile("(?i)(password|motdepasse|token|authorization|secret|jwt)");

    private final AuditLogRepository auditLogRepository;
    private final AuditAccessService auditAccessService;
    private final AuditLogMapper auditLogMapper;

    @Override
    public void logInfo(AuditAction action, AuditModule module, String entityType, Long entityId, String commentaire, String referenceMetier) {
        logAction(action, module, entityType, entityId, true, AuditSeverity.INFO, commentaire, referenceMetier, null, null, null, null, null, null, null);
    }

    @Override
    public void logWarning(AuditAction action, AuditModule module, String entityType, Long entityId, String commentaire, String referenceMetier) {
        logAction(action, module, entityType, entityId, false, AuditSeverity.WARNING, commentaire, referenceMetier, null, null, null, null, null, null, null);
    }

    @Override
    public void logCritical(AuditAction action, AuditModule module, String entityType, Long entityId, String commentaire, String referenceMetier, String errorMessage) {
        logAction(action, module, entityType, entityId, false, AuditSeverity.CRITICAL, commentaire, referenceMetier, null, null, errorMessage, null, null, null, null);
    }

    @Override
    public void logExport(AuditModule module, String entityType, Long entityId, String commentaire, String referenceMetier) {
        logAction(AuditAction.DATA_EXPORT, module, entityType, entityId, true, AuditSeverity.INFO, commentaire, referenceMetier, null, null, null, null, null, null, null);
    }

    @Override
    public void logSecurityEvent(AuditAction action, String commentaire, boolean success, String errorMessage) {
        logAction(action, AuditModule.AUTHENTIFICATION, "SECURITY", null, success,
                success ? AuditSeverity.INFO : AuditSeverity.WARNING,
                commentaire, null, null, null, errorMessage, null, null, null, null);
    }

    @Override
    public void logBusinessEvent(AuditAction action, AuditModule module, String entityType, Long entityId, boolean success, String commentaire, String referenceMetier) {
        logAction(action, module, entityType, entityId, success,
                success ? AuditSeverity.INFO : AuditSeverity.WARNING,
                commentaire, referenceMetier, null, null, success ? null : commentaire, null, null, null, null);
    }

    @Override
    @Transactional
    public void logAction(
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
    ) {
            persistAuditLog(action, module, entityType, entityId, success, severity, commentaire, referenceMetier,
                oldValue, newValue, errorMessage, caisseId, sessionCaisseId, siteId, siteLibelle);
            }

            @Override
            @Transactional(propagation = Propagation.REQUIRES_NEW)
            public void logActionRequiresNew(
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
            ) {
            persistAuditLog(action, module, entityType, entityId, success, severity, commentaire, referenceMetier,
                oldValue, newValue, errorMessage, caisseId, sessionCaisseId, siteId, siteLibelle);
            }

            private void persistAuditLog(
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
            ) {
        try {
            Utilisateur currentUser = SecurityUtils.getCurrentUser();

            AuditLog auditLog = AuditLog.builder()
                    .action(action)
                    .module(module)
                    .entityType(entityType != null ? entityType : "UNKNOWN")
                    .entityId(entityId)
                    .success(success)
                    .severity(severity != null ? severity : (success ? AuditSeverity.INFO : AuditSeverity.WARNING))
                    .commentaire(sanitize(commentaire))
                    .reason(sanitize(commentaire))
                    .referenceMetier(sanitize(referenceMetier))
                    .referenceNumber(sanitize(referenceMetier))
                    .oldValue(sanitize(oldValue))
                    .newValue(sanitize(newValue))
                    .oldValuesJson(sanitize(oldValue))
                    .newValuesJson(sanitize(newValue))
                    .errorMessage(sanitize(errorMessage))
                    .caisseId(caisseId)
                    .sessionCaisseId(sessionCaisseId)
                    .siteId(siteId)
                    .siteLibelle(siteLibelle)
                    .dateAction(LocalDateTime.now())
                    .build();

            if (currentUser != null) {
                auditLog.setUserId(currentUser.getId());
                auditLog.setUsername(currentUser.getUsername());
                if (currentUser.getRole() != null) {
                    RoleCode roleCode = currentUser.getRole().getCode();
                    auditLog.setRoleCode(roleCode);
                    auditLog.setUserRole(roleCode != null ? roleCode.name() : null);
                }
                if (auditLog.getSiteId() == null && currentUser.getSite() != null) {
                    auditLog.setSiteId(currentUser.getSite().getId());
                }
                if (auditLog.getSiteLibelle() == null && currentUser.getSite() != null) {
                    auditLog.setSiteLibelle(currentUser.getSite().getNomSite());
                }
            } else {
                auditLog.setUsername("ANONYMOUS");
                auditLog.setUserRole("ANONYMOUS");
            }

            enrichHttpContext(auditLog);
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Erreur lors de l'enregistrement audit (tolérée)", e);
        }
    }

    @Override
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
        logAction(
                action,
                inferModule(entityType, action),
                entityType,
                entityId,
                success,
                success ? AuditSeverity.INFO : AuditSeverity.WARNING,
                reason,
                referenceNumber,
                oldValuesJson,
                newValuesJson,
                errorMessage,
                null,
                null,
                null,
                null
        );
    }

    @Override
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
        log(action, entityType, entityId, success, reason, null, oldValuesJson, newValuesJson, errorMessage);
    }

    @Override
    public void logSuccess(AuditAction action, String entityType, Long entityId, String reason) {
        log(action, entityType, entityId, true, reason, null, null, null, null);
    }

    @Override
    public void logFailure(AuditAction action, String entityType, Long entityId, String reason, String errorMessage) {
        log(action, entityType, entityId, false, reason, null, null, null, errorMessage);
    }

    @Override
    public void logAccessDenied(String operation, String reason) {
        logSecurityEvent(AuditAction.ACCESS_DENIED, "Accès refusé à " + operation + " : " + reason, false, reason);
    }

    @Override
    public void logInvalidOperation(String entityType, Long entityId, String reason, String errorMessage) {
        log(AuditAction.INVALID_OPERATION, entityType, entityId, false, reason, null, null, null, errorMessage);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> search(AuditLogFilterRequest filter, Pageable pageable) {
        Specification<AuditLog> specification = buildSpecification(filter);
        return auditLogRepository.findAll(specification, normalizePage(pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> findByEntity(String entityType, Long entityId, Pageable pageable) {
        AuditLogFilterRequest filter = new AuditLogFilterRequest();
        filter.setEntityType(entityType);
        filter.setEntityId(entityId);
        return search(filter, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> findByUser(Long userId, Pageable pageable) {
        AuditLogFilterRequest filter = new AuditLogFilterRequest();
        filter.setUserId(userId);
        return search(filter, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> findBySessionCaisse(Long sessionCaisseId, Pageable pageable) {
        AuditLogFilterRequest filter = new AuditLogFilterRequest();
        filter.setSessionCaisseId(sessionCaisseId);
        return search(filter, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLog findByIdScoped(Long id) {
        AuditLog auditLog = auditLogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Audit log introuvable: " + id));

        if (auditAccessService.canCurrentUserAccessGlobalAudit()) {
            return auditLog;
        }

        Long scopedSiteId = auditAccessService.resolveSiteScopeForCurrentUser();
        if (scopedSiteId != null && scopedSiteId.equals(auditLog.getSiteId())) {
            return auditLog;
        }

        Utilisateur currentUser = SecurityUtils.getCurrentUser();
        if (currentUser != null && currentUser.getId() != null && currentUser.getId().equals(auditLog.getUserId())) {
            return auditLog;
        }

        throw new org.springframework.security.access.AccessDeniedException("Accès audit refusé");
    }

    @Override
    @Transactional(readOnly = true)
    public AuditStatsResponse buildStats(AuditLogFilterRequest filter) {
        List<AuditLog> logs = search(filter, PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, "dateAction"))).getContent();

        Map<String, Long> byModule = new LinkedHashMap<>();
        Map<String, Long> bySeverity = new LinkedHashMap<>();
        long failedLogs = 0;

        for (AuditLog logItem : logs) {
            String module = logItem.getModule() != null ? logItem.getModule().name() : "UNKNOWN";
            byModule.put(module, byModule.getOrDefault(module, 0L) + 1);

            String severity = logItem.getSeverity() != null ? logItem.getSeverity().name() : "INFO";
            bySeverity.put(severity, bySeverity.getOrDefault(severity, 0L) + 1);

            if (Boolean.FALSE.equals(logItem.getSuccess())) {
                failedLogs++;
            }
        }

        List<com.mini.credit.dto.audit.AuditLogView> criticalRecent = logs.stream()
                .filter(a -> a.getSeverity() == AuditSeverity.CRITICAL)
                .sorted((a, b) -> Objects.requireNonNullElse(b.getDateAction(), b.getDateCreation())
                        .compareTo(Objects.requireNonNullElse(a.getDateAction(), a.getDateCreation())))
                .limit(20)
                .map(auditLogMapper::toView)
                .toList();

        return AuditStatsResponse.builder()
                .totalLogs(logs.size())
                .failedLogs(failedLogs)
                .logsParModule(byModule)
                .logsParSeverite(bySeverity)
                .actionsCritiquesRecentes(criticalRecent)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportCsv(AuditLogFilterRequest filter) {
        List<AuditLog> logs = search(filter,
                PageRequest.of(0, EXPORT_MAX_ROWS, Sort.by(Sort.Direction.DESC, "dateAction"))).getContent();

        StringBuilder builder = new StringBuilder();
        builder.append("id,dateAction,username,userRole,module,action,severity,success,siteId,siteLibelle,caisseId,sessionCaisseId,entityType,entityId,referenceMetier,commentaire,ipAddress,errorMessage\n");

        for (AuditLog logItem : logs) {
            builder.append(csv(logItem.getId())).append(',')
                    .append(csv(logItem.getDateAction() != null ? logItem.getDateAction() : logItem.getDateCreation())).append(',')
                    .append(csv(logItem.getUsername())).append(',')
                    .append(csv(logItem.getUserRole())).append(',')
                    .append(csv(logItem.getModule())).append(',')
                    .append(csv(logItem.getAction())).append(',')
                    .append(csv(logItem.getSeverity())).append(',')
                    .append(csv(logItem.getSuccess())).append(',')
                    .append(csv(logItem.getSiteId())).append(',')
                    .append(csv(logItem.getSiteLibelle())).append(',')
                    .append(csv(logItem.getCaisseId())).append(',')
                    .append(csv(logItem.getSessionCaisseId())).append(',')
                    .append(csv(logItem.getEntityType())).append(',')
                    .append(csv(logItem.getEntityId())).append(',')
                    .append(csv(logItem.getReferenceMetier())).append(',')
                    .append(csv(logItem.getCommentaire())).append(',')
                    .append(csv(logItem.getIpAddress())).append(',')
                    .append(csv(logItem.getErrorMessage())).append('\n');
        }

        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private Specification<AuditLog> buildSpecification(AuditLogFilterRequest filter) {
        Utilisateur currentUser = SecurityUtils.getCurrentUser();
        Long scopedSiteId = auditAccessService.resolveSiteScopeForCurrentUser();
        boolean globalAccess = auditAccessService.canCurrentUserAccessGlobalAudit();

        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            if (filter != null) {
                if (filter.getDateDebut() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("dateAction"), filter.getDateDebut().atStartOfDay()));
                }
                if (filter.getDateFin() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("dateAction"), filter.getDateFin().atTime(23, 59, 59)));
                }
                if (filter.getModule() != null) {
                    predicates.add(cb.equal(root.get("module"), filter.getModule()));
                }
                if (filter.getAction() != null) {
                    predicates.add(cb.equal(root.get("action"), filter.getAction()));
                }
                if (filter.getSeverity() != null) {
                    predicates.add(cb.equal(root.get("severity"), filter.getSeverity()));
                }
                if (filter.getSuccess() != null) {
                    predicates.add(cb.equal(root.get("success"), filter.getSuccess()));
                }
                if (filter.getUserId() != null) {
                    predicates.add(cb.equal(root.get("userId"), filter.getUserId()));
                }
                if (filter.getSiteId() != null) {
                    predicates.add(cb.equal(root.get("siteId"), filter.getSiteId()));
                }
                if (filter.getCaisseId() != null) {
                    predicates.add(cb.equal(root.get("caisseId"), filter.getCaisseId()));
                }
                if (filter.getSessionCaisseId() != null) {
                    predicates.add(cb.equal(root.get("sessionCaisseId"), filter.getSessionCaisseId()));
                }
                if (filter.getEntityType() != null && !filter.getEntityType().isBlank()) {
                    predicates.add(cb.equal(root.get("entityType"), filter.getEntityType().trim()));
                }
                if (filter.getEntityId() != null) {
                    predicates.add(cb.equal(root.get("entityId"), filter.getEntityId()));
                }
                if (filter.getReferenceMetier() != null && !filter.getReferenceMetier().isBlank()) {
                    predicates.add(cb.like(cb.lower(root.get("referenceMetier")), "%" + filter.getReferenceMetier().toLowerCase(Locale.ROOT) + "%"));
                }
            }

            if (!globalAccess) {
                if (scopedSiteId != null) {
                    predicates.add(cb.equal(root.get("siteId"), scopedSiteId));
                } else if (currentUser != null) {
                    predicates.add(cb.equal(root.get("userId"), currentUser.getId()));
                }
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private Pageable normalizePage(Pageable pageable) {
        int page = pageable != null ? Math.max(pageable.getPageNumber(), 0) : 0;
        int size = pageable != null ? Math.min(Math.max(pageable.getPageSize(), 1), 200) : 50;
        Sort sort = pageable != null && pageable.getSort().isSorted()
                ? pageable.getSort()
                : Sort.by(Sort.Direction.DESC, "dateAction");
        return PageRequest.of(page, size, sort);
    }

    private AuditModule inferModule(String entityType, AuditAction action) {
        if (action == AuditAction.DATA_EXPORT) {
            return AuditModule.CONTROLE_INTERNE;
        }

        if (entityType == null) {
            return AuditModule.PARAMETRAGE;
        }

        String normalized = entityType.toLowerCase(Locale.ROOT);
        if (normalized.contains("session")) {
            return AuditModule.SESSION_CAISSE;
        }
        if (normalized.contains("operation")) {
            return AuditModule.OPERATION_CAISSE;
        }
        if (normalized.contains("depense")) {
            return AuditModule.DEPENSE_CAISSE;
        }
        if (normalized.contains("rapport")) {
            return AuditModule.RAPPORT_CAISSE;
        }
        if (normalized.contains("credit")) {
            return AuditModule.CREDIT;
        }
        if (normalized.contains("recette")) {
            return AuditModule.RECETTE_TERRAIN;
        }
        if (normalized.contains("security") || normalized.contains("auth")) {
            return AuditModule.AUTHENTIFICATION;
        }
        return AuditModule.CAISSE;
    }

    private String sanitize(String value) {
        if (value == null) {
            return null;
        }

        String text = value.trim();
        if (text.isEmpty()) {
            return null;
        }

        if (SENSITIVE_PATTERN.matcher(text).find()) {
            return "[REDACTED]";
        }

        return text;
    }

    private void enrichHttpContext(AuditLog auditLog) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return;
            }

            HttpServletRequest request = attributes.getRequest();
            String forwardedFor = request.getHeader("X-Forwarded-For");
            String ipAddress = (forwardedFor != null && !forwardedFor.isBlank())
                    ? forwardedFor.split(",")[0].trim()
                    : request.getRemoteAddr();

            auditLog.setIpAddress(ipAddress);
            auditLog.setUserAgent(request.getHeader("User-Agent"));
        } catch (Exception e) {
            log.debug("Contexte HTTP indisponible pour audit", e);
        }
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        boolean needQuotes = text.contains(",") || text.contains("\n") || text.contains("\r") || text.contains("\"");
        String escaped = text.replace("\"", "\"\"");
        return needQuotes ? "\"" + escaped + "\"" : escaped;
    }
}
