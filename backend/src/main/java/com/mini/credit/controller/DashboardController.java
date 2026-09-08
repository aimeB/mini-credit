package com.mini.credit.controller;

import com.mini.credit.dto.dashboard.CaisseDashboardResponse;
import com.mini.credit.dto.dashboard.DashboardGlobalResponse;
import com.mini.credit.dto.dashboard.PortefeuilleDashboardResponse;
import com.mini.credit.dto.dashboard.RetardDashboardResponse;
import com.mini.credit.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard analytics endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/portefeuille")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'GESTIONNAIRE', 'AGENT_TERRAIN')")
    @Operation(summary = "Get portfolio dashboard", description = "Retrieve portfolio analytics")
    public PortefeuilleDashboardResponse portefeuille() {
        return dashboardService.getPortefeuilleDashboard();
    }

    @GetMapping("/retard")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'GESTIONNAIRE', 'AGENT_TERRAIN')")
    @Operation(summary = "Get overdue dashboard", description = "Retrieve overdue credits analytics")
    public RetardDashboardResponse retard(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateReference
    ) {
        return dashboardService.getRetardDashboard(dateReference);
    }

    @GetMapping("/caisse/{sessionCaisseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'CAISSIER')")
    @Operation(summary = "Get caisse dashboard", description = "Retrieve caisse session analytics")
    public CaisseDashboardResponse caisse(@PathVariable Long sessionCaisseId) {
        return dashboardService.getCaisseDashboard(sessionCaisseId);
    }

    @GetMapping("/global")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU')")
    @Operation(summary = "Get global dashboard", description = "Retrieve global system analytics")
    public DashboardGlobalResponse global(
            @RequestParam @NotNull Long sessionCaisseId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateReference
    ) {
        return dashboardService.getDashboardGlobal(sessionCaisseId, dateReference);
    }
}


