package com.mini.credit.mapper;

import com.mini.credit.dto.referentiel.SiteDTO;
import com.mini.credit.dto.referentiel.CreateSiteRequest;
import com.mini.credit.dto.referentiel.UpdateSiteRequest;
import com.mini.credit.entity.referentiel.Site;
import org.springframework.stereotype.Component;

/**
 * Mapper pour Site entity.
 * Champs zone remplace ville/commune/adresse/reference cote metier.
 * La localisation administrative vient desormais de l Agence.
 */
@Component
public class SiteMapper {

    public SiteDTO toDTO(Site entity) {
        if (entity == null) return null;

        SiteDTO dto = new SiteDTO();
        dto.setId(entity.getId());
        dto.setCodeSite(entity.getCodeSite());
        dto.setNomSite(entity.getNomSite());
        dto.setZone(entity.getZone());
        dto.setActif(entity.getActif());

        if (entity.getAgence() != null) {
            dto.setAgenceId(entity.getAgence().getId());
            dto.setNomAgence(entity.getAgence().getNomAgence());
            dto.setVilleAgence(entity.getAgence().getVille());
            dto.setCommuneAgence(entity.getAgence().getCommune());
        }

        dto.setDateCreation(entity.getDateCreation());
        dto.setDateModification(entity.getDateModification());

        return dto;
    }

    public Site toEntity(CreateSiteRequest request) {
        if (request == null) return null;

        return Site.builder()
            .codeSite(request.getCodeSite().toUpperCase())
            .nomSite(request.getNomSite())
            .zone(request.getZone())
            .actif(true)
            .build();
    }

    public void updateEntityFromDTO(UpdateSiteRequest request, Site entity) {
        if (request == null || entity == null) return;

        if (request.getNomSite() != null) entity.setNomSite(request.getNomSite());
        if (request.getZone() != null)    entity.setZone(request.getZone());
        if (request.getActif() != null)   entity.setActif(request.getActif());
    }
}