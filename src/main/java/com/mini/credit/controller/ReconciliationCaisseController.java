package com.mini.credit.controller;

import com.mini.credit.dto.caisse.ReconciliationCaisseDTO;
import com.mini.credit.service.ReconciliationCaisseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * PHASE 11: Controller Réconciliation Caisse
 *
 * Endpoints pour gestion des réconciliations caisse automatiques
 */
@RestController
@RequestMapping("/api/reconciliations")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Réconciliations Caisse", description = "PHASE 11 - Gestion réconciliations caisse automatiques")
@SecurityRequirement(name = "bearerAuth")
public class ReconciliationCaisseController {

    private final ReconciliationCaisseService reconciliationCaisseService;

    /**
     * POST /api/reconciliations
     * Génère réconciliations pour sessions fermées (batch manuel)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Générer réconciliations", description = "Batch manuel - crée réconciliations pour sessions fermées avec écarts")
    public ResponseEntity<List<ReconciliationCaisseDTO>> genererReconciliations() {
        List<ReconciliationCaisseDTO> reconciliations = reconciliationCaisseService.genererReconciliationsToutes();
        return ResponseEntity.status(HttpStatus.CREATED).body(reconciliations);
    }

    /**
     * GET /api/reconciliations/session/{sessionCaisseId}
     * Récupère réconciliations d'une session
     */
    @GetMapping("/session/{sessionCaisseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT_BUREAU')")
    @Operation(summary = "Réconciliations par session", description = "Lister toutes les réconciliations d'une session caisse")
    public ResponseEntity<List<ReconciliationCaisseDTO>> getBySessionCaisseId(
            @PathVariable @NotNull Long sessionCaisseId) {
        List<ReconciliationCaisseDTO> reconciliations = reconciliationCaisseService.getBySessionCaisseId(sessionCaisseId);
        return ResponseEntity.ok(reconciliations);
    }

    /**
     * GET /api/reconciliations/en-attente
     * Récupère réconciliations en attente (statut CREEE)
     */
    @GetMapping("/en-attente")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Réconciliations en attente", description = "Lister toutes les réconciliations à traiter (statut CREEE)")
    public ResponseEntity<List<ReconciliationCaisseDTO>> getEnAttente() {
        List<ReconciliationCaisseDTO> reconciliations = reconciliationCaisseService.getEnAttente();
        return ResponseEntity.ok(reconciliations);
    }

    /**
     * POST /api/reconciliations/{id}/rapprocher
     * Marque réconciliation comme rapprochée (écart résolu)
     */
    @PostMapping("/{id}/rapprocher")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Rapprocher réconciliation", description = "Marquer une réconciliation comme rapprochée (écart résolu)")
    public ResponseEntity<ReconciliationCaisseDTO> rapprocheer(
            @PathVariable @NotNull Long id,
            @RequestParam @NotBlank(message = "Motif requis") String motif) {
        ReconciliationCaisseDTO reconciliation = reconciliationCaisseService.rapprocheer(id, motif);
        return ResponseEntity.ok(reconciliation);
    }

    /**
     * POST /api/reconciliations/{id}/rejeter
     * Rejette réconciliation (écart confirmé intentionnel)
     */
    @PostMapping("/{id}/rejeter")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Rejeter réconciliation", description = "Rejeter une réconciliation (écart confirmé intentionnel)")
    public ResponseEntity<ReconciliationCaisseDTO> rejeter(
            @PathVariable @NotNull Long id,
            @RequestParam @NotBlank(message = "Raison requise") String raison) {
        ReconciliationCaisseDTO reconciliation = reconciliationCaisseService.rejeter(id, raison);
        return ResponseEntity.ok(reconciliation);
    }

    /**
     * GET /api/reconciliations/total-en-attente
     * Total écarts en attente
     */
    @GetMapping("/total-en-attente")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT_BUREAU')")
    @Operation(summary = "Total écarts en attente", description = "Somme des écarts à traiter")
    public ResponseEntity<BigDecimal> getTotalEcartsEnAttente() {
        BigDecimal total = reconciliationCaisseService.getTotalEcartsEnAttente();
        return ResponseEntity.ok(total);
    }

    /**
     * GET /api/reconciliations/nombre-en-attente
     * Nombre de réconciliations en attente
     */
    @GetMapping("/nombre-en-attente")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT_BUREAU')")
    @Operation(summary = "Nombre en attente", description = "Nombre de réconciliations à traiter")
    public ResponseEntity<Long> getNombreReconciliationsEnAttente() {
        Long nombre = reconciliationCaisseService.getNombreReconciliationsEnAttente();
        return ResponseEntity.ok(nombre);
    }
}
