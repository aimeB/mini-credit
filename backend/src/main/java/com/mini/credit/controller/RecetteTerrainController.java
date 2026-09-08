package com.mini.credit.controller;

import com.mini.credit.dto.referentiel.CreateRecetteTerrainRequest;
import com.mini.credit.dto.referentiel.RecetteTerrainResponse;
import com.mini.credit.dto.referentiel.UpdateRecetteTerrainRequest;
import com.mini.credit.dto.referentiel.ValidateRecetteTerrainRequest;
import com.mini.credit.enums.RecetteStatut;
import com.mini.credit.service.RecetteTerrainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller pour gestion des recettes terrain journaliÃ¨res
 * 
 * Endpoints:
 * - AGENT_TERRAIN: crÃ©er, modifier (BROUILLON), soumettre, consulter propres recettes
 * - CHEF_BUREAU: valider recettes site, lister en attente
 * - CONTROLEUR: audit complet
 * - ADMIN: accÃ¨s total
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recettes-terrain")
@Tag(name = "Recette Terrain (Legacy)", description = "Module legacy en transition: lecture/backoffice, crÃ©ation terrain dÃ©placÃ©e vers CollecteJournaliereTerrain")
@SecurityRequirement(name = "bearer-jwt")
public class RecetteTerrainController {

    private final RecetteTerrainService recetteTerrainService;

