package com.mini.credit.dto.referentiel;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSiteRequest {

    private String nomSite;
    private Boolean actif;
    private Long agenceId;  // Optionnel — pour changer d'agence

    /** Zone opérationnelle du site — optionnel en mise à jour. */
    private String zone;

    // NO codeSite (immutable), NO reference/observation
    // Localisation vient de l'Agence
}
