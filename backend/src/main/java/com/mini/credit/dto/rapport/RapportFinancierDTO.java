package com.mini.credit.dto.rapport;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mini.credit.enums.PeriodiciteRapport;
import com.mini.credit.enums.StatutRapport;
import com.mini.credit.enums.TypeRapport;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * PHASE 12: DTO Rapport Financier
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RapportFinancierDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("typeRapport")
    private TypeRapport typeRapport;

    @JsonProperty("periodicite")
    private PeriodiciteRapport periodicite;

    @JsonProperty("dateDebut")
    private LocalDate dateDebut;

    @JsonProperty("dateFin")
    private LocalDate dateFin;

    @JsonProperty("dateGeneration")
    private LocalDateTime dateGeneration;

    @JsonProperty("statut")
    private StatutRapport statut;

    @JsonProperty("genereParNom")
    private String genereParNom;

    @JsonProperty("valideParNom")
    private String valideParNom;

    @JsonProperty("dateValidation")
    private LocalDateTime dateValidation;

    // ============ BILAN ============

    @JsonProperty("totalActif")
    private BigDecimal totalActif;

    @JsonProperty("totalPassif")
    private BigDecimal totalPassif;

    @JsonProperty("capitauxPropres")
    private BigDecimal capitauxPropres;

    // ============ FLUX ============

    @JsonProperty("soldeCaisseDebut")
    private BigDecimal soldeCaisseDebut;

    @JsonProperty("soldeCaisseFin")
    private BigDecimal soldeCaisseFin;

    @JsonProperty("totalCreditsDecaisses")
    private BigDecimal totalCreditsDecaisses;

    @JsonProperty("totalRembourses")
    private BigDecimal totalRembourses;

    @JsonProperty("totalEpargnesDeposes")
    private BigDecimal totalEpargnesDeposes;

    @JsonProperty("totalRetraitsEpargnes")
    private BigDecimal totalRetraitsEpargnes;

    // ============ COMPTE DE RÉSULTAT ============

    @JsonProperty("totalRevenus")
    private BigDecimal totalRevenus;

    @JsonProperty("totalCharges")
    private BigDecimal totalCharges;

    @JsonProperty("resultat")
    private BigDecimal resultat;

    @JsonProperty("interetsCredits")
    private BigDecimal interetsCredits;

    @JsonProperty("commissionsAgents")
    private BigDecimal commissionsAgents;

    @JsonProperty("interetsEpargnes")
    private BigDecimal interetsEpargnes;

    @JsonProperty("penalitesCollectees")
    private BigDecimal penalitesCollectees;

    @JsonProperty("penalitesEffacees")
    private BigDecimal penalitesEffacees;

    // ============ KPI ============

    @JsonProperty("nombreCreditsActifs")
    private Long nombreCreditsActifs;

    @JsonProperty("nombreCreditsRembourses")
    private Long nombreCreditsRembourses;

    @JsonProperty("nombreCreditsEnRetard")
    private Long nombreCreditsEnRetard;

    @JsonProperty("tauxRemboursement")
    private BigDecimal tauxRemboursement;

    @JsonProperty("nombreMembresActifs")
    private Long nombreMembresActifs;

    @JsonProperty("soldeEpargnesMoyen")
    private BigDecimal soldeEpargnesMoyen;

    @JsonProperty("totalEpargnesCaisse")
    private BigDecimal totalEpargnesCaisse;

    @JsonProperty("roa")
    private BigDecimal roa;

    @JsonProperty("roe")
    private BigDecimal roe;

    @JsonProperty("observation")
    private String observation;

    @JsonProperty("messageErreur")
    private String messageErreur;

    @JsonProperty("dateCreation")
    private LocalDateTime dateCreation;

    @JsonProperty("dateModification")
    private LocalDateTime dateModification;

    @JsonProperty("periodLabel")
    private String periodLabel;
}
