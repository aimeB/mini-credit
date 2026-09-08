package com.mini.credit.dto.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditStatistiques {
    private long totalLogsCount;
    private long successfulActionsCount;
    private long failedActionsCount;
    private long loginCount;
    private long logoutCount;
    private long createActionsCount;
    private long updateActionsCount;
    private long deleteActionsCount;
    private long approveActionsCount;
    private long rejectActionsCount;
    private double successRate;
}
