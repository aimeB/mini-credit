package com.mini.credit.controller;

import com.mini.credit.annotation.Auditable;
import com.mini.credit.dto.employe.CommissionDTO;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.service.CommissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * PHASE 8: Controller pour commissions agents.
 *
 * Endpoints:
 * - POST /api/commissions (créer commission)
 * - GET /api/commissions/{id} (consulter)
 * - GET /api/commissions/agent/{agentId} (historique agent)
 * - GET /api/commissions/periode/{dateDebut}/{dateFin} (par période)
 * - GET /api/commissions/validation/en-attente (en attente validation)
 * - GET /api/commissions/paiement/en-attente (en attente paiement)
 * - POST /api/commissions/{id}/valider (valider - AGENT_BUREAU)
 * - POST /api/commissions/{id}/payer (payer - CAISSIER/ADMIN)
 * - POST /api/commissions/{id}/annuler (annuler)
 * - GET /api/commissions/agent/{agentId}/total-paye (total payé)
 */
@RestController
@RequestMapping("/api/commissions")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Commission Agent", description = "Agent commission management endpoints (PHASE 8)")
@SecurityRequirement(name = "bearer-jwt")
public class CommissionController {

    private final CommissionService commissionService;

    /**
     * PHASE 8: Crée une commission pour un agent
     * Calcul automatique: totalRecettes * TAUX_COMMISSION_AGENT (2.5%)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT_BUREAU')")
    @Operation(summary = "Créer commission", description = "Créer une commission pour un agent et une période")
    @Auditable(action = AuditAction.COMMISSION_CREATED, entityType = "Commission")
    public CommissionDTO creerCommission(
            @RequestParam @NotNull Long agentId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam @NotNull @DecimalMin("0") BigDecimal totalRecettes,
            @RequestParam @NotNull Long nbRecettes) {

        return commissionService.creerCommission(agentId, dateDebut, dateFin, totalRecettes, nbRecettes);
    }

    /**
     * Récupère une commission par ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT_BUREAU', 'AGENT_TERRAIN')")
    @Operation(summary = "Consulter commission", description = "Récupérer détails d'une commission")
    public CommissionDTO getById(@PathVariable Long id) {
        return commissionService.getById(id);
    }

    /**
     * Récupère l'historique des commissions d'un agent
     */
    @GetMapping("/agent/{agentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT_BUREAU', 'AGENT_TERRAIN')")
    @Operation(summary = "Historique agent", description = "Lister les commissions d'un agent")
    public List<CommissionDTO> getByAgent(@PathVariable Long agentId) {
        return commissionService.getByAgent(agentId);
    }

    /**
     * Récupère les commissions d'une période donnée
     */
    @GetMapping("/periode/{dateDebut}/{dateFin}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT_BUREAU')")
    @Operation(summary = "Commissions par période", description = "Lister commissions pour une période")
    public List<CommissionDTO> getByPeriode(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        return commissionService.getByPeriode(dateDebut, dateFin);
    }

    /**
     * Récupère les commissions en attente de validation
     */
    @GetMapping("/validation/en-attente")
    @PreAuthorize("hasRole('AGENT_BUREAU')")
    @Operation(summary = "En attente validation", description = "Lister commissions à valider")
    public List<CommissionDTO> getEnAttenteValidation() {
        return commissionService.getEnAttenteValidation();
    }

    /**
     * Récupère les commissions en attente de paiement
     */
    @GetMapping("/paiement/en-attente")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAISSIER')")
    @Operation(summary = "En attente paiement", description = "Lister commissions à payer")
    public List<CommissionDTO> getEnAttentePaiement() {
        return commissionService.getEnAttentePaiement();
    }

    /**
     * PHASE 8: Valide une commission (gestionnaire/chef)
     */
    @PostMapping("/{id}/valider")
    @PreAuthorize("hasRole('AGENT_BUREAU')")
    @Operation(summary = "Valider commission (PHASE 8)", description = "Gestionnaire valide commission avant paiement")
    @Auditable(action = AuditAction.COMMISSION_VALIDATED, entityType = "Commission")
    public CommissionDTO validerCommission(@PathVariable Long id) {
        return commissionService.validerCommission(id);
    }

    /**
     * PHASE 8: Paie une commission (caissier/admin)
     */
    @PostMapping("/{id}/payer")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAISSIER')")
    @Operation(summary = "Payer commission (PHASE 8)", description = "Caissier paie commission à l'agent")
    @Auditable(action = AuditAction.COMMISSION_PAID, entityType = "Commission")
    public CommissionDTO payerCommission(@PathVariable Long id) {
        return commissionService.payerCommission(id);
    }

    /**
     * Annule une commission
     */
    @PostMapping("/{id}/annuler")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Annuler commission", description = "Annuler une commission non payée")
    @Auditable(action = AuditAction.COMMISSION_CANCELLED, entityType = "Commission")
    public CommissionDTO annulerCommission(
            @PathVariable Long id,
            @RequestParam String raison) {
        return commissionService.annulerCommission(id, raison);
    }

    /**
     * Total des commissions payées par agent (historique)
     */
    @GetMapping("/agent/{agentId}/total-paye")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT_BUREAU')")
    @Operation(summary = "Total payé", description = "Somme des commissions payées à un agent")
    public BigDecimal getTotalPayeByAgent(@PathVariable Long agentId) {
        return commissionService.getTotalPayeByAgent(agentId);
    }
}
