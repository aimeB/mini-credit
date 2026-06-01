package com.mini.credit.mapper;

import com.mini.credit.dto.credit.CreditResponse;
import com.mini.credit.dto.credit.DemandeCreditResponse;
import com.mini.credit.entity.credit.Credit;
import com.mini.credit.entity.credit.DemandeCredit;
import org.springframework.stereotype.Component;

@Component
public class CreditMapper {

    public DemandeCreditResponse toResponse(DemandeCredit demande) {
        return DemandeCreditResponse.builder()
                .id(demande.getId())
                .numeroDemande(demande.getNumeroDemande())
                .membreId(demande.getMembre() != null ? demande.getMembre().getId() : null)
                .membreNomComplet(demande.getMembre() != null ? demande.getMembre().getNomComplet() : null)
                .siteId(demande.getSite() != null ? demande.getSite().getId() : null)
                .siteNom(demande.getSite() != null ? demande.getSite().getNomSite() : null)
                .agentId(demande.getAgent() != null ? demande.getAgent().getId() : null)
                .dateDemande(demande.getDateDemande())
                .montantDemande(demande.getMontantDemande())
                .fraisDemandePayes(demande.getFraisDemandePayes())
                .devise(demande.getDevise())
                .dureeValeur(demande.getDureeValeur())
                .dureeUnite(demande.getDureeUnite())
                .periodiciteRemboursement(demande.getPeriodiciteRemboursement())
                .tauxInteret(demande.getTauxInteret())
                .objetCredit(demande.getObjetCredit())
                .activiteFinancee(demande.getActiviteFinancee())
                .revenusEstimes(demande.getRevenusEstimes())
                .chargesEstimees(demande.getChargesEstimees())
                .fraisDemande(demande.getFraisDemande())
                .depotGarantieRequis(demande.getDepotGarantieRequis())
                .depotGarantiePaye(demande.getDepotGarantiePaye())
                .statut(demande.getStatut())
                .commentaireDecision(demande.getCommentaireDecision())
                .dateDecision(demande.getDateDecision())
                .createdAt(demande.getDateCreation())
                .updatedAt(demande.getDateModification())
                .build();
    }

    public CreditResponse toResponse(Credit credit) {
        return CreditResponse.builder()
                .id(credit.getId())
                .numeroCredit(credit.getNumeroCredit())
                .demandeCreditId(credit.getDemandeCredit() != null ? credit.getDemandeCredit().getId() : null)
                .membreId(credit.getMembre() != null ? credit.getMembre().getId() : null)
                .membreNomComplet(credit.getMembre() != null ? credit.getMembre().getNomComplet() : null)
                .siteId(credit.getSite() != null ? credit.getSite().getId() : null)
                .siteNom(credit.getSite() != null ? credit.getSite().getNomSite() : null)
                .dateApprobation(credit.getDateApprobation())
                .dateDecaissement(credit.getDateDecaissement())
                .montantOctroye(credit.getMontantOctroye())
                .devise(credit.getDevise())
                .tauxInteret(credit.getTauxInteret())
                .dureeValeur(credit.getDureeValeur())
                .dureeUnite(credit.getDureeUnite())
                .periodiciteRemboursement(credit.getPeriodiciteRemboursement())
                .nombreEcheances(credit.getNombreEcheances())
                .principalTotal(credit.getPrincipalTotal())
                .interetTotal(credit.getInteretTotal())
                .penaliteTotal(credit.getPenaliteTotal())
                .totalARembourser(credit.getTotalARembourser())
                .encoursPrincipal(credit.getEncoursPrincipal())
                .statut(credit.getStatut())
                .motifContentieux(credit.getMotifContentieux())
                .createdAt(credit.getDateCreation())
                .updatedAt(credit.getDateModification())
                .build();
    }
}