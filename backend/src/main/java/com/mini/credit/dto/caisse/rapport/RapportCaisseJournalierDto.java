package com.mini.credit.dto.caisse.rapport;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class RapportCaisseJournalierDto {
    private LocalDate date;
    private long nombreSessions;
    private long nombreOperations;
    private long nombreSessionsNonCloturees;
    private BigDecimal totalEntrees;
    private BigDecimal totalSorties;
    private BigDecimal soldeTheoriqueTotal;
    private BigDecimal soldePhysiqueTotal;
    private BigDecimal ecartTotal;
    private long nombreDepenses;
    private BigDecimal montantDepenses;
    private long nombreEcarts;
    private BigDecimal montantEcarts;
}
