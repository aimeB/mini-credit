package com.mini.credit.mapper;

import com.mini.credit.dto.rapport.RapportFinancierDTO;
import com.mini.credit.entity.rapport.RapportFinancier;
import org.springframework.stereotype.Component;

/**
 * PHASE 12: Mapper Rapport Financier
 */
@Component
public class RapportFinancierMapper {

    public RapportFinancierDTO toDTO(RapportFinancier entity) {
        if (entity == null) {
            return null;
        }

        return RapportFinancierDTO.builder()
                .id(entity.getId())
                .typeRapport(entity.getTypeRapport())
                .periodicite(entity.getPeriodicite())
                .dateDebut(entity.getDateDebut())
                .dateFin(entity.getDateFin())
                .dateGeneration(entity.getDateGeneration())
                .statut(entity.getStatut())
                .genereParNom(entity.getGenerePar() != null ? entity.getGenerePar().getNomComplet() : null)
                .valideParNom(entity.getValidePar() != null ? entity.getValidePar().getNomComplet() : null)
                .dateValidation(entity.getDateValidation())
                .totalActif(entity.getTotalActif())
                .totalPassif(entity.getTotalPassif())
                .capitauxPropres(entity.getCapitauxPropres())
                .soldeCaisseDebut(entity.getSoldeCaisseDebut())
                .soldeCaisseFin(entity.getSoldeCaisseFin())
                .totalCreditsDecaisses(entity.getTotalCreditsDecaisses())
                .totalRembourses(entity.getTotalRembourses())
                .totalEpargnesDeposes(entity.getTotalEpargnesDeposes())
                .totalRetraitsEpargnes(entity.getTotalRetraitsEpargnes())
                .totalRevenus(entity.getTotalRevenus())
                .totalCharges(entity.getTotalCharges())
                .resultat(entity.getResultat())
                .interetsCredits(entity.getInteretsCredits())
                .commissionsAgents(entity.getCommissionsAgents())
                .interetsEpargnes(entity.getInteretsEpargnes())
                .penalitesCollectees(entity.getPenalitesCollectees())
                .penalitesEffacees(entity.getPenalitesEffacees())
                .nombreCreditsActifs(entity.getNombreCreditsActifs())
                .nombreCreditsRembourses(entity.getNombreCreditsRembourses())
                .nombreCreditsEnRetard(entity.getNombreCreditsEnRetard())
                .tauxRemboursement(entity.getTauxRemboursement())
                .nombreMembresActifs(entity.getNombreMembresActifs())
                .soldeEpargnesMoyen(entity.getSoldeEpargnesMoyen())
                .totalEpargnesCaisse(entity.getTotalEpargnesCaisse())
                .roa(entity.getRoa())
                .roe(entity.getRoe())
                .observation(entity.getObservation())
                .messageErreur(entity.getMessageErreur())
                .dateCreation(entity.getDateCreation())
                .dateModification(entity.getDateModification())
                .periodLabel(entity.getPeriodLabel())
                .build();
    }

    public RapportFinancier toEntity(RapportFinancierDTO dto) {
        if (dto == null) {
            return null;
        }

        return RapportFinancier.builder()
                .typeRapport(dto.getTypeRapport())
                .periodicite(dto.getPeriodicite())
                .dateDebut(dto.getDateDebut())
                .dateFin(dto.getDateFin())
                .dateGeneration(dto.getDateGeneration())
                .statut(dto.getStatut())
                .dateValidation(dto.getDateValidation())
                .totalActif(dto.getTotalActif())
                .totalPassif(dto.getTotalPassif())
                .capitauxPropres(dto.getCapitauxPropres())
                .soldeCaisseDebut(dto.getSoldeCaisseDebut())
                .soldeCaisseFin(dto.getSoldeCaisseFin())
                .totalCreditsDecaisses(dto.getTotalCreditsDecaisses())
                .totalRembourses(dto.getTotalRembourses())
                .totalEpargnesDeposes(dto.getTotalEpargnesDeposes())
                .totalRetraitsEpargnes(dto.getTotalRetraitsEpargnes())
                .totalRevenus(dto.getTotalRevenus())
                .totalCharges(dto.getTotalCharges())
                .resultat(dto.getResultat())
                .interetsCredits(dto.getInteretsCredits())
                .commissionsAgents(dto.getCommissionsAgents())
                .interetsEpargnes(dto.getInteretsEpargnes())
                .penalitesCollectees(dto.getPenalitesCollectees())
                .penalitesEffacees(dto.getPenalitesEffacees())
                .nombreCreditsActifs(dto.getNombreCreditsActifs())
                .nombreCreditsRembourses(dto.getNombreCreditsRembourses())
                .nombreCreditsEnRetard(dto.getNombreCreditsEnRetard())
                .tauxRemboursement(dto.getTauxRemboursement())
                .nombreMembresActifs(dto.getNombreMembresActifs())
                .soldeEpargnesMoyen(dto.getSoldeEpargnesMoyen())
                .totalEpargnesCaisse(dto.getTotalEpargnesCaisse())
                .roa(dto.getRoa())
                .roe(dto.getRoe())
                .observation(dto.getObservation())
                .messageErreur(dto.getMessageErreur())
                .build();
    }
}
