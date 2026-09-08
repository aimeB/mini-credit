package com.mini.credit.dto.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaisseHistoryDiagnosticReportDTO {

    private LocalDateTime generatedAt;
    private String generatedBy;
    private long sessionsAnalyzed;
    private long caissesAnalyzed;
    private long operationsAnalyzed;
    private long anomalyCount;
    private Map<String, Long> anomaliesByType;
    private boolean readOnlyMode;
    private boolean correctionsApplied;
    private List<CaisseHistoryDiagnosticAnomalyDTO> anomalies;
}