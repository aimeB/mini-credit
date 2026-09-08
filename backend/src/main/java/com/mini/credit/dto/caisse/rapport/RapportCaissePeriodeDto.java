package com.mini.credit.dto.caisse.rapport;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class RapportCaissePeriodeDto {
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private long nombreSessions;
    private long nombreSessionsNonCloturees;
    private BigDecimal totalEntrees;
    private BigDecimal totalSorties;
    private BigDecimal soldeTheoriqueTotal;
    private BigDecimal soldePhysiqueTotal;
    private BigDecimal ecartTotal;
    private long nombreOperations;
    private long nombreDepenses;
    private BigDecimal montantDepenses;
    private long nombreEcarts;
    private BigDecimal montantEcarts;
}
