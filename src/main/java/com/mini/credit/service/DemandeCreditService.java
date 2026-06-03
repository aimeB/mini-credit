package com.mini.credit.service;

import com.mini.credit.dto.credit.AnalyseRisqueRequest;
import com.mini.credit.dto.credit.CreditValidationResult;
import com.mini.credit.dto.credit.DemandeCreditCreateRequest;
import com.mini.credit.dto.credit.DemandeCreditResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DemandeCreditService {
    DemandeCreditResponse create(DemandeCreditCreateRequest request);
    DemandeCreditResponse getById(Long id);
    Page<DemandeCreditResponse> getAll(Pageable pageable);
    List<DemandeCreditResponse> getAll();
    Page<DemandeCreditResponse> getByMembre(Long membreId, Pageable pageable);  // PHASE 3B: Added Pageable
    List<DemandeCreditResponse> getByMembre(Long membreId);
    DemandeCreditResponse ajouterAnalyse(Long demandeId, AnalyseRisqueRequest request);
    boolean isCurrentUserRequest(Long demandeId);
    boolean isCurrentUserMembre(Long membreId);
    List<DemandeCreditResponse> getCurrentMemberRequests();

    /**
     * PHASE 4: Valide les critères strictes de crédit (frais, garantie, analyse terrain)
     */
    CreditValidationResult validerCredit(Long demandeId);
}