package com.mini.credit.entity.rapport;

import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.PeriodiciteRapport;
import com.mini.credit.enums.StatutRapport;
import com.mini.credit.enums.TypeRapport;
import jakarta.persistence.*;
import jakarta.persistence.Version;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * PHASE 12: Rapports Financiers Consolidés
 *
 * Stocke les rapports financiers générés: bilans, comptes de résultat, KPIs.
 *
 * Workflow:
 * 1. Batch quotidien génère rapport consolidé du jour
 * 2. Admin peut générer rapport manuel pour période spécifique
 * 3. Données agrégées: crédits, épargnes, pénalités, commissions, intérêts
 */
@Entity
@Table(name = "rapport_financier", indexes = {
        @Index(name = "idx_type_rapport", columnList = "typeRapport"),
        @Index(name = "idx_statut_rapport", columnList = "statut"),
        @Index(name = "idx_date_rapport", columnList = "dateDebut, dateFin"),
        @Index(name = "idx_date_generation", columnList = "dateGeneration")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RapportFinancier extends BaseEntity {

    @Version
    private Long version;

    /**
     * Type de rapport (BILAN, COMPTE_RESULTAT, KPI, etc.)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TypeRapport typeRapport;

    /**
     * Périodicité (QUOTIDIEN, MENSUEL, ANNUEL, PERSONNALISE)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PeriodiciteRapport periodicite;

    /**
     * Date de début de la période
     */
    @Column(name = "dateDebut", nullable = false)
    private LocalDate dateDebut;

    /**
     * Date de fin de la période
     */
    @Column(name = "dateFin", nullable = false)
    private LocalDate dateFin;

    /**
     * Date de génération du rapport
     */
    @Column(name = "dateGeneration", nullable = false)
    private LocalDateTime dateGeneration;

    /**
     * Statut (GENERE, EN_GENERATION, VALIDE, ARCHIVE, ERREUR)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutRapport statut = StatutRapport.GENERE;

    /**
     * Utilisateur qui a généré le rapport
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "genere_par_id", nullable = false)
    private Utilisateur generePar;

    /**
     * Utilisateur qui a validé (si validé)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valide_par_id")
    private Utilisateur validePar;

    /**
     * Date de validation si applicable
     */
    @Column(name = "dateValidation")
    private LocalDateTime dateValidation;

    // ============ DONNÉES BILAN ============

    /**
     * BILAN: Total actif (Caisse + Crédits + Épargnes)
     */
    @Column(name = "totalActif", precision = 18, scale = 2)
    private BigDecimal totalActif;

    /**
     * BILAN: Total passif (Dépôts épargnes + Dettes)
     */
    @Column(name = "totalPassif", precision = 18, scale = 2)
    private BigDecimal totalPassif;

    /**
     * BILAN: Capitaux propres (Actif - Passif)
     */
    @Column(name = "capitauxPropres", precision = 18, scale = 2)
    private BigDecimal capitauxPropres;

    // ============ DONNÉES FLUX ============

    /**
     * Solde caisse début
     */
    @Column(name = "soldeCaisseeDebut", precision = 18, scale = 2)
    private BigDecimal soldeCaisseDebut;

    /**
     * Solde caisse fin
     */
    @Column(name = "soldeCaisseFin", precision = 18, scale = 2)
    private BigDecimal soldeCaisseFin;

    /**
     * Total crédits décaissés (nouveaux crédits émis)
     */
    @Column(name = "totalCreditsDecaisses", precision = 18, scale = 2)
    private BigDecimal totalCreditsDecaisses;

    /**
     * Total remboursements reçus
     */
    @Column(name = "totalRembourses", precision = 18, scale = 2)
    private BigDecimal totalRembourses;

    /**
     * Total épargnes déposées
     */
    @Column(name = "totalEpargnesDeposes", precision = 18, scale = 2)
    private BigDecimal totalEpargnesDeposes;

    /**
     * Total retraits épargnes
     */
    @Column(name = "totalRetraitsEpargnes", precision = 18, scale = 2)
    private BigDecimal totalRetraitsEpargnes;

    // ============ DONNÉES COMPTE DE RÉSULTAT ============

    /**
     * Revenus: Intérêts crédits + Commissions
     */
    @Column(name = "totalRevenus", precision = 18, scale = 2)
    private BigDecimal totalRevenus;

    /**
     * Charges: Intérêts épargnes + Pénalités + Frais opérationnels
     */
    @Column(name = "totalCharges", precision = 18, scale = 2)
    private BigDecimal totalCharges;

    /**
     * Résultat: Revenus - Charges (profit/loss)
     */
    @Column(name = "resultat", precision = 18, scale = 2)
    private BigDecimal resultat;

    /**
     * Intérêts générés sur crédits
     */
    @Column(name = "interetsCredits", precision = 18, scale = 2)
    private BigDecimal interetsCredits;

    /**
     * Commissions agents
     */
    @Column(name = "commissionsAgents", precision = 18, scale = 2)
    private BigDecimal commissionsAgents;

    /**
     * Intérêts épargnes (charges)
     */
    @Column(name = "interetsEpargnes", precision = 18, scale = 2)
    private BigDecimal interetsEpargnes;

    /**
     * Pénalités en retard collectées
     */
    @Column(name = "penalitesCollectees", precision = 18, scale = 2)
    private BigDecimal penalitesCollectees;

    /**
     * Pénalités effacées (réduction charges)
     */
    @Column(name = "penalitesEffacees", precision = 18, scale = 2)
    private BigDecimal penalitesEffacees;

    // ============ DONNÉES KPI ============

    /**
     * KPI: Nombre total de crédits actifs
     */
    @Column(name = "nombreCreditsActifs")
    private Long nombreCreditsActifs;

    /**
     * KPI: Nombre de crédits remboursés
     */
    @Column(name = "nombreCreditsRembourses")
    private Long nombreCreditsRembourses;

    /**
     * KPI: Nombre de crédits en retard
     */
    @Column(name = "nombreCreditsEnRetard")
    private Long nombreCreditsEnRetard;

    /**
     * KPI: Taux de remboursement (%) = rembourses / actifs
     */
    @Column(name = "tauxRemboursement", precision = 5, scale = 2)
    private BigDecimal tauxRemboursement;

    /**
     * KPI: Nombre de membres actifs
     */
    @Column(name = "nombreMembresActifs")
    private Long nombreMembresActifs;

    /**
     * KPI: Solde moyen épargnes par membre
     */
    @Column(name = "soldeEpargnesMoyen", precision = 18, scale = 2)
    private BigDecimal soldeEpargnesMoyen;

    /**
     * KPI: Total épargnes en caisse
     */
    @Column(name = "totalEpargnesCaisse", precision = 18, scale = 2)
    private BigDecimal totalEpargnesCaisse;

    /**
     * KPI: ROA (Return on Assets) = Résultat / Total Actif
     */
    @Column(name = "roa", precision = 5, scale = 2)
    private BigDecimal roa;

    /**
     * KPI: ROE (Return on Equity) = Résultat / Capitaux Propres
     */
    @Column(name = "roe", precision = 5, scale = 2)
    private BigDecimal roe;

    /**
     * Observation/Commentaires
     */
    @Column(columnDefinition = "TEXT")
    private String observation;

    /**
     * Message d'erreur si génération échouée
     */
    @Column(columnDefinition = "TEXT")
    private String messageErreur;

    // ============ Helpers ============

    @Transient
    public boolean canBeValidated() {
        return this.statut == StatutRapport.GENERE;
    }

    @Transient
    public boolean canBeArchived() {
        return this.statut == StatutRapport.VALIDE;
    }

    @Transient
    public String getPeriodLabel() {
        return dateDebut.toString() + " à " + dateFin.toString();
    }
}
