package com.mini.credit.controller;

import com.mini.credit.dto.caisse.SessionCaisseCloseRequest;
import com.mini.credit.dto.caisse.DemanderAnnulationSessionRequest;
import com.mini.credit.dto.caisse.SessionCaisseOpeningContextResponse;
import com.mini.credit.dto.caisse.SessionCaisseOpenRequest;
import com.mini.credit.dto.caisse.SessionCaisseResponse;
import com.mini.credit.service.SessionCaisseAnomalieService;
import com.mini.credit.service.SessionCaisseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/sessions-caisse")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Session Caisse", description = "Caisse session management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class SessionCaisseController {

    private final SessionCaisseService sessionCaisseService;
    private final SessionCaisseAnomalieService sessionCaisseAnomalieService;

    @PostMapping("/ouverture")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAISSIER', 'CHEF_BUREAU')")
    @Operation(summary = "Open caisse session", description = "Open a new caisse session")
    public SessionCaisseResponse ouvrir(@Valid @RequestBody SessionCaisseOpenRequest request) {
        return sessionCaisseService.ouvrir(request);
    }

    @GetMapping("/ouverture-context")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAISSIER', 'CHEF_BUREAU')")
    @Operation(summary = "Get opening context", description = "Get opening context for caisse session")
    public SessionCaisseOpeningContextResponse getOpeningContext(
            @RequestParam Long caisseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateComptable
    ) {
        return sessionCaisseService.getOpeningContext(caisseId, dateComptable);
    }

    @PostMapping("/{id}/cloture")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAISSIER', 'CHEF_BUREAU')")
    @Operation(summary = "Close caisse session", description = "Close an open caisse session")
    public SessionCaisseResponse cloturer(@PathVariable Long id,
                                          @Valid @RequestBody SessionCaisseCloseRequest request) {
        return sessionCaisseService.cloturer(id, request);
    }

    @PostMapping("/{id}/annuler-test")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Annuler une session de test", description = "Annulation logique d'une session ouverte par erreur, sans mouvement ni session suivante")
    public SessionCaisseResponse annulerTest(@PathVariable Long id,
                                             @RequestBody DemanderAnnulationSessionRequest request) {
        return sessionCaisseAnomalieService.annulerSessionTest(id, request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'CAISSIER', 'CONTROLEUR', 'RCI', 'GERANT_GENERAL')")
    @Operation(summary = "Get caisse session by ID", description = "Retrieve caisse session details")
    public SessionCaisseResponse getById(@PathVariable Long id) {
        return sessionCaisseService.getById(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'CAISSIER', 'CONTROLEUR', 'RCI', 'GERANT_GENERAL')")
    @Operation(summary = "Get all caisse sessions", description = "Retrieve list of all caisse sessions")
    public List<SessionCaisseResponse> getAll() {
        return sessionCaisseService.getAll();
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'CAISSIER', 'CONTROLEUR', 'RCI')")
    public SessionCaisseResponse getSessionActive() {
        return sessionCaisseService.getSessionActive();
    }
}

