package com.mini.credit.controller;

import com.mini.credit.annotation.Auditable;
import com.mini.credit.dto.credit.AnalyseRisqueRequest;
import com.mini.credit.dto.credit.DemandeCreditCreateRequest;
import com.mini.credit.dto.credit.DemandeCreditResponse;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.service.DemandeCreditService;
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
@RequestMapping("/api/demandes-credit")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Demande Credit", description = "Credit request management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class DemandeCreditController {

    private final DemandeCreditService demandeCreditService;
    private final ScopeService scopeService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT_BUREAU', 'MEMBER')")
    @Operation(summary = "Create credit request", description = "Create a new credit request")
    @Auditable(action = AuditAction.DEMANDE_CREDIT_CREATED, entityType = "DemandeCredit", captureParameters = true, captureResult = true)
    public DemandeCreditResponse create(@Valid @RequestBody DemandeCreditCreateRequest request) {
        return demandeCreditService.create(request);
    }

    @PostMapping("/{id}/analyse")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE')")
    @Operation(summary = "Add risk analysis", description = "Add risk analysis to credit request")
    public DemandeCreditResponse ajouterAnalyse(@PathVariable Long id,
                                                @Valid @RequestBody AnalyseRisqueRequest request) {
        return demandeCreditService.ajouterAnalyse(id, request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@scopeService.canReadDemandeCredit(#id)")
    @Operation(summary = "Get credit request by ID", description = "Retrieve credit request details")
    public DemandeCreditResponse getById(@PathVariable Long id) {
        return demandeCreditService.getById(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE', 'AGENT_BUREAU')")
    @Operation(summary = "Get all credit requests", description = "Retrieve paginated list of credit requests")
    public Page<DemandeCreditResponse> getAll(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return demandeCreditService.getAll(pageable);
    }

    @GetMapping("/membre/{membreId}")
    @PreAuthorize("@scopeService.canReadMembre(#membreId)")
    @Operation(summary = "Get credit requests by member", description = "Retrieve paginated credit requests for a specific member")
    public Page<DemandeCreditResponse> getByMembre(@PathVariable Long membreId,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        // PHASE 3B: Limiter size max à 100 pour éviter les abus
        if (size > 100) size = 100;
        Pageable pageable = PageRequest.of(page, size);
        return demandeCreditService.getByMembre(membreId, pageable);
    }

    @GetMapping("/mes-demandes")
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "Get current member's credit requests", description = "Get all credit requests for the current authenticated member")
    public List<DemandeCreditResponse> getMesDemandes() {
        return demandeCreditService.getCurrentMemberRequests();
    }

    // ============ PHASE 4: VALIDATION CRÉDIT STRICTE ============

    @GetMapping("/{id}/validation")
    @PreAuthorize("hasAuthority('CONTROLEUR_CREDITS_VALIDATE')")
    @Operation(summary = "Validate credit request (PHASE 4)", description = "Check if credit request meets all PHASE 4 validation criteria: fees paid, guarantee deposited, terrain analysis complete")
    @Auditable(action = AuditAction.CREDIT_VALIDATION_CHECKED, entityType = "DemandeCredit")
    public com.mini.credit.dto.credit.CreditValidationResult validateCredit(@PathVariable Long id) {
        return demandeCreditService.validerCredit(id);
    }
}
