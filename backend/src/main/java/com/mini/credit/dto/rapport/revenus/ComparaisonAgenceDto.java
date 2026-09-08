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
public class ComparaisonAgenceDto {
    private Long agenceId;
    private String agenceNom;
    @Builder.Default
    private BigDecimal revenusReels = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal chargesConnues = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal resultatNetEstime = BigDecimal.ZERO;
    private BigDecimal margePourcentage;
    @Builder.Default
    private BigDecimal commissionsRetrait = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal fraisCredit = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal interetsCredit = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal penalitesCredit = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal carnetsVendus = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal autresRevenus = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal salairesPayes = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal salairesRestantAPayer = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal primes = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal transportTerrain = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal transportRestantAPayer = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal fonctionnement = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal achatCarnets = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal autresCharges = BigDecimal.ZERO;
    @Builder.Default
    private Long collectesValidees = 0L;
    @Builder.Default
    private Long membresActifs = 0L;
    @Builder.Default
    private Long creditsActifs = 0L;
    @Builder.Default
    private Boolean alerteDeficit = false;
    @Builder.Default
    private Boolean alerteChargesElevees = false;
}
