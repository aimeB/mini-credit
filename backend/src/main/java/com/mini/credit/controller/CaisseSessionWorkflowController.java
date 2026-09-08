package com.mini.credit.controller;

import com.mini.credit.dto.caisse.CloseSessionCaisseRequest;
import com.mini.credit.dto.caisse.DemanderAnnulationSessionRequest;
import com.mini.credit.dto.caisse.ReouvrirSessionControleeRequest;
import com.mini.credit.dto.caisse.SessionCaisseAnomalieResponse;
import com.mini.credit.dto.caisse.SessionCaisseCloseRequest;
import com.mini.credit.dto.caisse.SessionCaisseResponse;
import com.mini.credit.dto.caisse.SessionCaisseValidationRequest;
import com.mini.credit.dto.caisse.ValiderAnnulationSessionRequest;
import com.mini.credit.service.SessionCaisseAnomalieService;
import com.mini.credit.service.SessionCaisseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/caisses/sessions")
@RequiredArgsConstructor
@Tag(name = "Workflow Cloture Caisse", description = "Workflow de cloture et controle des sessions caisse")
@SecurityRequirement(name = "bearer-jwt")
public class CaisseSessionWorkflowController {

    private final SessionCaisseService sessionCaisseService;
    private final SessionCaisseAnomalieService sessionCaisseAnomalieService;

    @PostMapping("/{id}/pre-cloturer")
    @PreAuthorize("hasAuthority('SESSION_CAISSE_PRE_CLOSE')")
    @Operation(summary = "PrÃ©-clÃ´turer session caisse", description = "PrÃ©-clÃ´ture d'une session avec solde physique et observation")
    public SessionCaisseResponse preCloturer(@PathVariable Long id,
                                             @Valid @RequestBody CloseSessionCaisseRequest request) {
        SessionCaisseCloseRequest closeRequest = new SessionCaisseCloseRequest();
        closeRequest.setDateCloture(LocalDateTime.now());
        closeRequest.setSoldePhysique(request.getSoldePhysique());
        closeRequest.setObservation(request.getObservation());
        return sessionCaisseService.preCloturer(id, closeRequest);
    }

    @PostMapping("/{id}/cloturer")
    @PreAuthorize("hasAuthority('SESSION_CAISSE_PRE_CLOSE')")
    @Operation(summary = "Cloturer session caisse (alias prÃ©-clÃ´ture)", description = "Alias backward compatible vers la prÃ©-clÃ´ture")
    public SessionCaisseResponse cloturer(@PathVariable Long id,
                                          @Valid @RequestBody CloseSessionCaisseRequest request) {
        SessionCaisseCloseRequest closeRequest = new SessionCaisseCloseRequest();
        closeRequest.setDateCloture(LocalDateTime.now());
        closeRequest.setSoldePhysique(request.getSoldePhysique());
        closeRequest.setObservation(request.getObservation());
        return sessionCaisseService.preCloturer(id, closeRequest);
    }

    @PostMapping("/{id}/valider-controle")
    @PreAuthorize("hasAnyAuthority('SESSION_CAISSE_CONTROL_VALIDATE', 'CONTROLEUR_SESSION_CAISSE_VALIDATE')")
    @Operation(summary = "Valider controle", description = "Validation de controle final par CONTROLEUR ou ADMIN")
    public SessionCaisseResponse validerControle(@PathVariable Long id,
                                                 @RequestBody(required = false) SessionCaisseValidationRequest request) {
        return sessionCaisseService.validerControle(id, request != null ? request.getObservation() : null);
    }

    @PostMapping("/{id}/cloturer-finale")
    @PreAuthorize("hasAuthority('SESSION_CAISSE_FINAL_CLOSE')")
    @Operation(summary = "ClÃ´ture finale", description = "ClÃ´ture finale aprÃ¨s validation du contrÃ´le")
    public SessionCaisseResponse cloturerFinale(@PathVariable Long id,
                                                @RequestBody(required = false) SessionCaisseValidationRequest request) {
        return sessionCaisseService.cloturerFinale(id, request != null ? request.getObservation() : null);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CAISSE_READ', 'CONTROLEUR_SESSION_CAISSE_READ')")
    @Operation(summary = "Detail session caisse", description = "Retourne soldes et mouvements de la session")
    public SessionCaisseResponse getById(@PathVariable Long id) {
        return sessionCaisseService.getById(id);
    }

    @PostMapping("/{id}/demander-annulation")
    @PreAuthorize("hasAuthority('SESSION_CAISSE_ANOMALIE_REQUEST')")
    public SessionCaisseAnomalieResponse demanderAnnulation(@PathVariable Long id,
                                                            @RequestBody DemanderAnnulationSessionRequest request) {
        return sessionCaisseAnomalieService.demanderAnnulation(id, request);
    }

    @PostMapping("/{id}/valider-annulation")
    @PreAuthorize("hasAuthority('SESSION_CAISSE_ANOMALIE_VALIDATE')")
    public SessionCaisseAnomalieResponse validerAnnulation(@PathVariable Long id,
                                                           @RequestBody ValiderAnnulationSessionRequest request) {
        return sessionCaisseAnomalieService.validerAnnulation(id, request);
    }

    @PostMapping("/{id}/annuler-administrativement")
    @PreAuthorize("hasAuthority('SESSION_CAISSE_ADMIN_CANCEL')")
    public SessionCaisseResponse annulerAdministrativement(@PathVariable Long id,
                                                           @RequestBody DemanderAnnulationSessionRequest request) {
        return sessionCaisseAnomalieService.annulerAdministrativement(id, request);
    }

    @PostMapping("/{id}/reouvrir-controlee")
    @PreAuthorize("hasAuthority('SESSION_CAISSE_REOPEN_CONTROLLED')")
    public SessionCaisseResponse reouvrirControlee(@PathVariable Long id,
                                                   @RequestBody ReouvrirSessionControleeRequest request) {
        return sessionCaisseAnomalieService.reouvrirControlee(id, request);
    }

    @GetMapping("/{id}/anomalies")
    @PreAuthorize("hasAnyAuthority('SESSION_CAISSE_ANOMALIE_READ', 'CONTROLEUR_ECART_READ')")
    public List<SessionCaisseAnomalieResponse> getAnomaliesBySession(@PathVariable Long id) {
        return sessionCaisseAnomalieService.getAnomaliesBySession(id);
    }

    @GetMapping("/anomalies")
    @PreAuthorize("hasAnyAuthority('SESSION_CAISSE_ANOMALIE_READ', 'CONTROLEUR_ECART_READ')")
    public List<SessionCaisseAnomalieResponse> getAnomaliesGlobales() {
        return sessionCaisseAnomalieService.getAnomaliesGlobales();
    }
}

