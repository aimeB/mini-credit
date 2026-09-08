package com.mini.credit.service;

import com.mini.credit.dto.garantie.AjouterGarantieMaterielleRequest;
import com.mini.credit.dto.garantie.BloquerGarantieEpargneRequest;
import com.mini.credit.dto.garantie.GarantieCreditResponse;
import com.mini.credit.dto.garantie.GarantieMaterielleResponse;
import com.mini.credit.dto.garantie.RejeterGarantieRequest;
import com.mini.credit.dto.garantie.ValiderGarantieRequest;
import com.mini.credit.dto.garantie.VerifierGarantieCreditRequest;

import java.util.List;

public interface GarantieCreditWorkflowService {
    GarantieCreditResponse getByDemandeCreditId(Long demandeCreditId);

    GarantieCreditResponse verifier(Long demandeCreditId, VerifierGarantieCreditRequest request);

    GarantieCreditResponse bloquerEpargne(Long demandeCreditId, BloquerGarantieEpargneRequest request);

    GarantieMaterielleResponse ajouterGarantieMaterielle(Long demandeCreditId, AjouterGarantieMaterielleRequest request);

    GarantieMaterielleResponse accepterGarantieMaterielle(Long demandeCreditId, Long garantieMaterielleId, String commentaire);

    GarantieMaterielleResponse refuserGarantieMaterielle(Long demandeCreditId, Long garantieMaterielleId, String commentaire);

    GarantieCreditResponse valider(Long demandeCreditId, ValiderGarantieRequest request);

    GarantieCreditResponse rejeter(Long demandeCreditId, RejeterGarantieRequest request);

    List<GarantieMaterielleResponse> getGarantiesMaterielles(Long demandeCreditId);

    boolean isGarantieBloquee(Long demandeCreditId);

    boolean isGarantieValidee(Long demandeCreditId);

    void libererGarantie(Long demandeCreditId, String commentaire);
}