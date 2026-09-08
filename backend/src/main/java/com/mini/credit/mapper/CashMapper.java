package com.mini.credit.mapper;

import com.mini.credit.dto.caisse.CaisseResponse;
import com.mini.credit.dto.caisse.JournalCaisseResponse;
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
                .agenceId(caisse.getAgence() != null ? caisse.getAgence().getId() : (caisse.getSite() != null && caisse.getSite().getAgence() != null ? caisse.getSite().getAgence().getId() : null))
                .agenceNom(caisse.getAgence() != null ? caisse.getAgence().getNomAgence() : (caisse.getSite() != null && caisse.getSite().getAgence() != null ? caisse.getSite().getAgence().getNomAgence() : null))
                .siteId(caisse.getSite() != null ? caisse.getSite().getId() : null)
                .siteNom(caisse.getSite() != null ? caisse.getSite().getNomSite() : null)
                .antenneId(caisse.getAgence() != null ? caisse.getAgence().getId() : (caisse.getSite() != null && caisse.getSite().getAgence() != null ? caisse.getSite().getAgence().getId() : null))
                .antenneNom(caisse.getAgence() != null ? caisse.getAgence().getNomAgence() : (caisse.getSite() != null && caisse.getSite().getAgence() != null ? caisse.getSite().getAgence().getNomAgence() : null))
                .caissierResponsableId(caisse.getCaissierResponsable() != null ? caisse.getCaissierResponsable().getId() : null)
                .caissierResponsableNom(caisse.getCaissierResponsable() != null ? caisse.getCaissierResponsable().getNomComplet() : null)
                .caissierAffecteId(caisse.getCaissierResponsable() != null ? caisse.getCaissierResponsable().getId() : null)
                .caissierAffecteNom(caisse.getCaissierResponsable() != null ? caisse.getCaissierResponsable().getNomComplet() : null)
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
            .devise(session.getCaisse().getDevise())
            .siteId(session.getCaisse().getSite() != null ? session.getCaisse().getSite().getId() : null)
            .siteNom(session.getCaisse().getSite() != null ? session.getCaisse().getSite().getNomSite() : null)
            .antenneId(session.getCaisse().getAgence() != null ? session.getCaisse().getAgence().getId() : (session.getCaisse().getSite() != null && session.getCaisse().getSite().getAgence() != null ? session.getCaisse().getSite().getAgence().getId() : null))
            .antenneNom(session.getCaisse().getAgence() != null ? session.getCaisse().getAgence().getNomAgence() : (session.getCaisse().getSite() != null && session.getCaisse().getSite().getAgence() != null ? session.getCaisse().getSite().getAgence().getNomAgence() : null))
            .caissierResponsableNom(session.getCaisse().getCaissierResponsable() != null
                ? session.getCaisse().getCaissierResponsable().getNomComplet()
                : null)
                .utilisateurId(session.getUtilisateur().getId())
                .utilisateurNom(session.getUtilisateur().getNomComplet())
                .dateComptable(session.getDateComptable())
                .dateOuverture(session.getDateOuverture())
                .dateCloture(session.getDateCloture())
                .soldeOuverture(session.getSoldeOuverture())
                .totalEntrees(session.getTotalEntrees())
                .totalSorties(session.getTotalSorties())
                .soldeTheorique(session.getSoldeTheorique())
                .soldePhysique(session.getSoldePhysique())
            .ecart(session.getEcartCaisse())
                .ecartCaisse(session.getEcartCaisse())
                .statut(session.getStatut())
            .statutControle(session.getDateControle() != null ? "VALIDE" : "EN_ATTENTE")
                .observation(session.getObservation())
            .clotureParId(session.getFermePar() != null ? session.getFermePar().getId() : null)
            .clotureParNom(session.getFermePar() != null ? session.getFermePar().getNomComplet() : null)
            .fermeParId(session.getFermePar() != null ? session.getFermePar().getId() : null)
            .fermeParNom(session.getFermePar() != null ? session.getFermePar().getNomComplet() : null)
            .controleValideParId(session.getControleValidePar() != null ? session.getControleValidePar().getId() : null)
            .controleValideParNom(session.getControleValidePar() != null ? session.getControleValidePar().getNomComplet() : null)
            .dateControle(session.getDateControle())
                .motifAnnulation(session.getMotifAnnulation())
                .annuleePar(session.getAnnuleePar() != null ? session.getAnnuleePar().getNomComplet() : null)
                .dateAnnulation(session.getDateAnnulation())
                .statutCorrection(session.getStatutCorrection())
                .peutDemanderAnnulation(false)
                .peutValiderAnnulation(false)
                .peutAnnulerAdministrativement(false)
                .peutReouvrirControlee(false)
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
                .caisseLibelle(op.getCaisse().getLibelle())
                .siteId(op.getSite() != null ? op.getSite().getId() : (op.getCaisse() != null && op.getCaisse().getSite() != null ? op.getCaisse().getSite().getId() : null))
                .siteLibelle(op.getSite() != null ? op.getSite().getNomSite() : (op.getCaisse() != null && op.getCaisse().getSite() != null ? op.getCaisse().getSite().getNomSite() : null))
                .dateOperation(op.getDateOperation())
                .typeOperation(op.getTypeOperation())
                .categorieOperation(op.getCategorieOperation())
                .natureFinancement(op.getNatureFinancement())
                .montant(op.getMontant())
                .soldeApresOperation(op.getSoldeApresOperation())
                .devise(op.getDevise())
                .membreId(op.getMembre() != null ? op.getMembre().getId() : null)
                .creditId(op.getCredit() != null ? op.getCredit().getId() : null)
                .retraitEpargneId(op.getRetraitEpargneId())
                .remboursementId(op.getRemboursement() != null ? op.getRemboursement().getId() : null)
                .operationEpargneId(op.getOperationEpargne() != null ? op.getOperationEpargne().getId() : null)
                .depenseCaisseId(op.getDepenseCaisseId())
                .paiementCreditId(op.getPaiementCredit() != null ? op.getPaiementCredit().getId() : null)
                .agentId(op.getAgent() != null ? op.getAgent().getId() : null)
                .createdById(op.getCreatedBy() != null ? op.getCreatedBy().getId() : null)
                .utilisateurId(op.getUtilisateur() != null ? op.getUtilisateur().getId() : (op.getCreatedBy() != null ? op.getCreatedBy().getId() : null))
                .utilisateurNom(op.getUtilisateur() != null ? op.getUtilisateur().getNomComplet() : (op.getCreatedBy() != null ? op.getCreatedBy().getNomComplet() : null))
                .roleUtilisateur(op.getRoleUtilisateur())
                .modePaiement(op.getModePaiement())
                .source(op.getSource())
                .referenceExterne(op.getReferenceExterne())
                .referenceMetier(op.getReferenceMetier())
                .description(op.getDescription())
                .observation(op.getObservation())
                .commentaire(op.getCommentaire())
                .statutSession(op.getSessionCaisse() != null && op.getSessionCaisse().getStatut() != null ? op.getSessionCaisse().getStatut().name() : null)
                .createdAt(op.getDateCreation())
                .updatedAt(op.getDateModification())
                .build();
    }

    public JournalCaisseResponse toJournalResponse(OperationCaisse op) {
        return JournalCaisseResponse.builder()
                .operationId(op.getId())
                .sessionCaisseId(op.getSessionCaisse() != null ? op.getSessionCaisse().getId() : null)
                .caisseId(op.getCaisse() != null ? op.getCaisse().getId() : null)
                .caisseLibelle(op.getCaisse() != null ? op.getCaisse().getLibelle() : null)
                .siteId(op.getSite() != null ? op.getSite().getId() : (op.getCaisse() != null && op.getCaisse().getSite() != null ? op.getCaisse().getSite().getId() : null))
                .siteLibelle(op.getSite() != null ? op.getSite().getNomSite() : (op.getCaisse() != null && op.getCaisse().getSite() != null ? op.getCaisse().getSite().getNomSite() : null))
                .dateOperation(op.getDateOperation())
                .dateOperationJour(op.getDateOperation() != null ? op.getDateOperation().toLocalDate() : null)
                .heureOperation(op.getDateOperation() != null ? op.getDateOperation().toLocalTime() : null)
                .utilisateurId(op.getUtilisateur() != null ? op.getUtilisateur().getId() : (op.getCreatedBy() != null ? op.getCreatedBy().getId() : null))
                .utilisateurNom(op.getUtilisateur() != null ? op.getUtilisateur().getNomComplet() : (op.getCreatedBy() != null ? op.getCreatedBy().getNomComplet() : null))
                .roleUtilisateur(op.getRoleUtilisateur())
                .typeOperation(op.getTypeOperation())
                .categorie(op.getCategorieOperation())
                .natureFinancement(op.getNatureFinancement())
                .source(op.getSource())
                .montant(op.getMontant())
                .devise(op.getDevise())
                .soldeApresOperation(op.getSoldeApresOperation())
                .commentaire(op.getCommentaire() != null ? op.getCommentaire() : op.getObservation())
                .referenceMetier(op.getReferenceMetier() != null ? op.getReferenceMetier() : op.getReferenceExterne())
                .recetteId(op.getRecetteId())
                .depenseCaisseId(op.getDepenseCaisseId())
                .creditId(op.getCredit() != null ? op.getCredit().getId() : null)
                .retraitEpargneId(op.getRetraitEpargneId())
                .operationEpargneId(op.getOperationEpargne() != null ? op.getOperationEpargne().getId() : null)
                .statutSession(op.getSessionCaisse() != null && op.getSessionCaisse().getStatut() != null ? op.getSessionCaisse().getStatut().name() : null)
                .build();
    }
}