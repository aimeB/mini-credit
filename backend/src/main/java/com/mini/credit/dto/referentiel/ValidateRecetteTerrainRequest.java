package com.mini.credit.dto.referentiel;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour valider ou rejeter une recette terrain journalière
 * Réservé aux responsables site, contrôleurs et admins
 * Statut initial: SOUMISE → VALIDEE ou REJETEE
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateRecetteTerrainRequest {

    @NotNull(message = "Décision validation requise")
    @Pattern(regexp = "^(VALIDEE|REJETEE)$", message = "Décision doit être VALIDEE ou REJETEE")
    private String decision;

    @NotNull(message = "Utilisateur qui valide requis")
    private Long validePar;

    @Size(max = 1000, message = "Motif rejet max 1000 chars")
    private String motifRejet; // Obligatoire si decision = REJETEE
}
