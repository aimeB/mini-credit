package com.mini.credit.dto.caisse;

import com.mini.credit.enums.CategorieOperationCaisse;
import com.mini.credit.enums.ModePaiement;
import com.mini.credit.enums.NatureFinancementApprovisionnement;
import com.mini.credit.enums.SourceOperationCaisse;
import com.mini.credit.enums.TypeEvenementAuditOperation;
import com.mini.credit.enums.TypeOperationCaisse;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class OperationCaisseRequest {

    @NotNull
    private Long sessionCaisseId;

    @NotNull
    private Long caisseId;

    @NotNull
    private LocalDateTime dateOperation;

    @NotNull
    private TypeOperationCaisse typeOperation;

    @NotNull
    private CategorieOperationCaisse categorieOperation;

    private NatureFinancementApprovisionnement natureFinancement;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal montant;

    private String devise = "CDF";
    private Long membreId;
    private Long creditId;
    private Long retraitEpargneId;
    private Long remboursementId;
    private Long operationEpargneId;
    private Long depenseCaisseId;
    private Long agentId;
    private String description;
    private Long createdBy;
    private Long utilisateurId;
    private ModePaiement modePaiement;
    private String observation;
    private String commentaire;
    private String referenceMetier;

    /**
     * PATCH 6: Reference externe lisible selon la source.
     * - CREDIT_DECAISSEMENT: numeroCredit
     * - CREDIT_REMBOURSEMENT: numeroRecu
     * - RETRAIT_EPARGNE: "RETRAIT-{demandeId}"
     * - MANUEL/AUTRE: reference saisie ou commentaire
     * Nullable pour les sources sans reference specifique.
     */
    private String referenceExterne;

    /**
     * PATCH 4 / PATCH 6: Source de l'operation — obligatoire pour la tracabilite.
     * Determine quels champs de reference (recetteId, creditId, etc.) sont requis.
     */
    @NotNull
    private SourceOperationCaisse source;

    /**
     * PHASE 6B.2: Reference to RecetteJournaliereTerrain that triggered this operation.
     * Obligatoire uniquement si source == RECETTE_JOURNALIERE.
     */
    private Long recetteId;

    /**
     * Type d'événement d'audit attendu pour l'opération.
     * Backward compatible: si absent, le service applique un défaut sécurisé.
     */
    private TypeEvenementAuditOperation typeEvenementAudit;
}