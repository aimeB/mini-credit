package com.mini.credit.dto.referentiel;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiteDTO {
    private Long id;
    private String codeSite;
    private String nomSite;

    /** Zone opérationnelle du site. */
    private String zone;

    private Boolean actif;
    private Long agenceId;
    private String nomAgence;       // Dénormalisation affichage
    private String villeAgence;     // Localisation admin de l'agence (lecture seule)
    private String communeAgence;   // Localisation admin de l'agence (lecture seule)

    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
}
