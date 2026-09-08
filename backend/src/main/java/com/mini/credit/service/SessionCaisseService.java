package com.mini.credit.service;

import com.mini.credit.dto.caisse.SessionCaisseCloseRequest;
import com.mini.credit.dto.caisse.SessionCaisseOpeningContextResponse;
import com.mini.credit.dto.caisse.SessionCaisseOpenRequest;
import com.mini.credit.dto.caisse.SessionCaisseResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface SessionCaisseService {

    SessionCaisseResponse ouvrir(SessionCaisseOpenRequest request);

    SessionCaisseResponse preCloturer(Long sessionId, SessionCaisseCloseRequest request);

    SessionCaisseResponse cloturer(Long sessionId, SessionCaisseCloseRequest request);

    SessionCaisseResponse cloturerFinale(Long sessionId, String observation);

    SessionCaisseResponse getById(Long id);

    List<SessionCaisseResponse> getAll();

    SessionCaisseResponse getSessionActive();

    SessionCaisseResponse getSessionOuverteByCaisse(Long caisseId);

    SessionCaisseResponse validerControle(Long sessionId, String observation);

    SessionCaisseOpeningContextResponse getOpeningContext(Long caisseId, LocalDate dateComptable);

    BigDecimal calculerSoldeOuvertureAutomatique(Long caisseId, LocalDate dateComptable);
}