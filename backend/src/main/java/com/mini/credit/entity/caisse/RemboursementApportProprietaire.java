package com.mini.credit.entity.caisse;

import com.mini.credit.entity.agence.Agence;
import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.StatutRemboursementApport;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "remboursement_apport_proprietaire")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemboursementApportProprietaire extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "antenne_id", nullable = false)
    private Agence antenne;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal montant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatutRemboursementApport statut = StatutRemboursementApport.DEMANDE;

    @Column(name = "motif_demande", columnDefinition = "TEXT")
    private String motifDemande;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_par_id")
    private Utilisateur demandePar;

    @Column(name = "date_demande", nullable = false)
    private LocalDateTime dateDemande;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valide_par_id")
    private Utilisateur validePar;

    @Column(name = "date_validation")
    private LocalDateTime dateValidation;

    @Column(name = "motif_validation", columnDefinition = "TEXT")
    private String motifValidation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paye_par_id")
    private Utilisateur payePar;

    @Column(name = "date_paiement")
    private LocalDateTime datePaiement;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operation_caisse_id")
    private OperationCaisse operationCaisse;

    @Column(columnDefinition = "TEXT")
    private String commentaire;
}
