package com.mini.credit.controller;

import com.mini.credit.annotation.Auditable;
import com.mini.credit.dto.credit.ApprobationCreditRequest;
import com.mini.credit.dto.credit.CreditResponse;
import com.mini.credit.dto.credit.DecaissementCreditRequest;
import com.mini.credit.dto.credit.RemboursementRequest;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.service.CreditService;
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
@RequestMapping("/api/credits")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Credit", description = "Credit management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class CreditController {

    private final CreditService creditService;
    private final ScopeService scopeService;

    @PostMapping("/demande/{demandeId}/approbation")
    @PreAuthorize("hasAuthority('CREDIT_APPROVE')")
    @Operation(summary = "Approve credit request", description = "Approve a credit request and create contract")
    @Auditable(action = AuditAction.CREDIT_APPROVED, entityType = "Credit", entityIdExpression = "#demandeId", captureResult = true)
    public CreditResponse approuver(@PathVariable Long demandeId,
                                    @Valid @RequestBody ApprobationCreditRequest request) {
        return creditService.approuverDemande(demandeId, request);
    }

    @PostMapping("/{creditId}/remboursements")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE', 'CAISSIER', 'MEMBER')")
    @Operation(summary = "Register credit repayment", description = "Register a repayment for a credit")
    @Auditable(action = AuditAction.REMBOURSEMENT_CREATED, entityType = "Credit", entityIdExpression = "#creditId", captureParameters = true)
    public void rembourser(@PathVariable Long creditId,
                           @Valid @RequestBody RemboursementRequest request) {
        creditService.enregistrerRemboursement(creditId, request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@scopeService.canReadCredit(#id)")
    @Operation(summary = "Get credit by ID", description = "Retrieve credit details")
    public CreditResponse getById(@PathVariable Long id) {
        return creditService.getById(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE', 'AGENT_BUREAU', 'AGENT_TERRAIN')")
    @Operation(summary = "Get all credits", description = "Retrieve paginated list of all credits")
    public Page<CreditResponse> getAll(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int size) {
        // PHASE 3B: Limiter size max à 100 pour éviter les abus
        if (size > 100) size = 100;
        Pageable pageable = PageRequest.of(page, size);
        return creditService.getAll(pageable);
    }

    @GetMapping("/membre/{membreId}")
    @PreAuthorize("@scopeService.canReadMembre(#membreId)")
    @Operation(summary = "Get credits by member", description = "Retrieve all credits for a specific member")
    public List<CreditResponse> getByMembre(@PathVariable Long membreId) {
        return creditService.getByMembre(membreId);
    }

    @PostMapping("/{creditId}/decaissement")
    @PreAuthorize("hasAuthority('CREDIT_DISBURSE')")
    @Operation(summary = "Disburse credit", description = "Disburse funds for an approved credit")
    public CreditResponse decaisser(@PathVariable Long creditId,
                                    @Valid @RequestBody DecaissementCreditRequest request) {
        return creditService.decaisserCredit(creditId, request);
    }
}
