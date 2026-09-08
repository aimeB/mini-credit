package com.mini.credit.controller;

import com.mini.credit.annotation.Auditable;
import com.mini.credit.dto.caisse.EcartCaisseDTO;
import com.mini.credit.dto.request.EcartCaisseJustifierRequest;
import com.mini.credit.dto.request.EcartCaisseResoudreRequest;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.service.EcartCaisseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
 * PATCH 11 â€” Controller pour Ã©carts caisse.
 *
 * RBAC basÃ© sur les rÃ´les rÃ©els du systÃ¨me (RoleCode) :
 *   ADMIN, CHEF_BUREAU, CONTROLEUR, CAISSIER, RCI
 *
 * Mapping mÃ©tier 3N â†’ technique :
 *   Chef de Bureau  =  RoleCode.CHEF_BUREAU
 *   ContrÃ´leur     =  CONTROLEUR   (RoleCode.CONTROLEUR)
 *   Caissier       =  CAISSIER     (RoleCode.CAISSIER)
 *   RCI            =  RCI          (RoleCode.RCI) â€” rÃ´le officiel distinct (PATCH 11)
 *
 * RCI â‰  Chef de Bureau. Ces deux rÃ´les mÃ©tier sont distincts :
 *   RCI   : audit, enquÃªte, contrÃ´le interne, lecture Ã©carts.
 *   CHEF_BUREAU : supervision commerciale, acceptation variance.
 *
 * Workflow Ã©carts :
 *   1. DÃ©tection automatique (backend â€” clÃ´ture session ou validation recette)
 *   2. Justification initiale facultative (CAISSIER, CONTROLEUR)
 *   3. Ouverture enquÃªte formelle â†’ EN_INVESTIGATION (CONTROLEUR, RCI)
 *   4. RÃ©solution â†’ RESOLU (CONTROLEUR)
 *   5. Acceptation variance â†’ ACCEPTE (Chef de Bureau uniquement)
 *
 * Seuils :
 *   Le flag seuilDÃ©passÃ© est calculÃ© Ã  la crÃ©ation via EcartThresholdConfigService.
 *   La valeur du seuil est paramÃ©trable (caisse.ecart.seuil-validation-rci).
 *   Aucun seuil n'est codÃ© en dur dans le controller ou l'entitÃ©.
 */
@RestController
@RequestMapping("/api/ecarts-caisse")
@RequiredArgsConstructor
@Tag(name = "Ã‰cart Caisse", description = "Gestion des Ã©carts de caisse â€” workflow investigation/rÃ©solution (PATCH 8)")
@SecurityRequirement(name = "bearer-jwt")
public class EcartCaisseController {

    private final EcartCaisseService ecartService;

    // ===== LECTURE =====

    /**
     * PATCH 11 â€” Liste tous les Ã©carts caisse.
     * RCI peut accÃ©der Ã  la liste globale dans le cadre de sa mission d'audit.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTROLEUR', 'CHEF_BUREAU', 'RCI')")
    @Operation(summary = "Tous les Ã©carts", description = "Liste tous les Ã©carts caisse")
    public List<EcartCaisseDTO> getAll() {
        return ecartService.getAll();
    }

    /**
     * RÃ©cupÃ¨re un Ã©cart par ID.
     * RCI peut consulter le dÃ©tail d'un Ã©cart dans le cadre d'un audit.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTROLEUR', 'CHEF_BUREAU', 'CAISSIER', 'RCI')")
    @Operation(summary = "DÃ©tail Ã©cart", description = "RÃ©cupÃ©rer les dÃ©tails d'un Ã©cart")
    public EcartCaisseDTO getById(@PathVariable Long id) {
        return ecartService.getById(id);
    }

    /**
     * PATCH 11 â€” RÃ©cupÃ¨re les Ã©carts d'une session caisse.
     * RCI peut consulter les Ã©carts d'une session dans le cadre d'un audit.
     */
    @GetMapping("/session/{sessionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTROLEUR', 'CHEF_BUREAU', 'CAISSIER', 'RCI')")
    @Operation(summary = "Ã‰carts par session", description = "Lister les Ã©carts pour une session caisse donnÃ©e")
    public List<EcartCaisseDTO> getBySession(@PathVariable Long sessionId) {
        return ecartService.getBySessionCaisseId(sessionId);
    }

