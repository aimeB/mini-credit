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
public class MasseSalarialeDto {
    @Builder.Default
    private BigDecimal masseSalarialeMensuellePrevue = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal salairesPayes = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal salairesRestantAPayer = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal resultatPrevisionnelApresSalairesAPayer = BigDecimal.ZERO;
    @Builder.Default
    private Integer nombreEmployesActifs = 0;
    private String periodePaie;
    @Builder.Default
    private Boolean paiementSuperieurAuPrevu = false;
    private String alerte;
    @Builder.Default
    private List<DetailMasseSalarialeDto> detailsEmployes = new ArrayList<>();
}