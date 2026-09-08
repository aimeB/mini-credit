package com.mini.credit.controller;

import com.mini.credit.dto.document.TicketDuplicataRequest;
import com.mini.credit.dto.document.TicketPrintRequest;
import com.mini.credit.dto.document.TicketRecuResponse;
import com.mini.credit.dto.document.TicketVerificationResponse;
import com.mini.credit.service.TicketRecuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets-recus")
@RequiredArgsConstructor
@Tag(name = "Tickets Reçus", description = "Tickets/reçus imprimables des opérations épargne cash")
@SecurityRequirement(name = "bearer-jwt")
public class TicketRecuController {

    private final TicketRecuService ticketRecuService;

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('TICKET_RECU_READ') and @ticketRecuService.canReadTicket(#id)")
    @Operation(summary = "Détail ticket reçu")
    public TicketRecuResponse getById(@PathVariable Long id) {
        return ticketRecuService.getById(id);
    }

    @GetMapping("/by-operation-epargne/{operationEpargneId}")
    @PreAuthorize("hasAuthority('TICKET_RECU_READ')")
    @Operation(summary = "Tickets liés à une opération épargne")
    public List<TicketRecuResponse> getByOperationEpargne(@PathVariable Long operationEpargneId) {
        return ticketRecuService.getByOperationEpargne(operationEpargneId);
    }

    @GetMapping("/by-demande-retrait/{demandeRetraitId}")
    @PreAuthorize("hasAuthority('TICKET_RECU_READ')")
    @Operation(summary = "Tickets liés à une demande retrait")
    public List<TicketRecuResponse> getByDemandeRetrait(@PathVariable Long demandeRetraitId) {
        return ticketRecuService.getByDemandeRetrait(demandeRetraitId);
    }

    @GetMapping("/by-membre/{membreId}")
    @PreAuthorize("hasAuthority('TICKET_RECU_READ') and @ticketRecuService.canReadMembreTickets(#membreId)")
    @Operation(summary = "Historique tickets d'un membre")
    public List<TicketRecuResponse> getByMembre(@PathVariable Long membreId) {
        return ticketRecuService.getByMembre(membreId);
    }

    @PostMapping("/{id}/impression")
    @PreAuthorize("hasAuthority('TICKET_RECU_PRINT') and @ticketRecuService.canReadTicket(#id)")
    @Operation(summary = "Marquer impression ou échec impression")
    public TicketRecuResponse marquerImpression(@PathVariable Long id, @Valid @RequestBody TicketPrintRequest request) {
        return ticketRecuService.marquerImpression(id, request);
    }

    @PostMapping("/{id}/duplicata")
    @PreAuthorize("hasAuthority('TICKET_RECU_DUPLICATA') and @ticketRecuService.canReadTicket(#id)")
    @Operation(summary = "Générer duplicata de ticket")
    public TicketRecuResponse genererDuplicata(@PathVariable Long id, @Valid @RequestBody TicketDuplicataRequest request) {
        return ticketRecuService.genererDuplicata(id, request);
    }

    @GetMapping(value = "/{id}/print", produces = MediaType.TEXT_HTML_VALUE)
    @PreAuthorize("hasAuthority('TICKET_RECU_PRINT') and @ticketRecuService.canReadTicket(#id)")
    @Operation(summary = "HTML imprimable ticket thermique")
    public String getPrintableHtml(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean duplicata) {
        return ticketRecuService.getPrintableHtml(id, duplicata);
    }

    @GetMapping("/verify/{codeVerification}")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Vérifier authenticité ticket reçu")
    public TicketVerificationResponse verify(@PathVariable String codeVerification) {
        return ticketRecuService.verify(codeVerification);
    }
}
