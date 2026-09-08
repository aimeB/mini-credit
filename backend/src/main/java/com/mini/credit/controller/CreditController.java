package com.mini.credit.controller;

import com.mini.credit.annotation.Auditable;
import com.mini.credit.dto.credit.ApprobationCreditRequest;
import com.mini.credit.dto.credit.CreditContratResponse;
import com.mini.credit.dto.credit.CreditEnCoursResponse;
import com.mini.credit.dto.credit.CreditDetailResponse;
import com.mini.credit.dto.credit.CreditRembourseResponse;
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
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'CAISSIER', 'MEMBER')")
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

    @GetMapping("/{id}/detail")
    @PreAuthorize("@scopeService.canReadCredit(#id)")
    @Operation(summary = "Get credit file detail", description = "Retrieve consolidated credit file detail")
    public CreditDetailResponse getDetail(@PathVariable Long id) {
        return creditService.getDetail(id);
    }

    @GetMapping("/{id}/contrat")
    @PreAuthorize("@scopeService.canReadCredit(#id)")
    @Operation(summary = "Get credit contract", description = "Retrieve printable credit contract data")
    public CreditContratResponse getContrat(@PathVariable Long id) {
        return creditService.getContrat(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'GESTIONNAIRE', 'AGENT_TERRAIN')")
    @Operation(summary = "Get all credits", description = "Retrieve paginated list of all credits")
    public Page<CreditResponse> getAll(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int size) {
        // PHASE 3B: Limiter size max Ã  100 pour Ã©viter les abus
        if (size > 100) size = 100;
        Pageable pageable = PageRequest.of(page, size);
        return creditService.getAll(pageable);
    }

    @GetMapping("/a-decaisser")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'GESTIONNAIRE', 'CAISSIER', 'CONTROLEUR')")
    @Operation(summary = "Get approved credits to disburse", description = "Retrieve approved credits awaiting disbursement")
    public List<CreditResponse> getCreditsADecaisser() {
        return creditService.getCreditsADecaisser();
    }

    @GetMapping("/en-cours")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'GESTIONNAIRE', 'CAISSIER', 'CONTROLEUR')")
    @Operation(summary = "Get active credits", description = "Retrieve approved and disbursed credits that are not closed")
    public List<CreditEnCoursResponse> getCreditsEnCours() {
        return creditService.getCreditsEnCours();
    }

    @GetMapping("/rembourses")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'CONTROLEUR', 'COO', 'RCI', 'GERANT_GENERAL')")
    @Operation(summary = "Get repaid credits", description = "Retrieve credits fully repaid or closed")
    public List<CreditRembourseResponse> getCreditsRembourses() {
        return creditService.getCreditsRembourses();
    }

    @GetMapping("/membre/{membreId}")
    @PreAuthorize("@scopeService.canReadMembre(#membreId)")
    @Operation(summary = "Get credits by member", description = "Retrieve all credits for a specific member")
    public List<CreditResponse> getByMembre(@PathVariable Long membreId) {
        return creditService.getByMembre(membreId);
    }

    @PostMapping("/{creditId}/decaissement")
    @PreAuthorize("hasRole('CAISSIER')")
    @Operation(summary = "Disburse credit", description = "Disburse funds for an approved credit")
    public CreditResponse decaisser(@PathVariable Long creditId,
                                    @Valid @RequestBody DecaissementCreditRequest request) {
        return creditService.decaisserCredit(creditId, request);
    }
}

