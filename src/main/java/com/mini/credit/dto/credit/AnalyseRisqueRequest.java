package com.mini.credit.dto.credit;

import com.mini.credit.enums.NiveauRisque;
import com.mini.credit.enums.RecommandationRisque;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class AnalyseRisqueRequest {

    @NotNull
    private Long analysteId;

    private LocalDate dateVisite;
    private String lieuVisite;
    private Boolean activiteVerifiee;
    private String descriptionActivite;
    private String ancienneteActivite;
    private BigDecimal chiffreAffairesEstime;
    private BigDecimal revenuNetEstime;
    private BigDecimal chargesMensuelles;
    private BigDecimal capaciteRemboursement;
    private NiveauRisque risqueNiveau;
    private BigDecimal scoreRisque;

    @NotNull
    private RecommandationRisque recommandation;

    private String commentaire;
}