    /**
     * RÃ©cupÃ¨re les Ã©carts dÃ©tectÃ©s pour un jour.
     * RCI peut consulter les Ã©carts d'une journÃ©e dans le cadre d'un contrÃ´le.
     */
    @GetMapping("/jour/{dateJour}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTROLEUR', 'CHEF_BUREAU', 'RCI')")
    @Operation(summary = "Ã‰carts du jour", description = "Lister Ã©carts dÃ©tectÃ©s pour une date")
    public List<EcartCaisseDTO> getByDateJour(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateJour) {
        return ecartService.getByDateJour(dateJour);
    }

    /**
     * PATCH 11 â€” RÃ©cupÃ¨re les Ã©carts en investigation ou dÃ©tectÃ©s.
     * RCI a accÃ¨s Ã  cet endpoint dans le cadre de sa mission d'enquÃªte.
     */
    @GetMapping("/enquete/en-attente")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTROLEUR', 'RCI')")
    @Operation(summary = "Ã‰carts en attente", description = "Lister Ã©carts DETECTE ou EN_INVESTIGATION")
    public List<EcartCaisseDTO> getEnInvestigation() {
        return ecartService.getEnInvestigation();
    }

    /**
     * PATCH 11 â€” RÃ©cupÃ¨re les Ã©carts nÃ©cessitant validation hiÃ©rarchique.
     * RCI peut consulter ces Ã©carts dans le cadre de son audit de contrÃ´le interne.
    * La validation elle-mÃªme reste rÃ©servÃ©e au Chef de Bureau.
     */
    @GetMapping("/validation/hierarchique")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'RCI')")
    @Operation(summary = "Ã‰carts pour validation", description = "Ã‰carts Ã  seuil dÃ©passÃ© nÃ©cessitant validation Chef de Bureau")
    public List<EcartCaisseDTO> getRequiringValidation() {
        return ecartService.getRequiringRCIValidation();
    }

    /**
     * PATCH 11 â€” Total Ã©carts non rÃ©solus pour un jour.
     * RCI peut consulter ce total dans le cadre de son suivi de contrÃ´le interne.
     */
    @GetMapping("/total-non-resolu/jour/{dateJour}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTROLEUR', 'CHEF_BUREAU', 'RCI')")
    @Operation(summary = "Total Ã©carts jour", description = "Somme Ã©carts non rÃ©solus pour une date")
    public BigDecimal getTotalNonResoluByDateJour(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateJour) {
        return ecartService.getTotalEcartsNonResolusByDateJour(dateJour);
    }

    // ===== CRÃ‰ATION (automatique via backend) =====

    /**
     * CrÃ©e un Ã©cart caisse (gÃ©nÃ©ralement appelÃ© automatiquement par le backend).
     * Accessible uniquement par ADMIN et CONTROLEUR pour correction manuelle si nÃ©cessaire.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTROLEUR')")
    @Operation(summary = "CrÃ©er Ã©cart", description = "CrÃ©er un Ã©cart caisse (normalement automatique)")
    @Auditable(action = AuditAction.ECART_CAISSE_DETECTE, entityType = "EcartCaisse")
    public EcartCaisseDTO detecterEcart(
            @RequestParam(required = false) Long sessionCaisseId,
            @RequestParam(required = false) Long recetteId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateJour,
            @RequestParam @NotNull String typeEcart,
            @RequestParam @NotNull @DecimalMin("0.01") BigDecimal montantEcart,
            @RequestParam @NotNull String description,
            @RequestParam(required = false) Boolean seuilDepasse) {

        return ecartService.detecterEcart(sessionCaisseId, recetteId, dateJour, typeEcart,
                montantEcart, description, seuilDepasse);
    }

    // ===== WORKFLOW =====

    /**
     * PATCH 8 â€” Justification initiale d'un Ã©cart.
     * CAISSIER ou CONTROLEUR peut ajouter une justification sans changer le statut.
     *
     * NOTE : La validation minimale de 10 caractÃ¨res est provisoire (technique).
     * Elle n'est pas dÃ©finie dans les documents 3N fournis.
     */
    @PostMapping("/{id}/justifier")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTROLEUR', 'CAISSIER')")
    @Operation(summary = "Justifier Ã©cart", description = "Ajouter une justification initiale (CAISSIER ou CONTROLEUR)")
    @Auditable(action = AuditAction.ECART_CAISSE_JUSTIFIE, entityType = "EcartCaisse")
    public EcartCaisseDTO justifier(
            @PathVariable Long id,
            @RequestBody @Valid EcartCaisseJustifierRequest request) {
        return ecartService.justifier(id, request.getJustification());
    }

    /**
     * PATCH 11 â€” Ouvre une enquÃªte formelle sur un Ã©cart.
     * RCI peut ouvrir ou dÃ©clencher une enquÃªte dans le cadre de sa mission de contrÃ´le interne.
     * CONTROLEUR reste l'acteur opÃ©rationnel principal.
     */
    @PostMapping("/{id}/ouvrir-enquete")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTROLEUR', 'RCI')")
    @Operation(summary = "Ouvrir enquÃªte", description = "CONTROLEUR ou RCI ouvre enquÃªte formelle â€” statut â†’ EN_INVESTIGATION")
    @Auditable(action = AuditAction.ECART_CAISSE_ENQUETE, entityType = "EcartCaisse")
    public EcartCaisseDTO ouvrirEnquete(
            @PathVariable Long id,
            @RequestBody @Valid EcartCaisseJustifierRequest request) {
        return ecartService.enqueterEcart(id, request.getJustification());
    }

    /**
     * RÃ©sout un Ã©cart (CONTROLEUR).
     * Passe en RESOLU.
     */
    @PostMapping("/{id}/resoudre")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTROLEUR')")
    @Operation(summary = "RÃ©soudre Ã©cart", description = "CONTROLEUR rÃ©sout l'Ã©cart avec raison documentÃ©e")
    @Auditable(action = AuditAction.ECART_CAISSE_RESOLU, entityType = "EcartCaisse")
    public EcartCaisseDTO resoudre(
            @PathVariable Long id,
            @RequestBody @Valid EcartCaisseResoudreRequest request) {
        return ecartService.resoudreEcart(id, request.getRaison());
    }

    /**
    * PATCH 11 â€” Accepte un Ã©cart (Chef de Bureau uniquement).
     * Passe en ACCEPTE (variance normalisÃ©e).
     * RCI n'accepte PAS les variances â€” cela reste la responsabilitÃ© du Chef de Bureau.
     */
    @PostMapping("/{id}/accepter")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU')")
    @Operation(summary = "Accepter Ã©cart", description = "Le Chef de Bureau accepte la variance â€” RCI ne peut pas accepter")
    @Auditable(action = AuditAction.ECART_CAISSE_ACCEPTE, entityType = "EcartCaisse")
    public EcartCaisseDTO accepter(@PathVariable Long id) {
        return ecartService.accepterEcart(id);
    }

    /**
     * Rejette un Ã©cart (ADMIN uniquement â€” erreur systÃ¨me prÃ©sumÃ©e).
     */
    @PostMapping("/{id}/rejeter")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Rejeter Ã©cart", description = "ADMIN rejette un Ã©cart (erreur systÃ¨me prÃ©sumÃ©e)")
    @Auditable(action = AuditAction.ECART_CAISSE_REJETE, entityType = "EcartCaisse")
    public EcartCaisseDTO rejeter(@PathVariable Long id) {
        return ecartService.rejeterEcart(id);
    }
}
