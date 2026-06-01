package com.mini.credit.dto.rapport;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class BilanMensuelDTO {
    private LocalDate mois;
    private BigDecimal totalEncaissementsMois;
    private BigDecimal totalDecaissementsMois;
    private BigDecimal soldeMois;
    private BigDecimal portefeuillesCredits;
    private BigDecimal tauxRecouvrement;
}