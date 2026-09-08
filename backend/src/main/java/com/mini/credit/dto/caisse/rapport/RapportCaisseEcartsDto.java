package com.mini.credit.dto.caisse.rapport;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class RapportCaisseEcartsDto {
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private long nombreEcarts;
    private BigDecimal montantTotal;
    private long nombreEcartsOuverts;
    private List<RapportCaisseEcartLigneDto> lignes;
}
