package com.mini.credit.entity.caisse;

import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.StatutFicheJournaliere;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Formula;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * PHASE 6B.1: Fiche journalière consolidée agent terrain.
 *
 * Représente la synthèse journalière des recettes collectées par un agent.
 * Consolidation des RecetteJournaliereTerrain individuelles en un seul document.
 *
 * Procédure 3N: Fiche journalière d'agent terrain pour contrôle caisse quotidien.
 *
 * Statuts workflow:
 * - BROUILLON: fiche créée, recettes encodées
 * - SOUMISE: consolidation effectuée (phase 6B.2), en attente validation
 * - VALIDEE: contrôleur approuve, variance OK
 * - REJETEE: contrôleur rejette (variance > seuil)
 * - ANNULEE: annulée (admin)
 */
@Entity
@Table(name = "fiche_journaliere_agent_terrain", indexes = {
        @Index(name = "idx_fiche_utilisateur_id", columnList = "utilisateur_id"),
        @Index(name = "idx_fiche_site_id", columnList = "site_id"),
        @Index(name = "idx_fiche_date", columnList = "date_fiche"),
        @Index(name = "idx_fiche_statut", columnList = "statut"),
        @Index(name = "idx_fiche_valide_par_id", columnList = "valide_par_id"),
        @Index(name = "idx_fiche_agent_date", columnList = "utilisateur_id,date_fiche"),
        @Index(name = "idx_fiche_statut_date", columnList = "statut,date_fiche")
},
uniqueConstraints = {
        @UniqueConstraint(name = "uq_fiche_agent_date", columnNames = {"utilisateur_id", "date_fiche"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FicheJournaliereAgentTerrain extends BaseEntity {

    // ============ RELATIONS ============

    /**
     * Agent terrain qui a collecté les recettes
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "utilisateur_id", nullable = false, foreignKey = @ForeignKey(name = "fk_fiche_agent"))
    private Utilisateur agentTerrain;

    /**
     * Site de l'agent (dénormalisé pour requêtes rapides)
     * Récupéré depuis Utilisateur.site
     */
    @ManyToOne(fetch = FetchType.EAGER, optional = true)
    @JoinColumn(name = "site_id", foreignKey = @ForeignKey(name = "fk_fiche_site"))
    private Site site;

    /**
     * Contrôleur qui a validé la fiche (phase 6B.3)
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "valide_par_id", foreignKey = @ForeignKey(name = "fk_fiche_valide_par"))
    private Utilisateur validePar;

    /**
     * Recettes liées à cette fiche
     * Lien depuis RecetteJournaliereTerrain.ficheJournaliere (phase 6B.2)
     */
    @Builder.Default
    @OneToMany(mappedBy = "ficheJournaliere", fetch = FetchType.LAZY, cascade = CascadeType.REFRESH)
    private Set<RecetteJournaliereTerrain> recettes = new HashSet<>();

    // ============ IDENTITÉ & CONTEXTE ============

    /**
     * Date de la fiche journalière
     */
    @Column(name = "date_fiche", nullable = false)
    private LocalDate dateFiche;

    /**
     * Statut de la fiche
     * Default: BROUILLON (au moment création en phase 6B.1)
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutFicheJournaliere statut = StatutFicheJournaliere.BROUILLON;

    // ============ CONSOLIDATION FINANCIÈRE ============

    /**
     * Total épargne collectée (DEPOT + INTERET)
     * Calculé en phase 6B.2 via SUM
     */
    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal epargneCollecteeTotal = BigDecimal.ZERO;

    /**
     * Total remboursements crédit collectés
     * Calculé en phase 6B.2 via SUM
     */
    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal remboursementCollectes = BigDecimal.ZERO;

    /**
     * Total frais collectés
     * Calculé en phase 6B.2 via SUM
     */
    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal fraisCollectes = BigDecimal.ZERO;

    /**
     * Total autres recettes collectées
     * Calculé en phase 6B.2 via SUM
     */
    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal autresRecettes = BigDecimal.ZERO;

    /**
     * MONTANT TOTAL THÉORIQUE = épargne + remb + frais + autres
     * Read-only, calculé par formule SQL
     */
    @Formula("epargne_collectee_total + remboursement_collectes + frais_collectes + autres_recettes")
    @Column(insertable = false, updatable = false)
    private BigDecimal montantTotalCollecte;

    // ============ DÉNOMBREMENTS ============

    /**
     * Nombre de membres visités (DISTINCT) en cette journée
     * Calculé en phase 6B.2 via COUNT(DISTINCT membre_id)
     */
    @Builder.Default
    @Column(nullable = false)
    private Integer nombreMembresVisites = 0;

    /**
     * Nombre de nouveaux membres (dateAdhesion = dateFiche)
     * Calculé en phase 6B.2 via Membre.dateAdhesion
     */
    @Builder.Default
    @Column(nullable = false)
    private Integer nombreNouveauxMembres = 0;

    /**
     * Nombre de carnets distribués (TBD métier - Phase 6B.1: init à 0)
     * À clarifier: source et timing saisie
     */
    @Builder.Default
    @Column(nullable = false)
    private Integer nombreCarnetsDistribues = 0;

    // ============ CONTRÔLE CAISSE ============

    /**
     * Total espèces remises (physiquement)
     * Saisi par contrôleur au moment validation (phase 6B.3)
     */
    @Column(name = "total_especes_remises", precision = 19, scale = 2)
    private BigDecimal totalEspecesRemises;

    /**
     * Variance absolue: ABS(totalEspecesTheoriques - totalEspecesRemises)
     * Calculé en phase 6B.3 lors validation
     */
    @Builder.Default
    @Column(precision = 19, scale = 2)
    private BigDecimal variance = BigDecimal.ZERO;

    /**
     * Variance en pourcentage: (variance / montantTotal) * 100
     * Calculé en phase 6B.3 lors validation
     */
    @Builder.Default
    @Column(name = "variance_percentage")
    private Double variancePercentage = 0.0;

    /**
     * Excédent: IF totalEspecesRemises > montantTotal THEN différence ELSE 0
     * Calculé en phase 6B.3
     */
    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal excedent = BigDecimal.ZERO;

    /**
     * Manquant: IF totalEspecesRemises < montantTotal THEN différence ELSE 0
     * Calculé en phase 6B.3
     */
    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal manquant = BigDecimal.ZERO;

    // ============ OBSERVATIONS ============

    /**
     * Observations agent terrain
     * Synthèse des notes des recettes individuelles (GROUP_CONCAT en phase 6B.2)
     */
    @Column(name = "observations_agent", columnDefinition = "TEXT")
    private String observationsAgent;

    /**
     * Observations contrôleur (saisie au moment validation)
     * Phase 6B.3
     */
    @Column(name = "observations_controleur", columnDefinition = "TEXT")
    private String observationsControleur;

    // ============ STATUT & VALIDATION ============

    /**
     * Date validation par contrôleur
     * Auto-générée NOW() en phase 6B.3
     */
    @Column(name = "date_validation")
    private LocalDateTime dateValidation;

    /**
     * Motif rejet (si statut = REJETEE)
     * Saisi par contrôleur en phase 6B.3
     */
    @Column(name = "motif_rejet", columnDefinition = "TEXT")
    private String motifRejet;

    /**
        * Raison annulation (si statut = ANNULEE)
     * Optionnel, pour traçabilité admin
     */
    @Column(name = "raison_annulation", columnDefinition = "TEXT")
    private String raisonAnnulation;

    // ============ HELPERS ============

    /**
     * Check if fiche can be edited (only in BROUILLON state)
     */
    @Transient
    public boolean isEditable() {
        return this.statut == StatutFicheJournaliere.BROUILLON;
    }

    /**
     * Check if fiche can be submitted for consolidation
     */
    @Transient
    public boolean canBeSubmitted() {
        return this.statut == StatutFicheJournaliere.BROUILLON
                && this.agentTerrain != null
                && this.dateFiche != null;
    }

    /**
     * Alias for montantTotalCollecte (for convenience)
     */
    @Transient
    public BigDecimal getTotalEspecesTheoriques() {
        return this.montantTotalCollecte;
    }
}
