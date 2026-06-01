package com.mini.credit.service;

import com.mini.credit.dto.credit.ApprobationCreditRequest;
import com.mini.credit.dto.credit.CreditResponse;
import com.mini.credit.dto.credit.DecaissementCreditRequest;
import com.mini.credit.dto.credit.RemboursementRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CreditService {
    CreditResponse approuverDemande(Long demandeId, ApprobationCreditRequest request);
    CreditResponse getById(Long id);
    List<CreditResponse> getAll();  // Keep for backward compatibility
    Page<CreditResponse> getAll(Pageable pageable);  // PHASE 3B: New paginated version
    List<CreditResponse> getByMembre(Long membreId);
    CreditResponse decaisserCredit(Long creditId, DecaissementCreditRequest request);
    void enregistrerRemboursement(Long creditId, RemboursementRequest request);

}