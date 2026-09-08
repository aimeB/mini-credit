package com.mini.credit.controller;

import com.mini.credit.dto.document.QuittanceCreateRequest;
import com.mini.credit.dto.document.QuittanceResponse;
import com.mini.credit.service.QuittanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quittances")
@RequiredArgsConstructor
@Tag(name = "Quittance", description = "Receipts management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class QuittanceController {

    private final QuittanceService quittanceService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'CAISSIER')")
    @Operation(summary = "Create receipt", description = "Create a new receipt")
    public QuittanceResponse create(@Valid @RequestBody QuittanceCreateRequest request) {
        return quittanceService.create(request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'CAISSIER', 'GESTIONNAIRE', 'AGENT_TERRAIN') or (hasRole('MEMBER') and @quittanceService.isCurrentUserReceipt(#id))")
    @Operation(summary = "Get receipt by ID", description = "Retrieve receipt details")
    public QuittanceResponse getById(@PathVariable Long id) {
        return quittanceService.getById(id);
    }

    @GetMapping("/membre/{membreId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'GESTIONNAIRE', 'AGENT_TERRAIN') or (hasRole('MEMBER') and @quittanceService.isCurrentUserMembre(#membreId))")
    @Operation(summary = "Get receipts by member", description = "Retrieve all receipts for a member")
    public List<QuittanceResponse> getByMembre(@PathVariable Long membreId) {
        return quittanceService.getByMembre(membreId);
    }
}

