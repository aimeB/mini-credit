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
public class TransportFixeSiteDetailDto {
    private Long siteId;
    private String siteNom;
    private Long agenceId;
    private String agenceNom;
    private BigDecimal montantJournalierParAgent;
    private Integer nombreAgentsTerrainActifs;
    private Integer nombreJoursPeriode;
    private BigDecimal transportPrevuSite;
    private BigDecimal transportPayeSite;
    private BigDecimal transportRestantSite;
}
