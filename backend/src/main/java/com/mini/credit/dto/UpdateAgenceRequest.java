package com.mini.credit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour modifier une agence
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateAgenceRequest {

    @NotBlank(message = "Nom agence est obligatoire")
    @Size(min = 3, max = 100, message = "Nom agence doit être entre 3 et 100 caractères")
    private String nomAgence;

    @Size(max = 255, message = "Adresse ne peut pas dépasser 255 caractères")
    private String adresse;

    @Size(max = 100, message = "Commune ne peut pas dépasser 100 caractères")
    private String commune;

    @Size(max = 100, message = "Quartier ne peut pas dépasser 100 caractères")
    private String quartier;

    @Size(max = 255, message = "Référence ne peut pas dépasser 255 caractères")
    private String reference;

    @Size(max = 20, message = "Téléphone ne peut pas dépasser 20 caractères")
    private String telephone;

    @Size(max = 100, message = "Ville ne peut pas dépasser 100 caractères")
    private String ville;

    @NotNull(message = "Statut actif est obligatoire")
    private Boolean actif;

    @Size(max = 1000, message = "Description ne peut pas dépasser 1000 caractères")
    private String description;

    private Long chefBureauId;          // 🆕 Chef de Bureau (optionnel, avec validation)

    // Note: code_agence n'est pas modifiable (clé unique)
}
