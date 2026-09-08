package com.mini.credit.dto.rapport;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class BilanJournalierDTO {
    private LocalDate date;
    private BigDecimal totalEncaissements;
    private BigDecimal totalDecaissements;
    private BigDecimal soldeJournee;
    private Long nombreOperations;
}