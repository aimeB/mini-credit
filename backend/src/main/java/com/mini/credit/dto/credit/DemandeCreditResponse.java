package com.mini.credit.dto.credit;

import com.mini.credit.enums.DureeUnite;
import com.mini.credit.enums.PeriodiciteRemboursement;
import com.mini.credit.enums.StatutDemandeCredit;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class DemandeCreditResponse {
    private Long id;
    private String numeroDemande;
    private Long membreId;
    private String membreNomComplet;
    private Long siteId;
    private String siteNom;
    private Long agentId;
    private LocalDate dateDemande;
    private BigDecimal montantDemande;
    private BigDecimal fraisDemandePayes;
    private String devise;
    private Integer dureeValeur;
    private DureeUnite dureeUnite;
    private PeriodiciteRemboursement periodiciteRemboursement;
    private BigDecimal tauxInteret;
    private String objetCredit;
    private String gagePropose;
    private String activiteFinancee;
    private BigDecimal revenusEstimes;
    private BigDecimal chargesEstimees;
    private BigDecimal fraisDemande;
    private BigDecimal depotGarantieRequis;
    private BigDecimal depotGarantiePaye;
    private BigDecimal montantGarantieBloque;
    private Boolean garantieBloquee;
    private String statutGarantie;
    private StatutDemandeCredit statut;
    private String commentaireDecision;
    private LocalDateTime dateDecision;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private AnalyseRisqueCreditResponse analyseRisque;
}