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
public class ApportFinancementDto {
    @Builder.Default
    private BigDecimal totalApprovisionnements = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal apportsProprietaire = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal transfertsInternes = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal pretsRecus = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal remboursementsAvance = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal autresFinancements = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal approvisionnementsNonQualifies = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal capitalInjecteARecuperer = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal remboursementsApportPayes = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal capitalInjecteRestantARecuperer = BigDecimal.ZERO;
    @Builder.Default
    private Boolean capitalInjecteIndicatif = true;
    private String commentairePedagogique;
}
