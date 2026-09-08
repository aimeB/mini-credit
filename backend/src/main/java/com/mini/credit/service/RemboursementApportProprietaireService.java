package com.mini.credit.service;

import com.mini.credit.dto.caisse.RemboursementApportPaiementRequest;
import com.mini.credit.dto.caisse.RemboursementApportRequest;
import com.mini.credit.dto.caisse.RemboursementApportResponse;
import com.mini.credit.dto.caisse.RemboursementApportValidationRequest;

import java.util.List;

public interface RemboursementApportProprietaireService {
    RemboursementApportResponse demander(RemboursementApportRequest request);
    RemboursementApportResponse valider(Long id, RemboursementApportValidationRequest request);
    RemboursementApportResponse rejeter(Long id, RemboursementApportValidationRequest request);
    RemboursementApportResponse payer(Long id, RemboursementApportPaiementRequest request);
    List<RemboursementApportResponse> lister();
}
