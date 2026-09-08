package com.mini.credit.dto.referentiel;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO pour créer une recette terrain journalière
 * Statut initial: BROUILLON
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRecetteTerrainRequest {

    private Long agentTerrainId;

    private Long siteId;

    @NotNull(message = "Date recette requise")
    @PastOrPresent(message = "Date ne peut pas être future")
    private LocalDate dateRecette;

    // ===== Opérations terrain =====

    @NotNull(message = "Nombre membres visités requis")
    @Min(value = 0, message = "Membres visités >= 0")
    private Integer membresVisites;

    @NotNull(message = "Nombre nouveaux membres requis")
    @Min(value = 0, message = "Nouveaux membres >= 0")
    private Integer nouveauxMembres;

    @NotNull(message = "Nombre carnets distribués requis")
    @Min(value = 0, message = "Carnets distribués >= 0")
    private Integer carnetDistribues;

    // ===== Collecte épargne =====

    @NotNull(message = "Montant épargne collectée requis")
    @DecimalMin(value = "0.0", inclusive = true, message = "Épargne collectée >= 0")
    private BigDecimal epargneCollectee;

    @Size(max = 255, message = "Source type max 255 chars")
    private String epargneSourceType;

    // ===== Collecte crédit =====

    @NotNull(message = "Montant remboursements crédit requis")
    @DecimalMin(value = "0.0", inclusive = true, message = "Remboursements >= 0")
    private BigDecimal remboursementsCreditCollectes;

    @Size(max = 500, message = "Credit IDs max 500 chars (JSON)")
    private String creditIdsTraites; // JSON: "[1001, 1002]" ou null

    // ===== Frais et demandes =====

    @NotNull(message = "Montant frais collectés requis")
    @DecimalMin(value = "0.0", inclusive = true, message = "Frais collectés >= 0")
    private BigDecimal fraisCollectes;

    @NotNull(message = "Nombre demandes crédit requis")
    @Min(value = 0, message = "Demandes crédit >= 0")
    private Integer demandesCreditRecueillies;

    @Size(max = 500, message = "Demande IDs max 500 chars (JSON)")
    private String demandesCreditIds; // JSON: "[501, 502]" ou null

    // ===== Trésorerie =====

    @NotNull(message = "Montant espèces remises requis")
    @DecimalMin(value = "0.0", inclusive = true, message = "Espèces remises >= 0")
    private BigDecimal especesRemises;

    @DecimalMin(value = "0.0", inclusive = true, message = "Espèces émises >= 0")
    private BigDecimal especesEmises;

    // ===== Metadata =====

    @Size(max = 1000, message = "Observations max 1000 chars")
    private String observations;

    @Size(max = 255, message = "Chemin pièce jointe max 255 chars")
    private String pieceJointePath;
}
