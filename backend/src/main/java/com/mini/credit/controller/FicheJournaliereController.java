package com.mini.credit.controller;

import com.mini.credit.annotation.Auditable;
import com.mini.credit.dto.caisse.CreateFicheJournaliereRequest;
import com.mini.credit.dto.caisse.FicheJournaliereResponse;
import com.mini.credit.dto.caisse.UpdateFicheJournaliereRequest;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.service.FicheJournaliereService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * PHASE 6B.1: Controller for FicheJournaliereAgentTerrain
 *
 * CRUD endpoints only (consolidation/validation in later phases).
 *
 * Endpoints (Phase 6B.1):
 * - POST /api/fiches-journalieres (create)
 * - GET /api/fiches-journalieres/{id} (read)
 * - GET /api/fiches-journalieres/agent/{agentId}/date/{dateFiche} (read by agent+date)
 * - GET /api/fiches-journalieres/agent/{agentId} (list by agent)
 * - GET /api/fiches-journalieres/date/{dateFiche} (list by date)
 * - GET /api/fiches-journalieres/plage (list by date range)
 * - GET /api/fiches-journalieres/site/{siteId} (list by site)
 * - GET /api/fiches-journalieres/statut/{statut} (list by status)
 * - PUT /api/fiches-journalieres/{id} (update - BROUILLON only)
 * - DELETE /api/fiches-journalieres/{id} (delete - BROUILLON only)
 *
 * Excluded from Phase 6B.1:
 * - Consolidation (phase 6B.2)
 * - Validation (phase 6B.3)
 * - Generate operation (phase 6B.2)
 * - Reject/cancel operations (phase 6B.3)
 */
@RestController
@RequestMapping("/api/fiches-journalieres")
@RequiredArgsConstructor
@Tag(name = "Fiche Journalière Agent Terrain", description = "Daily consolidated fiche management (PHASE 6B.1)")
@SecurityRequirement(name = "bearer-jwt")
public class FicheJournaliereController {

    private final FicheJournaliereService ficheService;

    /**
     * Create new fiche journalière
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTIONNAIRE')")
    @Operation(summary = "Create daily fiche", description = "Create a new consolidated daily fiche for an agent")
    @Auditable(action = AuditAction.FICHE_JOURNALIERE_CREATED, entityType = "FicheJournaliereAgentTerrain")
    public ResponseEntity<FicheJournaliereResponse> create(@Valid @RequestBody CreateFicheJournaliereRequest request) {
        FicheJournaliereResponse response = ficheService.creerFiche(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get fiche by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTIONNAIRE', 'CONTROLEUR', 'AGENT_TERRAIN')")
    @Operation(summary = "Get fiche by ID", description = "Retrieve a specific daily fiche by ID")
    public ResponseEntity<FicheJournaliereResponse> getById(@PathVariable Long id) {
        FicheJournaliereResponse response = ficheService.getById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get fiche by agent and date
     */
    @GetMapping("/agent/{agentId}/date/{dateFiche}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTIONNAIRE', 'CONTROLEUR')")
    @Operation(summary = "Get fiche by agent and date", description = "Retrieve fiche for specific agent on specific date")
    public ResponseEntity<FicheJournaliereResponse> getByAgentAndDate(
            @PathVariable Long agentId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFiche) {
        FicheJournaliereResponse response = ficheService.getByAgentAndDate(agentId, dateFiche);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all fiches for an agent
     */
    @GetMapping("/agent/{agentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTIONNAIRE', 'CONTROLEUR')")
    @Operation(summary = "Get agent fiches", description = "List all fiches for a specific agent")
    public ResponseEntity<List<FicheJournaliereResponse>> getByAgent(@PathVariable Long agentId) {
        List<FicheJournaliereResponse> responses = ficheService.getByAgent(agentId);
        return ResponseEntity.ok(responses);
    }

    /**
     * Get all fiches for a specific date
     */
    @GetMapping("/date/{dateFiche}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTIONNAIRE', 'CONTROLEUR')")
    @Operation(summary = "Get fiches by date", description = "List all fiches for a specific date")
    public ResponseEntity<List<FicheJournaliereResponse>> getByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFiche) {
        List<FicheJournaliereResponse> responses = ficheService.getByDate(dateFiche);
        return ResponseEntity.ok(responses);
    }

    /**
     * Get fiches for a date range
     */
    @GetMapping("/plage")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTIONNAIRE', 'CONTROLEUR')")
    @Operation(summary = "Get fiches by date range", description = "List fiches within a date range")
    public ResponseEntity<List<FicheJournaliereResponse>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        List<FicheJournaliereResponse> responses = ficheService.getByDateRange(dateDebut, dateFin);
        return ResponseEntity.ok(responses);
    }

    /**
     * Get all fiches for a site
     */
    @GetMapping("/site/{siteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTIONNAIRE')")
    @Operation(summary = "Get site fiches", description = "List all fiches for a specific site")
    public ResponseEntity<List<FicheJournaliereResponse>> getBySite(@PathVariable Long siteId) {
        List<FicheJournaliereResponse> responses = ficheService.getBySite(siteId);
        return ResponseEntity.ok(responses);
    }

    /**
     * Get fiches by status
     */
    @GetMapping("/statut/{statut}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTIONNAIRE', 'CONTROLEUR')")
    @Operation(summary = "Get fiches by status", description = "List all fiches with a specific status")
    public ResponseEntity<List<FicheJournaliereResponse>> getByStatut(@PathVariable String statut) {
        List<FicheJournaliereResponse> responses = ficheService.getByStatut(statut);
        return ResponseEntity.ok(responses);
    }

    /**
     * Update fiche (only BROUILLON can be updated)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTIONNAIRE')")
    @Operation(summary = "Update fiche", description = "Update an existing BROUILLON fiche")
    @Auditable(action = AuditAction.FICHE_JOURNALIERE_UPDATED, entityType = "FicheJournaliereAgentTerrain")
    public ResponseEntity<FicheJournaliereResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateFicheJournaliereRequest request) {
        FicheJournaliereResponse response = ficheService.update(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete fiche (only BROUILLON can be deleted)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTIONNAIRE')")
    @Operation(summary = "Delete fiche", description = "Delete a BROUILLON fiche")
    @Auditable(action = AuditAction.FICHE_JOURNALIERE_DELETED, entityType = "FicheJournaliereAgentTerrain")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ficheService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
