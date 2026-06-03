package com.mini.credit.mapper;

import com.mini.credit.dto.credit.PenaliteCreditDTO;
import com.mini.credit.entity.credit.PenaliteCredit;
import org.springframework.stereotype.Component;

/**
 * PHASE 10: Mapper pour PenaliteCredit
 */
@Component
public class PenaliteCreditMapper {

    /**
     * Convertit PenaliteCredit entity vers DTO
     */
    public PenaliteCreditDTO toDTO(PenaliteCredit entity) {
        if (entity == null) {
            return null;
        }

        return PenaliteCreditDTO.builder()
                .id(entity.getId())
                .creditId(entity.getCredit() != null ? entity.getCredit().getId() : null)
                .demandeCreditId(entity.getDemandeCredit() != null ? entity.getDemandeCredit().getId() : null)
                .dateEchéance(entity.getDateEchéance())
                .nombreJoursRetard(entity.getNombreJoursRetard())
                .montantPenalite(entity.getMontantPenalite())
                .tauxApplique(entity.getTauxApplique())
                .statut(entity.getStatut())
                .dateCreationPenalite(entity.getDateCreationPenalite())
                .dateAcquittement(entity.getDateAcquittement())
                .acquitteParId(entity.getAcquitteePar() != null ? entity.getAcquitteePar().getId() : null)
                .dateEffacement(entity.getDateEffacement())
                .effaceeParId(entity.getEffaceePar() != null ? entity.getEffaceePar().getId() : null)
                .motifEffacement(entity.getMotifEffacement())
                .observation(entity.getObservation())
                .dateCreation(entity.getDateCreation())
                .dateModification(entity.getDateModification())
                .build();
    }

    /**
     * Convertit DTO vers PenaliteCredit entity
     * Note: id et relationships complexes ne sont pas set (handled by service)
     */
    public PenaliteCredit toEntity(PenaliteCreditDTO dto) {
        if (dto == null) {
            return null;
        }

        return PenaliteCredit.builder()
                .dateEchéance(dto.getDateEchéance())
                .nombreJoursRetard(dto.getNombreJoursRetard())
                .montantPenalite(dto.getMontantPenalite())
                .tauxApplique(dto.getTauxApplique())
                .statut(dto.getStatut() != null ? dto.getStatut() : com.mini.credit.enums.StatutPenalite.CREEE)
                .dateCreationPenalite(dto.getDateCreationPenalite())
                .dateAcquittement(dto.getDateAcquittement())
                .dateEffacement(dto.getDateEffacement())
                .motifEffacement(dto.getMotifEffacement())
                .observation(dto.getObservation())
                .build();
    }
}
