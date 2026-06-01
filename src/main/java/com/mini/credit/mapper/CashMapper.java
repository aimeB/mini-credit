package com.mini.credit.mapper;

import com.mini.credit.dto.caisse.CaisseResponse;
import com.mini.credit.dto.caisse.OperationCaisseResponse;
import com.mini.credit.dto.caisse.SessionCaisseResponse;
import com.mini.credit.entity.caisse.Caisse;
import com.mini.credit.entity.caisse.OperationCaisse;
import com.mini.credit.entity.caisse.SessionCaisse;
import org.springframework.stereotype.Component;

@Component
public class CashMapper {

    public CaisseResponse toResponse(Caisse caisse) {
        return CaisseResponse.builder()
                .id(caisse.getId())
                .codeCaisse(caisse.getCodeCaisse())
                .libelle(caisse.getLibelle())
                .siteId(caisse.getSite() != null ? caisse.getSite().getId() : null)
                .siteNom(caisse.getSite() != null ? caisse.getSite().getNomSite() : null)
                .devise(caisse.getDevise())
                .actif(caisse.getActif())
                .createdAt(caisse.getDateCreation())
                .updatedAt(caisse.getDateModification())
                .build();
    }

    public SessionCaisseResponse toResponse(SessionCaisse session) {
        return SessionCaisseResponse.builder()
                .id(session.getId())
                .caisseId(session.getCaisse().getId())
                .caisseCode(session.getCaisse().getCodeCaisse())
                .utilisateurId(session.getUtilisateur().getId())
                .utilisateurNom(session.getUtilisateur().getNomComplet())
                .dateOuverture(session.getDateOuverture())
                .dateCloture(session.getDateCloture())
                .soldeOuverture(session.getSoldeOuverture())
                .totalEntrees(session.getTotalEntrees())
                .totalSorties(session.getTotalSorties())
                .soldeTheorique(session.getSoldeTheorique())
                .soldePhysique(session.getSoldePhysique())
                .ecartCaisse(session.getEcartCaisse())
                .statut(session.getStatut())
                .observation(session.getObservation())
                .createdAt(session.getDateCreation())
                .updatedAt(session.getDateModification())
                .build();
    }

    public OperationCaisseResponse toResponse(OperationCaisse op) {
        return OperationCaisseResponse.builder()
                .id(op.getId())
                .numeroPiece(op.getNumeroPiece())
                .sessionCaisseId(op.getSessionCaisse().getId())
                .caisseId(op.getCaisse().getId())
                .dateOperation(op.getDateOperation())
                .typeOperation(op.getTypeOperation())
                .categorieOperation(op.getCategorieOperation())
                .montant(op.getMontant())
                .devise(op.getDevise())
                .membreId(op.getMembre() != null ? op.getMembre().getId() : null)
                .creditId(op.getCredit() != null ? op.getCredit().getId() : null)
                .remboursementId(op.getRemboursement() != null ? op.getRemboursement().getId() : null)
                .operationEpargneId(op.getOperationEpargne() != null ? op.getOperationEpargne().getId() : null)
                .paiementCreditId(op.getPaiementCredit() != null ? op.getPaiementCredit().getId() : null)
                .agentId(op.getAgent() != null ? op.getAgent().getId() : null)
                .createdById(op.getCreatedBy() != null ? op.getCreatedBy().getId() : null)
                .modePaiement(op.getModePaiement())
                .description(op.getDescription())
                .createdAt(op.getDateCreation())
                .updatedAt(op.getDateModification())
                .build();
    }
}