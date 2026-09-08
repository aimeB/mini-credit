package com.mini.credit.dto.referentiel;

import lombok.*;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateAgentTerrainRequest {
    
    private Long siteId;
    private LocalDate dateAffectation;
    private Boolean actif;
    
    /**
     * Nouveau gestionnaire (optionnel en mise à jour).
     * Si fourni : doit être un Employe actif avec fonction = GESTIONNAIRE.
     */
    private Long gestionnaireId;
    
    // Phase 4: Optional - update additional site assignments
    private Set<Long> siteIds;
}
