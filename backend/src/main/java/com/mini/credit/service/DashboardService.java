package com.mini.credit.service;

import com.mini.credit.dto.dashboard.CaisseDashboardResponse;
import com.mini.credit.dto.dashboard.DashboardGlobalResponse;
import com.mini.credit.dto.dashboard.PortefeuilleDashboardResponse;
import com.mini.credit.dto.dashboard.RetardDashboardResponse;

import java.time.LocalDate;

public interface DashboardService {
    PortefeuilleDashboardResponse getPortefeuilleDashboard();
    RetardDashboardResponse getRetardDashboard(LocalDate dateReference);
    CaisseDashboardResponse getCaisseDashboard(Long sessionCaisseId);
    DashboardGlobalResponse getDashboardGlobal(Long sessionCaisseId, LocalDate dateReference);
}