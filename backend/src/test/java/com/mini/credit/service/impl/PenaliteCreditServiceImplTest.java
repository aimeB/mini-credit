package com.mini.credit.service.impl;

import com.mini.credit.mapper.PenaliteCreditMapper;
import com.mini.credit.repository.credit.CreditRepository;
import com.mini.credit.repository.credit.EcheanceCreditRepository;
import com.mini.credit.repository.credit.PenaliteCreditRepository;
import com.mini.credit.service.ParametreMetierService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PenaliteCreditServiceImplTest {

    @Mock private CreditRepository creditRepository;
    @Mock private EcheanceCreditRepository echeanceCreditRepository;
    @Mock private PenaliteCreditRepository penaliteCreditRepository;
    @Mock private PenaliteCreditMapper penaliteCreditMapper;
    @Mock private ParametreMetierService parametreMetierService;

    @InjectMocks private PenaliteCreditServiceImpl service;

    @Test
    void penaliteRetardParJour2500() {
        when(parametreMetierService.getDecimal("PENALITE_RETARD_JOURNALIERE")).thenReturn(new BigDecimal("2500"));

        BigDecimal penalite = service.calculerMontantPenalite(3L);

        assertThat(penalite).isEqualByComparingTo("7500");
    }

    @Test
    void aucunRetardPasDePenalite() {
        assertThat(service.calculerMontantPenalite(0L)).isEqualByComparingTo("0");
        assertThat(service.calculerMontantPenalite(null)).isEqualByComparingTo("0");
    }
}