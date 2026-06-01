package com.mini.credit.controller;

import com.mini.credit.dto.credit.PaiementInitialDemandeCreditRequest;
import com.mini.credit.dto.credit.PaiementInitialDemandeCreditResponse;
import com.mini.credit.service.PaiementInitialDemandeCreditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/demandes-credit")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Paiement Initial Demande Credit", description = "Initial payment for credit request endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class PaiementInitialDemandeCreditController {

    private final PaiementInitialDemandeCreditService service;

    @PostMapping("/{id}/paiement-initial")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE', 'CAISSIER', 'MEMBER')")
    @Operation(summary = "Record initial payment", description = "Record initial payment for a credit request")
    public PaiementInitialDemandeCreditResponse payer(
            @PathVariable Long id,
            @Valid @RequestBody PaiementInitialDemandeCreditRequest request
    ) {
        return service.enregistrerPaiementInitial(id, request);
    }
}
