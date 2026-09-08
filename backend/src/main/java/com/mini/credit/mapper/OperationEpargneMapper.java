package com.mini.credit.mapper;

import com.mini.credit.dto.epargne.OperationEpargneDTO;
import com.mini.credit.entity.epargne.OperationEpargne;
import org.springframework.stereotype.Component;

/**
 * PHASE 9: Mapper pour OperationEpargne (Intérêts)
 */
@Component
public class OperationEpargneMapper {

    /**
     * Convertit OperationEpargne entity vers DTO
     */
    public OperationEpargneDTO toDTO(OperationEpargne entity) {
        if (entity == null) {
            return null;
        }

        return OperationEpargneDTO.builder()
                .id(entity.getId())
                .compteEpargneId(entity.getCompteEpargne() != null ? entity.getCompteEpargne().getId() : null)
                .membreId(entity.getMembre() != null ? entity.getMembre().getId() : null)
                .dateOperation(entity.getDateOperation())
                .typeOperation(entity.getTypeOperation())
                .montant(entity.getMontant())
                .sens(entity.getSens())
                .modePaiement(entity.getModePaiement())
                .observation(entity.getObservation())
                .dateCreation(entity.getDateCreation())
                .dateModification(entity.getDateModification())
                .build();
    }

    /**
     * Convertit DTO vers OperationEpargne entity
     * Note: id n'est pas set (auto-generated)
     */
    public OperationEpargne toEntity(OperationEpargneDTO dto) {
        if (dto == null) {
            return null;
        }

        return OperationEpargne.builder()
                .dateOperation(dto.getDateOperation())
                .typeOperation(dto.getTypeOperation())
                .montant(dto.getMontant())
                .sens(dto.getSens())
                .modePaiement(dto.getModePaiement())
                .observation(dto.getObservation())
                .build();
    }
}
