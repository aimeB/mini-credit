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

    private String analyseId;

    private LocalDate dateVisite;
    private String lieuVisite;
    private Boolean activiteVerifiee;
    private String descriptionActivite;
    private String ancienneteActivite;
    private BigDecimal chiffreAffairesEstime;
    private String chiffreAffairesDevise;
    private BigDecimal revenuNetEstime;
    private String revenuNetDevise;
    private BigDecimal chargesMensuelles;
    private String chargesMensuellesDevise;
    private BigDecimal capaciteRemboursement;
    private String capaciteRemboursementDevise;
    private String montantDemandeDevise;
    private String fraisDemandeDevise;
    private String depotRequisDevise;
    private String depotPayeDevise;
    private NiveauRisque risqueNiveau;
    private BigDecimal scoreRisque;
    private Boolean scoreRisqueCorrigeManuellement;

    @NotNull
    private RecommandationRisque recommandation;

    private String commentaire;
}
