package com.mini.credit.controller;

import com.mini.credit.annotation.Auditable;
import com.mini.credit.dto.epargne.OperationEpargneRequest;
import com.mini.credit.dto.epargne.OperationEpargneResponse;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.service.OperationEpargneService;
import com.mini.credit.service.security.ScopeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/operations-epargne")
@RequiredArgsConstructor
@Tag(name = "Operation Epargne", description = "Savings operations management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class OperationEpargneController {

    private final OperationEpargneService operationEpargneService;
    private final ScopeService scopeService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'GESTIONNAIRE', 'CAISSIER')")
    @Operation(summary = "Record savings operation", description = "Record a new savings operation")
    @Auditable(action = AuditAction.OPERATION_EPARGNE_CREATED, entityType = "OperationEpargne", captureParameters = true, captureResult = true)
    public OperationEpargneResponse enregistrer(@Valid @RequestBody OperationEpargneRequest request) {
        return operationEpargneService.enregistrer(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'GESTIONNAIRE') or hasAuthority('CONTROLEUR_EPARGNE_OPERATION_READ')")
    @Operation(summary = "Get all savings operations", description = "Retrieve paginated list of all savings operations")
    public Page<OperationEpargneResponse> getAll(@RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "10") int size) {
        // PHASE 3B: Limiter size max Ã  100 pour Ã©viter les abus
        if (size > 100) size = 100;
        Pageable pageable = PageRequest.of(page, size);
        return operationEpargneService.getAll(pageable);
    }

    @GetMapping("/compte/{compteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'GESTIONNAIRE') or (hasAuthority('CONTROLEUR_EPARGNE_OPERATION_READ') and @scopeService.canReadCompteEpargne(#compteId)) or (hasRole('AGENT_TERRAIN') and @scopeService.canReadCompteEpargne(#compteId)) or (hasRole('MEMBER') and @operationEpargneService.isCurrentUserAccount(#compteId))")
    @Operation(summary = "Get operations by account", description = "Retrieve savings operations for an account")
    public List<OperationEpargneResponse> getByCompte(@PathVariable Long compteId) {
        return operationEpargneService.getByCompte(compteId);
    }

    @GetMapping("/membre/{membreId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'GESTIONNAIRE') or (hasAuthority('CONTROLEUR_EPARGNE_OPERATION_READ') and @scopeService.canReadMembreEpargne(#membreId)) or (hasRole('AGENT_TERRAIN') and @scopeService.canReadMembreEpargne(#membreId)) or (hasRole('MEMBER') and @operationEpargneService.isCurrentUserMembre(#membreId))")
    @Operation(summary = "Get operations by member", description = "Retrieve savings operations for a member")
    public List<OperationEpargneResponse> getByMembre(@PathVariable Long membreId) {
        return operationEpargneService.getByMembre(membreId);
    }
}

