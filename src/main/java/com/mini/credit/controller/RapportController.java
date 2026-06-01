package com.mini.credit.controller;

import com.mini.credit.dto.rapport.*;
import com.mini.credit.service.RapportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/rapports")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Rapport", description = "Report and Analytics endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class RapportController {

    private final RapportService rapportService;

    @GetMapping("/bilan-journalier")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESPONSABLE')")
    @Operation(summary = "Get daily balance", description = "Get daily financial balance report")
    public ResponseEntity<BilanJournalierDTO> getBilanJournalier(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        BilanJournalierDTO bilan = rapportService.genererBilanJournalier(date);
        return ResponseEntity.ok(bilan);
    }

    @GetMapping("/bilan-hebdomadaire")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESPONSABLE')")
    @Operation(summary = "Get weekly balance", description = "Get weekly financial balance report")
    public ResponseEntity<BilanHebdomadaireDTO> getBilanHebdomadaire(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut) {
        BilanHebdomadaireDTO bilan = rapportService.genererBilanHebdomadaire(dateDebut);
        return ResponseEntity.ok(bilan);
    }

    @GetMapping("/bilan-mensuel")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESPONSABLE')")
    @Operation(summary = "Get monthly balance", description = "Get monthly financial balance report")
    public ResponseEntity<BilanMensuelDTO> getBilanMensuel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate mois) {
        BilanMensuelDTO bilan = rapportService.genererBilanMensuel(mois);
        return ResponseEntity.ok(bilan);
    }

    // New endpoints for analytics and KPI

    @GetMapping("/kpi-dashboard")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESPONSABLE') or hasRole('AGENT_BUREAU')")
    @Operation(summary = "Get KPI dashboard", description = "Get key performance indicators dashboard")
    public ResponseEntity<KPIDashboardDTO> getKPIDashboard() {
        KPIDashboardDTO kpi = rapportService.getKPIDashboard();
        return ResponseEntity.ok(kpi);
    }

    @GetMapping("/kpi")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESPONSABLE') or hasRole('AGENT_BUREAU')")
    @Operation(summary = "Get KPI dashboard", description = "Get key performance indicators dashboard")
    public ResponseEntity<KPIDashboardDTO> getKPI() {
        KPIDashboardDTO kpi = rapportService.getKPIDashboard();
        return ResponseEntity.ok(kpi);
    }

    @GetMapping("/financier")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESPONSABLE') or hasRole('AGENT_BUREAU')")
    @Operation(summary = "Get financial report", description = "Get financial report for period")
    public ResponseEntity<RapportFinancierDTO> getRapportFinancier(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        RapportFinancierDTO rapport = rapportService.getRapportFinancier(dateDebut, dateFin);
        return ResponseEntity.ok(rapport);
    }

    @GetMapping("/risque")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESPONSABLE') or hasRole('AGENT_BUREAU')")
    @Operation(summary = "Get risk report", description = "Get risk assessment report")
    public ResponseEntity<RapportRisqueDTO> getRapportRisque() {
        RapportRisqueDTO rapport = rapportService.getRapportRisque();
        return ResponseEntity.ok(rapport);
    }

    @GetMapping("/collecte")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESPONSABLE') or hasRole('AGENT_BUREAU')")
    @Operation(summary = "Get collection report", description = "Get collection report for period")
    public ResponseEntity<RapportCollecteDTO> getRapportCollecte(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        RapportCollecteDTO rapport = rapportService.getRapportCollecte(dateDebut, dateFin);
        return ResponseEntity.ok(rapport);
    }
}