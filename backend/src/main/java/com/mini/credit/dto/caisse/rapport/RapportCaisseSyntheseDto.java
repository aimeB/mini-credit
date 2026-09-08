package com.mini.credit.dto.caisse.rapport;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class RapportCaisseSyntheseDto {
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private long nombreSessions;
    private long nombreSessionsOuvertes;
    private BigDecimal totalEntrees;
    private BigDecimal totalSorties;
    private BigDecimal ecartTotal;
    private BigDecimal montantDepenses;
    private long nombreEcarts;
}
