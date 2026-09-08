package com.mini.credit.dto.dashboard;


import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PortefeuilleDashboardResponse {
    private long totalCredits;
    private long creditsActifs;
    private long creditsRembourses;
    private long creditsEnRetard;
    private BigDecimal montantTotalOctroye;
    private BigDecimal encoursPrincipal;
    private BigDecimal interetsTotaux;
    private BigDecimal penalitesTotales;
}