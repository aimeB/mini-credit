package com.mini.credit.service.impl;

import com.mini.credit.dto.credit.PenaliteCreditResponse;
import com.mini.credit.dto.credit.PenaliteEcheanceResponse;
import com.mini.credit.entity.credit.Credit;
import com.mini.credit.entity.credit.EcheanceCredit;
import com.mini.credit.enums.StatutEcheance;
import com.mini.credit.exception.ResourceNotFoundException;
import com.mini.credit.repository.credit.CreditRepository;
import com.mini.credit.repository.credit.EcheanceCreditRepository;
import com.mini.credit.service.PenaliteService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PenaliteServiceImpl implements PenaliteService {

    @Value("${app.credit.penalite-journaliere:2500}")
    private BigDecimal penaliteJournaliere;

    private final CreditRepository creditRepository;
    private final EcheanceCreditRepository echeanceCreditRepository;

    @Override
    public PenaliteCreditResponse calculerPenalitesCredit(Long creditId, LocalDate dateReference) {
        Credit credit = creditRepository.findById(creditId)
                .orElseThrow(() -> new ResourceNotFoundException("Crédit introuvable"));

        List<EcheanceCredit> echeances = echeanceCreditRepository.findByCreditIdOrderByNumeroEcheanceAsc(creditId);

        List<PenaliteEcheanceResponse> details = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (EcheanceCredit echeance : echeances) {
            BigDecimal penaliteCalculee = calculerPenalite(echeance, dateReference);

            BigDecimal resteAPayerCalcule = nvl(echeance.getPrincipalPrevu())
                    .add(nvl(echeance.getInteretPrevu()))
                    .add(penaliteCalculee)
                    .subtract(nvl(echeance.getTotalPaye()))
                    .max(BigDecimal.ZERO);

            if (resteAPayerCalcule.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            if (penaliteCalculee.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            long joursRetard = ChronoUnit.DAYS.between(echeance.getDateEcheance(), dateReference);

            details.add(PenaliteEcheanceResponse.builder()
                    .echeanceId(echeance.getId())
                    .numeroEcheance(echeance.getNumeroEcheance())
                    .dateEcheance(echeance.getDateEcheance())
                    .joursRetard(joursRetard)
                    .penaliteCumulee(penaliteCalculee)
                    .resteAPayer(resteAPayerCalcule)
                    .build());

            total = total.add(penaliteCalculee);
        }

        return PenaliteCreditResponse.builder()
                .creditId(credit.getId())
                .numeroCredit(credit.getNumeroCredit())
                .penaliteTotaleCalculee(total)
                .echeancesEnRetard(details)
                .build();
    }

    @Override
    public void appliquerPenalitesCredit(Long creditId, LocalDate dateReference) {
        Credit credit = creditRepository.findById(creditId)
                .orElseThrow(() -> new ResourceNotFoundException("Crédit introuvable"));

        List<EcheanceCredit> echeances = echeanceCreditRepository.findByCreditIdOrderByNumeroEcheanceAsc(creditId);

        BigDecimal penaliteTotaleRestante = BigDecimal.ZERO;

        for (EcheanceCredit echeance : echeances) {
            appliquerCalculSurEcheance(echeance, dateReference);

            BigDecimal penaliteRestante = nvl(echeance.getPenaliteCumulee())
                    .subtract(nvl(echeance.getPenalitePayee()))
                    .max(BigDecimal.ZERO);

            penaliteTotaleRestante = penaliteTotaleRestante.add(penaliteRestante);
        }

        echeanceCreditRepository.saveAll(echeances);

        credit.setPenaliteTotal(penaliteTotaleRestante);
        creditRepository.save(credit);
    }

    private void appliquerCalculSurEcheance(EcheanceCredit echeance, LocalDate dateReference) {
        BigDecimal penalite = calculerPenalite(echeance, dateReference);

        echeance.setPenaliteCumulee(penalite);

        BigDecimal resteAPayer = nvl(echeance.getPrincipalPrevu())
                .add(nvl(echeance.getInteretPrevu()))
                .add(nvl(echeance.getPenaliteCumulee()))
                .subtract(nvl(echeance.getTotalPaye()))
                .max(BigDecimal.ZERO);

        echeance.setResteAPayer(resteAPayer);

        if (resteAPayer.compareTo(BigDecimal.ZERO) == 0) {
            echeance.setStatut(StatutEcheance.PAYE);
        } else if (penalite.compareTo(BigDecimal.ZERO) > 0) {
            echeance.setStatut(StatutEcheance.EN_RETARD);
        } else {
            echeance.setStatut(StatutEcheance.A_PAYER);
        }
    }

    private BigDecimal calculerPenalite(EcheanceCredit echeance, LocalDate dateReference) {
        BigDecimal resteSansNouvellePenalite = nvl(echeance.getPrincipalPrevu())
                .add(nvl(echeance.getInteretPrevu()))
                .subtract(nvl(echeance.getTotalPaye()))
                .max(BigDecimal.ZERO);

        if (resteSansNouvellePenalite.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        if (!dateReference.isAfter(echeance.getDateEcheance())) {
            return BigDecimal.ZERO;
        }

        long joursRetard = ChronoUnit.DAYS.between(echeance.getDateEcheance(), dateReference);
        return penaliteJournaliere.multiply(BigDecimal.valueOf(joursRetard));
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}