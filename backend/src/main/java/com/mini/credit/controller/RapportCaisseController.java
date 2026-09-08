package com.mini.credit.controller;

import com.mini.credit.dto.caisse.rapport.RapportCaisseDepensesDto;
import com.mini.credit.dto.caisse.rapport.RapportCaisseEcartsDto;
import com.mini.credit.dto.caisse.rapport.RapportCaisseJournalierDto;
import com.mini.credit.dto.caisse.rapport.RapportCaissePeriodeDto;
import com.mini.credit.dto.caisse.rapport.RapportCaisseSessionDto;
import com.mini.credit.dto.caisse.rapport.RapportCaisseSyntheseDto;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.service.RapportCaisseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/rapports/caisse")
@RequiredArgsConstructor
@Tag(name = "Rapports Caisse", description = "Rapports caisse, exports et contrôle interne")
@SecurityRequirement(name = "bearerAuth")
public class RapportCaisseController {

    private final RapportCaisseService rapportCaisseService;

    @GetMapping("/session/{sessionId}")
    @PreAuthorize("hasAuthority('RAPPORT_CAISSE_READ')")
    @Operation(summary = "Rapport session caisse", description = "Rapport détaillé d'une session de caisse")
    public RapportCaisseSessionDto getRapportSession(@PathVariable Long sessionId) {
        return rapportCaisseService.getRapportSession(sessionId);
    }

    @GetMapping("/session/{sessionId}/export")
    @PreAuthorize("hasAuthority('RAPPORT_CAISSE_EXPORT')")
    @Operation(summary = "Export session caisse", description = "Export CSV du rapport de session")
    public ResponseEntity<byte[]> exportRapportSession(
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "csv") String format
    ) {
        ensureCsv(format);
        byte[] payload = rapportCaisseService.exportRapportSessionCsv(sessionId);
        return buildCsvResponse(payload, "rapport-caisse-session-" + sessionId + ".csv");
    }

    @GetMapping("/journalier")
    @PreAuthorize("hasAuthority('RAPPORT_CAISSE_READ')")
    @Operation(summary = "Rapport journalier caisse", description = "Synthèse quotidienne caisse")
    public RapportCaisseJournalierDto getRapportJournalier(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long caisseId,
            @RequestParam(required = false) Long siteId
    ) {
        return rapportCaisseService.getRapportJournalier(date, caisseId, siteId);
    }

    @GetMapping("/journalier/export")
    @PreAuthorize("hasAuthority('RAPPORT_CAISSE_EXPORT')")
    @Operation(summary = "Export journalier caisse", description = "Export CSV du rapport journalier")
    public ResponseEntity<byte[]> exportRapportJournalier(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long caisseId,
            @RequestParam(required = false) Long siteId,
            @RequestParam(defaultValue = "csv") String format
    ) {
        ensureCsv(format);
        byte[] payload = rapportCaisseService.exportRapportJournalierCsv(date, caisseId, siteId);
        return buildCsvResponse(payload, "rapport-caisse-journalier-" + date + ".csv");
    }

    @GetMapping("/periode")
    @PreAuthorize("hasAuthority('RAPPORT_CAISSE_READ')")
    @Operation(summary = "Rapport période caisse", description = "Synthèse caisse sur une période")
    public RapportCaissePeriodeDto getRapportPeriode(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(required = false) Long caisseId,
            @RequestParam(required = false) Long siteId
    ) {
        return rapportCaisseService.getRapportPeriode(dateDebut, dateFin, caisseId, siteId);
    }

    @GetMapping("/periode/export")
    @PreAuthorize("hasAuthority('RAPPORT_CAISSE_EXPORT')")
    @Operation(summary = "Export période caisse", description = "Export CSV du rapport période")
    public ResponseEntity<byte[]> exportRapportPeriode(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(required = false) Long caisseId,
            @RequestParam(required = false) Long siteId,
            @RequestParam(defaultValue = "csv") String format
    ) {
        ensureCsv(format);
        byte[] payload = rapportCaisseService.exportRapportPeriodeCsv(dateDebut, dateFin, caisseId, siteId);
        return buildCsvResponse(payload, "rapport-caisse-periode-" + dateDebut + "-" + dateFin + ".csv");
    }

    @GetMapping("/depenses")
    @PreAuthorize("hasAuthority('RAPPORT_CAISSE_READ')")
    @Operation(summary = "Rapport dépenses caisse", description = "Rapport détaillé des dépenses de caisse")
    public RapportCaisseDepensesDto getRapportDepenses(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(required = false) Long caisseId,
            @RequestParam(required = false) Long siteId
    ) {
        return rapportCaisseService.getRapportDepenses(dateDebut, dateFin, caisseId, siteId);
    }

    @GetMapping("/depenses/export")
    @PreAuthorize("hasAuthority('RAPPORT_CAISSE_EXPORT')")
    @Operation(summary = "Export dépenses caisse", description = "Export CSV du rapport dépenses")
    public ResponseEntity<byte[]> exportRapportDepenses(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(required = false) Long caisseId,
            @RequestParam(required = false) Long siteId,
            @RequestParam(defaultValue = "csv") String format
    ) {
        ensureCsv(format);
        byte[] payload = rapportCaisseService.exportRapportDepensesCsv(dateDebut, dateFin, caisseId, siteId);
        return buildCsvResponse(payload, "rapport-caisse-depenses-" + dateDebut + "-" + dateFin + ".csv");
    }

    @GetMapping("/ecarts")
    @PreAuthorize("hasAnyAuthority('RAPPORT_CAISSE_READ','RAPPORT_CAISSE_AUDIT_READ')")
    @Operation(summary = "Rapport écarts caisse", description = "Rapport détaillé des écarts caisse")
    public RapportCaisseEcartsDto getRapportEcarts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(required = false) Long caisseId,
            @RequestParam(required = false) Long siteId
    ) {
        return rapportCaisseService.getRapportEcarts(dateDebut, dateFin, caisseId, siteId);
    }

    @GetMapping("/ecarts/export")
    @PreAuthorize("hasAnyAuthority('RAPPORT_CAISSE_EXPORT','RAPPORT_CAISSE_AUDIT_READ')")
    @Operation(summary = "Export écarts caisse", description = "Export CSV du rapport écarts")
    public ResponseEntity<byte[]> exportRapportEcarts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(required = false) Long caisseId,
            @RequestParam(required = false) Long siteId,
            @RequestParam(defaultValue = "csv") String format
    ) {
        ensureCsv(format);
        byte[] payload = rapportCaisseService.exportRapportEcartsCsv(dateDebut, dateFin, caisseId, siteId);
        return buildCsvResponse(payload, "rapport-caisse-ecarts-" + dateDebut + "-" + dateFin + ".csv");
    }

    @GetMapping("/synthese")
    @PreAuthorize("hasAuthority('RAPPORT_CAISSE_READ')")
    @Operation(summary = "Synthèse caisse", description = "Vue synthétique pour contrôle interne")
    public RapportCaisseSyntheseDto getRapportSynthese(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(required = false) Long caisseId,
            @RequestParam(required = false) Long siteId
    ) {
        return rapportCaisseService.getRapportSynthese(dateDebut, dateFin, caisseId, siteId);
    }

    private void ensureCsv(String format) {
        if (!"csv".equalsIgnoreCase(format)) {
            throw new BusinessException("Seul le format csv est supporté pour le moment");
        }
    }

    private ResponseEntity<byte[]> buildCsvResponse(byte[] payload, String fileName) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment().filename(fileName).build());
        return ResponseEntity.ok().headers(headers).body(payload);
    }
}
