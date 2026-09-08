package com.mini.credit.service;

import com.mini.credit.dto.credit.ApprobationCreditRequest;
import com.mini.credit.dto.credit.CreditContratResponse;
import com.mini.credit.dto.credit.CreditEnCoursResponse;
import com.mini.credit.dto.credit.CreditDetailResponse;
import com.mini.credit.dto.credit.CreditRembourseResponse;
import com.mini.credit.dto.credit.CreditResponse;
import com.mini.credit.dto.credit.DecaissementCreditRequest;
import com.mini.credit.dto.credit.RemboursementRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CreditService {
    CreditResponse approuverDemande(Long demandeId, ApprobationCreditRequest request);
    CreditResponse getById(Long id);
    CreditDetailResponse getDetail(Long id);
    CreditContratResponse getContrat(Long id);
    List<CreditResponse> getAll();  // Keep for backward compatibility
    Page<CreditResponse> getAll(Pageable pageable);  // PHASE 3B: New paginated version
    List<CreditResponse> getCreditsADecaisser();
    List<CreditEnCoursResponse> getCreditsEnCours();
    List<CreditRembourseResponse> getCreditsRembourses();
    List<CreditResponse> getByMembre(Long membreId);
    CreditResponse decaisserCredit(Long creditId, DecaissementCreditRequest request);
    void enregistrerRemboursement(Long creditId, RemboursementRequest request);
    Long enregistrerRemboursementDepuisCollecte(Long creditId,
                                                RemboursementRequest request,
                                                Long collecteId,
                                                Long ligneCollecteId,
                                                Long validateurId,
                                                Long antenneId);

}