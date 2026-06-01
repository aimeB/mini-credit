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
public class RapportFinancierDTO {
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private BigDecimal totalEncaissements;
    private BigDecimal totalDecaissements;
    private BigDecimal solde;
    private BigDecimal interets;
    private BigDecimal penalites;
    private BigDecimal totalRevenues;
}
