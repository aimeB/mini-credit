package com.mini.credit.mapper;

import com.mini.credit.dto.epargne.DemandeRetraitEpargneDTO;
import com.mini.credit.entity.epargne.DemandeRetraitEpargne;
import org.springframework.stereotype.Component;

/**
 * Mapper pour DemandeRetraitEpargne (PHASE 5).
 */
@Component
public class DemandeRetraitEpargneMapper {

    public DemandeRetraitEpargneDTO toDTO(DemandeRetraitEpargne entity) {
        if (entity == null) {
            return null;
        }

        return DemandeRetraitEpargneDTO.builder()
                .id(entity.getId())
                .compteEpargneId(entity.getCompteEpargne() != null ? entity.getCompteEpargne().getId() : null)
                .membreId(entity.getMembre() != null ? entity.getMembre().getId() : null)
                .montantDemande(entity.getMontantDemande())
                .statut(entity.getStatut() != null ? entity.getStatut().name() : null)
                .dateDemande(entity.getDateDemande())
                .motifRejet(entity.getMotifRejet())
                .valideParId(entity.getValidePar() != null ? entity.getValidePar().getId() : null)
                .dateValidation(entity.getDateValidation())
                .observation(entity.getObservation())
                .build();
    }

    public DemandeRetraitEpargne toEntity(DemandeRetraitEpargneDTO dto) {
        if (dto == null) {
            return null;
        }

        return DemandeRetraitEpargne.builder()
                .montantDemande(dto.getMontantDemande())
                .motifRejet(dto.getMotifRejet())
                .dateDemande(dto.getDateDemande())
                .dateValidation(dto.getDateValidation())
                .observation(dto.getObservation())
                .build();
    }
}
