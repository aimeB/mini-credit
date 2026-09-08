package com.mini.credit.entity.caisse;

import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.entity.credit.Credit;
import com.mini.credit.entity.credit.PaiementCredit;
import com.mini.credit.entity.credit.RemboursementCredit;
import com.mini.credit.entity.epargne.OperationEpargne;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.AgentTerrain;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.CategorieOperationCaisse;
import com.mini.credit.enums.ModePaiement;
import com.mini.credit.enums.NatureFinancementApprovisionnement;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.enums.SourceOperationCaisse;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "nature_financement", length = 40)
    private NatureFinancementApprovisionnement natureFinancement;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal montant;

    @Column(name = "solde_apres_operation", precision = 18, scale = 2)
    private BigDecimal soldeApresOperation;

    @Column(nullable = false, length = 10)
    private String devise = "CDF";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membre_id")
    private Membre membre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_id")
    private Credit credit;

    @Column(name = "retrait_epargne_id")
    private Long retraitEpargneId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "remboursement_id")
    private RemboursementCredit remboursement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operation_epargne_id")
    private OperationEpargne operationEpargne;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private AgentTerrain agent;

    /**
     * PHASE 6B.2: Link to RecetteJournaliereTerrain that triggered this operation.
     * Nullable for backward compatibility with existing operations.
     * ⚠️ IMPORTANT: recetteId is MANDATORY ONLY if source = RECETTE_JOURNALIERE
     * For other sources (CREDIT_DECAISSEMENT, RETRAIT_EPARGNE, etc.), use referenceExterne
     * Used for traceability: which receipt generated which operation.
     */
    @Column(name = "recette_id", nullable = true)
    private Long recetteId;

    /**
     * Q3: Source of this operation for traceability and validation.
     * Determines which foreign key (recetteId, creditId, etc.) should be populated.
     * ENUM values: RECETTE_JOURNALIERE, RETRAIT_EPARGNE, CREDIT_DECAISSEMENT, etc.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "source_operation", nullable = false, length = 50)
    private SourceOperationCaisse source;

    /**
     * External reference for sources other than RECETTE_JOURNALIERE.
     * Provides traceability for non-receipt operations:
     * - CREDIT_DECAISSEMENT: reference numéro dossier crédit
     * - RETRAIT_EPARGNE: reference compte épargne
     * - MANUEL: reference manuelle saisie
     * - AJUSTEMENT: reference transaction originale
     * - APPROVISIONNEMENT: reference bon de caisse
     * etc.
     */
    @Column(name = "reference_externe", length = 100, nullable = true)
    private String referenceExterne;

    @Column(name = "reference_metier", length = 120)
    private String referenceMetier;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String observation;

    @Column(columnDefinition = "TEXT")
    private String commentaire;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Utilisateur createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_utilisateur", length = 50)
    private RoleCode roleUtilisateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    private Site site;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paiement_credit_id")
    private PaiementCredit paiementCredit;

    @Column(name = "depense_caisse_id")
    private Long depenseCaisseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_paiement", length = 30)
    private ModePaiement modePaiement;
}