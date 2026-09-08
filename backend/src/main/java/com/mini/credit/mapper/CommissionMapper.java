package com.mini.credit.mapper;

import com.mini.credit.dto.employe.CommissionDTO;
import com.mini.credit.entity.employe.Commission;
import org.springframework.stereotype.Component;

/**
 * PHASE 8: Mapper pour Commission.
 */
@Component
public class CommissionMapper {

    public CommissionDTO toDTO(Commission entity) {
        if (entity == null) {
            return null;
        }

        return CommissionDTO.builder()
                .id(entity.getId())
                .agentId(entity.getAgent() != null ? entity.getAgent().getId() : null)
                .datePeriodeDebut(entity.getDatePeriodeDebut())
                .datePeriodeFin(entity.getDatePeriodeFin())
                .totalRecettes(entity.getTotalRecettes())
                .tauxCommission(entity.getTauxCommission())
                .montantCommission(entity.getMontantCommission())
                .nbRecettes(entity.getNbRecettes())
                .statut(entity.getStatut() != null ? entity.getStatut().name() : null)
                .valideParId(entity.getValideePar() != null ? entity.getValideePar().getId() : null)
                .dateValidation(entity.getDateValidation())
                .payeeAId(entity.getPayeeA() != null ? entity.getPayeeA().getId() : null)
                .datePaiement(entity.getDatePaiement())
                .observation(entity.getObservation())
                .build();
    }

    public Commission toEntity(CommissionDTO dto) {
        if (dto == null) {
            return null;
        }

        return Commission.builder()
                .datePeriodeDebut(dto.getDatePeriodeDebut())
                .datePeriodeFin(dto.getDatePeriodeFin())
                .totalRecettes(dto.getTotalRecettes())
                .tauxCommission(dto.getTauxCommission())
                .montantCommission(dto.getMontantCommission())
                .nbRecettes(dto.getNbRecettes())
                .observation(dto.getObservation())
                .build();
    }
}
