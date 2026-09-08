package com.mini.credit.service;

import com.mini.credit.dto.caisse.OperationCaisseRequest;
import com.mini.credit.dto.caisse.OperationCaisseResponse;
import com.mini.credit.dto.caisse.JournalCaisseFilterRequest;
import com.mini.credit.dto.caisse.JournalCaisseResponse;
import com.mini.credit.dto.caisse.RequalificationNatureFinancementRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OperationCaisseService {

    OperationCaisseResponse enregistrer(OperationCaisseRequest request);

    OperationCaisseResponse enregistrerDepuisCollecteValidee(OperationCaisseRequest request);

    List<OperationCaisseResponse> getBySession(Long sessionId);

    List<OperationCaisseResponse> getByCaisse(Long caisseId);

    List<OperationCaisseResponse> getAll();  // Keep for backward compatibility
    Page<OperationCaisseResponse> getAll(Pageable pageable);  // PHASE 3B: New paginated version

    Page<JournalCaisseResponse> getJournal(JournalCaisseFilterRequest filter, Pageable pageable);

    OperationCaisseResponse requalifierNatureFinancement(Long operationId, RequalificationNatureFinancementRequest request);
}