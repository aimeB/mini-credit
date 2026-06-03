package com.mini.credit.entity.credit;

import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.StatutPenalite;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * PHASE 10: Pénalité de retard pour crédit
 *
 * Montant = PENALITE_RETARD_JOURNALIERE × nombre_jours_retard
 * Exemple: 2500 FC/jour × 5 jours = 12500 FC
 *
 * Workflow:
 * - Créée automatiquement si crédit.dateEchéance < today
 * - Batch job quotidien (00:01) calcule pénalités en attente
 * - Statut: CREEE → ACQUITTEE (payée) ou EFFACEE (pardon)
 */
@Entity
@Table(name = "penalite_credit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PenaliteCredit extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "credit_id", nullable = false)
    private Credit credit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "demande_credit_id", nullable = false)
    private DemandeCredit demandeCredit;

    @Column(name = "date_echéance", nullable = false)
    private LocalDate dateEchéance;

    @Column(name = "nombre_jours_retard", nullable = false)
    private Long nombreJoursRetard = 0L;

    @Column(name = "montant_penalite", nullable = false, precision = 18, scale = 2)
    private BigDecimal montantPenalite;

    @Column(name = "taux_applique", nullable = false, precision = 5, scale = 2)
    private BigDecimal tauxApplique;  // PENALITE_RETARD_JOURNALIERE (PHASE 1)

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutPenalite statut = StatutPenalite.CREEE;

    @Column(name = "date_creation_penalite", nullable = false)
    private LocalDateTime dateCreationPenalite;

    @Column(name = "date_acquittement")
    private LocalDateTime dateAcquittement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acquittee_par")
    private Utilisateur acquitteePar;

    @Column(name = "date_effacement")
    private LocalDateTime dateEffacement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "effacee_par")
    private Utilisateur effaceePar;

    @Column(name = "motif_effacement", length = 500)
    private String motifEffacement;

    @Column(name = "observation", length = 500)
    private String observation;

    /**
     * Helper: Vérifie si la pénalité peut être acquittée
     */
    @Transient
    public boolean canBeAcquitted() {
        return statut == StatutPenalite.CREEE;
    }

    /**
     * Helper: Vérifie si la pénalité peut être effacée
     */
    @Transient
    public boolean canBeErased() {
        return statut == StatutPenalite.CREEE;
    }

    /**
     * Helper: Vérifie si c'est une pénalité active (non résolue)
     */
    @Transient
    public boolean isActive() {
        return statut == StatutPenalite.CREEE;
    }
}
