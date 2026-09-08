package com.mini.credit.dto.rapport.revenus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenuKpiDto {
    @Builder.Default
    private BigDecimal fraisAnalyseCredit = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal fraisRetraitEpargne = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal interetsCredit = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal penalitesCredit = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal carnetsVendus = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal revenusDivers = BigDecimal.ZERO;
}
