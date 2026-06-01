package com.mini.credit.controller.credit;

import com.mini.credit.dto.credit.PaiementInitialDemandeCreditRequest;
import com.mini.credit.dto.credit.PaiementInitialDemandeCreditResponse;
import com.mini.credit.service.PaiementInitialDemandeCreditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/demandes-credit")
@RequiredArgsConstructor
public class DemandeCreditPaiementController {

    private final PaiementInitialDemandeCreditService paiementInitialDemandeCreditService;

    @PostMapping("/{demandeId}/paiement-initial")
    @ResponseStatus(HttpStatus.OK)
    public PaiementInitialDemandeCreditResponse enregistrerPaiementInitial(
            @PathVariable Long demandeId,
            @Valid @RequestBody PaiementInitialDemandeCreditRequest request
    ) {
        return paiementInitialDemandeCreditService.enregistrerPaiementInitial(demandeId, request);
    }
}