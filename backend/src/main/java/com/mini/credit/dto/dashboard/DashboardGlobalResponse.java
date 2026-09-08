package com.mini.credit.dto.dashboard;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardGlobalResponse {
    private PortefeuilleDashboardResponse portefeuille;
    private RetardDashboardResponse retard;
    private CaisseDashboardResponse caisse;
}