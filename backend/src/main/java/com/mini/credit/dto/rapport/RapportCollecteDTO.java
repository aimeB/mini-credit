package com.mini.credit.dto.rapport;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RapportCollecteDTO {
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private BigDecimal totalCollecte;
    private BigDecimal totalEpargne;
    private long nombreMembres;
    private long nombreOperations;
    private BigDecimal moyenneParMembre;
    private String tendance;
}
