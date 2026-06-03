package com.mini.credit.entity.caisse;

import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.StatutReconciliation;
import jakarta.persistence.*;
import jakarta.persistence.Version;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PHASE 11: Réconciliation Caisse Automatique
 *
 * Représente une réconciliation automatique d'une SessionCaisse.
 *
 * Workflow:
 * 1. Batch quotidien détecte SessionCaisse.soldePhysique != soldeTheorique
 * 2. Crée ReconciliationCaisse avec statut CREEE
 * 3. Peut créer automatiquement EcartCaisse associé
 * 4. Agent peut marquer RAPPROCHEE (réconciliée) ou REJETEE
 */
@Entity
@Table(name = "reconciliation_caisse", indexes = {
        @Index(name = "idx_session_caisse", columnList = "session_caisse_id"),
        @Index(name = "idx_statut_reconciliation", columnList = "statut"),
        @Index(name = "idx_date_creation_reconciliation", columnList = "dateCreationReconciliation")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReconciliationCaisse extends BaseEntity {

    @Version
    private Long version;

    /**
     * Session caisse à réconcilier
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_caisse_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reconciliation_session"))
    private SessionCaisse sessionCaisse;

    /**
     * Écart caisse associé (créé automatiquement si nécessaire)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ecart_caisse_id", foreignKey = @ForeignKey(name = "fk_reconciliation_ecart"))
    private EcartCaisse ecartCaisse;

    /**
     * Montant attendu (soldeTheorique de la session)
     */
    @Column(name = "montant_attendu", nullable = false, precision = 18, scale = 2)
    private BigDecimal montantAttendu;

    /**
     * Montant observé (soldePhysique de la session)
     */
    @Column(name = "montant_observe", precision = 18, scale = 2)
    private BigDecimal montantObserve;

    /**
     * Montant de l'écart (observé - attendu, peut être négatif)
     */
    @Column(name = "montant_ecart", nullable = false, precision = 18, scale = 2)
    private BigDecimal montantEcart;

    /**
     * Statut de la réconciliation (CREEE, RAPPROCHEE, REJETEE)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutReconciliation statut = StatutReconciliation.CREEE;

    /**
     * Date de création automatique de la réconciliation (batch)
     */
    @Column(name = "dateCreationReconciliation", nullable = false)
    private LocalDateTime dateCreationReconciliation;

    /**
     * Date de rapprochement (quand marquée RAPPROCHEE)
     */
    @Column(name = "dateRapprochement")
    private LocalDateTime dateRapprochement;

    /**
     * Agent qui a rapproché
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rapproche_par_id", foreignKey = @ForeignKey(name = "fk_reconciliation_rapproche_par"))
    private Utilisateur rapprochePar;

    /**
     * Motif du rapprochement (ajustement, correction, etc.)
     */
    @Column(columnDefinition = "TEXT")
    private String motifRapprochement;

    /**
     * Raison du rejet si applicable
     */
    @Column(columnDefinition = "TEXT")
    private String raisonRejet;

    /**
     * Observation générale
     */
    @Column(columnDefinition = "TEXT")
    private String observation;

    // ============ Helpers ============

    @Transient
    public boolean canBeRapprochee() {
        return this.statut == StatutReconciliation.CREEE;
    }

    @Transient
    public boolean canBeRejectee() {
        return this.statut == StatutReconciliation.CREEE;
    }

    @Transient
    public boolean isDeficit() {
        return this.montantEcart.compareTo(BigDecimal.ZERO) < 0;
    }

    @Transient
    public boolean isSurplus() {
        return this.montantEcart.compareTo(BigDecimal.ZERO) > 0;
    }
}
