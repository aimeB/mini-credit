package com.mini.credit.controller;

import com.mini.credit.dto.dashboard.controleinterne.DashboardControleInterneResponse;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.enums.security.AuditModule;
import com.mini.credit.service.DashboardControleInterneService;
import com.mini.credit.service.audit.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard Controle Interne", description = "Supervision transversale consultative basee sur les statuts existants")
@SecurityRequirement(name = "bearer-jwt")
public class DashboardControleInterneController {

    private final DashboardControleInterneService dashboardControleInterneService;
    private final AuditService auditService;

    @GetMapping("/controle-interne")
    @PreAuthorize("hasAnyAuthority('DASHBOARD_CONTROLE_INTERNE_READ')")
    @Operation(summary = "Dashboard controle interne", description = "Vue consolidée des alertes operationnelles et indicateurs de controle")
    public DashboardControleInterneResponse getDashboardControleInterne(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(required = false) Long siteId,
            @RequestParam(required = false) Long caisseId
    ) {
        DashboardControleInterneResponse response = dashboardControleInterneService.getDashboardControleInterne(
                date,
                dateDebut,
                dateFin,
                siteId,
                caisseId
        );

        auditService.logBusinessEvent(
                AuditAction.RAPPORT_GENERE,
                AuditModule.CONTROLE_INTERNE,
                "DashboardControleInterne",
                null,
                true,
                "Consultation dashboard controle interne",
                "DASHBOARD-CONTROLE-INTERNE"
        );

        return response;
    }
}
