package com.mini.credit.entity.credit;

import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.entity.epargne.OperationEpargne;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.AgentTerrain;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.ModePaiement;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "remboursement_credit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemboursementCredit extends BaseEntity {



    @Column(name = "numero_recu", nullable = false, unique = true, length = 50)
    private String numeroRecu;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "credit_id", nullable = false)
    private Credit credit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "echeance_id")
    private EcheanceCredit echeance;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membre_id", nullable = false)
    private Membre membre;

    @Column(name = "date_paiement", nullable = false)
    private LocalDateTime datePaiement;

    @Column(name = "montant_principal", nullable = false, precision = 18, scale = 2)
    private BigDecimal montantPrincipal = BigDecimal.ZERO;

    @Column(name = "montant_interet", nullable = false, precision = 18, scale = 2)
    private BigDecimal montantInteret = BigDecimal.ZERO;

    @Column(name = "montant_penalite", nullable = false, precision = 18, scale = 2)
    private BigDecimal montantPenalite = BigDecimal.ZERO;

    @Column(name = "montant_total", nullable = false, precision = 18, scale = 2)
    private BigDecimal montantTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_paiement", length = 20)
    private ModePaiement modePaiement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_caisse_id")
    private SessionCaisse sessionCaisse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operation_epargne_id")
    private OperationEpargne operationEpargne;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private AgentTerrain agent;

    private String observation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Utilisateur createdBy;
}