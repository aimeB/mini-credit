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
public class FondsMembresProtegesDto {
    @Builder.Default
    private BigDecimal epargneDisponibleMembres = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal epargneBloqueeGaranties = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal retraitsEpargneValidesNonPayesNonInclus = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal totalFondsMembres = BigDecimal.ZERO;
    private String commentairePedagogique;
}
