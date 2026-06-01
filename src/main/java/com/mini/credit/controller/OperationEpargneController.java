package com.mini.credit.controller;

import com.mini.credit.dto.epargne.OperationEpargneRequest;
import com.mini.credit.dto.epargne.OperationEpargneResponse;
import com.mini.credit.service.OperationEpargneService;
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
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Operation Epargne", description = "Savings operations management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class OperationEpargneController {

    private final OperationEpargneService operationEpargneService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE', 'AGENT_BUREAU', 'CAISSIER')")
    @Operation(summary = "Record savings operation", description = "Record a new savings operation")
    public OperationEpargneResponse enregistrer(@Valid @RequestBody OperationEpargneRequest request) {
        return operationEpargneService.enregistrer(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE', 'AGENT_BUREAU')")
    @Operation(summary = "Get all savings operations", description = "Retrieve paginated list of all savings operations")
    public Page<OperationEpargneResponse> getAll(@RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "10") int size) {
        // PHASE 3B: Limiter size max à 100 pour éviter les abus
        if (size > 100) size = 100;
        Pageable pageable = PageRequest.of(page, size);
        return operationEpargneService.getAll(pageable);
    }

    @GetMapping("/compte/{compteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE', 'AGENT_BUREAU') or (hasRole('MEMBER') and @operationEpargneService.isCurrentUserAccount(#compteId))")
    @Operation(summary = "Get operations by account", description = "Retrieve savings operations for an account")
    public List<OperationEpargneResponse> getByCompte(@PathVariable Long compteId) {
        return operationEpargneService.getByCompte(compteId);
    }

    @GetMapping("/membre/{membreId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE', 'AGENT_BUREAU') or (hasRole('MEMBER') and @operationEpargneService.isCurrentUserMembre(#membreId))")
    @Operation(summary = "Get operations by member", description = "Retrieve savings operations for a member")
    public List<OperationEpargneResponse> getByMembre(@PathVariable Long membreId) {
        return operationEpargneService.getByMembre(membreId);
    }
}
