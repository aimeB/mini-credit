package com.mini.credit.service;

import com.mini.credit.dto.credit.PaiementInitialDemandeCreditRequest;
import com.mini.credit.dto.credit.PaiementInitialDemandeCreditResponse;

public interface PaiementInitialDemandeCreditService {

    PaiementInitialDemandeCreditResponse enregistrerPaiementInitial(
            Long demandeId,
            PaiementInitialDemandeCreditRequest request
    );
}