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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comptes-epargne")
@RequiredArgsConstructor
@Tag(name = "Compte Epargne", description = "Savings account management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class CompteEpargneController {

    private final CompteEpargneService compteEpargneService;
    private final ScopeService scopeService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create missing savings account", description = "Administrative repair: create a missing savings account")
    public CompteEpargneResponse create(@Valid @RequestBody CompteEpargneCreateRequest request) {
        return compteEpargneService.create(request);
    }

    @PostMapping("/membre/{membreId}/repair")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Repair member savings account", description = "Administrative repair for missing savings account")
    public CompteEpargneResponse repairForMember(@PathVariable Long membreId) {
        return compteEpargneService.createMissingForMember(membreId);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'GESTIONNAIRE') or hasAuthority('CONTROLEUR_EPARGNE_READ')")
    @Operation(summary = "Get all savings accounts", description = "Retrieve all savings accounts")
    public List<CompteEpargneResponse> getAll() {
        return compteEpargneService.getAll();
    }

    @GetMapping("/guichet/retrait")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAISSIER')")
    @Operation(summary = "Get active accounts for counter withdrawal", description = "Retrieve active savings accounts selectable by cashier for withdrawal request encoding")
    public List<CompteEpargneResponse> getComptesActifsPourRetraitGuichet() {
        return compteEpargneService.getComptesActifsPourRetraitGuichet();
    }

    @GetMapping("/mes-membres")
    @PreAuthorize("hasRole('AGENT_TERRAIN')")
    @Operation(summary = "Get scoped accounts for current AGENT_TERRAIN", description = "Retrieve savings accounts scoped to current agent terrain perimeter")
    public List<CompteEpargneResponse> getMesMembresComptes() {
        return compteEpargneService.getForCurrentAgentPerimeter();
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "Get current member accounts", description = "Retrieve savings accounts for authenticated member")
    public List<CompteEpargneResponse> getMesComptes() {
        return compteEpargneService.getForCurrentMember();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@scopeService.canReadCompteEpargne(#id)")
    @Operation(summary = "Get account by ID", description = "Retrieve savings account details")
    public CompteEpargneResponse getById(@PathVariable Long id) {
        return compteEpargneService.getById(id);
    }

    @GetMapping("/membre/{membreId}")
    @PreAuthorize("@scopeService.canReadMembreEpargne(#membreId)")
    @Operation(summary = "Get accounts by member", description = "Retrieve savings accounts for a member")
    public List<CompteEpargneResponse> getByMembre(@PathVariable Long membreId) {
        return compteEpargneService.getByMembre(membreId);
    }

    @GetMapping("/membre/{membreId}/actif")
    @PreAuthorize("@scopeService.canReadMembre(#membreId)")
    @Operation(summary = "Get active account by member", description = "Retrieve active savings account for a member")
    public ResponseEntity<CompteEpargneResponse> getActiveByMembre(@PathVariable Long membreId) {
        return compteEpargneService.getActiveByMembre(membreId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

