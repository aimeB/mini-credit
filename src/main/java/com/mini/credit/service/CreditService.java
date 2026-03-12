package com.mini.credit.service;

import com.mini.credit.entity.credit.Credit;
import com.mini.credit.entity.credit.DemandeCredit;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.enums.StatutCredit;
import com.mini.credit.enums.StatutDemandeCredit;
import com.mini.credit.repository.credit.CreditRepository;
import com.mini.credit.repository.credit.DemandeCreditRepository;
import com.mini.credit.repository.membre.MembreRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CreditService {

    private final CreditRepository creditRepository;
    private final DemandeCreditRepository demandeCreditRepository;
    private final MembreRepository membreRepository;

    @Transactional
    public Credit approuverEtCreerCredit(Long demandeId) {
        DemandeCredit demande = demandeCreditRepository.findById(demandeId)
                .orElseThrow(() -> new IllegalArgumentException("Demande introuvable"));

        Membre membre = demande.getMembre();

        boolean hasActiveCredit = creditRepository.existsByMembreIdAndStatutIn(
                membre.getId(),
                List.of(
                        StatutCredit.APPROUVE,
                        StatutCredit.DECAISSE,
                        StatutCredit.EN_COURS,
                        StatutCredit.EN_RETARD,
                        StatutCredit.CONTENTIEUX
                )
        );

        if (hasActiveCredit) {
            throw new IllegalStateException("Le membre a déjà un crédit actif");
        }

        if (demande.getDepotGarantieRequis().compareTo(BigDecimal.ZERO) > 0
                && demande.getDepotGarantiePaye().compareTo(demande.getDepotGarantieRequis()) < 0) {
            throw new IllegalStateException("Le dépôt de garantie requis n'est pas totalement payé");
        }

        if (demande.getAnalyseRisque() == null) {
            throw new IllegalStateException("Aucune analyse de risque liée à cette demande");
        }

        Credit credit = Credit.builder()
                .numeroCredit("CR-" + System.currentTimeMillis())
                .demandeCredit(demande)
                .membre(membre)
                .site(demande.getSite())
                .dateApprobation(LocalDate.now())
                .montantOctroye(demande.getMontantDemande())
                .devise(demande.getDevise())
                .tauxInteret(demande.getTauxInteret())
                .dureeValeur(demande.getDureeValeur())
                .dureeUnite(demande.getDureeUnite())
                .periodiciteRemboursement(demande.getPeriodiciteRemboursement())
                .nombreEcheances(demande.getDureeValeur())
                .principalTotal(demande.getMontantDemande())
                .interetTotal(BigDecimal.ZERO)
                .penaliteTotal(BigDecimal.ZERO)
                .totalARembourser(demande.getMontantDemande())
                .encoursPrincipal(demande.getMontantDemande())
                .statut(StatutCredit.APPROUVE)
                .build();

        demande.setStatut(StatutDemandeCredit.APPROUVEE);

        demandeCreditRepository.save(demande);
        return creditRepository.save(credit);
    }
}