    // ========== CRUD ==========

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'CONTROLEUR')")
    @Operation(summary = "Lister toutes les recettes", description = "RÃ©cupÃ¨re liste complÃ¨te des recettes actives")
    public List<RecetteTerrainResponse> getAll() {
        return recetteTerrainService.getAll();
    }

    @GetMapping("/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'CONTROLEUR', 'AGENT_TERRAIN')")
    @Operation(summary = "Lister les recettes (paginÃ©)", description = "RÃ©cupÃ¨re recettes avec pagination et filtres optionnels")
    public ResponseEntity<Page<RecetteTerrainResponse>> getRecettePaginated(
        Pageable pageable,
        @RequestParam(required = false) String statut,
        @RequestParam(required = false) Long siteId,
        @RequestParam(required = false) Long agentTerrainId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {

        RecetteStatut recetteStatut = statut != null ? RecetteStatut.valueOf(statut.toUpperCase()) : null;
        Page<RecetteTerrainResponse> page = recetteTerrainService.searchAndFilter(
            pageable, recetteStatut, siteId, agentTerrainId, dateDebut, dateFin);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'CONTROLEUR', 'AGENT_TERRAIN')")
    @Operation(summary = "RÃ©cupÃ©rer recette par ID", description = "Retourne dÃ©tails recette avec audit trail")
    public ResponseEntity<RecetteTerrainResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(recetteTerrainService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "CrÃ©er recette terrain (legacy)", description = "Endpoint legacy, crÃ©ation manuelle rÃ©servÃ©e ADMIN", deprecated = true)
    public ResponseEntity<RecetteTerrainResponse> create(@Valid @RequestBody CreateRecetteTerrainRequest request) {
        RecetteTerrainResponse response = recetteTerrainService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Modifier recette terrain (legacy)", description = "Endpoint legacy, modification manuelle rÃ©servÃ©e ADMIN", deprecated = true)
    public ResponseEntity<RecetteTerrainResponse> update(
        @PathVariable Long id,
        @Valid @RequestBody UpdateRecetteTerrainRequest request) {
        RecetteTerrainResponse response = recetteTerrainService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer recette terrain (legacy)", description = "Endpoint legacy, suppression manuelle rÃ©servÃ©e ADMIN", deprecated = true)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        recetteTerrainService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ========== RECHERCHES ==========

    @GetMapping("/agent/{agentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'CONTROLEUR', 'AGENT_TERRAIN')")
    @Operation(summary = "Recettes par agent", description = "Lister toutes les recettes d'un agent")
    public List<RecetteTerrainResponse> getByAgent(@PathVariable Long agentId) {
        return recetteTerrainService.getByAgentTerrain(agentId);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('AGENT_TERRAIN')")
    @Operation(summary = "Mes recettes", description = "Lister les recettes de l'agent terrain connectÃ©")
    public List<RecetteTerrainResponse> getMine() {
        return recetteTerrainService.getMine();
    }

    @GetMapping("/site/{siteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'CONTROLEUR')")
    @Operation(summary = "Recettes par site", description = "Lister toutes les recettes d'un site")
    public List<RecetteTerrainResponse> getBySite(@PathVariable Long siteId) {
        return recetteTerrainService.getBySite(siteId);
    }

    @GetMapping("/site/{siteId}/attente")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU')")
    @Operation(summary = "Recettes en attente de validation", description = "Recettes SOUMISE du site (dashboard responsable)")
    public List<RecetteTerrainResponse> getEnAttenteValidation(@PathVariable Long siteId) {
        return recetteTerrainService.getEnAttenteValidation(siteId);
    }

    @GetMapping("/date/{dateRecette}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'CONTROLEUR')")
    @Operation(summary = "Recettes par date", description = "Lister toutes les recettes d'une date donnÃ©e")
    public List<RecetteTerrainResponse> getByDate(
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateRecette) {
        return recetteTerrainService.getByDateRecette(dateRecette);
    }

    @GetMapping("/statut/{statut}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'CONTROLEUR')")
    @Operation(summary = "Recettes par statut", description = "Lister les recettes filtrÃ©es par statut")
    public List<RecetteTerrainResponse> getByStatut(@PathVariable String statut) {
        RecetteStatut enumStatut = RecetteStatut.valueOf(statut.toUpperCase());
        return recetteTerrainService.getByStatut(enumStatut);
    }

    @GetMapping("/search/range")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'CONTROLEUR')")
    @Operation(summary = "Recettes par plage de dates", description = "Lister les recettes entre deux dates")
    public List<RecetteTerrainResponse> getByDateRange(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return recetteTerrainService.getByDateRange(debut, fin);
    }

    // ========== WORKFLOW ==========

    @PostMapping("/{id}/soumettre")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soumettre recette pour validation (legacy)", description = "Endpoint legacy, soumission manuelle rÃ©servÃ©e ADMIN", deprecated = true)
    public ResponseEntity<RecetteTerrainResponse> soumettre(@PathVariable Long id) {
        RecetteTerrainResponse response = recetteTerrainService.soumettre(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/valider")
    @PreAuthorize("hasAuthority('CONTROLEUR_RECETTES_VALIDATE')")
    @Operation(summary = "Valider ou rejeter recette", description = "Change statut SOUMISE â†’ VALIDEE/REJETEE")
    public ResponseEntity<RecetteTerrainResponse> valider(
        @PathVariable Long id,
        @Valid @RequestBody ValidateRecetteTerrainRequest request) {
        RecetteTerrainResponse response = recetteTerrainService.valider(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/rejeter")
    @PreAuthorize("hasAuthority('CONTROLEUR_RECETTES_VALIDATE')")
    @Operation(summary = "Rejeter recette", description = "Raccourci pour valider avec REJETEE")
    public ResponseEntity<RecetteTerrainResponse> rejeter(
        @PathVariable Long id,
        @RequestParam String motif,
        @RequestParam Long validePar) {
        RecetteTerrainResponse response = recetteTerrainService.rejeter(id, motif, validePar);
        return ResponseEntity.ok(response);
    }

    // ========== MÃ‰TIERS ==========

    @GetMapping("/{id}/ecarts")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'CONTROLEUR')")
    @Operation(summary = "Calculer Ã©carts", description = "Retourne excÃ©dent/manquant trÃ©sorerie")
    public ResponseEntity<RecetteTerrainResponse> calculerEcarts(@PathVariable Long id) {
        RecetteTerrainResponse response = recetteTerrainService.calculerEcarts(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/agent/{agentId}/date/{dateRecette}/total")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'CONTROLEUR', 'AGENT_TERRAIN')")
    @Operation(summary = "Total collectÃ© par agent/date", description = "Retourne total Ã©pargne+remboursements+frais")
    public ResponseEntity<java.math.BigDecimal> getTotalCollecte(
        @PathVariable Long agentId,
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateRecette) {
        java.math.BigDecimal total = recetteTerrainService.getTotalCollecteByAgentAndDate(agentId, dateRecette);
        return ResponseEntity.ok(total);
    }
}

