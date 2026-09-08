package com.mini.credit.mapper;

import com.mini.credit.dto.caisse.CreateFicheJournaliereRequest;
import com.mini.credit.dto.caisse.FicheJournaliereResponse;
import com.mini.credit.dto.caisse.UpdateFicheJournaliereRequest;
import com.mini.credit.entity.caisse.FicheJournaliereAgentTerrain;
import com.mini.credit.entity.referentiel.Utilisateur;
import org.springframework.stereotype.Component;

/**
 * PHASE 6B.1: Mapper pour FicheJournaliereAgentTerrain
 * 
 * Maps between entity, request DTOs, and response DTO.
 */
@Component
public class FicheJournaliereMapper {

    /**
     * Convert entity to response DTO (complete projection)
     */
    public FicheJournaliereResponse toResponse(FicheJournaliereAgentTerrain entity) {
        if (entity == null) {
            return null;
        }

        return FicheJournaliereResponse.builder()
                .id(entity.getId())
                .agentTerrainId(entity.getAgentTerrain() != null ? entity.getAgentTerrain().getId() : null)
                .agentTerrainNom(entity.getAgentTerrain() != null ? entity.getAgentTerrain().getNomComplet() : null)
                .siteId(entity.getSite() != null ? entity.getSite().getId() : null)
                .siteName(entity.getSite() != null ? entity.getSite().getNomSite() : null)
                .dateFiche(entity.getDateFiche())
                .statut(entity.getStatut() != null ? entity.getStatut().name() : null)
                
                // Consolidation financière
                .epargneCollecteeTotal(entity.getEpargneCollecteeTotal())
                .remboursementCollectes(entity.getRemboursementCollectes())
                .fraisCollectes(entity.getFraisCollectes())
                .autresRecettes(entity.getAutresRecettes())
                .montantTotalCollecte(entity.getMontantTotalCollecte())
                
                // Dénombrements
                .nombreMembresVisites(entity.getNombreMembresVisites())
                .nombreNouveauxMembres(entity.getNombreNouveauxMembres())
                .nombreCarnetsDistribues(entity.getNombreCarnetsDistribues())
                
                // Contrôle caisse
                .totalEspecesRemises(entity.getTotalEspecesRemises())
                .variance(entity.getVariance())
                .variancePercentage(entity.getVariancePercentage())
                .excedent(entity.getExcedent())
                .manquant(entity.getManquant())
                
                // Observations
                .observationsAgent(entity.getObservationsAgent())
                .observationsControleur(entity.getObservationsControleur())
                
                // Validation
                .valideParId(entity.getValidePar() != null ? entity.getValidePar().getId() : null)
                .valideParNom(entity.getValidePar() != null ? entity.getValidePar().getNomComplet() : null)
                .dateValidation(entity.getDateValidation())
                .motifRejet(entity.getMotifRejet())
                .raisonAnnulation(entity.getRaisonAnnulation())
                
                // Audit
                .dateCreation(entity.getDateCreation())
                .dateModification(entity.getDateModification())
                .build();
    }

    /**
     * Create entity from create request + agent
     */
    public FicheJournaliereAgentTerrain toEntityFromCreateRequest(CreateFicheJournaliereRequest request, 
                                                                   Utilisateur agent) {
        if (request == null) {
            return null;
        }

        return FicheJournaliereAgentTerrain.builder()
                .agentTerrain(agent)
                .site(agent != null ? agent.getSite() : null)
                .dateFiche(request.getDateFiche())
                .observationsAgent(request.getObservationsAgent())
                // Other fields default to 0 or BROUILLON per entity @Builder
                .build();
    }

    /**
     * Update entity from update request (only BROUILLON can be updated)
     */
    public void updateFromRequest(UpdateFicheJournaliereRequest request, FicheJournaliereAgentTerrain entity) {
        if (request == null || entity == null) {
            return;
        }

        if (request.getObservationsAgent() != null) {
            entity.setObservationsAgent(request.getObservationsAgent());
        }
    }
}
