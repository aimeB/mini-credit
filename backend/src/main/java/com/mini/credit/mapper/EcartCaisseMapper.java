package com.mini.credit.mapper;

import com.mini.credit.dto.caisse.EcartCaisseDTO;
import com.mini.credit.entity.caisse.EcartCaisse;
import org.springframework.stereotype.Component;

/**
 * PHASE 7: Mapper pour EcartCaisse.
 */
@Component
public class EcartCaisseMapper {

    public EcartCaisseDTO toDTO(EcartCaisse entity) {
        if (entity == null) {
            return null;
        }

        return EcartCaisseDTO.builder()
                .id(entity.getId())
                .sessionCaisseId(entity.getSessionCaisse() != null ? entity.getSessionCaisse().getId() : null)
                .recetteId(entity.getRecette() != null ? entity.getRecette().getId() : null)
                .dateJour(entity.getDateJour())
                .typeEcart(entity.getTypeEcart() != null ? entity.getTypeEcart().name() : null)
                .montantEcart(entity.getMontantEcart())
                .description(entity.getDescription())
                .statut(entity.getStatut() != null ? entity.getStatut().name() : null)
                .notesInvestigation(entity.getNotesInvestigation())
                .raisonResolution(entity.getRaisonResolution())
                .enqueteParId(entity.getEnquetePar() != null ? entity.getEnquetePar().getId() : null)
                .dateEnquete(entity.getDateEnquete())
                .valideParId(entity.getValidePar() != null ? entity.getValidePar().getId() : null)
                .dateValidation(entity.getDateValidation())
                .seuilDepassé(entity.getSeuilDepassé())
                .build();
    }

    public EcartCaisse toEntity(EcartCaisseDTO dto) {
        if (dto == null) {
            return null;
        }

        return EcartCaisse.builder()
                .dateJour(dto.getDateJour())
                .typeEcart(dto.getTypeEcart() != null ? com.mini.credit.enums.TypeEcartCaisse.valueOf(dto.getTypeEcart()) : null)
                .montantEcart(dto.getMontantEcart())
                .description(dto.getDescription())
                .notesInvestigation(dto.getNotesInvestigation())
                .raisonResolution(dto.getRaisonResolution())
                .seuilDepassé(dto.getSeuilDepassé())
                .build();
    }
}
