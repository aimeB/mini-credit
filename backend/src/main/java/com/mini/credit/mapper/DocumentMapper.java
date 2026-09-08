package com.mini.credit.mapper;

import com.mini.credit.dto.document.ContratCreditResponse;
import com.mini.credit.dto.document.QuittanceResponse;
import com.mini.credit.entity.credit.ContratCredit;
import com.mini.credit.entity.document.Quittance;
import org.springframework.stereotype.Component;

@Component
public class DocumentMapper {

    public ContratCreditResponse toResponse(ContratCredit contrat) {
        return ContratCreditResponse.builder()
                .id(contrat.getId())
                .creditId(contrat.getCredit().getId())
                .numeroContrat(contrat.getNumeroContrat())
                .dateSignature(contrat.getDateSignature())
                .lieuSignature(contrat.getLieuSignature())
                .objetContrat(contrat.getObjetContrat())
                .clausesSpecifiques(contrat.getClausesSpecifiques())
                .fichierUrl(contrat.getFichierUrl())
                .signeParMembre(contrat.getSigneParMembre())
                .signeParInstitution(contrat.getSigneParInstitution())
                .nomSignataireInstitution(contrat.getNomSignataireInstitution())
                .fonctionSignataireInstitution(contrat.getFonctionSignataireInstitution())
                .createdAt(contrat.getDateCreation())
                .updatedAt(contrat.getDateModification())
                .build();
    }

    public QuittanceResponse toResponse(Quittance quittance) {
        return QuittanceResponse.builder()
                .id(quittance.getId())
                .numeroQuittance(quittance.getNumeroQuittance())
                .membreId(quittance.getMembre() != null ? quittance.getMembre().getId() : null)
                .membreNomComplet(quittance.getMembre() != null ? quittance.getMembre().getNomComplet() : null)
                .typeQuittance(quittance.getTypeQuittance())
                .referenceOperation(quittance.getReferenceOperation())
                .montant(quittance.getMontant())
                .devise(quittance.getDevise())
                .dateEmission(quittance.getDateEmission())
                .fichierUrl(quittance.getFichierUrl())
                .createdAt(quittance.getDateCreation())
                .updatedAt(quittance.getDateModification())
                .build();
    }
}
