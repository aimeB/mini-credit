package com.mini.credit.controller;


import com.mini.credit.dto.referentiel.AgentTerrainResponse;
import com.mini.credit.repository.agentTerrain.AgentTerrainRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/agents-terrain")
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Agent Terrain", description = "Field agent management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class AgentTerrainController {

    private final AgentTerrainRepository agentTerrainRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE', 'AGENT_TERRAIN')")
    @Operation(summary = "Get all field agents", description = "Retrieve list of all field agents")
    public List<AgentTerrainResponse> getAll() {
        return agentTerrainRepository.findAll()
                .stream()
                .map(AgentTerrainResponse::fromEntity)
                .toList();
    }
}
