package com.mini.credit.dto.referentiel;

import com.mini.credit.entity.referentiel.Site;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SiteResponse {
    private Long id;
    private String codeSite;
    private String nomSite;

    /** Zone opérationnelle du site. */
    private String zone;

    private Boolean actif;
    private Long agenceId;
    private String nomAgence;       // Dénormalisation affichage
    private String villeAgence;     // Localisation administrative de l'agence
    private String communeAgence;   // Localisation administrative de l'agence

    public static SiteResponse fromEntity(Site site) {
        return SiteResponse.builder()
                .id(site.getId())
                .codeSite(site.getCodeSite())
                .nomSite(site.getNomSite())
                .zone(site.getZone())
                .actif(site.getActif())
                .agenceId(site.getAgence() != null ? site.getAgence().getId() : null)
                .nomAgence(site.getAgence() != null ? site.getAgence().getNomAgence() : null)
                .villeAgence(site.getAgence() != null ? site.getAgence().getVille() : null)
                .communeAgence(site.getAgence() != null ? site.getAgence().getCommune() : null)
                .build();
    }
}