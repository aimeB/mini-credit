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
public class MouvementNonRevenuParAntenneDto {
    private Long antenneId;
    private String antenneNom;
    @Builder.Default
    private BigDecimal epargneCollectee = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal principalCreditRembourse = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal garantiesDepotGarantie = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal approvisionnementsCaisse = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal retraitsEpargne = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal decaissementsCredit = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal autresMouvementsNonRevenus = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal totalHorsRevenus = BigDecimal.ZERO;
}
