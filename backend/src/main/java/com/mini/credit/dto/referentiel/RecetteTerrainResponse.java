package com.mini.credit.dto.referentiel;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO de réponse pour recette terrain journalière
 * Contient toutes les informations de la recette avec audit trail
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecetteTerrainResponse {

    // ===== Identifiants =====
    private Long id;

    // ===== Relations =====
    private Long agentTerrainId;
    private String agentTerrainMatricule;
    private String agentTerrainNom;

    private Long siteId;
    private String siteNom;

    // ===== Date =====
    private LocalDate dateRecette;

    // ===== Statut workflow =====
    private String statut; // BROUILLON, SOUMISE, VALIDEE, REJETEE

    // ===== Opérations terrain =====
    private Integer membresVisites;
    private Integer nouveauxMembres;
    private Integer carnetDistribues;

    // ===== Collecte épargne =====
    private BigDecimal epargneCollectee;
    private String epargneSourceType;

    // ===== Collecte crédit =====
    private BigDecimal remboursementsCreditCollectes;
    private String creditIdsTraites;

    // ===== Frais et demandes =====
    private BigDecimal fraisCollectes;
    private Integer demandesCreditRecueillies;
    private String demandesCreditIds;

    // ===== Trésorerie =====
    private BigDecimal especesRemises;
    private BigDecimal especesEmises;
    private BigDecimal excedent;
    private BigDecimal manquant;

    // ===== Metadata =====
    private String observations;
    private String pieceJointePath;

    // ===== Validation =====
    private LocalDateTime dateValidation;
    private Long valideParId;
    private String valideParNom;
    private String motifRejet;

    // ===== Audit =====
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    private Long modifiePar;

    /**
     * Calcule le total collecté (épargne + remboursements + frais)
     */
    @JsonProperty("totalCollecte")
    public BigDecimal getTotalCollecte() {
        BigDecimal total = BigDecimal.ZERO;
        if (epargneCollectee != null) total = total.add(epargneCollectee);
        if (remboursementsCreditCollectes != null) total = total.add(remboursementsCreditCollectes);
        if (fraisCollectes != null) total = total.add(fraisCollectes);
        return total;
    }

    /**
     * Indique l'écart (excédent ou manquant) en une seule valeur
     * Positive = excédent, Négative = manquant, 0 = équilibré
     */
    @JsonProperty("ecart")
    public BigDecimal getEcart() {
        if (excedent != null && excedent.compareTo(BigDecimal.ZERO) > 0) {
            return excedent;
        } else if (manquant != null && manquant.compareTo(BigDecimal.ZERO) > 0) {
            return manquant.negate();
        }
        return BigDecimal.ZERO;
    }
}
