package com.mini.credit.controller;

import com.mini.credit.dto.referentiel.*;
import com.mini.credit.enums.RecetteStatut;
import com.mini.credit.service.CollecteTerrainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/collectes-terrain")
@Tag(name = "Collecte Terrain", description = "Collecte journaliÃ¨re terrain dÃ©taillÃ©e par membre")
@SecurityRequirement(name = "bearer-jwt")
public class CollecteTerrainController {

    private final CollecteTerrainService collecteTerrainService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT_TERRAIN', 'CAISSIER', 'CONTROLEUR', 'CHEF_BUREAU', 'GESTIONNAIRE', 'RCI')")
    @Operation(summary = "Lister collectes terrain", description = "Liste paginÃ©e et filtrÃ©e selon le rÃ´le connectÃ©")
    public ResponseEntity<Page<CollecteTerrainResponse>> list(
        @RequestParam(required = false) String statut,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate dateDebut,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate dateFin,
        @RequestParam(required = false) Long agentId,
        @RequestParam(required = false) Long siteId,
        @RequestParam(required = false) Long antenneId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        RecetteStatut statutFilter = statut != null ? RecetteStatut.valueOf(statut.toUpperCase()) : null;
        return ResponseEntity.ok(
            collecteTerrainService.list(statutFilter, dateDebut, dateFin, agentId, siteId, antenneId, page, size)
        );
    }

    @GetMapping("/today")
    @PreAuthorize("hasRole('AGENT_TERRAIN')")
    @Operation(summary = "Ma collecte du jour")
    public ResponseEntity<CollecteTerrainResponse> getToday() {
        CollecteTerrainResponse response = collecteTerrainService.getToday();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT_TERRAIN', 'CAISSIER', 'CONTROLEUR', 'CHEF_BUREAU', 'GESTIONNAIRE', 'RCI')")
    @Operation(summary = "DÃ©tail d'une collecte")
    public ResponseEntity<CollecteTerrainResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(collecteTerrainService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('AGENT_TERRAIN')")
    @Operation(summary = "CrÃ©er ma collecte du jour")
    public ResponseEntity<CollecteTerrainResponse> create(@Valid @RequestBody(required = false) CreateCollecteTerrainRequest request) {
        CollecteTerrainResponse response = collecteTerrainService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/lignes")
    @PreAuthorize("hasRole('AGENT_TERRAIN')")
    @Operation(summary = "Ajouter une ligne membre")
    public ResponseEntity<CollecteMembreLigneResponse> addLigne(
        @PathVariable Long id,
        @Valid @RequestBody CreateCollecteMembreLigneRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(collecteTerrainService.addLigne(id, request));
    }

    @PutMapping("/{id}/lignes/{ligneId}")
    @PreAuthorize("hasRole('AGENT_TERRAIN')")
    @Operation(summary = "Modifier une ligne membre")
    public ResponseEntity<CollecteMembreLigneResponse> updateLigne(
        @PathVariable Long id,
        @PathVariable Long ligneId,
        @Valid @RequestBody UpdateCollecteMembreLigneRequest request) {
        return ResponseEntity.ok(collecteTerrainService.updateLigne(id, ligneId, request));
    }

    @DeleteMapping("/{id}/lignes/{ligneId}")
    @PreAuthorize("hasRole('AGENT_TERRAIN')")
    @Operation(summary = "Supprimer une ligne membre")
    public ResponseEntity<Void> deleteLigne(@PathVariable Long id, @PathVariable Long ligneId) {
        collecteTerrainService.deleteLigne(id, ligneId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/soumettre")
    @PreAuthorize("hasRole('AGENT_TERRAIN')")
    @Operation(summary = "Soumettre la collecte")
    public ResponseEntity<CollecteTerrainResponse> soumettre(
        @PathVariable Long id,
        @RequestBody(required = false) CreateCollecteTerrainRequest request) {
        return ResponseEntity.ok(collecteTerrainService.soumettre(id, request));
    }

    @PostMapping({"/{id}/confirmer-billetage", "/{id}/billetage/confirmer"})
    @PreAuthorize("hasRole('CAISSIER')")
    @Operation(summary = "Confirmer le billetage de la collecte")
    public ResponseEntity<CollecteTerrainResponse> confirmerBilletage(
        @PathVariable Long id,
        @Valid @RequestBody ConfirmerBilletageRequest request) {
        return ResponseEntity.ok(collecteTerrainService.confirmerBilletage(id, request));
    }

    @PostMapping("/{id}/valider")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTROLEUR')")
    @Operation(summary = "Valider ou rejeter la collecte")
    public ResponseEntity<CollecteTerrainResponse> valider(
        @PathVariable Long id,
        @Valid @RequestBody ValidateCollecteTerrainRequest request) {
        return ResponseEntity.ok(collecteTerrainService.valider(id, request));
    }

    @PostMapping("/{id}/rejeter")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTROLEUR')")
    @Operation(summary = "Rejeter la collecte")
    public ResponseEntity<CollecteTerrainResponse> rejeter(
        @PathVariable Long id,
        @Valid @RequestBody ValidateCollecteTerrainRequest request) {
        return ResponseEntity.ok(collecteTerrainService.rejeter(id, request));
    }

    @GetMapping("/{id}/recap")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTROLEUR', 'AGENT_TERRAIN', 'CAISSIER', 'CHEF_BUREAU', 'GESTIONNAIRE', 'RCI')")
    @Operation(summary = "RÃ©capitulatif de la collecte")
    public ResponseEntity<CollecteRecapResponse> recap(@PathVariable Long id) {
        return ResponseEntity.ok(collecteTerrainService.recap(id));
    }
}

