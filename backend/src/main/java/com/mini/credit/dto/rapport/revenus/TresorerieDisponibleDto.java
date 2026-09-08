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
public class TresorerieDisponibleDto {
    @Builder.Default
    private BigDecimal soldeCaisseTheoriqueActif = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal retraitsEpargneValidesNonPayes = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal fondsMembresAProteger = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal tresorerieApresProtectionMembres = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal creditsApprouvesNonDecaisses = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal depensesValideesNonPayees = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal salairesRestantAPayer = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal transportRestantAPayer = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal fondsMinimumSecurite = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal margePrudence = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal totalEngagementsCourtTerme = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal tresorerieDisponibleApresEngagements = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal tresorerieRecuperablePrudente = BigDecimal.ZERO;
    @Builder.Default
    private Boolean soldeCaisseIndicatif = true;
    private String commentairePedagogique;
}
