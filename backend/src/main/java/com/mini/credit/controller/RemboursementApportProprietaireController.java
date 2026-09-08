package com.mini.credit.controller;

import com.mini.credit.annotation.Auditable;
import com.mini.credit.dto.caisse.RemboursementApportPaiementRequest;
import com.mini.credit.dto.caisse.RemboursementApportRequest;
import com.mini.credit.dto.caisse.RemboursementApportResponse;
import com.mini.credit.dto.caisse.RemboursementApportValidationRequest;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.service.RemboursementApportProprietaireService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/remboursements-apport-proprietaire")
@RequiredArgsConstructor
@Tag(name = "Remboursement apport propriétaire", description = "Workflow recommandé de récupération d'apport propriétaire")
@SecurityRequirement(name = "bearer-jwt")
public class RemboursementApportProprietaireController {

    private final RemboursementApportProprietaireService remboursementService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU')")
    @Operation(summary = "Créer une demande de remboursement d'apport propriétaire")
    @Auditable(action = AuditAction.MODIFICATION_OPERATION, entityType = "RemboursementApportProprietaire", captureParameters = true, captureResult = true, reason = "Demande remboursement apport propriétaire")
    public RemboursementApportResponse demander(@Valid @RequestBody RemboursementApportRequest request) {
        return remboursementService.demander(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'COO', 'GERANT_GENERAL', 'RCI', 'CAISSIER')")
    @Operation(summary = "Lister les remboursements d'apport propriétaire")
    public List<RemboursementApportResponse> lister() {
        return remboursementService.lister();
    }

    @PostMapping("/{id}/valider")
    @PreAuthorize("hasAnyRole('ADMIN', 'COO', 'GERANT_GENERAL')")
    @Operation(summary = "Valider une demande de remboursement d'apport propriétaire")
    @Auditable(action = AuditAction.MODIFICATION_OPERATION, entityType = "RemboursementApportProprietaire", entityIdExpression = "#id", captureParameters = true, captureResult = true, reason = "Validation remboursement apport propriétaire")
    public RemboursementApportResponse valider(
            @PathVariable Long id,
            @Valid @RequestBody RemboursementApportValidationRequest request
    ) {
        return remboursementService.valider(id, request);
    }

    @PostMapping("/{id}/rejeter")
    @PreAuthorize("hasAnyRole('ADMIN', 'COO', 'GERANT_GENERAL')")
    @Operation(summary = "Rejeter une demande de remboursement d'apport propriétaire")
    @Auditable(action = AuditAction.MODIFICATION_OPERATION, entityType = "RemboursementApportProprietaire", entityIdExpression = "#id", captureParameters = true, captureResult = true, reason = "Rejet remboursement apport propriétaire")
    public RemboursementApportResponse rejeter(
            @PathVariable Long id,
            @Valid @RequestBody RemboursementApportValidationRequest request
    ) {
        return remboursementService.rejeter(id, request);
    }

    @PostMapping("/{id}/payer")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAISSIER')")
    @Operation(summary = "Payer un remboursement d'apport propriétaire validé")
    @Auditable(action = AuditAction.OPERATION_CAISSE_CREATED, entityType = "RemboursementApportProprietaire", entityIdExpression = "#id", captureParameters = true, captureResult = true, reason = "Paiement remboursement apport propriétaire")
    public RemboursementApportResponse payer(
            @PathVariable Long id,
            @Valid @RequestBody RemboursementApportPaiementRequest request
    ) {
        return remboursementService.payer(id, request);
    }
}
