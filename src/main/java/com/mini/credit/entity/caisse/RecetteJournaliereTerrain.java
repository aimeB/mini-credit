package com.mini.credit.entity.caisse;

import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.StatutRecetteJournaliere;
import com.mini.credit.enums.TypeRecette;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * PHASE 6: Recette journalière terrain - Encodage contrôleur.
 *
 * Représente une recette collectée par agent terrain (papier → système).
 * Le contrôleur encode et valide que:
 *   argent remis (cash) = somme des recettes
 */
@Entity
@Table(name = "recette_journaliere_terrain", indexes = {
        @Index(name = "idx_date_jour", columnList = "dateJour"),
        @Index(name = "idx_statut", columnList = "statut"),
        @Index(name = "idx_membre_id", columnList = "membre_id"),
        @Index(name = "idx_agent_id", columnList = "agent_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecetteJournaliereTerrain extends BaseEntity {

    /**
     * Agent terrain qui a collecté la recette
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false, foreignKey = @ForeignKey(name = "fk_recette_agent"))
    private Utilisateur agent;

    /**
     * Membre concerné par la recette
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membre_id", nullable = false, foreignKey = @ForeignKey(name = "fk_recette_membre"))
    private Membre membre;

    /**
     * Date de la recette (jour collecte)
     */
    @Column(name = "dateJour", nullable = false)
    private LocalDate dateJour;

    /**
     * Type de recette (DEPOT, REMBOURSEMENT_CREDIT, etc)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeRecette typeRecette;

    /**
     * Montant de la recette
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal montant;

    /**
     * Statut de la recette
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutRecetteJournaliere statut = StatutRecetteJournaliere.CREEE;

    /**
     * Observations agent terrain
     */
    @Column(length = 500)
    private String observation;

    /**
     * Numéro de référence papier (si applicable)
     */
    @Column(length = 50)
    private String referencePapier;

    /**
     * VALIDATION PHASE 6: Contrôleur qui valide
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valide_par_id", foreignKey = @ForeignKey(name = "fk_recette_valide_par"))
    private Utilisateur validePar;

    /**
     * Date validation CONTROLEUR
     */
    @Column(name = "dateValidation")
    private LocalDateTime dateValidation;

    /**
     * Motif rejet (si rejetée)
     */
    @Column(columnDefinition = "TEXT")
    private String motifRejet;

    /**
     * Cash remis pour cette recette (encodé contrôleur)
     */
    @Column(precision = 19, scale = 2)
    private BigDecimal cashRemis;

    /**
     * Variance: abs(cashRemis - montant)
     * Calculée à la validation
     */
    @Column(precision = 19, scale = 2)
    private BigDecimal variance;

    // ============ Helpers ============

    @Transient
    public boolean canBeValidated() {
        return this.statut == StatutRecetteJournaliere.CREEE ||
               this.statut == StatutRecetteJournaliere.EN_ATTENTE_VALIDATION;
    }

    @Transient
    public boolean hasVariance() {
        return variance != null && variance.compareTo(java.math.BigDecimal.ZERO) > 0;
    }
}
