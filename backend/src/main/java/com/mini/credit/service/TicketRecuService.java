package com.mini.credit.service;

import com.mini.credit.dto.document.TicketDuplicataRequest;
import com.mini.credit.dto.document.TicketPrintRequest;
import com.mini.credit.dto.document.TicketRecuGenerationRequest;
import com.mini.credit.dto.document.TicketRecuResponse;
import com.mini.credit.dto.document.TicketVerificationResponse;

import java.util.List;

public interface TicketRecuService {
    TicketRecuResponse genererDepuisOperation(TicketRecuGenerationRequest request);
    TicketRecuResponse getById(Long id);
    List<TicketRecuResponse> getByOperationEpargne(Long operationEpargneId);
    List<TicketRecuResponse> getByDemandeRetrait(Long demandeRetraitId);
    List<TicketRecuResponse> getByMembre(Long membreId);
    TicketRecuResponse marquerImpression(Long id, TicketPrintRequest request);
    TicketRecuResponse genererDuplicata(Long id, TicketDuplicataRequest request);
    String getPrintableHtml(Long id, boolean duplicata);
    TicketVerificationResponse verify(String codeVerification);
    boolean canReadTicket(Long id);
    boolean canReadMembreTickets(Long membreId);
}
