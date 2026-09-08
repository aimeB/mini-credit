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
public class CarnetMargeDto {
    @Builder.Default
    private Integer nombreCarnetsVendus = 0;
    @Builder.Default
    private BigDecimal montantVentesCarnets = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal coutAchatUnitaireCarnet = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal coutTotalCarnets = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal margeCarnets = BigDecimal.ZERO;
}
