package com.mini.credit.mapper;

import com.mini.credit.dto.audit.AuditLogView;
import com.mini.credit.entity.audit.AuditLog;
import org.springframework.stereotype.Component;

@Component
public class AuditLogMapper {

    public AuditLogView toView(AuditLog entity) {
        if (entity == null) {
            return null;
        }

        return AuditLogView.builder()
                .id(entity.getId())
                .action(entity.getAction())
                .module(entity.getModule())
                .entityType(entity.getEntityType())
                .entityId(entity.getEntityId())
                .userId(entity.getUserId())
                .username(entity.getUsername())
                .userRole(entity.getUserRole())
                .siteId(entity.getSiteId())
                .siteLibelle(entity.getSiteLibelle())
                .caisseId(entity.getCaisseId())
                .sessionCaisseId(entity.getSessionCaisseId())
                .dateAction(entity.getDateAction() != null ? entity.getDateAction() : entity.getDateCreation())
                .ipAddress(entity.getIpAddress())
                .userAgent(entity.getUserAgent())
                .oldValue(entity.getOldValue() != null ? entity.getOldValue() : entity.getOldValuesJson())
                .newValue(entity.getNewValue() != null ? entity.getNewValue() : entity.getNewValuesJson())
                .commentaire(entity.getCommentaire() != null ? entity.getCommentaire() : entity.getReason())
                .severity(entity.getSeverity())
                .success(entity.getSuccess())
                .errorMessage(entity.getErrorMessage())
                .referenceMetier(entity.getReferenceMetier() != null ? entity.getReferenceMetier() : entity.getReferenceNumber())
                .build();
    }
}
