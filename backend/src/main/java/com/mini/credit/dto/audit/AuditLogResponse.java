package com.mini.credit.dto.audit;

import com.mini.credit.enums.ActionAudit;
import com.mini.credit.enums.security.RoleCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogResponse {
    private Long id;
    private ActionAudit action;
    private String entityType;
    private Long entityId;
    private String username;
    private RoleCode roleCode;
    private Long userId;
    private String referenceNumber;
    private String reason;
    private Boolean success;
    private String errorMessage;
    private String oldValuesJson;
    private String newValuesJson;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdDate;
}
