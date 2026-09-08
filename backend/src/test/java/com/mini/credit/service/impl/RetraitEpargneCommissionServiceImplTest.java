package com.mini.credit.service.impl;

import com.mini.credit.exception.BusinessException;
import com.mini.credit.service.RetraitEpargneCommissionService.CommissionRetrait;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RetraitEpargneCommissionServiceImplTest {

    private RetraitEpargneCommissionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RetraitEpargneCommissionServiceImpl();
    }

    @Test
    void grillesRetraitSontContinuesSansTrouNiChevauchement() {
        assertDoesNotThrow(() -> service.verifierContinuiteGrilles());
    }

    @Test
    void retraitCdf999Refuse() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.calculer("CDF", new BigDecimal("999.00")));

        assertEquals("Le montant minimum de retrait est de 1 000 CDF.", exception.getMessage());
    }

    @Test
    void retraitCdf1000AccepteEtApplique9Pourcent() {
        CommissionRetrait commission = service.calculer("CDF", new BigDecimal("1000.00"));

        assertEquals(new BigDecimal("9.00"), commission.tauxPourcentage());
        assertEquals(new BigDecimal("90.00"), commission.commission());
        assertEquals(new BigDecimal("1090.00"), commission.montantTotalDebite());
    }

    @Test
    void retraitUsd099Refuse() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.calculer("USD", new BigDecimal("0.99")));

        assertEquals("Le montant minimum de retrait est de 1 USD.", exception.getMessage());
    }

    @Test
    void retraitUsd1AccepteEtApplique9Pourcent() {
        CommissionRetrait commission = service.calculer("USD", new BigDecimal("1.00"));

        assertEquals(new BigDecimal("9.00"), commission.tauxPourcentage());
        assertEquals(new BigDecimal("0.09"), commission.commission());
        assertEquals(new BigDecimal("1.09"), commission.montantTotalDebite());
    }

    @Test
    void appliqueLesTranchesCdfAuxBornesMetier() {
        assertCdfCommission("10000.00", "9.00", "900.00", "10900.00");
        assertCdfCommission("15000.00", "8.00", "1200.00", "16200.00");
        assertCdfCommission("24001.00", "6.00", "1440.06", "25441.06");
        assertCdfCommission("49001.00", "5.50", "2695.06", "51696.06");
        assertCdfCommission("100000.00", "4.50", "4500.00", "104500.00");
        assertCdfCommission("100001.00", "4.00", "4000.04", "104001.04");
        assertCdfCommission("1500001.00", "1.40", "21000.01", "1521001.01");
    }

    private void assertCdfCommission(String montant, String taux, String commissionAttendue, String totalAttendu) {
        CommissionRetrait commission = service.calculer("CDF", new BigDecimal(montant));

        assertEquals(new BigDecimal(taux), commission.tauxPourcentage());
        assertEquals(new BigDecimal(commissionAttendue), commission.commission());
        assertEquals(new BigDecimal(totalAttendu), commission.montantTotalDebite());
    }
}