package com.mini.credit.dto.audit;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class AuditStatsResponse {
    private long totalLogs;
    private long failedLogs;
    private Map<String, Long> logsParModule;
    private Map<String, Long> logsParSeverite;
    private List<AuditLogView> actionsCritiquesRecentes;
}
