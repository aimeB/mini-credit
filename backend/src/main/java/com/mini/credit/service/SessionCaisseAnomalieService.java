package com.mini.credit.service;

import com.mini.credit.dto.caisse.DemanderAnnulationSessionRequest;
import com.mini.credit.dto.caisse.ReouvrirSessionControleeRequest;
import com.mini.credit.dto.caisse.SessionCaisseAnomalieResponse;
import com.mini.credit.dto.caisse.SessionCaisseResponse;
import com.mini.credit.dto.caisse.ValiderAnnulationSessionRequest;

import java.util.List;

public interface SessionCaisseAnomalieService {

    SessionCaisseAnomalieResponse demanderAnnulation(Long sessionId, DemanderAnnulationSessionRequest request);

    SessionCaisseAnomalieResponse validerAnnulation(Long sessionId, ValiderAnnulationSessionRequest request);

    SessionCaisseResponse annulerAdministrativement(Long sessionId, DemanderAnnulationSessionRequest request);

    SessionCaisseResponse annulerSessionTest(Long sessionId, DemanderAnnulationSessionRequest request);

    SessionCaisseResponse reouvrirControlee(Long sessionId, ReouvrirSessionControleeRequest request);

    List<SessionCaisseAnomalieResponse> getAnomaliesBySession(Long sessionId);

    List<SessionCaisseAnomalieResponse> getAnomaliesGlobales();
}
