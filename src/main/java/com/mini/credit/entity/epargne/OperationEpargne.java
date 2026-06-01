package com.mini.credit.entity.epargne;

import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.AgentTerrain;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.ModePaiement;
import com.mini.credit.enums.SensOperation;
import com.mini.credit.enums.TypeOperationEpargne;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "operation_epargne")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperationEpargne extends BaseEntity {



    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compte_epargne_id", nullable = false)
    private CompteEpargne compteEpargne;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membre_id", nullable = false)
    private Membre membre;

    @Column(name = "date_operation", nullable = false)
    private LocalDateTime dateOperation;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_operation", nullable = false, length = 30)
    private TypeOperationEpargne typeOperation;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal montant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SensOperation sens;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_paiement", length = 20)
    private ModePaiement modePaiement;

    @Column(name = "reference_externe", length = 100)
    private String referenceExterne;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private AgentTerrain agent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_caisse_id")
    private SessionCaisse sessionCaisse;

    private String observation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Utilisateur createdBy;
}
