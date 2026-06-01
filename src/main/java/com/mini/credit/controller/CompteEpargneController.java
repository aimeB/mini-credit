package com.mini.credit.controller;

import com.mini.credit.dto.epargne.CompteEpargneCreateRequest;
import com.mini.credit.dto.epargne.CompteEpargneResponse;
import com.mini.credit.service.CompteEpargneService;
import com.mini.credit.service.security.ScopeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comptes-epargne")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Compte Epargne", description = "Savings account management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class CompteEpargneController {

    private final CompteEpargneService compteEpargneService;
    private final ScopeService scopeService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE', 'AGENT_BUREAU')")
    @Operation(summary = "Create savings account", description = "Create a new savings account")
    public CompteEpargneResponse create(@Valid @RequestBody CompteEpargneCreateRequest request) {
        return compteEpargneService.create(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE', 'AGENT_BUREAU')")
    @Operation(summary = "Get all savings accounts", description = "Retrieve all savings accounts")
    public List<CompteEpargneResponse> getAll() {
        return compteEpargneService.getAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@scopeService.canReadCompteEpargne(#id)")
    @Operation(summary = "Get account by ID", description = "Retrieve savings account details")
    public CompteEpargneResponse getById(@PathVariable Long id) {
        return compteEpargneService.getById(id);
    }

    @GetMapping("/membre/{membreId}")
    @PreAuthorize("@scopeService.canReadMembre(#membreId)")
    @Operation(summary = "Get accounts by member", description = "Retrieve savings accounts for a member")
    public List<CompteEpargneResponse> getByMembre(@PathVariable Long membreId) {
        return compteEpargneService.getByMembre(membreId);
    }
}
