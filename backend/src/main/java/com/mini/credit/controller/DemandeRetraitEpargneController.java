package com.mini.credit.controller;

import com.mini.credit.annotation.Auditable;
import com.mini.credit.dto.epargne.DemandeRetraitEpargneDTO;
import com.mini.credit.enums.StatutDemandeRetrait;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.service.DemandeRetraitEpargneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controller pour demandes de retrait épargne (PHASE 5).
 *
 * Endpoints:
 * - POST /api/demandes-retrait-epargne (créer)
 * - GET /api/demandes-retrait-epargne/{id} (consulter)
 * - GET /api/demandes-retrait-epargne/compte/{compteId} (lister par compte)
 * - GET /api/demandes-retrait-epargne/validation/en-attente (lister en attente)
 * - POST /api/demandes-retrait-epargne/{id}/valider (valider - CONTROLEUR)
 * - POST /api/demandes-retrait-epargne/{id}/rejeter (rejeter - CONTROLEUR)
 * - POST /api/demandes-retrait-epargne/{id}/decaisser (décaisser - CAISSIER)
 * - POST /api/demandes-retrait-epargne/{id}/annuler (annuler)
 */
@RestController
@RequestMapping("/api/demandes-retrait-epargne")
@RequiredArgsConstructor
@Tag(name = "Demande Retrait Épargne", description = "Withdrawal request management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class DemandeRetraitEpargneController {

    private final DemandeRetraitEpargneService demandeRetraitEpargneService;
    private final com.mini.credit.service.security.ScopeService scopeService;

    /**
     * Crée une nouvelle demande de retrait épargne
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('CAISSIER', 'ADMIN') or (hasRole('MEMBER') and @scopeService.canReadCompteEpargne(#compteEpargneId))")
    @Operation(summary = "Create withdrawal request", description = "Create a new savings withdrawal request")
    @Auditable(action = AuditAction.DEMANDE_RETRAIT_EPARGNE_CREATED, entityType = "DemandeRetraitEpargne")
    public DemandeRetraitEpargneDTO creerDemande(
            @RequestParam @NotNull(message = "compteEpargneId est requis") Long compteEpargneId,
            @RequestParam @NotNull @DecimalMin("0.01") BigDecimal montant,
            @RequestParam(required = false, defaultValue = "0") @DecimalMin("0.00") BigDecimal fraisRetrait,
            @RequestParam(required = false) String observation) {
        return demandeRetraitEpargneService.creerDemande(compteEpargneId, montant, fraisRetrait, observation);
    }

    /**
     * Récupère les demandes, avec filtre statut pour la file de paiement CAISSIER.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'CONTROLEUR') or (hasRole('CAISSIER') and #statut == T(com.mini.credit.enums.StatutDemandeRetrait).VALIDEE)")
    @Operation(summary = "List withdrawal requests", description = "List withdrawal requests, optionally filtered by status")
    public List<DemandeRetraitEpargneDTO> getAll(@RequestParam(required = false) StatutDemandeRetrait statut) {
        return demandeRetraitEpargneService.getAll(statut);
    }

    /**
     * Récupère une demande par ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTROLEUR', 'CAISSIER') or @scopeService.canReadDemandeRetraitEpargne(#id)")
    @Operation(summary = "Get withdrawal request", description = "Retrieve withdrawal request details")
    public DemandeRetraitEpargneDTO getById(@PathVariable Long id) {
        return demandeRetraitEpargneService.getById(id);
    }

    /**
     * Récupère les demandes d'un compte épargne
     */
    @GetMapping("/compte/{compteEpargneId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTROLEUR', 'CAISSIER') or @scopeService.canReadCompteEpargne(#compteEpargneId)")
    @Operation(summary = "Get withdrawal requests by account", description = "List withdrawal requests for a specific savings account")
    public List<DemandeRetraitEpargneDTO> getByCompteEpargne(@PathVariable Long compteEpargneId) {
        return demandeRetraitEpargneService.getByCompteEpargne(compteEpargneId);
    }

    /**
     * Récupère les demandes en attente de validation CONTROLEUR
     */
    @GetMapping("/validation/en-attente")
    @PreAuthorize("hasRole('CONTROLEUR')")
    @Operation(summary = "Get pending validations", description = "List withdrawal requests waiting for CONTROLEUR validation")
    public List<DemandeRetraitEpargneDTO> getEnAttenteValidation() {
        return demandeRetraitEpargneService.getEnAttenteValidation();
    }

    /**
     * PHASE 5: Valide une demande de retrait
     * Vérif solde CompteEpargne >= montant
     */
    @PostMapping("/{id}/valider")
    @PreAuthorize("hasAuthority('CONTROLEUR_RETRAITS_VALIDATE')")
    @Operation(summary = "Validate withdrawal request (PHASE 5)", description = "CONTROLEUR validates withdrawal by checking account balance")
    @Auditable(action = AuditAction.DEMANDE_RETRAIT_EPARGNE_VALIDATED, entityType = "DemandeRetraitEpargne")
    public DemandeRetraitEpargneDTO validerDemande(@PathVariable Long id) {
        return demandeRetraitEpargneService.validerDemande(id);
    }

    /**
     * Rejette une demande de retrait avec motif
     */
    @PostMapping("/{id}/rejeter")
    @PreAuthorize("hasAuthority('CONTROLEUR_RETRAITS_VALIDATE')")
    @Operation(summary = "Reject withdrawal request", description = "CONTROLEUR rejects withdrawal with reason")
    @Auditable(action = AuditAction.DEMANDE_RETRAIT_EPARGNE_REJECTED, entityType = "DemandeRetraitEpargne")
    public DemandeRetraitEpargneDTO rejeterDemande(
            @PathVariable Long id,
            @RequestParam String motif) {
        return demandeRetraitEpargneService.rejeterDemande(id, motif);
    }

    /**
     * PHASE 5: Décaisse un retrait épargne
     * Crée OperationEpargne type RETRAIT
     */
    @PostMapping("/{id}/decaisser")
    @PreAuthorize("hasAuthority('OPERATION_CAISSE_CREATE')")
    @Operation(summary = "Disburse withdrawal (PHASE 5)", description = "CAISSIER executes validated withdrawal, creates OperationEpargne")
    public DemandeRetraitEpargneDTO decaisserRetrait(@PathVariable Long id) {
        return demandeRetraitEpargneService.decaisserRetrait(id);
    }

    /**
     * Annule une demande de retrait
     */
    @PostMapping("/{id}/annuler")
    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
    @Operation(summary = "Cancel withdrawal request", description = "Cancel a withdrawal request (before disbursement)")
    @Auditable(action = AuditAction.DEMANDE_RETRAIT_EPARGNE_CANCELLED, entityType = "DemandeRetraitEpargne")
    public DemandeRetraitEpargneDTO annulerDemande(@PathVariable Long id) {
        return demandeRetraitEpargneService.annulerDemande(id);
    }
}
