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
    private String ville;
    private String commune;
    private String adresse;
    private Boolean actif;

    public static SiteResponse fromEntity(Site site) {
        return SiteResponse.builder()
                .id(site.getId())
                .codeSite(site.getCodeSite())
                .nomSite(site.getNomSite())
                .ville(site.getVille())
                .commune(site.getCommune())
                .adresse(site.getAdresse())
                .actif(site.getActif())
                .build();
    }
}