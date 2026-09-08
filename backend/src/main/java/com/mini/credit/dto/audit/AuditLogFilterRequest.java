package com.mini.credit.dto.audit;

import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.enums.security.AuditModule;
import com.mini.credit.enums.security.AuditSeverity;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class AuditLogFilterRequest {
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateDebut;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFin;

    private AuditModule module;
    private AuditAction action;
    private AuditSeverity severity;
    private Boolean success;
    private Long userId;
    private Long siteId;
    private Long caisseId;
    private Long sessionCaisseId;
    private String entityType;
    private Long entityId;
    private String referenceMetier;
}
