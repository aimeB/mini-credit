package com.mini.credit.service;

import com.mini.credit.dto.caisse.SessionCaisseCloseRequest;
import com.mini.credit.dto.caisse.SessionCaisseOpenRequest;
import com.mini.credit.dto.caisse.SessionCaisseResponse;

import java.util.List;

public interface SessionCaisseService {

    SessionCaisseResponse ouvrir(SessionCaisseOpenRequest request);

    SessionCaisseResponse cloturer(Long sessionId, SessionCaisseCloseRequest request);

    SessionCaisseResponse getById(Long id);

    List<SessionCaisseResponse> getAll();

    SessionCaisseResponse getSessionActive();
}