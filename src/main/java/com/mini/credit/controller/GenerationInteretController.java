package com.mini.credit.controller;

import com.mini.credit.annotation.Auditable;
import com.mini.credit.dto.epargne.OperationEpargneDTO;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.service.GenerationInteretService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * PHASE 9: Controller pour génération automatique intérêts épargne
 *
 * Endpoints:
 * - POST /api/interets/tous (batch manuel)
 * - POST /api/interets/compte/{compteId} (compte spécifique)
 * - POST /api/interets/membre/{membreId} (tous les comptes du membre)
 * - GET /api/interets/total/{membreId} (somme intérêts générés)
 * - GET /api/interets/taux (taux courant)
 */
@RestController
@RequestMapping("/api/interets")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Intérêts Épargne", description = "Automatic savings interest generation endpoints (PHASE 9)")
@SecurityRequirement(name = "bearer-jwt")
public class GenerationInteretController {

    private final GenerationInteretService generationInteretService;

    /**
     * PHASE 9: Déclenche batch génération intérêts pour TOUS les comptes actifs
     * Généralement appelé le 1er du mois (via @Scheduled)
     * Peut être déclenché manuellement via cet endpoint
     *
     * @return List des opérations créées
     */
    @PostMapping("/tous")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Batch intérêts", description = "Générer intérêts pour tous les comptes actifs")
    @Auditable(action = AuditAction.INTERET_EPARGNE_GENERE, entityType = "Lot Intérêts")
    public List<OperationEpargneDTO> genererInteretsTous() {
        return generationInteretService.genererInteretsTous();
    }

    /**
     * PHASE 9: Génère intérêt pour UN compte épargne
     * Permet génération ponctuelle ou test
     *
     * @param compteEpargneId ID du compte
     * @return Opération créée
     */
    @PostMapping("/compte/{compteEpargneId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Intérêt compte", description = "Générer intérêt pour un compte spécifique")
    @Auditable(action = AuditAction.INTERET_EPARGNE_ACCRUED, entityType = "Intérêt Compte")
    public OperationEpargneDTO genererInteretCompte(@PathVariable @NotNull Long compteEpargneId) {
        return generationInteretService.genererInteretCompte(compteEpargneId);
    }

    /**
     * PHASE 9: Génère intérêts pour TOUS les comptes d'un membre
     * Utile pour tests ou rattrapage manuel
     *
     * @param membreId ID du membre
     * @return List des opérations créées
     */
    @PostMapping("/membre/{membreId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Intérêts membre", description = "Générer intérêts pour tous comptes d'un membre")
    @Auditable(action = AuditAction.INTERET_EPARGNE_GENERE, entityType = "Intérêts Membre")
    public List<OperationEpargneDTO> genererInteretsParMembre(@PathVariable @NotNull Long membreId) {
        return generationInteretService.genererInteretsParMembre(membreId);
    }

    /**
     * PHASE 9: Récupère le taux intérêt épargne courant
     * Utilise TAUX_INTERET_EPARGNE (PHASE 1)
     *
     * @return Taux en BigDecimal (ex: 0.05 pour 5%)
     */
    @GetMapping("/taux")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER', 'AGENT_TERRAIN')")
    @Operation(summary = "Taux courant", description = "Récupère taux intérêt épargne paramétré")
    public BigDecimal getTauxInteret() {
        return generationInteretService.getTauxInteretEpargne();
    }

    /**
     * PHASE 9: Récupère total intérêts générés pour un membre
     * Somme historique de tous les intérêts acquis
     *
     * @param membreId ID du membre
     * @return Total en BigDecimal
     */
    @GetMapping("/total/{membreId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER', 'AGENT_TERRAIN')")
    @Operation(summary = "Total intérêts", description = "Somme totale intérêts générés pour un membre")
    public BigDecimal getTotalInteretsGeneres(@PathVariable @NotNull Long membreId) {
        return generationInteretService.getTotalInteretsGeneres(membreId);
    }
}
