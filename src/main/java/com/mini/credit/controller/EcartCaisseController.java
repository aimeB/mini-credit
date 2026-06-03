package com.mini.credit.controller;

import com.mini.credit.annotation.Auditable;
import com.mini.credit.dto.caisse.EcartCaisseDTO;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.service.EcartCaisseService;
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
 * PHASE 7: Controller pour écarts caisse.
 *
 * Endpoints:
 * - POST /api/ecarts-caisse (créer écart)
 * - GET /api/ecarts-caisse/{id} (consulter)
 * - GET /api/ecarts-caisse/jour/{dateJour} (écarts du jour)
 * - GET /api/ecarts-caisse/enquete/en-attente (écarts en investigation - CONTROLEUR)
 * - GET /api/ecarts-caisse/validation/rci (écarts pour validation R.C.I.)
 * - POST /api/ecarts-caisse/{id}/enqueter (enquête CONTROLEUR)
 * - POST /api/ecarts-caisse/{id}/resoudre (résolution CONTROLEUR)
 * - POST /api/ecarts-caisse/{id}/accepter (validation R.C.I.)
 * - POST /api/ecarts-caisse/{id}/rejeter (rejet)
 * - GET /api/ecarts-caisse/total-non-resolu/jour/{dateJour} (somme écarts)
 */
@RestController
@RequestMapping("/api/ecarts-caisse")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Écart Caisse", description = "Cash discrepancy management endpoints (PHASE 7)")
@SecurityRequirement(name = "bearer-jwt")
public class EcartCaisseController {

    private final EcartCaisseService ecartService;

    /**
     * PHASE 7: Crée un écart caisse détecté
     * (Généralement appelé automatiquement lors de validation recettes ou clôture session)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTROLEUR')")
    @Operation(summary = "Créer écart", description = "Créer un écart caisse détecté")
    @Auditable(action = AuditAction.ECART_CAISSE_DETECTE, entityType = "EcartCaisse")
    public EcartCaisseDTO detecterEcart(
            @RequestParam(required = false) Long sessionCaisseId,
            @RequestParam(required = false) Long recetteId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateJour,
            @RequestParam @NotNull String typeEcart,
            @RequestParam @NotNull @DecimalMin("0.01") BigDecimal montantEcart,
            @RequestParam @NotNull String description,
            @RequestParam(required = false) Boolean seuilDepassé) {

        return ecartService.detecterEcart(sessionCaisseId, recetteId, dateJour, typeEcart, montantEcart, description, seuilDepassé);
    }

    /**
     * Récupère un écart par ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTROLEUR', 'RESPONSABLE_CONTROLES_INTERNES')")
    @Operation(summary = "Consulter écart", description = "Récupérer détails d'un écart")
    public EcartCaisseDTO getById(@PathVariable Long id) {
        return ecartService.getById(id);
    }

    /**
     * Récupère les écarts détectés pour un jour
     */
    @GetMapping("/jour/{dateJour}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTROLEUR', 'RESPONSABLE_CONTROLES_INTERNES')")
    @Operation(summary = "Écarts du jour", description = "Lister écarts détectés pour une date")
    public List<EcartCaisseDTO> getByDateJour(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateJour) {
        return ecartService.getByDateJour(dateJour);
    }

    /**
     * Récupère les écarts en investigation (CONTROLEUR)
     */
    @GetMapping("/enquete/en-attente")
    @PreAuthorize("hasAuthority('CONTROLEUR_ECART_VALIDATE')")
    @Operation(summary = "Écarts en attente", description = "Lister écarts pour investigation CONTROLEUR")
    public List<EcartCaisseDTO> getEnInvestigation() {
        return ecartService.getEnInvestigation();
    }

    /**
     * Récupère les écarts nécessitant validation R.C.I.
     */
    @GetMapping("/validation/rci")
    @PreAuthorize("hasRole('RESPONSABLE_CONTROLES_INTERNES')")
    @Operation(summary = "Écarts pour R.C.I.", description = "Écarts montant > seuil nécessitant validation R.C.I.")
    public List<EcartCaisseDTO> getRequiringRCIValidation() {
        return ecartService.getRequiringRCIValidation();
    }

    /**
     * PHASE 7: Enquête d'un écart (CONTROLEUR)
     * Passe en EN_INVESTIGATION
     */
    @PostMapping("/{id}/enqueter")
    @PreAuthorize("hasAuthority('CONTROLEUR_ECART_VALIDATE')")
    @Operation(summary = "Enquêter écart (PHASE 7)", description = "CONTROLEUR enquête et documente investigation")
    @Auditable(action = AuditAction.ECART_CAISSE_ENQUETE, entityType = "EcartCaisse")
    public EcartCaisseDTO enqueterEcart(
            @PathVariable Long id,
            @RequestParam String notesInvestigation) {
        return ecartService.enqueterEcart(id, notesInvestigation);
    }

    /**
     * PHASE 7: Résout un écart (CONTROLEUR)
     * Passe en RESOLU
     */
    @PostMapping("/{id}/resoudre")
    @PreAuthorize("hasAuthority('CONTROLEUR_ECART_VALIDATE')")
    @Operation(summary = "Résoudre écart (PHASE 7)", description = "CONTROLEUR résout écart avec raison")
    @Auditable(action = AuditAction.ECART_CAISSE_RESOLU, entityType = "EcartCaisse")
    public EcartCaisseDTO resoudreEcart(
            @PathVariable Long id,
            @RequestParam String raisonResolution) {
        return ecartService.resoudreEcart(id, raisonResolution);
    }

    /**
     * PHASE 7: Accepte un écart (R.C.I.)
     * Passe en ACCEPTE (variance normalisée)
     */
    @PostMapping("/{id}/accepter")
    @PreAuthorize("hasRole('RESPONSABLE_CONTROLES_INTERNES')")
    @Operation(summary = "Accepter écart (PHASE 7)", description = "R.C.I. accepte variance comme normale")
    @Auditable(action = AuditAction.ECART_CAISSE_ACCEPTE, entityType = "EcartCaisse")
    public EcartCaisseDTO accepterEcart(@PathVariable Long id) {
        return ecartService.accepterEcart(id);
    }

    /**
     * Rejette un écart (présumé erreur)
     */
    @PostMapping("/{id}/rejeter")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Rejeter écart", description = "Rejeter écart (erreur système présumée)")
    @Auditable(action = AuditAction.ECART_CAISSE_REJETE, entityType = "EcartCaisse")
    public EcartCaisseDTO rejeterEcart(@PathVariable Long id) {
        return ecartService.rejeterEcart(id);
    }

    /**
     * Total écarts non résolus pour un jour
     */
    @GetMapping("/total-non-resolu/jour/{dateJour}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTROLEUR', 'RESPONSABLE_CONTROLES_INTERNES')")
    @Operation(summary = "Total écarts jour", description = "Somme écarts non résolus pour une date")
    public BigDecimal getTotalNonResoluByDateJour(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateJour) {
        return ecartService.getTotalEcartsNonResolusByDateJour(dateJour);
    }
}
