package com.mini.credit.mapper;

import com.mini.credit.dto.caisse.OperationCaisseResponse;
import com.mini.credit.entity.caisse.OperationCaisse;
import org.springframework.stereotype.Component;

@Component
public class OperationCaisseMapper {

    public OperationCaisseResponse toResponse(OperationCaisse operation) {
        if (operation == null) {
            return null;
        }

        return OperationCaisseResponse.builder()
                .id(operation.getId())
                .numeroPiece(operation.getNumeroPiece())
                .sessionCaisseId(operation.getSessionCaisse() != null ? operation.getSessionCaisse().getId() : null)
                .caisseId(operation.getCaisse() != null ? operation.getCaisse().getId() : null)
                .dateOperation(operation.getDateOperation())
                .typeOperation(operation.getTypeOperation())
                .categorieOperation(operation.getCategorieOperation())
                .montant(operation.getMontant())
                .devise(operation.getDevise())
                .membreId(operation.getMembre() != null ? operation.getMembre().getId() : null)
                .creditId(operation.getCredit() != null ? operation.getCredit().getId() : null)
                .remboursementId(operation.getRemboursement() != null ? operation.getRemboursement().getId() : null)
                .operationEpargneId(operation.getOperationEpargne() != null ? operation.getOperationEpargne().getId() : null)
                .agentId(operation.getAgent() != null ? operation.getAgent().getId() : null)
                .createdById(operation.getCreatedBy() != null ? operation.getCreatedBy().getId() : null)
                .paiementCreditId(operation.getPaiementCredit() != null ? operation.getPaiementCredit().getId() : null)
                .modePaiement(operation.getModePaiement())
                .description(operation.getDescription())
                .createdAt(operation.getDateCreation())
                .updatedAt(operation.getDateModification())
                .build();
    }
}