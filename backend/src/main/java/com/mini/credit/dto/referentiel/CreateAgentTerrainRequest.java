package com.mini.credit.dto.referentiel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAgentTerrainRequest {
    
    @NotNull(message = "Utilisateur requis")
    private Long utilisateurId;
    
    @NotBlank(message = "Matricule requis")
    @Size(min = 2, max = 50, message = "Matricule: 2-50 caractères")
    private String matricule;
    
    @NotNull(message = "Site principal requis")
    private Long siteId;

    /**
     * Employe avec fonction = GESTIONNAIRE responsable de cet agent terrain.
     * Obligatoire à la création.
     */
    @NotNull(message = "Gestionnaire requis")
    private Long gestionnaireId;
    
    private LocalDate dateAffectation;
    
    // Phase 4: Optional additional sites
    private Set<Long> siteIds;
}
