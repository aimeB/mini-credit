package com.mini.credit.mapper;

import com.mini.credit.dto.caisse.ReconciliationCaisseDTO;
import com.mini.credit.entity.caisse.ReconciliationCaisse;
import org.springframework.stereotype.Component;

/**
 * PHASE 11: Mapper Réconciliation Caisse
 */
@Component
public class ReconciliationCaisseMapper {

    public ReconciliationCaisseDTO toDTO(ReconciliationCaisse entity) {
        if (entity == null) {
            return null;
        }

        return ReconciliationCaisseDTO.builder()
                .id(entity.getId())
                .sessionCaisseId(entity.getSessionCaisse() != null ? entity.getSessionCaisse().getId() : null)
                .ecartCaisseId(entity.getEcartCaisse() != null ? entity.getEcartCaisse().getId() : null)
                .montantAttendu(entity.getMontantAttendu())
                .montantObserve(entity.getMontantObserve())
                .montantEcart(entity.getMontantEcart())
                .statut(entity.getStatut())
                .dateCreationReconciliation(entity.getDateCreationReconciliation())
                .dateRapprochement(entity.getDateRapprochement())
                .rapprochePar(entity.getRapprochePar() != null ? entity.getRapprochePar().getNomComplet() : null)
                .motifRapprochement(entity.getMotifRapprochement())
                .raisonRejet(entity.getRaisonRejet())
                .observation(entity.getObservation())
                .dateCreation(entity.getDateCreation())
                .dateModification(entity.getDateModification())
                .isDeficit(entity.isDeficit())
                .isSurplus(entity.isSurplus())
                .build();
    }

    public ReconciliationCaisse toEntity(ReconciliationCaisseDTO dto) {
        if (dto == null) {
            return null;
        }

        return ReconciliationCaisse.builder()
                .montantAttendu(dto.getMontantAttendu())
                .montantObserve(dto.getMontantObserve())
                .montantEcart(dto.getMontantEcart())
                .statut(dto.getStatut())
                .dateCreationReconciliation(dto.getDateCreationReconciliation())
                .dateRapprochement(dto.getDateRapprochement())
                .motifRapprochement(dto.getMotifRapprochement())
                .raisonRejet(dto.getRaisonRejet())
                .observation(dto.getObservation())
                .build();
    }
}
