package com.mini.credit.service.audit;

import com.mini.credit.entity.audit.AuditLog;
import com.mini.credit.entity.caisse.OperationCaisse;
import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.exception.ResourceNotFoundException;
import com.mini.credit.repository.audit.AuditLogRepository;
import com.mini.credit.repository.caisse.OperationCaisseRepository;
import com.mini.credit.repository.caisse.SessionCaisseRepository;
import com.mini.credit.service.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditAccessService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AuditLogRepository auditLogRepository;
    private final SessionCaisseRepository sessionCaisseRepository;
    private final OperationCaisseRepository operationCaisseRepository;

    public boolean canCurrentUserAccessGlobalAudit() {
        Utilisateur currentUser = SecurityUtils.getCurrentUserOrThrow();
        RoleCode roleCode = currentUser.getRole() != null ? currentUser.getRole().getCode() : null;
        return roleCode == RoleCode.ADMIN || roleCode == RoleCode.RCI;
    }

    public Long resolveSiteScopeForCurrentUser() {
        Utilisateur currentUser = SecurityUtils.getCurrentUserOrThrow();
        if (canCurrentUserAccessGlobalAudit()) {
            return null;
        }

        if (currentUser.getSite() != null && currentUser.getSite().getId() != null) {
            return currentUser.getSite().getId();
        }

        return null;
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getEntityAuditTrailScoped(String entityType, Long entityId, int page, int size) {
        int normalizedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int normalizedPage = Math.max(page, 0);

        checkEntityAuditAccess(entityType, entityId);

        Pageable pageable = PageRequest.of(
                normalizedPage,
                normalizedSize,
                Sort.by("dateCreation").descending()
        );
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable);
    }

    private void checkEntityAuditAccess(String entityType, Long entityId) {
        Utilisateur currentUser = SecurityUtils.getCurrentUserOrThrow();
        RoleCode roleCode = currentUser.getRole() != null ? currentUser.getRole().getCode() : null;
        if (roleCode == null) {
            throw new AccessDeniedException("Rôle utilisateur introuvable");
        }

        if (roleCode == RoleCode.ADMIN || roleCode == RoleCode.RCI || roleCode == RoleCode.CONTROLEUR) {
            return;
        }

        if (roleCode == RoleCode.CHEF_BUREAU) {
            Long entitySiteId = resolveEntitySiteId(entityType, entityId);
            Long userSiteId = currentUser.getSite() != null ? currentUser.getSite().getId() : null;
            if (userSiteId == null || entitySiteId == null || !userSiteId.equals(entitySiteId)) {
                throw new AccessDeniedException("Accès audit refusé: périmètre site dépassé");
            }
            return;
        }

        if (roleCode == RoleCode.CAISSIER) {
            checkCaissierScope(currentUser, entityType, entityId);
            return;
        }

        throw new AccessDeniedException("Accès audit interdit pour ce rôle");
    }

    private void checkCaissierScope(Utilisateur currentUser, String entityType, Long entityId) {
        if ("SessionCaisse".equalsIgnoreCase(entityType)) {
            SessionCaisse session = loadSession(entityId);
            if (session.getUtilisateur() == null || !session.getUtilisateur().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("Accès audit refusé: session non appartenant au caissier");
            }
            return;
        }

        if ("OperationCaisse".equalsIgnoreCase(entityType)) {
            OperationCaisse operation = operationCaisseRepository.findByIdWithAuditContext(entityId)
                    .orElseThrow(() -> new ResourceNotFoundException("Opération caisse introuvable"));
            SessionCaisse session = operation.getSessionCaisse();
            if (session == null || session.getUtilisateur() == null
                    || !session.getUtilisateur().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("Accès audit refusé: opération hors périmètre caissier");
            }
            return;
        }

        throw new AccessDeniedException("Accès audit refusé pour ce type d'entité");
    }

    private void assertSameSite(Utilisateur user, Long entitySiteId) {
        Long userSiteId = user.getSite() != null ? user.getSite().getId() : null;
        if (userSiteId == null || entitySiteId == null || !userSiteId.equals(entitySiteId)) {
            throw new AccessDeniedException("Accès audit refusé: site non autorisé");
        }
    }

    private Long resolveEntitySiteId(String entityType, Long entityId) {
        if ("SessionCaisse".equalsIgnoreCase(entityType)) {
            SessionCaisse session = loadSession(entityId);
            return session.getCaisse() != null && session.getCaisse().getSite() != null
                    ? session.getCaisse().getSite().getId()
                    : null;
        }

        if ("OperationCaisse".equalsIgnoreCase(entityType)) {
            OperationCaisse operation = operationCaisseRepository.findByIdWithAuditContext(entityId)
                    .orElseThrow(() -> new ResourceNotFoundException("Opération caisse introuvable"));
            return operation.getCaisse() != null && operation.getCaisse().getSite() != null
                    ? operation.getCaisse().getSite().getId()
                    : null;
        }

        throw new AccessDeniedException("Accès audit refusé pour ce type d'entité");
    }

    private SessionCaisse loadSession(Long sessionId) {
        return sessionCaisseRepository.findByIdWithAuditContext(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session caisse introuvable"));
    }
}
