package com.mini.credit.controller;

import com.mini.credit.annotation.Auditable;
import com.mini.credit.dto.credit.PenaliteCreditDTO;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.service.PenaliteCreditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * PHASE 10: Controller pour pénalités de retard crédit
 *
 * Endpoints:
 * - POST /api/penalites (créer - batch automatique)
 * - GET /api/penalites/credit/{creditId} (consulter crédit)
 * - GET /api/penalites/demande/{demandeCreditId} (consulter demande)
 * - GET /api/penalites/en-attente (lister en attente)
 * - POST /api/penalites/{id}/acquitter (marquer payée)
 * - POST /api/penalites/{id}/effacer (pardon/remise)
 * - GET /api/penalites/total-credit/{creditId} (somme)
 * - GET /api/penalites/total-attente/{creditId} (somme en attente)
 */
@RestController
@RequestMapping("/api/penalites")
@RequiredArgsConstructor
@Tag(name = "Pénalités Crédit", description = "Late payment penalty management endpoints (PHASE 10)")
@SecurityRequirement(name = "bearer-jwt")
public class PenaliteCreditController {

    private final PenaliteCreditService penaliteCreditService;

    /**
     * PHASE 10: Déclenche batch génération pénalités (normalement quotidien 00:01)
     * Vérifie tous les crédits en retard et crée pénalités
     *
     * @return List des pénalités créées
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Batch pénalités", description = "Générer pénalités pour crédits en retard")
    @Auditable(action = AuditAction.PENALITE_CREEE, entityType = "Lot Pénalités")
    public List<PenaliteCreditDTO> genererPenalites() {
        return penaliteCreditService.genererPenalitesTous();
    }

    /**
     * Récupère les pénalités d'un crédit
     *
     * @param creditId ID du crédit
     * @return List des pénalités
     */
    @GetMapping("/credit/{creditId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTIONNAIRE', 'MEMBER')")
    @Operation(summary = "Pénalités crédit", description = "Lister pénalités d'un crédit")
    public List<PenaliteCreditDTO> getByCreditId(@PathVariable @NotNull Long creditId) {
        return penaliteCreditService.getByCreditId(creditId);
    }

    /**
     * Récupère les pénalités d'une demande de crédit
     *
     * @param demandeCreditId ID de la demande
     * @return List des pénalités
     */
    @GetMapping("/demande/{demandeCreditId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTIONNAIRE', 'MEMBER')")
    @Operation(summary = "Pénalités demande", description = "Lister pénalités d'une demande crédit")
    public List<PenaliteCreditDTO> getByDemandeCreditId(@PathVariable @NotNull Long demandeCreditId) {
        return penaliteCreditService.getByDemandeCreditId(demandeCreditId);
    }

    /**
     * Récupère toutes les pénalités en attente (CREEES)
     *
     * @return List des pénalités
     */
    @GetMapping("/en-attente")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "En attente", description = "Lister pénalités en attente de paiement")
    public List<PenaliteCreditDTO> getEnAttente() {
        return penaliteCreditService.getEnAttente();
    }

    /**
     * PHASE 10: Acquitte une pénalité (marque comme payée)
     *
     * @param penaliteId ID de la pénalité
     * @return Pénalité mise à jour
     */
    @PostMapping("/{penaliteId}/acquitter")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Acquitter pénalité", description = "Marquer pénalité comme payée")
    @Auditable(action = AuditAction.PENALITE_ACQUITTEE, entityType = "Pénalité")
    public PenaliteCreditDTO acquitterPenalite(@PathVariable @NotNull Long penaliteId) {
        return penaliteCreditService.acquitterPenalite(penaliteId);
    }

    /**
     * PHASE 10: Efface une pénalité (pardon/remise)
     * Nécessite motif documenté
     *
     * @param penaliteId ID de la pénalité
     * @param motif Motif de l'effacement
     * @return Pénalité mise à jour
     */
    @PostMapping("/{penaliteId}/effacer")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Effacer pénalité", description = "Effacer pénalité avec motif (pardon/remise)")
    @Auditable(action = AuditAction.PENALITE_EFFACEE, entityType = "Pénalité")
    public PenaliteCreditDTO effacerPenalite(
            @PathVariable @NotNull Long penaliteId,
            @RequestParam @NotBlank String motif) {
        return penaliteCreditService.effacerPenalite(penaliteId, motif);
    }

    /**
     * Récupère montant total pénalités d'un crédit (tous statuts)
     *
     * @param creditId ID du crédit
     * @return Somme en BigDecimal
     */
    @GetMapping("/total-credit/{creditId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTIONNAIRE', 'MEMBER')")
    @Operation(summary = "Total pénalités", description = "Somme des pénalités créées pour un crédit")
    public BigDecimal getTotalPenalitesCredit(@PathVariable @NotNull Long creditId) {
        return penaliteCreditService.getTotalPenalitesCredit(creditId);
    }

    /**
     * Récupère montant total pénalités EN ATTENTE pour un crédit (CREEES seulement)
     *
     * @param creditId ID du crédit
     * @return Somme en BigDecimal
     */
    @GetMapping("/total-attente/{creditId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTIONNAIRE', 'MEMBER')")
    @Operation(summary = "Total en attente", description = "Somme des pénalités en attente de paiement")
    public BigDecimal getTotalPenalitesEnAttenteCredit(@PathVariable @NotNull Long creditId) {
        return penaliteCreditService.getTotalPenalitesEnAttenteCredit(creditId);
    }

    /**
     * Récupère le taux de pénalité journalière paramétré
     *
     * @return Taux en BigDecimal
     */
    @GetMapping("/taux")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTIONNAIRE')")
    @Operation(summary = "Taux pénalité", description = "Récupère taux pénalité journalière paramétré (PHASE 1)")
    public BigDecimal getTauxPenalite() {
        return penaliteCreditService.getTauxPenaliteJournaliere();
    }
}
