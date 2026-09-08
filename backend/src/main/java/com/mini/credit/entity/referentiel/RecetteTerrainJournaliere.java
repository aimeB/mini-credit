package com.mini.credit.entity.referentiel;

import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.enums.RecetteStatut;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entité représentant une recette journalière collectée par un agent terrain
 * 
 * Contient:
 * - Informations de collecte (membres, épargne, crédits, frais)
 * - Gestion trésorerie (espèces remises/émises, écarts)
 * - Workflow validation (BROUILLON → SOUMISE → VALIDEE/REJETEE)
 * - Audit trail complet
 */
@Entity
@Table(name = "recette_terrain_journaliere", 
       uniqueConstraints = @UniqueConstraint(name = "uk_agent_date_recette", columnNames = {"agent_terrain_id", "date_recette"}),
       indexes = {
           @Index(name = "idx_rtj_agent_terrain", columnList = "agent_terrain_id"),
           @Index(name = "idx_rtj_site", columnList = "site_id"),
           @Index(name = "idx_rtj_date_recette", columnList = "date_recette"),
           @Index(name = "idx_rtj_statut", columnList = "statut"),
           @Index(name = "idx_rtj_agent_date", columnList = "agent_terrain_id,date_recette"),
           @Index(name = "idx_rtj_site_statut", columnList = "site_id,statut")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecetteTerrainJournaliere extends BaseEntity {

    // ========== RELATIONS ==========
    
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "agent_terrain_id", nullable = false)
    private AgentTerrain agentTerrain;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    // ========== TIMING ==========

    @Column(name = "date_recette", nullable = false)
    private LocalDate dateRecette;

    // ========== OPÉRATIONS TERRAIN ==========

    @Column(name = "membres_visites", nullable = false)
    @Builder.Default
    private Integer membresVisites = 0;

    @Column(name = "nouveaux_membres", nullable = false)
    @Builder.Default
    private Integer nouveauxMembres = 0;

    @Column(name = "carnet_distribues", nullable = false)
    @Builder.Default
    private Integer carnetDistribues = 0;

    // ========== COLLECTE ÉPARGNE ==========

    @Column(name = "epargne_collectee", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal epargneCollectee = BigDecimal.ZERO;

    @Column(name = "epargne_source_type", length = 255)
    private String epargneSourceType; // "VOLONTAIRE", "COTISATION", "MIXTE", etc

    // ========== COLLECTE CRÉDIT ==========

    @Column(name = "remboursements_credit_collectes", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal remboursementsCreditCollectes = BigDecimal.ZERO;

    @Column(name = "credit_ids_traites", columnDefinition = "JSON")
    private String creditIdsTraites; // JSON: "[1001, 1002, 1003]"

    // ========== FRAIS ET DEMANDES ==========

    @Column(name = "frais_collectes", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal fraisCollectes = BigDecimal.ZERO;

    @Column(name = "demandes_credit_recueillies", nullable = false)
    @Builder.Default
    private Integer demandesCreditRecueillies = 0;

    @Column(name = "demandes_credit_ids", columnDefinition = "JSON")
    private String demandesCreditIds; // JSON: "[501, 502, 503]"

    // ========== TRÉSORERIE ==========

    @Column(name = "especes_remises", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal especesRemises = BigDecimal.ZERO;

    @Column(name = "especes_emises", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal especesEmises = BigDecimal.ZERO;

    @Column(name = "excedent", precision = 15, scale = 2)
    private BigDecimal excedent; // Calculé: max(0, especesRemises - especesEmises)

    @Column(name = "manquant", precision = 15, scale = 2)
    private BigDecimal manquant; // Calculé: max(0, especesEmises - especesRemises)

    // ========== METADATA ==========

    @Column(name = "observations", columnDefinition = "LONGTEXT")
    private String observations;

    @Column(name = "piece_jointe_path", length = 500)
    private String pieceJointePath;

    // ========== WORKFLOW VALIDATION ==========

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    @Builder.Default
    private RecetteStatut statut = RecetteStatut.BROUILLON;

    @Column(name = "date_validation")
    private LocalDateTime dateValidation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valide_par")
    private Utilisateur validePar; // Responsable site ou contrôleur qui valide

    @Column(name = "motif_rejet", columnDefinition = "LONGTEXT")
    private String motifRejet; // Si statut = REJETEE

    // ========== AUDIT ==========

    @Column(name = "modifie_par")
    private Long modifiePar;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @Column(name = "actif", nullable = false)
    @Builder.Default
    private Boolean actif = true;

    // ========== METHODS ==========

    /**
     * Calcule les écarts (excédent ou manquant)
     * Appelé automatiquement avant persistance
     */
    @PrePersist
    @PreUpdate
    public void calculerEcarts() {
        if (especesRemises != null && especesEmises != null) {
            BigDecimal difference = especesRemises.subtract(especesEmises);
            
            if (difference.compareTo(BigDecimal.ZERO) > 0) {
                this.excedent = difference;
                this.manquant = null;
            } else if (difference.compareTo(BigDecimal.ZERO) < 0) {
                this.manquant = difference.abs();
                this.excedent = null;
            } else {
                this.excedent = null;
                this.manquant = null;
            }
        }
    }

    /**
     * Retourne le total collecté (épargne + remboursements + frais)
     */
    public BigDecimal getTotalCollecte() {
        return (epargneCollectee != null ? epargneCollectee : BigDecimal.ZERO)
            .add(remboursementsCreditCollectes != null ? remboursementsCreditCollectes : BigDecimal.ZERO)
            .add(fraisCollectes != null ? fraisCollectes : BigDecimal.ZERO);
    }

    /**
     * Indique si la recette peut être modifiée (seulement en BROUILLON)
     */
    public boolean isModifiable() {
        return this.statut == RecetteStatut.BROUILLON;
    }

    /**
     * Indique si la recette peut être soumise (doit être en BROUILLON + complet)
     */
    public boolean isSubmittable() {
        return this.statut == RecetteStatut.BROUILLON && isComplete();
    }

    /**
     * Vérifie que tous les champs obligatoires sont remplis
     */
    public boolean isComplete() {
        return this.agentTerrain != null
            && this.site != null
            && this.dateRecette != null
            && this.membresVisites != null && this.membresVisites >= 0
            && this.epargneCollectee != null && this.epargneCollectee.compareTo(BigDecimal.ZERO) >= 0
            && this.remboursementsCreditCollectes != null && this.remboursementsCreditCollectes.compareTo(BigDecimal.ZERO) >= 0
            && this.fraisCollectes != null && this.fraisCollectes.compareTo(BigDecimal.ZERO) >= 0
            && this.especesRemises != null && this.especesRemises.compareTo(BigDecimal.ZERO) >= 0
            && this.especesEmises != null && this.especesEmises.compareTo(BigDecimal.ZERO) >= 0;
    }
}
