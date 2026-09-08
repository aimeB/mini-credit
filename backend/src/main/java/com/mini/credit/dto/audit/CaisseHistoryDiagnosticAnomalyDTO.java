package com.mini.credit.dto.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaisseHistoryDiagnosticAnomalyDTO {

    private String anomalyType;
    private String severity;
    private Long sessionId;
    private Long caisseId;
    private Long operationId;
    private Long siteId;
    private BigDecimal expectedAmount;
    private BigDecimal actualAmount;
    private BigDecimal delta;
    private String currentStatus;
    private LocalDateTime dateOuverture;
    private LocalDateTime dateCloture;
    private String recommendation;
    private String details;
}