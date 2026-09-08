package com.mini.credit.dto.referentiel;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO pour mettre à jour une recette terrain journalière
 * Possible seulement si statut = BROUILLON
 * Agent terrain ne peut pas modifier après soumis
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRecetteTerrainRequest {

    @NotNull(message = "Nombre membres visités requis")
    @Min(value = 0, message = "Membres visités >= 0")
    private Integer membresVisites;

    @NotNull(message = "Nombre nouveaux membres requis")
    @Min(value = 0, message = "Nouveaux membres >= 0")
    private Integer nouveauxMembres;

    @NotNull(message = "Nombre carnets distribués requis")
    @Min(value = 0, message = "Carnets distribués >= 0")
    private Integer carnetDistribues;

    @NotNull(message = "Montant épargne collectée requis")
    @DecimalMin(value = "0.0", inclusive = true, message = "Épargne collectée >= 0")
    private BigDecimal epargneCollectee;

    @Size(max = 255, message = "Source type max 255 chars")
    private String epargneSourceType;

    @NotNull(message = "Montant remboursements crédit requis")
    @DecimalMin(value = "0.0", inclusive = true, message = "Remboursements >= 0")
    private BigDecimal remboursementsCreditCollectes;

    @Size(max = 500, message = "Credit IDs max 500 chars")
    private String creditIdsTraites;

    @NotNull(message = "Montant frais collectés requis")
    @DecimalMin(value = "0.0", inclusive = true, message = "Frais collectés >= 0")
    private BigDecimal fraisCollectes;

    @NotNull(message = "Nombre demandes crédit requis")
    @Min(value = 0, message = "Demandes crédit >= 0")
    private Integer demandesCreditRecueillies;

    @Size(max = 500, message = "Demande IDs max 500 chars")
    private String demandesCreditIds;

    @NotNull(message = "Montant espèces remises requis")
    @DecimalMin(value = "0.0", inclusive = true, message = "Espèces remises >= 0")
    private BigDecimal especesRemises;

    @DecimalMin(value = "0.0", inclusive = true, message = "Espèces émises >= 0")
    private BigDecimal especesEmises;

    @Size(max = 1000, message = "Observations max 1000 chars")
    private String observations;

    @Size(max = 255, message = "Chemin pièce jointe max 255 chars")
    private String pieceJointePath;
}
