package com.mini.credit.dto.rapport;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KPIDashboardDTO {
    private long totalClients;
    private long totalCreditActifs;
    private BigDecimal totalPortefeuilleActuel;
    private BigDecimal tauxDefautPortefeuille;
    private BigDecimal revenus30Jours;
    private BigDecimal depenses30Jours;
    private BigDecimal benefice30Jours;
    private BigDecimal epargneCollectee;
}
