package com.mini.credit.dto.dashboard;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class RetardDashboardResponse {
    private long nombreCreditsEnRetard;
    private long nombreEcheancesEnRetard;
    private BigDecimal montantTotalEnRetard;
    private BigDecimal penalitesCumulees;
    private List<RetardEcheanceItemResponse> echeances;
}