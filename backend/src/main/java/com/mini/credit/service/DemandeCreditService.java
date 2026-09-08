package com.mini.credit.service;

import com.mini.credit.dto.credit.AnalyseRisqueRequest;
import com.mini.credit.dto.credit.CreditValidationResult;
import com.mini.credit.dto.credit.DemandeCreditCreateRequest;
import com.mini.credit.dto.credit.DemandeCreditResponse;
import com.mini.credit.dto.credit.FraisCreditAEncaisserResponse;
import com.mini.credit.dto.credit.PreAnalyseRequest;
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
    DemandeCreditResponse preAnalyser(Long demandeId, String commentaire);
    DemandeCreditResponse preAnalyserDecision(Long demandeId, PreAnalyseRequest request);
    DemandeCreditResponse enregistrerObservationRisque(Long demandeId, String commentaire);
    DemandeCreditResponse validerAnalyseRisque(Long demandeId, String commentaire);
    DemandeCreditResponse controlerRisque(Long demandeId, String commentaire);
    DemandeCreditResponse controlerGarantie(Long demandeId, String commentaire);
    DemandeCreditResponse rejeter(Long demandeId, String commentaire);
    boolean isCurrentUserRequest(Long demandeId);
    boolean isCurrentUserMembre(Long membreId);
    List<DemandeCreditResponse> getCurrentMemberRequests();
    List<FraisCreditAEncaisserResponse> getFraisCreditAEncaisser();

    /**
     * PHASE 4: Valide les critères strictes de crédit (frais, garantie, analyse terrain)
     */
    CreditValidationResult validerCredit(Long demandeId);
}