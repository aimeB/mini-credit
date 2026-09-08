package com.mini.credit.service;

import com.mini.credit.dto.caisse.*;

import java.time.LocalDate;
import java.util.List;

public interface DepenseCaisseService {

    DepenseCaisseResponse creer(DepenseCaisseCreateRequest request);

    List<DepenseCaisseBeneficiaireSalaireResponse> getBeneficiairesSalaire(Long caisseId);

    PaieEmployePreviewResponse getPaiePreview(Long employeId, String periodePaie);

    DepenseCaisseResponse rattacherPaie(Long id, DepenseCaisseRattachementPaieRequest request);

    DepenseCaisseResponse rattacherTransport(Long id, DepenseCaisseRattachementTransportRequest request);

    List<DepenseCaisseResponse> getAll(String statut, Long caisseId, Long siteId, Long sessionCaisseId, LocalDate dateDebut, LocalDate dateFin);

    DepenseCaisseResponse getById(Long id);

    DepenseCaisseResponse soumettre(Long id, DepenseCaisseSubmitRequest request);

    DepenseCaisseResponse valider(Long id, DepenseCaisseValidateRequest request);

    DepenseCaisseResponse rejeter(Long id, DepenseCaisseRejectRequest request);

    DepenseCaisseResponse payer(Long id, DepenseCaissePayRequest request);

    DepenseCaisseResponse annuler(Long id, String commentaire);
}