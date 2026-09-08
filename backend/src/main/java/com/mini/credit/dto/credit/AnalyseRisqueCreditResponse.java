package com.mini.credit.dto.credit;

import com.mini.credit.enums.NiveauRisque;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class AnalyseRisqueCreditResponse {
    private BigDecimal scoreTotal;
    private NiveauRisque niveauRisque;
    private BigDecimal montantDemande;
    private BigDecimal revenusMensuels;
    private BigDecimal chargesMensuelles;
    private BigDecimal capaciteRemboursement;
    private BigDecimal mensualiteEstimee;
    private BigDecimal ratioMensualiteCapacite;
    private BigDecimal ratioMontantRevenu;
    private BigDecimal garantieEpargneRequise;
    private BigDecimal garantieEpargneDisponible;
    private BigDecimal garantieMaterielleRequise;
    private BigDecimal garantieMaterielleDeclaree;
    private List<CritereAnalyseRisqueDto> criteres;
}