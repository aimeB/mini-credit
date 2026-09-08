package com.mini.credit.controller;

import com.mini.credit.dto.credit.PaiementInitialDemandeCreditRequest;
import com.mini.credit.dto.credit.PaiementInitialDemandeCreditResponse;
import com.mini.credit.service.PaiementInitialDemandeCreditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/demandes-credit")
@RequiredArgsConstructor
public class DemandeCreditPaiementController {

    private final PaiementInitialDemandeCreditService paiementInitialDemandeCreditService;

    @PostMapping("/{demandeId}/paiement-initial")
    @PreAuthorize("hasRole('CAISSIER')")
    @ResponseStatus(HttpStatus.OK)
    public PaiementInitialDemandeCreditResponse enregistrerPaiementInitial(
            @PathVariable Long demandeId,
            @Valid @RequestBody PaiementInitialDemandeCreditRequest request
    ) {
        return paiementInitialDemandeCreditService.enregistrerPaiementInitial(demandeId, request);
    }
}