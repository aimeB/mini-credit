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
public class TransportFixePrevuDto {
    @Builder.Default
    private BigDecimal totalTransportPrevu = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal totalTransportPaye = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal totalTransportRestant = BigDecimal.ZERO;
    @Builder.Default
    private Integer nombreAgentsTerrain = 0;
    @Builder.Default
    private Integer nombreSitesConfigures = 0;
    @Builder.Default
    private Integer nombreSitesSansMontant = 0;
    @Builder.Default
    private Integer nombreJoursPeriode = 0;
    private String periodeCharge;
    private String commentaireCalcul;
    @Builder.Default
    private List<TransportFixeSiteDetailDto> detailsParSite = new ArrayList<>();
    @Builder.Default
    private List<TransportFixeDetailDto> details = new ArrayList<>();
}
