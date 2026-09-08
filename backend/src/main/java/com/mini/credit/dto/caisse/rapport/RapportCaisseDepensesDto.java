package com.mini.credit.dto.caisse.rapport;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class RapportCaisseDepensesDto {
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private long nombreDepenses;
    private BigDecimal montantTotal;
    private List<RapportCaisseDepenseLigneDto> lignes;
}
