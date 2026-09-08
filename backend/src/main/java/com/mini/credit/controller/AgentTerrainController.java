package com.mini.credit.controller;


import com.mini.credit.dto.referentiel.AgentTerrainResponse;
import com.mini.credit.dto.referentiel.CreateAgentTerrainRequest;
import com.mini.credit.dto.referentiel.UpdateAgentTerrainRequest;
import com.mini.credit.service.AgentTerrainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/agents-terrain")
@Tag(name = "Agent Terrain", description = "Field agent management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class AgentTerrainController {

    private final AgentTerrainService agentTerrainService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'AGENT_TERRAIN')")
    @Operation(summary = "Get all field agents", description = "Retrieve list of all field agents")
    public List<AgentTerrainResponse> getAll() {
        return agentTerrainService.getAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'AGENT_TERRAIN')")
    @Operation(summary = "Get field agent by ID", description = "Retrieve a specific field agent by ID")
    public ResponseEntity<AgentTerrainResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(agentTerrainService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU')")
    @Operation(summary = "Create new field agent", description = "Create a new field agent")
    public ResponseEntity<AgentTerrainResponse> create(@Valid @RequestBody CreateAgentTerrainRequest request) {
        AgentTerrainResponse response = agentTerrainService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU')")
    @Operation(summary = "Update field agent", description = "Update an existing field agent")
    public ResponseEntity<AgentTerrainResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateAgentTerrainRequest request) {
        AgentTerrainResponse response = agentTerrainService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU')")
    @Operation(summary = "Delete field agent", description = "Soft delete a field agent")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        agentTerrainService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ===== PHASE 4: Site Assignment Endpoints =====

    @PostMapping("/{agentId}/sites/{siteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU')")
    @Operation(summary = "Assign site to agent", description = "Assign an additional operational site to a field agent")
    public ResponseEntity<Void> assignSiteToAgent(@PathVariable Long agentId, @PathVariable Long siteId) {
        agentTerrainService.assignerSite(agentId, siteId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{agentId}/sites/{siteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU')")
    @Operation(summary = "Remove site from agent", description = "Remove an operational site assignment from a field agent")
    public ResponseEntity<Void> removeSiteFromAgent(@PathVariable Long agentId, @PathVariable Long siteId) {
        agentTerrainService.retirerSite(agentId, siteId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{agentId}/sites")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'AGENT_TERRAIN')")
    @Operation(summary = "Get agent's assigned sites", description = "Retrieve all sites assigned to a specific field agent")
    public ResponseEntity<Set<Long>> getSitesByAgent(@PathVariable Long agentId) {
        return ResponseEntity.ok(agentTerrainService.getSitesByAgent(agentId));
    }

    @GetMapping("/by-site/{siteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU')")
    @Operation(summary = "Get agents by site", description = "Retrieve all field agents assigned to a specific site")
    public ResponseEntity<List<AgentTerrainResponse>> getAgentsBySite(@PathVariable Long siteId) {
        return ResponseEntity.ok(agentTerrainService.getAgentsBySite(siteId));
    }

    @GetMapping("/by-gestionnaire/{gestionnaireId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'GESTIONNAIRE')")
    @Operation(summary = "Get agents by gestionnaire", description = "Retrieve all active field agents supervised by a gestionnaire")
    public ResponseEntity<List<AgentTerrainResponse>> getAgentsByGestionnaire(@PathVariable Long gestionnaireId) {
        return ResponseEntity.ok(agentTerrainService.getAgentsByGestionnaire(gestionnaireId));
    }

    @GetMapping("/anomalies/{agenceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU')")
    @Operation(summary = "Get personnel anomalies for an agence",
        description = "Agents terrain dont l'employÃ© ou le gestionnaire n'appartient pas Ã  la mÃªme agence que leur site")
    public ResponseEntity<List<AgentTerrainResponse>> getAnomaliesByAgence(@PathVariable Long agenceId) {
        return ResponseEntity.ok(agentTerrainService.getAnomaliesByAgence(agenceId));
    }
}


