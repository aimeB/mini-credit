package com.mini.credit.dto.rapport.revenus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyntheseComparaisonAgencesDto {
    @Builder.Default
    private List<ComparaisonAgenceDto> agences = new ArrayList<>();
    @Builder.Default
    private BigDecimal totalRevenusAgences = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal totalChargesAgences = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal totalResultatAgences = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal chargesGlobalesSiege = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal resultatGlobalApresChargesSiege = BigDecimal.ZERO;
    private ComparaisonAgenceDto agencePlusRevenus;
    private ComparaisonAgenceDto agencePlusRentable;
    private ComparaisonAgenceDto agencePlusCharges;
    @Builder.Default
    private List<ComparaisonAgenceDto> agencesDeficitaires = new ArrayList<>();
}
