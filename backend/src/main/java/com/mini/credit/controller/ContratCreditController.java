package com.mini.credit.controller;

import com.mini.credit.dto.document.ContratCreditCreateRequest;
import com.mini.credit.dto.document.ContratCreditResponse;
import com.mini.credit.service.ContratCreditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contrats-credit")
@RequiredArgsConstructor
@Tag(name = "Contrat Credit", description = "Credit contract management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class ContratCreditController {

    private final ContratCreditService contratCreditService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU')")
    @Operation(summary = "Create credit contract", description = "Create a new credit contract")
    public ContratCreditResponse create(@Valid @RequestBody ContratCreditCreateRequest request) {
        return contratCreditService.create(request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'GESTIONNAIRE', 'AGENT_TERRAIN') or (hasRole('MEMBER') and @contratCreditService.isCurrentUserContract(#id))")
    @Operation(summary = "Get contract by ID", description = "Retrieve credit contract details")
    public ContratCreditResponse getById(@PathVariable Long id) {
        return contratCreditService.getById(id);
    }

    @GetMapping("/credit/{creditId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'GESTIONNAIRE', 'AGENT_TERRAIN') or (hasRole('MEMBER') and @contratCreditService.isCurrentUserCredit(#creditId))")
    @Operation(summary = "Get contract by credit", description = "Retrieve credit contract for a specific credit")
    public ContratCreditResponse getByCreditId(@PathVariable Long creditId) {
        return contratCreditService.getByCreditId(creditId);
    }
}


