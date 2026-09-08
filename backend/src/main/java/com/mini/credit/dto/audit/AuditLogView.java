package com.mini.credit.dto.audit;

import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.enums.security.AuditModule;
import com.mini.credit.enums.security.AuditSeverity;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AuditLogView {
    private Long id;
    private AuditAction action;
    private AuditModule module;
    private String entityType;
    private Long entityId;
    private Long userId;
    private String username;
    private String userRole;
    private Long siteId;
    private String siteLibelle;
    private Long caisseId;
    private Long sessionCaisseId;
    private LocalDateTime dateAction;
    private String ipAddress;
    private String userAgent;
    private String oldValue;
    private String newValue;
    private String commentaire;
    private AuditSeverity severity;
    private Boolean success;
    private String errorMessage;
    private String referenceMetier;
}
