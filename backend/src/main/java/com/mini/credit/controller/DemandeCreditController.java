package com.mini.credit.controller;

import com.mini.credit.annotation.Auditable;
import com.mini.credit.dto.credit.AnalyseRisqueRequest;
import com.mini.credit.dto.credit.DemandeCreditCreateRequest;
import com.mini.credit.dto.credit.DemandeCreditResponse;
import com.mini.credit.dto.credit.FraisCreditAEncaisserResponse;
import com.mini.credit.dto.credit.PreAnalyseRequest;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.service.DemandeCreditService;
import com.mini.credit.service.security.ScopeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/demandes-credit")
@RequiredArgsConstructor
@Tag(name = "Demande Credit", description = "Credit request management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class DemandeCreditController {

    private final DemandeCreditService demandeCreditService;
    private final ScopeService scopeService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTIONNAIRE', 'MEMBER')")
    @Operation(summary = "Create credit request", description = "Create a new credit request")
    @Auditable(action = AuditAction.DEMANDE_CREDIT_CREATED, entityType = "DemandeCredit", captureParameters = true, captureResult = true)
    public DemandeCreditResponse create(@Valid @RequestBody DemandeCreditCreateRequest request) {
        return demandeCreditService.create(request);
    }

    @PostMapping({"/{id}/analyse", "/{id}/analyse-risque"})
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTROLEUR')")
    @Operation(summary = "Add risk analysis", description = "Add risk analysis to credit request")
    public DemandeCreditResponse ajouterAnalyse(@PathVariable Long id,
                                                @Valid @RequestBody AnalyseRisqueRequest request) {
        return demandeCreditService.ajouterAnalyse(id, request);
    }

    @PostMapping("/{id}/pre-analyse")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTIONNAIRE')")
    @Operation(summary = "Pre-analyse demande (legacy)", description = "Compatibilité: transmet vers la décision structurée de pré-analyse")
    @Auditable(action = AuditAction.MODIFICATION_OPERATION, entityType = "DemandeCredit")
    public DemandeCreditResponse preAnalyser(@PathVariable Long id,
                                             @RequestParam(required = false) String commentaire) {
        return demandeCreditService.preAnalyser(id, commentaire);
    }

    @PostMapping("/{id}/pre-analyse/decision")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTIONNAIRE')")
    @Operation(summary = "Décision de pré-analyse", description = "Pré-analyse métier: retour complément ou transmission après blocage garantie 20%")
    @Auditable(action = AuditAction.MODIFICATION_OPERATION, entityType = "DemandeCredit")
    public DemandeCreditResponse preAnalyserDecision(@PathVariable Long id,
                                                     @Valid @RequestBody PreAnalyseRequest request) {
        return demandeCreditService.preAnalyserDecision(id, request);
    }

    @PostMapping({"/{id}/controle-risque", "/{id}/controler-risque"})
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTROLEUR')")
    @Operation(summary = "Valider analyse risque (legacy)", description = "Validation explicite de l'analyse risque (compatibilité chemin legacy)")
    @Auditable(action = AuditAction.CREDIT_VALIDATION_CHECKED, entityType = "DemandeCredit")
    public DemandeCreditResponse controlerRisque(@PathVariable Long id,
                                                 @RequestParam(required = false) String commentaire) {
        return demandeCreditService.controlerRisque(id, commentaire);
    }

    @PostMapping("/{id}/analyse-risque/observation")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTROLEUR')")
    @Operation(summary = "Enregistrer observation risque", description = "Enregistre une observation de contrôle sans changement d'étape métier")
    @Auditable(action = AuditAction.MODIFICATION_OPERATION, entityType = "DemandeCredit")
    public DemandeCreditResponse enregistrerObservationRisque(@PathVariable Long id,
                                                              @RequestParam String commentaire) {
        return demandeCreditService.enregistrerObservationRisque(id, commentaire);
    }

    @PostMapping("/{id}/analyse-risque/valider")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTROLEUR')")
    @Operation(summary = "Valider analyse risque", description = "Valide l'analyse risque complète avant passage à la garantie")
    @Auditable(action = AuditAction.CREDIT_VALIDATION_CHECKED, entityType = "DemandeCredit")
    public DemandeCreditResponse validerAnalyseRisque(@PathVariable Long id,
                                                      @RequestParam String commentaire) {
        return demandeCreditService.validerAnalyseRisque(id, commentaire);
    }

    @PostMapping({"/{id}/controle-garantie", "/{id}/controler-garantie"})
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTROLEUR')")
    @Operation(summary = "Contrôle garantie", description = "Transition ANALYSE_TERRAIN_VALIDEE -> VALIDATION_CHEF")
    @Auditable(action = AuditAction.CREDIT_VALIDATION_CHECKED, entityType = "DemandeCredit")
    public DemandeCreditResponse controlerGarantie(@PathVariable Long id,
                                                   @RequestParam(required = false) String commentaire) {
        return demandeCreditService.controlerGarantie(id, commentaire);
    }

    @PostMapping("/{id}/rejeter")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'CONTROLEUR')")
    @Operation(summary = "Rejeter demande", description = "Rejette la demande avec commentaire obligatoire")
    @Auditable(action = AuditAction.INVALID_OPERATION, entityType = "DemandeCredit")
    public DemandeCreditResponse rejeter(@PathVariable Long id,
                                         @RequestParam String commentaire) {
        return demandeCreditService.rejeter(id, commentaire);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@scopeService.canReadDemandeCredit(#id)")
    @Operation(summary = "Get credit request by ID", description = "Retrieve credit request details")
    public DemandeCreditResponse getById(@PathVariable Long id) {
        return demandeCreditService.getById(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'GESTIONNAIRE', 'CONTROLEUR')")
    @Operation(summary = "Get all credit requests", description = "Retrieve paginated list of credit requests")
    public Page<DemandeCreditResponse> getAll(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return demandeCreditService.getAll(pageable);
    }

    @GetMapping("/frais-a-encaisser")
    @PreAuthorize("hasAnyRole('CAISSIER', 'ADMIN', 'CHEF_BUREAU', 'RCI')")
    @Operation(summary = "Frais crédit à encaisser", description = "Liste minimale des demandes crédit avec frais de demande/analyse incomplets")
    public List<FraisCreditAEncaisserResponse> getFraisCreditAEncaisser() {
        return demandeCreditService.getFraisCreditAEncaisser();
    }

    @GetMapping("/membre/{membreId}")
    @PreAuthorize("@scopeService.canReadMembre(#membreId)")
    @Operation(summary = "Get credit requests by member", description = "Retrieve paginated credit requests for a specific member")
    public Page<DemandeCreditResponse> getByMembre(@PathVariable Long membreId,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        // PHASE 3B: Limiter size max Ã  100 pour Ã©viter les abus
        if (size > 100) size = 100;
        Pageable pageable = PageRequest.of(page, size);
        return demandeCreditService.getByMembre(membreId, pageable);
    }

    @GetMapping("/mes-demandes")
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "Get current member's credit requests", description = "Get all credit requests for the current authenticated member")
    public List<DemandeCreditResponse> getMesDemandes() {
        return demandeCreditService.getCurrentMemberRequests();
    }

    // ============ PHASE 4: VALIDATION CRÃ‰DIT STRICTE ============

    @GetMapping("/{id}/validation")
    @PreAuthorize("hasAuthority('CONTROLEUR_CREDITS_VALIDATE')")
    @Operation(summary = "Validate credit request (PHASE 4)", description = "Check if credit request meets all PHASE 4 validation criteria: fees paid, guarantee deposited, terrain analysis complete")
    @Auditable(action = AuditAction.CREDIT_VALIDATION_CHECKED, entityType = "DemandeCredit")
    public com.mini.credit.dto.credit.CreditValidationResult validateCredit(@PathVariable Long id) {
        return demandeCreditService.validerCredit(id);
    }
}

