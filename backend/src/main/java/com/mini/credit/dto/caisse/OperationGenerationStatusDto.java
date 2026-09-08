package com.mini.credit.dto.caisse;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * PHASE 6B.2: DTO pour le statut de génération des opérations d'une recette.
 * 
 * Utilisé pour afficher au frontend le statut de génération automatique des opérations
 * (OperationEpargne, OperationCaisse) suite à la validation d'une RecetteJournaliereTerrain.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperationGenerationStatusDto implements Serializable {

    /**
     * Statut de génération: NON_GENEREE, GENEREE, PARTIELLE, ERREUR
     */
    @JsonProperty("generationStatus")
    private String generationStatus;

    /**
     * Nombre d'opérations épargne générées
     */
    @JsonProperty("operationEpargneCount")
    private Integer operationEpargneCount;

    /**
     * Nombre d'opérations caisse générées
     */
    @JsonProperty("operationCaisseCount")
    private Integer operationCaisseCount;

    /**
     * Date/heure de génération si disponible
     */
    @JsonProperty("generatedAt")
    private LocalDateTime generatedAt;

    /**
     * ID de l'utilisateur qui a généré ou validé
     */
    @JsonProperty("generatedByUserId")
    private Long generatedByUserId;

    /**
     * Nom de l'utilisateur qui a généré ou validé
     */
    @JsonProperty("generatedByName")
    private String generatedByName;

    /**
     * Message d'erreur si génération échouée
     */
    @JsonProperty("errorMessage")
    private String errorMessage;

    /**
     * Message d'information général
     */
    @JsonProperty("message")
    private String message;
}
