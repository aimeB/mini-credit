package com.mini.credit.dto.credit;

import com.mini.credit.enums.StatutCredit;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class CreditContratResponse {
    private Long creditId;
    private String numeroCredit;
    private Long demandeCreditId;
    private String numeroDemande;
    private StatutCredit statut;

    private String membreNomComplet;
    private String membreAdresse;
    private String membreTelephone;

    private BigDecimal montantAccorde;
    private String devise;
    private BigDecimal tauxInteret;
    private Integer dureeValeur;
    private String dureeUnite;
    private LocalDate datePret;
    private LocalDate dateDecaissement;
    private String objetCredit;
    private String gagePropose;

    private BigDecimal garantieRegleMontant;
    private String garantieRegleLibelle;
    private BigDecimal montantGarantieRequis;
    private BigDecimal montantGarantieBloque;
    private String sourceGarantie;
    private String garantiesMateriellesAcceptees;
    private String penaliteRetardLibelle;

    private BigDecimal totalPrincipal;
    private BigDecimal totalInteret;
    private BigDecimal totalAPayer;

    private String antenneNom;
    private String agentTerrainNom;
    private String gestionnaireNom;
    private String controleurNom;
    private String chefBureauNom;
    private String caissierNom;

    private List<CreditDetailResponse.EcheanceDetail> echeancier;
}
