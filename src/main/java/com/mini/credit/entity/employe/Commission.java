package com.mini.credit.entity.employe;

import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.StatutCommission;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * PHASE 8: Commission Agent - Calcul et paiement.
 *
 * Représente une commission calculée sur recettes collectées par agent.
 * Basée sur TAUX_COMMISSION_AGENT (2.5% par défaut).
 */
@Entity
@Table(name = "commission", indexes = {
        @Index(name = "idx_agent_id", columnList = "agent_id"),
        @Index(name = "idx_periode", columnList = "datePeriodeDebut, datePeriodeFin"),
        @Index(name = "idx_statut", columnList = "statut"),
        @Index(name = "idx_date_creation", columnList = "dateCreation")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Commission extends BaseEntity {

    /**
     * Agent terrain bénéficiaire
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id", nullable = false, foreignKey = @ForeignKey(name = "fk_commission_agent"))
    private Utilisateur agent;

    /**
     * Période début (jour 1 du calcul)
     */
    @Column(name = "datePeriodeDebut", nullable = false)
    private LocalDate datePeriodeDebut;

    /**
     * Période fin (dernier jour du calcul)
     */
    @Column(name = "datePeriodeFin", nullable = false)
    private LocalDate datePeriodeFin;

    /**
     * Total recettes collectées (base calcul)
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalRecettes;

    /**
     * Taux de commission appliqué (ex: 0.025 pour 2.5%)
     */
    @Column(nullable = false, precision = 10, scale = 6)
    private BigDecimal tauxCommission;

    /**
     * Montant brut commission calculé
     * = totalRecettes * tauxCommission
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal montantCommission;

    /**
     * Nombre de recettes validées (count)
     */
    @Column(nullable = false)
    private Long nbRecettes = 0L;

    /**
     * Statut de la commission
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutCommission statut = StatutCommission.CREEE;

    /**
     * Validée par (gestionnaire/chef)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validee_par_id", foreignKey = @ForeignKey(name = "fk_commission_validee_par"))
    private Utilisateur valideePar;

    /**
     * Date validation
     */
    @Column(name = "dateValidation")
    private LocalDateTime dateValidation;

    /**
     * Payée à (même que agent normalement)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payee_a_id", foreignKey = @ForeignKey(name = "fk_commission_payee_a"))
    private Utilisateur payeeA;

    /**
     * Date de paiement effectif
     */
    @Column(name = "datePaiement")
    private LocalDateTime datePaiement;

    /**
     * Observations (ex: raison annulation)
     */
    @Column(columnDefinition = "TEXT")
    private String observation;

    // ============ Helpers ============

    @Transient
    public boolean canBeValidated() {
        return this.statut == StatutCommission.CREEE;
    }

    @Transient
    public boolean canBePaid() {
        return this.statut == StatutCommission.VALIDEE;
    }
}
