package com.mini.credit.mapper;

import com.mini.credit.dto.referentiel.CreateAgentTerrainRequest;
import com.mini.credit.dto.referentiel.AgentTerrainResponse;
import com.mini.credit.dto.referentiel.UpdateAgentTerrainRequest;
import com.mini.credit.entity.referentiel.AgentTerrain;
import com.mini.credit.entity.referentiel.Site;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mapper pour convertir AgentTerrain entity ↔ DTOs
 * Gère conversion de l'entité vers response DTO et vice versa
 * Inclut gestion des relations N:M avec Sites
 */
@Component
public class AgentTerrainMapper {

    /**
     * Convertit une entité AgentTerrain en DTO (Response)
     * Inclut tous les champs + siteIds (Phase 4)
     * 
     * @param entity AgentTerrain entity à convertir
     * @return AgentTerrainResponse avec tous les champs, ou null si entity null
     */
    public AgentTerrainResponse toDTO(AgentTerrain entity) {
        if (entity == null) {
            return null;
        }

        return AgentTerrainResponse.builder()
            .id(entity.getId())
            .matricule(entity.getMatricule())
            .utilisateurId(entity.getUtilisateur() != null ? entity.getUtilisateur().getId() : null)
            .siteId(entity.getSite() != null ? entity.getSite().getId() : null)
            .sitePrincipalNom(entity.getSite() != null ? entity.getSite().getNomSite() : null)
            .nomCompletUtilisateur(entity.getUtilisateur() != null ? 
                entity.getUtilisateur().getNomComplet() : null)
            .username(entity.getUtilisateur() != null ? 
                entity.getUtilisateur().getUsername() : null)
            .employeNomComplet(entity.getUtilisateur() != null && entity.getUtilisateur().getEmploye() != null
                ? entity.getUtilisateur().getEmploye().getNomComplet() : null)
            .employeId(entity.getUtilisateur() != null && entity.getUtilisateur().getEmploye() != null
                ? entity.getUtilisateur().getEmploye().getId() : null)
            .employeTelephone(entity.getUtilisateur() != null && entity.getUtilisateur().getEmploye() != null
                ? entity.getUtilisateur().getEmploye().getTelephone() : null)
            .agentAgenceId(entity.getUtilisateur() != null && entity.getUtilisateur().getEmploye() != null
                && entity.getUtilisateur().getEmploye().getAgence() != null
                ? entity.getUtilisateur().getEmploye().getAgence().getId() : null)
            .agentAgenceNom(entity.getUtilisateur() != null && entity.getUtilisateur().getEmploye() != null
                && entity.getUtilisateur().getEmploye().getAgence() != null
                ? entity.getUtilisateur().getEmploye().getAgence().getNomAgence() : null)
            .siteId(entity.getSite() != null ? entity.getSite().getId() : null)
            .sitePrincipalNom(entity.getSite() != null ? entity.getSite().getNomSite() : null)
            .siteAgenceId(entity.getSite() != null && entity.getSite().getAgence() != null
                ? entity.getSite().getAgence().getId() : null)
            .siteAgenceNom(entity.getSite() != null && entity.getSite().getAgence() != null
                ? entity.getSite().getAgence().getNomAgence() : null)
            .gestionnaireId(entity.getGestionnaire() != null ? entity.getGestionnaire().getId() : null)
            .gestionnaireNomComplet(entity.getGestionnaire() != null ? entity.getGestionnaire().getNomComplet() : null)
            .gestionnaireTelephone(entity.getGestionnaire() != null ? entity.getGestionnaire().getTelephone() : null)
            .gestionnaireAgenceId(entity.getGestionnaire() != null && entity.getGestionnaire().getAgence() != null
                ? entity.getGestionnaire().getAgence().getId() : null)
            .gestionnaireAgenceNom(entity.getGestionnaire() != null && entity.getGestionnaire().getAgence() != null
                ? entity.getGestionnaire().getAgence().getNomAgence() : null)
            .dateAffectation(entity.getDateAffectation())
            .actif(entity.getActif())
            .dateCreation(entity.getDateCreation())
            .dateModification(entity.getDateModification())
            // Phase 4: Extract siteIds from sitesAffectes relationship
            .siteIds(entity.getSitesAffectes() != null && !entity.getSitesAffectes().isEmpty()
                ? entity.getSitesAffectes().stream()
                    .map(Site::getId)
                    .collect(Collectors.toSet())
                : Set.of())
            .build();
    }

    /**
     * Convertit une CreateAgentTerrainRequest en entité AgentTerrain
     * Exclut ID et timestamps (générés par la DB)
     * Note: utilisateur, site et sitesAffectes sont set par le service (dependencies)
     * 
     * @param request Request DTO avec champs création
     * @return AgentTerrain entity avec champs normalisés, ou null si request null
     */
    public AgentTerrain toEntity(CreateAgentTerrainRequest request) {
        if (request == null) {
            return null;
        }

        return AgentTerrain.builder()
            .matricule(request.getMatricule() != null ? 
                request.getMatricule().toUpperCase() : null)
            .dateAffectation(request.getDateAffectation())
            .actif(true)
            // utilisateur: set by service
            // site: set by service
            // sitesAffectes: set by service
            .build();
    }

    /**
     * Met à jour AgentTerrain entity à partir d'UpdateAgentTerrainRequest
     * Préserve les champs immuables (id, matricule, utilisateur, dateCreation)
     * Gère optionnellement site et sitesAffectes
     * 
     * @param request Request DTO avec champs mise à jour
     * @param entity AgentTerrain entity à mettre à jour
     */
    public void updateEntityFromDTO(UpdateAgentTerrainRequest request, AgentTerrain entity) {
        if (request == null || entity == null) {
            return;
        }

        if (request.getSiteId() != null) {
            // site: service handles dependency and validation
        }
        if (request.getDateAffectation() != null) {
            entity.setDateAffectation(request.getDateAffectation());
        }
        if (request.getActif() != null) {
            entity.setActif(request.getActif());
        }
        
        // sitesAffectes: service handles relationship management
    }
}
