package com.mini.credit.dto.referentiel;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollecteRecapResponse {
    private Long collecteId;
    private Integer membresVisites;
    private Integer nouveauxMembres;
    private Integer carnetsVendusDistribues;
    private BigDecimal totalEpargne;
    private BigDecimal totalRemboursements;
    private BigDecimal totalFrais;
    private BigDecimal totalGeneralAttendu;
    private BigDecimal especesRemises;
    private BigDecimal ecartTresorerie;
}