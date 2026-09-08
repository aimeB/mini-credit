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
public class PositionCreditDto {
    @Builder.Default
    private BigDecimal capitalDecaisse = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal principalRecupere = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal capitalRestantDehors = BigDecimal.ZERO;
    @Builder.Default
    private Boolean capitalRestantEstime = true;
    @Builder.Default
    private BigDecimal interetsEncaisses = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal penalitesEncaisses = BigDecimal.ZERO;
    @Builder.Default
    private Long nombreCreditsActifs = 0L;
    @Builder.Default
    private Long nombreCreditsRembourses = 0L;
    private String commentairePedagogique;
}