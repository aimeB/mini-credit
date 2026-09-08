package com.mini.credit.dto.caisse;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * PHASE 6B.1: Response DTO for FicheJournaliereAgentTerrain
 * 
 * Complete projection of entity for API responses.
 * Includes all fields for read operations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FicheJournaliereResponse implements Serializable {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("agentTerrainId")
    private Long agentTerrainId;

    @JsonProperty("agentTerrainNom")
    private String agentTerrainNom;

    @JsonProperty("siteId")
    private Long siteId;

    @JsonProperty("siteName")
    private String siteName;

    @JsonProperty("dateFiche")
    private LocalDate dateFiche;

    @JsonProperty("statut")
    private String statut;

    // Consolidation financière
    @JsonProperty("epargneCollecteeTotal")
    private BigDecimal epargneCollecteeTotal;

    @JsonProperty("remboursementCollectes")
    private BigDecimal remboursementCollectes;

    @JsonProperty("fraisCollectes")
    private BigDecimal fraisCollectes;

    @JsonProperty("autresRecettes")
    private BigDecimal autresRecettes;

    @JsonProperty("montantTotalCollecte")
    private BigDecimal montantTotalCollecte;

    // Dénombrements
    @JsonProperty("nombreMembresVisites")
    private Integer nombreMembresVisites;

    @JsonProperty("nombreNouveauxMembres")
    private Integer nombreNouveauxMembres;

    @JsonProperty("nombreCarnetsDistribues")
    private Integer nombreCarnetsDistribues;

    // Contrôle caisse
    @JsonProperty("totalEspecesRemises")
    private BigDecimal totalEspecesRemises;

    @JsonProperty("variance")
    private BigDecimal variance;

    @JsonProperty("variancePercentage")
    private Double variancePercentage;

    @JsonProperty("excedent")
    private BigDecimal excedent;

    @JsonProperty("manquant")
    private BigDecimal manquant;

    // Observations
    @JsonProperty("observationsAgent")
    private String observationsAgent;

    @JsonProperty("observationsControleur")
    private String observationsControleur;

    // Validation
    @JsonProperty("valideParId")
    private Long valideParId;

    @JsonProperty("valideParNom")
    private String valideParNom;

    @JsonProperty("dateValidation")
    private LocalDateTime dateValidation;

    @JsonProperty("motifRejet")
    private String motifRejet;

    @JsonProperty("raisonAnnulation")
    private String raisonAnnulation;

    // Audit
    @JsonProperty("dateCreation")
    private LocalDateTime dateCreation;

    @JsonProperty("dateModification")
    private LocalDateTime dateModification;
}
