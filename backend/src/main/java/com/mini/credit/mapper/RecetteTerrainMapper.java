package com.mini.credit.mapper;

import com.mini.credit.dto.referentiel.CreateRecetteTerrainRequest;
import com.mini.credit.dto.referentiel.RecetteTerrainResponse;
import com.mini.credit.dto.referentiel.UpdateRecetteTerrainRequest;
import com.mini.credit.entity.referentiel.RecetteTerrainJournaliere;
import com.mini.credit.enums.RecetteStatut;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Mapper pour RecetteTerrainJournaliere
 * Gère les conversions entre Entity et DTOs
 */
@Component
public class RecetteTerrainMapper {

    /**
     * Convertit CreateRecetteTerrainRequest → RecetteTerrainJournaliere Entity
     * Statut initial: BROUILLON
     */
    public RecetteTerrainJournaliere toEntity(CreateRecetteTerrainRequest request) {
        if (request == null) {
            return null;
        }

        return RecetteTerrainJournaliere.builder()
            .dateRecette(request.getDateRecette())
            .membresVisites(request.getMembresVisites() != null ? request.getMembresVisites() : 0)
            .nouveauxMembres(request.getNouveauxMembres() != null ? request.getNouveauxMembres() : 0)
            .carnetDistribues(request.getCarnetDistribues() != null ? request.getCarnetDistribues() : 0)
            .epargneCollectee(request.getEpargneCollectee() != null ? request.getEpargneCollectee() : java.math.BigDecimal.ZERO)
            .epargneSourceType(request.getEpargneSourceType())
            .remboursementsCreditCollectes(request.getRemboursementsCreditCollectes() != null ? request.getRemboursementsCreditCollectes() : java.math.BigDecimal.ZERO)
            .creditIdsTraites(request.getCreditIdsTraites())
            .fraisCollectes(request.getFraisCollectes() != null ? request.getFraisCollectes() : java.math.BigDecimal.ZERO)
            .demandesCreditRecueillies(request.getDemandesCreditRecueillies() != null ? request.getDemandesCreditRecueillies() : 0)
            .demandesCreditIds(request.getDemandesCreditIds())
            .especesRemises(request.getEspecesRemises() != null ? request.getEspecesRemises() : java.math.BigDecimal.ZERO)
            .especesEmises(request.getEspecesEmises() != null ? request.getEspecesEmises() : java.math.BigDecimal.ZERO)
            .observations(request.getObservations())
            .pieceJointePath(request.getPieceJointePath())
            .statut(RecetteStatut.BROUILLON)
            .actif(true)
            .build();
    }

    /**
     * Applique les modifications de UpdateRecetteTerrainRequest à l'entité existante
     * Ne modifie que les champs updatables (pas agent/site/date)
     */
    public void updateEntityFromDTO(UpdateRecetteTerrainRequest request, RecetteTerrainJournaliere entity) {
        if (request == null || entity == null) {
            return;
        }

        entity.setMembresVisites(request.getMembresVisites() != null ? request.getMembresVisites() : 0);
        entity.setNouveauxMembres(request.getNouveauxMembres() != null ? request.getNouveauxMembres() : 0);
        entity.setCarnetDistribues(request.getCarnetDistribues() != null ? request.getCarnetDistribues() : 0);
        entity.setEpargneCollectee(request.getEpargneCollectee() != null ? request.getEpargneCollectee() : java.math.BigDecimal.ZERO);
        entity.setEpargneSourceType(request.getEpargneSourceType());
        entity.setRemboursementsCreditCollectes(request.getRemboursementsCreditCollectes() != null ? request.getRemboursementsCreditCollectes() : java.math.BigDecimal.ZERO);
        entity.setCreditIdsTraites(request.getCreditIdsTraites());
        entity.setFraisCollectes(request.getFraisCollectes() != null ? request.getFraisCollectes() : java.math.BigDecimal.ZERO);
        entity.setDemandesCreditRecueillies(request.getDemandesCreditRecueillies() != null ? request.getDemandesCreditRecueillies() : 0);
        entity.setDemandesCreditIds(request.getDemandesCreditIds());
        entity.setEspecesRemises(request.getEspecesRemises() != null ? request.getEspecesRemises() : java.math.BigDecimal.ZERO);
        entity.setEspecesEmises(request.getEspecesEmises() != null ? request.getEspecesEmises() : java.math.BigDecimal.ZERO);
        entity.setObservations(request.getObservations());
        entity.setPieceJointePath(request.getPieceJointePath());
        
        // Le calcul des écarts se fera via @PreUpdate
    }

    /**
     * Convertit RecetteTerrainJournaliere Entity → RecetteTerrainResponse DTO
     */
    public RecetteTerrainResponse toDTO(RecetteTerrainJournaliere entity) {
        if (entity == null) {
            return null;
        }

        return RecetteTerrainResponse.builder()
            .id(entity.getId())
            .agentTerrainId(entity.getAgentTerrain() != null ? entity.getAgentTerrain().getId() : null)
            .agentTerrainMatricule(entity.getAgentTerrain() != null ? entity.getAgentTerrain().getMatricule() : null)
            .agentTerrainNom(entity.getAgentTerrain() != null && entity.getAgentTerrain().getUtilisateur() != null 
                ? entity.getAgentTerrain().getUtilisateur().getNomComplet()
                : null)
            .siteId(entity.getSite() != null ? entity.getSite().getId() : null)
            .siteNom(entity.getSite() != null ? entity.getSite().getNomSite() : null)
            .dateRecette(entity.getDateRecette())
            .statut(entity.getStatut() != null ? entity.getStatut().name() : null)
            .membresVisites(entity.getMembresVisites())
            .nouveauxMembres(entity.getNouveauxMembres())
            .carnetDistribues(entity.getCarnetDistribues())
            .epargneCollectee(entity.getEpargneCollectee())
            .epargneSourceType(entity.getEpargneSourceType())
            .remboursementsCreditCollectes(entity.getRemboursementsCreditCollectes())
            .creditIdsTraites(entity.getCreditIdsTraites())
            .fraisCollectes(entity.getFraisCollectes())
            .demandesCreditRecueillies(entity.getDemandesCreditRecueillies())
            .demandesCreditIds(entity.getDemandesCreditIds())
            .especesRemises(entity.getEspecesRemises())
            .especesEmises(entity.getEspecesEmises())
            .excedent(entity.getExcedent())
            .manquant(entity.getManquant())
            .observations(entity.getObservations())
            .pieceJointePath(entity.getPieceJointePath())
            .dateValidation(entity.getDateValidation())
            .valideParId(entity.getValidePar() != null ? entity.getValidePar().getId() : null)
            .valideParNom(entity.getValidePar() != null 
                ? entity.getValidePar().getNomComplet()
                : null)
            .motifRejet(entity.getMotifRejet())
            .dateCreation(entity.getDateCreation())
            .dateModification(entity.getDateModification())
            .modifiePar(entity.getModifiePar())
            .build();
    }
}
