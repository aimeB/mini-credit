package com.mini.credit.mapper;

import com.mini.credit.dto.epargne.CompteEpargneResponse;
import com.mini.credit.dto.epargne.OperationEpargneResponse;
import com.mini.credit.entity.epargne.CompteEpargne;
import com.mini.credit.entity.epargne.OperationEpargne;
import org.springframework.stereotype.Component;

@Component
public class SavingMapper {

    public CompteEpargneResponse toResponse(CompteEpargne compte) {
        return CompteEpargneResponse.builder()
                .id(compte.getId())
                .membreId(compte.getMembre().getId())
                .membreNomComplet(compte.getMembre().getNomComplet())
                .numeroCompte(compte.getNumeroCompte())
                .typeCompte(compte.getTypeCompte())
                .soldeDisponible(compte.getSoldeDisponible())
                .soldeBloque(compte.getSoldeBloque())
                .statut(compte.getStatut())
                .dateOuverture(compte.getDateOuverture())
                .dateFermeture(compte.getDateFermeture())
                .createdAt(compte.getDateCreation())
                .updatedAt(compte.getDateModification())
                .build();
    }

    public OperationEpargneResponse toResponse(OperationEpargne op) {
        return OperationEpargneResponse.builder()
                .id(op.getId())
                .compteEpargneId(op.getCompteEpargne().getId())
                .numeroCompte(op.getCompteEpargne().getNumeroCompte())
                .membreId(op.getMembre().getId())
                .membreNomComplet(op.getMembre().getNomComplet())
                .dateOperation(op.getDateOperation())
                .typeOperation(op.getTypeOperation())
                .montant(op.getMontant())
                .sens(op.getSens())
                .modePaiement(op.getModePaiement())
                .referenceExterne(op.getReferenceExterne())
                .agentId(op.getAgent() != null ? op.getAgent().getId() : null)
                .sessionCaisseId(op.getSessionCaisse() != null ? op.getSessionCaisse().getId() : null)
                .observation(op.getObservation())
                .createdAt(op.getDateCreation())
                .updatedAt(op.getDateModification())
                .build();
    }
}