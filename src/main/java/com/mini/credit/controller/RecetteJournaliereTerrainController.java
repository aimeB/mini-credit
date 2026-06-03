package com.mini.credit.controller;

import com.mini.credit.annotation.Auditable;
import com.mini.credit.dto.caisse.RecetteJournaliereTerrainDTO;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.service.RecetteJournaliereTerrainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * PHASE 6: Controller pour recettes journalières terrain.
 *
 * Endpoints:
 * - POST /api/recettes-journalieres (créer recette)
 * - GET /api/recettes-journalieres/{id} (consulter)
 * - GET /api/recettes-journalieres/jour/{dateJour} (lister par jour)
 * - GET /api/recettes-journalieres/agent/{agentId}/jour/{dateJour} (recettes agent)
 * - GET /api/recettes-journalieres/validation/en-attente (recettes en attente - CONTROLEUR)
 * - GET /api/recettes-journalieres/validation/jour/{dateJour} (en attente pour un jour)
 * - POST /api/recettes-journalieres/{id}/valider (valider - CONTROLEUR)
 * - POST /api/recettes-journalieres/{id}/rejeter (rejeter - CONTROLEUR)
 * - POST /api/recettes-journalieres/{id}/annuler (annuler)
 * - GET /api/recettes-journalieres/somme/jour/{dateJour} (somme jour)
 * - GET /api/recettes-journalieres/somme-validee/jour/{dateJour} (somme validée)
 */
@RestController
@RequestMapping("/api/recettes-journalieres")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Recette Journalière Terrain", description = "Daily receipt management endpoints (PHASE 6)")
@SecurityRequirement(name = "bearer-jwt")
public class RecetteJournaliereTerrainController {

    private final RecetteJournaliereTerrainService recetteService;

    /**
     * PHASE 6: Crée une nouvelle recette journalière
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTROLEUR', 'AGENT_TERRAIN', 'AGENT_BUREAU')")
    @Operation(summary = "Create receipt", description = "Create a new daily receipt from terrain data")
    @Auditable(action = AuditAction.RECETTE_JOURNALIERE_CREATED, entityType = "RecetteJournaliereTerrain")
    public RecetteJournaliereTerrainDTO creerRecette(
            @RequestParam @NotNull Long agentId,
            @RequestParam @NotNull Long membreId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateJour,
            @RequestParam @NotNull String typeRecette,
            @RequestParam @NotNull @DecimalMin("0.01") BigDecimal montant,
            @RequestParam(required = false) String observation,
            @RequestParam(required = false) String referencePapier) {

        return recetteService.creerRecette(agentId, membreId, dateJour, typeRecette, montant, observation, referencePapier);
    }

    /**
     * Récupère une recette par ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTROLEUR', 'AGENT_TERRAIN', 'AGENT_BUREAU')")
    @Operation(summary = "Get receipt", description = "Retrieve a specific receipt details")
    public RecetteJournaliereTerrainDTO getById(@PathVariable Long id) {
        return recetteService.getById(id);
    }

    /**
     * Récupère les recettes d'un jour donné
     */
    @GetMapping("/jour/{dateJour}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTROLEUR', 'AGENT_BUREAU')")
    @Operation(summary = "Get receipts by date", description = "List all receipts for a specific date")
    public List<RecetteJournaliereTerrainDTO> getByDateJour(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateJour) {
        return recetteService.getByDateJour(dateJour);
    }

    /**
     * Récupère les recettes d'un agent pour un jour
     */
    @GetMapping("/agent/{agentId}/jour/{dateJour}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTROLEUR', 'AGENT_BUREAU')")
    @Operation(summary = "Get agent receipts", description = "List receipts for a specific agent and date")
    public List<RecetteJournaliereTerrainDTO> getByAgentAndDateJour(
            @PathVariable Long agentId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateJour) {
        return recetteService.getByAgentAndDateJour(agentId, dateJour);
    }

    /**
     * Récupère les recettes en attente de validation
     */
    @GetMapping("/validation/en-attente")
    @PreAuthorize("hasAuthority('CONTROLEUR_RECETTES_VALIDATE')")
    @Operation(summary = "Get pending receipts", description = "List all receipts waiting for CONTROLEUR validation")
    public List<RecetteJournaliereTerrainDTO> getEnAttenteValidation() {
        return recetteService.getEnAttenteValidation();
    }

    /**
     * Récupère les recettes en attente pour un jour
     */
    @GetMapping("/validation/jour/{dateJour}")
    @PreAuthorize("hasAuthority('CONTROLEUR_RECETTES_VALIDATE')")
    @Operation(summary = "Get pending receipts by date", description = "List receipts waiting for validation on a specific date")
    public List<RecetteJournaliereTerrainDTO> getEnAttenteValidationByDateJour(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateJour) {
        return recetteService.getEnAttenteValidationByDateJour(dateJour);
    }

    /**
     * PHASE 6: Valide une recette (réconciliation)
     * Calcule variance: abs(cashRemis - montant)
     */
    @PostMapping("/{id}/valider")
    @PreAuthorize("hasAuthority('CONTROLEUR_RECETTES_VALIDATE')")
    @Operation(summary = "Validate receipt (PHASE 6)", description = "CONTROLEUR validates receipt by reconciling cash amount")
    @Auditable(action = AuditAction.RECETTE_JOURNALIERE_VALIDATED, entityType = "RecetteJournaliereTerrain")
    public RecetteJournaliereTerrainDTO validerRecette(
            @PathVariable Long id,
            @RequestParam @NotNull @DecimalMin("0") BigDecimal cashRemis) {
        return recetteService.validerRecette(id, cashRemis);
    }

    /**
     * Rejette une recette
     */
    @PostMapping("/{id}/rejeter")
    @PreAuthorize("hasAuthority('CONTROLEUR_RECETTES_VALIDATE')")
    @Operation(summary = "Reject receipt", description = "CONTROLEUR rejects a receipt with reason")
    @Auditable(action = AuditAction.RECETTE_JOURNALIERE_REJECTED, entityType = "RecetteJournaliereTerrain")
    public RecetteJournaliereTerrainDTO rejeterRecette(
            @PathVariable Long id,
            @RequestParam String motif) {
        return recetteService.rejeterRecette(id, motif);
    }

    /**
     * Annule une recette
     */
    @PostMapping("/{id}/annuler")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT_BUREAU')")
    @Operation(summary = "Cancel receipt", description = "Cancel a receipt before validation")
    @Auditable(action = AuditAction.RECETTE_JOURNALIERE_CANCELLED, entityType = "RecetteJournaliereTerrain")
    public RecetteJournaliereTerrainDTO annulerRecette(@PathVariable Long id) {
        return recetteService.annulerRecette(id);
    }

    /**
     * Somme des montants pour un jour
     */
    @GetMapping("/somme/jour/{dateJour}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTROLEUR', 'AGENT_BUREAU')")
    @Operation(summary = "Get daily receipts total", description = "Get sum of all receipts for a date")
    public BigDecimal getSommeByDateJour(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateJour) {
        return recetteService.getSommeByDateJour(dateJour);
    }

    /**
     * Somme des montants validés pour un jour
     */
    @GetMapping("/somme-validee/jour/{dateJour}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTROLEUR', 'AGENT_BUREAU')")
    @Operation(summary = "Get validated receipts total", description = "Get sum of validated receipts for a date")
    public BigDecimal getSommeValideeByDateJour(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateJour) {
        return recetteService.getSommeValideeByDateJour(dateJour);
    }
}
