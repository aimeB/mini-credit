package com.mini.credit.mapper;

import com.mini.credit.dto.caisse.DepenseCaisseResponse;
import com.mini.credit.entity.caisse.DepenseCaisse;
import org.springframework.stereotype.Component;

@Component
public class DepenseCaisseMapper {

    public DepenseCaisseResponse toResponse(DepenseCaisse depense) {
        if (depense == null) {
            return null;
        }

        return DepenseCaisseResponse.builder()
                .id(depense.getId())
                .sessionCaisseId(depense.getSessionCaisse() != null ? depense.getSessionCaisse().getId() : null)
                .caisseId(depense.getCaisse() != null ? depense.getCaisse().getId() : null)
                .caisseCode(depense.getCaisse() != null ? depense.getCaisse().getCodeCaisse() : null)
                .siteId(depense.getSite() != null ? depense.getSite().getId() : null)
                .siteNom(depense.getSite() != null ? depense.getSite().getNomSite() : null)
                .categorie(depense.getCategorie())
                .montant(depense.getMontant())
                .devise(depense.getDevise())
                .motif(depense.getMotif())
                .beneficiaire(depense.getBeneficiaire())
                .beneficiaireId(depense.getBeneficiaireUtilisateur() != null ? depense.getBeneficiaireUtilisateur().getId() : null)
                .beneficiaireNom(depense.getBeneficiaireNom())
                .beneficiaireRole(depense.getBeneficiaireRole())
                .beneficiaireAgence(depense.getBeneficiaireAgence())
                .employeId(depense.getEmploye() != null ? depense.getEmploye().getId() : null)
                .employeMatricule(depense.getEmploye() != null ? depense.getEmploye().getMatricule() : null)
                .employeNomComplet(depense.getEmploye() != null ? depense.getEmploye().getNomComplet() : null)
                .employePoste(depense.getEmploye() != null && depense.getEmploye().getFonction() != null ? depense.getEmploye().getFonction().name() : null)
                .salaireBase(depense.getEmploye() != null ? depense.getEmploye().getSalaireBase() : null)
                .primeFixe(depense.getEmploye() != null ? depense.getEmploye().getPrimeFixe() : null)
                .bonusVariable(depense.getEmploye() != null ? depense.getEmploye().getBonusVariable() : null)
                .periodePaie(depense.getPeriodePaie())
                .typePaiementPersonnel(depense.getTypePaiementPersonnel())
                .montantRemunerationReference(depense.getMontantRemunerationReference())
                .montantEcartRemuneration(depense.getMontantEcartRemuneration())
                .motifEcartRemuneration(depense.getMotifEcartRemuneration())
                .naturePaiementPaie(depense.getNaturePaiementPaie())
                .montantSalaireDu(depense.getMontantSalaireDu())
                .montantDejaPaye(depense.getMontantDejaPaye())
                .montantRestantApresPaiement(depense.getMontantRestantApresPaiement())
                .montantRetenue(depense.getMontantRetenue())
                .motifRetenue(depense.getMotifRetenue())
                .motifPaiementPartiel(depense.getMotifPaiementPartiel())
                .commentairePaie(depense.getCommentairePaie())
                .periodeCharge(depense.getPeriodeCharge())
                .typeChargeFixe(depense.getTypeChargeFixe())
                .siteChargeId(depense.getSiteCharge() != null ? depense.getSiteCharge().getId() : null)
                .siteChargeNom(depense.getSiteCharge() != null ? depense.getSiteCharge().getNomSite() : null)
                .montantChargeFixeReference(depense.getMontantChargeFixeReference())
                .montantEcartChargeFixe(depense.getMontantEcartChargeFixe())
                .commentaireRapprochement(depense.getCommentaireRapprochement())
                .epargneCollecteeReference(depense.getEpargneCollecteeReference())
                .remboursementCollecteReference(depense.getRemboursementCollecteReference())
                .nombreCarnetsVendus(depense.getNombreCarnetsVendus())
                .primeMobilisationEpargne(depense.getPrimeMobilisationEpargne())
                .primeMobilisationRemboursement(depense.getPrimeMobilisationRemboursement())
                .bonusCarnets(depense.getBonusCarnets())
                .primeMotivationManuelle(depense.getPrimeMotivationManuelle())
                .motifPrimeMotivationManuelle(depense.getMotifPrimeMotivationManuelle())
                .modeCalculPaie(depense.getModeCalculPaie())
                .detailCalculPaieJson(depense.getDetailCalculPaieJson())
                .justificatifUrl(depense.getJustificatifUrl())
                .statut(depense.getStatut())
                .demandeParId(depense.getDemandePar() != null ? depense.getDemandePar().getId() : null)
                .demandeParNom(depense.getDemandePar() != null ? depense.getDemandePar().getNomComplet() : null)
                .valideParId(depense.getValidePar() != null ? depense.getValidePar().getId() : null)
                .valideParNom(depense.getValidePar() != null ? depense.getValidePar().getNomComplet() : null)
                .payeParId(depense.getPayePar() != null ? depense.getPayePar().getId() : null)
                .payeParNom(depense.getPayePar() != null ? depense.getPayePar().getNomComplet() : null)
                .dateDemande(depense.getDateDemande())
                .dateSoumission(depense.getDateSoumission())
                .dateValidation(depense.getDateValidation())
                .datePaiement(depense.getDatePaiement())
                .operationCaisseId(depense.getOperationCaisse() != null ? depense.getOperationCaisse().getId() : null)
                .commentaireValidation(depense.getCommentaireValidation())
                .motifRejet(depense.getMotifRejet())
                .createdAt(depense.getDateCreation())
                .updatedAt(depense.getDateModification())
                .build();
    }
}