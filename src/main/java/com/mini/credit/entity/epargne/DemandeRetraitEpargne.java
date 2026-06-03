package com.mini.credit.entity.epargne;

import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.StatutDemandeRetrait;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entité représentant une demande de retrait épargne (PHASE 5).
 *
 * Workflow:
 * - Créée par MEMBRE/AGENT_TERRAIN
 * - Validée par CONTROLEUR (vérif solde)
 * - Décaissée par CAISSIER (crée OperationEpargne)
 *
 * Attributs:
 * - compteEpargne: compte source du retrait
 * - montantDemande: montant demandé
 * - statut: état du workflow
 * - validePar: utilisateur qui a validé
 * - motifRejet: raison du rejet si applicable
 */
@Entity
@Table(name = "demande_retrait_epargne")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandeRetraitEpargne extends BaseEntity {

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compte_epargne_id", nullable = false)
    private CompteEpargne compteEpargne;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membre_id", nullable = false)
    private Membre membre;

    @Column(name = "montant_demande", nullable = false, precision = 18, scale = 2)
    private BigDecimal montantDemande;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatutDemandeRetrait statut = StatutDemandeRetrait.CREEE;

    @Column(name = "date_demande", nullable = false)
    private LocalDateTime dateDemande;

    @Column(name = "motif_rejet", columnDefinition = "TEXT")
    private String motifRejet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valide_par_id")
    private Utilisateur validePar;

    @Column(name = "date_validation")
    private LocalDateTime dateValidation;

    @Column(name = "observation", columnDefinition = "TEXT")
    private String observation;

    // ============ Méthodes helper ============

    /**
     * Vérifie si le retrait peut être validé (solde suffisant)
     */
    @Transient
    public boolean canBeValidated() {
        if (compteEpargne == null || montantDemande == null) {
            return false;
        }
        return compteEpargne.getSoldeDisponible() != null &&
                compteEpargne.getSoldeDisponible().compareTo(montantDemande) >= 0;
    }

    /**
     * Vérifie si le retrait peut être décaissé
     */
    @Transient
    public boolean canBeDisbursed() {
        return statut == StatutDemandeRetrait.VALIDEE;
    }
}
