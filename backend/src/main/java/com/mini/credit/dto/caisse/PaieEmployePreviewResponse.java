package com.mini.credit.dto.caisse;

import com.mini.credit.enums.ModeCalculPaie;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PaieEmployePreviewResponse {
    private Long employeId;
    private String matricule;
    private String nomComplet;
    private String poste;
    private String periodePaie;
    private BigDecimal salaireBase;
    private BigDecimal epargneCollecteeValidee;
    private BigDecimal remboursementCreditCollecteValide;
    private Integer nombreCarnetsVendus;
    private BigDecimal primeMobilisationEpargne;
    private BigDecimal primeMobilisationRemboursement;
    private BigDecimal bonusCarnets;
    private BigDecimal primeMotivationManuelle;
    private boolean primeMotivationManuelleAutorisee;
    private BigDecimal totalPrimes;
    private BigDecimal totalBonus;
    private BigDecimal totalAPayer;
    private ModeCalculPaie modeCalcul;
}