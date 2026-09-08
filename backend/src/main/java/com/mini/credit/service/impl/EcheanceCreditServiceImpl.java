package com.mini.credit.service.impl;

import com.mini.credit.dto.credit.EcheanceCreditResponse;
import com.mini.credit.entity.credit.Credit;
import com.mini.credit.entity.credit.EcheanceCredit;
import com.mini.credit.exception.ResourceNotFoundException;
import com.mini.credit.repository.credit.CreditRepository;
import com.mini.credit.repository.credit.EcheanceCreditRepository;
import com.mini.credit.service.EcheanceCreditService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EcheanceCreditServiceImpl implements EcheanceCreditService {

    private final CreditRepository creditRepository;
    private final EcheanceCreditRepository echeanceCreditRepository;

    @Override
    public List<EcheanceCreditResponse> getByCreditId(Long creditId) {
        Credit credit = creditRepository.findById(creditId)
                .orElseThrow(() -> new ResourceNotFoundException("Crédit introuvable"));

        List<EcheanceCredit> echeances = echeanceCreditRepository.findByCreditIdOrderByNumeroEcheanceAsc(credit.getId());

        return echeances.stream()
                .map(this::toResponse)
                .toList();
    }

    private EcheanceCreditResponse toResponse(EcheanceCredit echeance) {
        BigDecimal principalRestant = echeance.getPrincipalPrevu()
                .subtract(echeance.getPrincipalPaye())
                .max(BigDecimal.ZERO);

        BigDecimal interetRestant = echeance.getInteretPrevu()
                .subtract(echeance.getInteretPaye())
                .max(BigDecimal.ZERO);

        BigDecimal penaliteRestante = echeance.getPenaliteCumulee()
                .subtract(echeance.getPenalitePayee())
                .max(BigDecimal.ZERO);

        return EcheanceCreditResponse.builder()
                .id(echeance.getId())
                .numeroEcheance(echeance.getNumeroEcheance())
                .dateEcheance(echeance.getDateEcheance())
                .principalPrevu(echeance.getPrincipalPrevu())
                .interetPrevu(echeance.getInteretPrevu())
                .penaliteCumulee(echeance.getPenaliteCumulee())
                .totalPrevu(echeance.getTotalPrevu())
                .principalPaye(echeance.getPrincipalPaye())
                .interetPaye(echeance.getInteretPaye())
                .penalitePayee(echeance.getPenalitePayee())
                .totalPaye(echeance.getTotalPaye())
                .resteAPayer(echeance.getResteAPayer())
                .principalRestant(principalRestant)
                .interetRestant(interetRestant)
                .penaliteRestante(penaliteRestante)
                .dateDernierPaiement(echeance.getDateDernierPaiement())
                .statut(echeance.getStatut())
                .build();
    }
}