package com.mini.credit.mapper;

import com.mini.credit.dto.caisse.RecetteJournaliereTerrainDTO;
import com.mini.credit.entity.caisse.RecetteJournaliereTerrain;
import com.mini.credit.enums.TypeRecette;
import org.springframework.stereotype.Component;

/**
 * PHASE 6: Mapper pour RecetteJournaliereTerrain.
 */
@Component
public class RecetteJournaliereTerrainMapper {

    public RecetteJournaliereTerrainDTO toDTO(RecetteJournaliereTerrain entity) {
        if (entity == null) {
            return null;
        }

        return RecetteJournaliereTerrainDTO.builder()
                .id(entity.getId())
                .agentId(entity.getAgent() != null ? entity.getAgent().getId() : null)
                .membreId(entity.getMembre() != null ? entity.getMembre().getId() : null)
                .dateJour(entity.getDateJour())
                .typeRecette(entity.getTypeRecette() != null ? entity.getTypeRecette().name() : null)
                .montant(entity.getMontant())
                .statut(entity.getStatut() != null ? entity.getStatut().name() : null)
                .observation(entity.getObservation())
                .referencePapier(entity.getReferencePapier())
                .valideParId(entity.getValidePar() != null ? entity.getValidePar().getId() : null)
                .dateValidation(entity.getDateValidation())
                .motifRejet(entity.getMotifRejet())
                .cashRemis(entity.getCashRemis())
                .variance(entity.getVariance())
                // PHASE 6B.2: Statut de génération des opérations
                // Initialisation avec valeurs par défaut
                // TODO: Ajouter des champs à l'entité pour stocker le statut réel
                .operationGenerationStatus("NON_GENEREE")
                .operationEpargneCount(0)
                .operationCaisseCount(0)
                .operationGenerationErrorMessage(null)
                .build();
    }

    public RecetteJournaliereTerrain toEntity(RecetteJournaliereTerrainDTO dto) {
        if (dto == null) {
            return null;
        }

        return RecetteJournaliereTerrain.builder()
                .dateJour(dto.getDateJour())
                .typeRecette(dto.getTypeRecette() != null ? TypeRecette.valueOf(dto.getTypeRecette()) : null)
                .montant(dto.getMontant())
                .observation(dto.getObservation())
                .referencePapier(dto.getReferencePapier())
                .motifRejet(dto.getMotifRejet())
                .cashRemis(dto.getCashRemis())
                .variance(dto.getVariance())
                .build();
    }
}
