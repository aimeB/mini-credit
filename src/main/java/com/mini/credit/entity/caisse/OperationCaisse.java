package com.mini.credit.entity.caisse;

import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.entity.credit.Credit;
import com.mini.credit.entity.credit.PaiementCredit;
import com.mini.credit.entity.credit.RemboursementCredit;
import com.mini.credit.entity.epargne.OperationEpargne;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.AgentTerrain;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.CategorieOperationCaisse;
import com.mini.credit.enums.ModePaiement;
import com.mini.credit.enums.TypeOperationCaisse;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "operation_caisse")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperationCaisse extends BaseEntity {

    @Column(name = "numero_piece", nullable = false, unique = true, length = 50)
    private String numeroPiece;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_caisse_id", nullable = false)
    private SessionCaisse sessionCaisse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "caisse_id", nullable = false)
    private Caisse caisse;

    @Column(name = "date_operation", nullable = false)
    private LocalDateTime dateOperation;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_operation", nullable = false, length = 10)
    private TypeOperationCaisse typeOperation;

    @Enumerated(EnumType.STRING)
    @Column(name = "categorie_operation", nullable = false, length = 40)
    private CategorieOperationCaisse categorieOperation;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal montant;

    @Column(nullable = false, length = 10)
    private String devise = "CDF";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membre_id")
    private Membre membre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_id")
    private Credit credit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "remboursement_id")
    private RemboursementCredit remboursement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operation_epargne_id")
    private OperationEpargne operationEpargne;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private AgentTerrain agent;

    @Column(columnDefinition = "TEXT")
    private String description;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Utilisateur createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paiement_credit_id")
    private PaiementCredit paiementCredit;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_paiement", length = 30)
    private ModePaiement modePaiement;
}