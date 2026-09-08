package com.mini.credit.service;

import java.math.BigDecimal;

public interface RetraitEpargneCommissionService {

    CommissionRetrait calculer(String devise, BigDecimal montantRetrait);

    void verifierContinuiteGrilles();

    record CommissionRetrait(
            String devise,
            BigDecimal montantRetrait,
            BigDecimal tauxPourcentage,
            BigDecimal commission,
            BigDecimal montantTotalDebite,
            BigDecimal montantMin,
            BigDecimal montantMax
    ) {}
}