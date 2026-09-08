package com.mini.credit.mapper;

import com.mini.credit.dto.epargne.DemandeRetraitEpargneDTO;
import com.mini.credit.entity.epargne.CompteEpargne;
import com.mini.credit.entity.epargne.DemandeRetraitEpargne;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.Utilisateur;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Mapper pour DemandeRetraitEpargne (PHASE 5).
 */
@Component
public class DemandeRetraitEpargneMapper {

    public DemandeRetraitEpargneDTO toDTO(DemandeRetraitEpargne entity) {
        if (entity == null) {
            return null;
        }

        CompteEpargne compte = entity.getCompteEpargne();
        Membre membre = entity.getMembre();
        Utilisateur validePar = entity.getValidePar();

        return DemandeRetraitEpargneDTO.builder()
                .id(entity.getId())
                .compteEpargneId(compte != null ? compte.getId() : null)
                .referenceRetrait(resolveReferenceRetrait(entity))
                .numeroCompte(compte != null ? compte.getNumeroCompte() : null)
                .compteEpargneNumero(compte != null ? compte.getNumeroCompte() : null)
                .membreId(membre != null ? membre.getId() : null)
                .membreNom(resolveMembreNom(membre))
                .montantDemande(entity.getMontantDemande())
                .fraisRetrait(entity.getFraisRetrait())
                .tauxCommissionRetrait(entity.getTauxCommissionRetrait())
                .montantTotalDebite(entity.getMontantTotalDebite())
                .montantRemisAuMembre(entity.getMontantDemande())
                .soldeDisponible(compte != null ? compte.getSoldeDisponible() : null)
                .soldeBloque(compte != null ? compte.getSoldeBloque() : null)
                .statut(entity.getStatut() != null ? entity.getStatut().name() : null)
                .dateDemande(entity.getDateDemande())
                .createdAt(entity.getDateCreation())
                .motifRejet(entity.getMotifRejet())
                .valideParId(validePar != null ? validePar.getId() : null)
                .valideParNom(validePar != null ? validePar.getNomComplet() : null)
                .dateValidation(entity.getDateValidation())
                .observation(entity.getObservation())
                .build();
    }

    public DemandeRetraitEpargne toEntity(DemandeRetraitEpargneDTO dto) {
        if (dto == null) {
            return null;
        }

        return DemandeRetraitEpargne.builder()
            .referenceRetrait(dto.getReferenceRetrait())
                .montantDemande(dto.getMontantDemande())
            .fraisRetrait(dto.getFraisRetrait())
            .tauxCommissionRetrait(dto.getTauxCommissionRetrait())
            .montantTotalDebite(dto.getMontantTotalDebite())
                .motifRejet(dto.getMotifRejet())
                .dateDemande(dto.getDateDemande())
                .dateValidation(dto.getDateValidation())
                .observation(dto.getObservation())
                .build();
    }

    private String resolveMembreNom(Membre membre) {
        if (membre == null) {
            return null;
        }
        if (membre.getNomComplet() != null && !membre.getNomComplet().isBlank()) {
            return membre.getNomComplet();
        }
        return membre.getNom();
    }

    private String resolveReferenceRetrait(DemandeRetraitEpargne entity) {
        if (entity.getReferenceRetrait() != null && !entity.getReferenceRetrait().isBlank()) {
            return entity.getReferenceRetrait();
        }
        if (entity.getId() == null) {
            return null;
        }
        LocalDateTime referenceDate = entity.getDateDemande() != null ? entity.getDateDemande() : entity.getDateCreation();
        int year = referenceDate != null ? referenceDate.getYear() : LocalDateTime.now().getYear();
        return String.format("RET-%d-%04d", year, entity.getId());
    }
}
