package com.mini.credit.controller;

import com.mini.credit.annotation.Auditable;
import com.mini.credit.dto.garantie.AjouterGarantieMaterielleRequest;
import com.mini.credit.dto.garantie.BloquerGarantieEpargneRequest;
import com.mini.credit.dto.garantie.GarantieCreditResponse;
import com.mini.credit.dto.garantie.GarantieMaterielleResponse;
import com.mini.credit.dto.garantie.RejeterGarantieRequest;
import com.mini.credit.dto.garantie.ValiderGarantieRequest;
import com.mini.credit.dto.garantie.VerifierGarantieCreditRequest;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.service.GarantieCreditWorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/demandes-credit/{demandeCreditId}/garantie")
@RequiredArgsConstructor
@Tag(name = "Garantie crédit", description = "Workflow de garantie crédit 3N")
@SecurityRequirement(name = "bearer-jwt")
public class GarantieCreditWorkflowController {

    private final GarantieCreditWorkflowService garantieCreditWorkflowService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'CONTROLEUR', 'GESTIONNAIRE', 'CAISSIER', 'RCI')")
    @Operation(summary = "Lire la garantie crédit", description = "Retourne le résumé de garantie du dossier crédit")
    public ResponseEntity<GarantieCreditResponse> getGarantie(@PathVariable Long demandeCreditId) {
        return ResponseEntity.ok(garantieCreditWorkflowService.getByDemandeCreditId(demandeCreditId));
    }

    @PostMapping("/verifier")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTROLEUR')")
    @Operation(summary = "Vérifier la garantie", description = "Calcule la garantie requise et le manquant éventuel")
    @Auditable(action = AuditAction.GARANTIE_VERIFIED, entityType = "GarantieCredit", entityIdExpression = "#demandeCreditId")
    public ResponseEntity<GarantieCreditResponse> verifier(@PathVariable Long demandeCreditId,
                                                           @RequestBody(required = false) VerifierGarantieCreditRequest request) {
        return ResponseEntity.ok(garantieCreditWorkflowService.verifier(demandeCreditId, request));
    }

    @PostMapping("/bloquer-epargne")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTROLEUR')")
    @Operation(summary = "Bloquer la garantie épargne", description = "Bloque automatiquement la garantie si le solde est suffisant")
    @Auditable(action = AuditAction.GARANTIE_BLOCKED, entityType = "GarantieCredit", entityIdExpression = "#demandeCreditId")
    public ResponseEntity<GarantieCreditResponse> bloquerEpargne(@PathVariable Long demandeCreditId,
                                                                 @RequestBody(required = false) BloquerGarantieEpargneRequest request) {
        return ResponseEntity.ok(garantieCreditWorkflowService.bloquerEpargne(demandeCreditId, request));
    }

    @PostMapping("/materielle")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTROLEUR')")
    @Operation(summary = "Ajouter une garantie matérielle", description = "Enregistre un bien matériel rattaché au dossier crédit")
    @Auditable(action = AuditAction.GARANTIE_MATERIAL_ADDED, entityType = "GarantieMaterielle", entityIdExpression = "#demandeCreditId")
    public ResponseEntity<GarantieMaterielleResponse> ajouterGarantieMaterielle(@PathVariable Long demandeCreditId,
                                                                                @Valid @RequestBody AjouterGarantieMaterielleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(garantieCreditWorkflowService.ajouterGarantieMaterielle(demandeCreditId, request));
    }

    @PostMapping("/materielles/{garantieMaterielleId}/accepter")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTROLEUR')")
    @Operation(summary = "Accepter une garantie matérielle", description = "Valide une garantie matérielle contrôlée")
    @Auditable(action = AuditAction.GARANTIE_MATERIAL_ACCEPTED, entityType = "GarantieMaterielle", entityIdExpression = "#garantieMaterielleId")
    public ResponseEntity<GarantieMaterielleResponse> accepterGarantieMaterielle(@PathVariable Long demandeCreditId,
                                                                                 @PathVariable Long garantieMaterielleId,
                                                                                 @RequestParam(required = false) String commentaire) {
        return ResponseEntity.ok(garantieCreditWorkflowService.accepterGarantieMaterielle(demandeCreditId, garantieMaterielleId, commentaire));
    }

    @PostMapping("/materielles/{garantieMaterielleId}/rejeter")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTROLEUR')")
    @Operation(summary = "Refuser une garantie matérielle", description = "Refuse une garantie matérielle non acceptable")
    @Auditable(action = AuditAction.GARANTIE_MATERIAL_REJECTED, entityType = "GarantieMaterielle", entityIdExpression = "#garantieMaterielleId")
    public ResponseEntity<GarantieMaterielleResponse> refuserGarantieMaterielle(@PathVariable Long demandeCreditId,
                                                                                @PathVariable Long garantieMaterielleId,
                                                                                @RequestParam String commentaire) {
        return ResponseEntity.ok(garantieCreditWorkflowService.refuserGarantieMaterielle(demandeCreditId, garantieMaterielleId, commentaire));
    }

    @PostMapping("/valider")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTROLEUR')")
    @Operation(summary = "Valider la garantie", description = "Valide la garantie épargne et les garanties matérielles")
    @Auditable(action = AuditAction.GARANTIE_VALIDATED, entityType = "GarantieCredit", entityIdExpression = "#demandeCreditId")
    public ResponseEntity<GarantieCreditResponse> valider(@PathVariable Long demandeCreditId,
                                                          @Valid @RequestBody ValiderGarantieRequest request) {
        return ResponseEntity.ok(garantieCreditWorkflowService.valider(demandeCreditId, request));
    }

    @PostMapping("/rejeter")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTROLEUR')")
    @Operation(summary = "Rejeter la garantie", description = "Rejette la garantie du dossier crédit")
    @Auditable(action = AuditAction.GARANTIE_REJECTED, entityType = "GarantieCredit", entityIdExpression = "#demandeCreditId")
    public ResponseEntity<GarantieCreditResponse> rejeter(@PathVariable Long demandeCreditId,
                                                          @Valid @RequestBody RejeterGarantieRequest request) {
        return ResponseEntity.ok(garantieCreditWorkflowService.rejeter(demandeCreditId, request));
    }

    @GetMapping("/materielles")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'CONTROLEUR', 'GESTIONNAIRE', 'CAISSIER', 'RCI')")
    @Operation(summary = "Lister les garanties matérielles", description = "Retourne les garanties matérielles du dossier crédit")
    public ResponseEntity<List<GarantieMaterielleResponse>> getMaterielles(@PathVariable Long demandeCreditId) {
        return ResponseEntity.ok(garantieCreditWorkflowService.getGarantiesMaterielles(demandeCreditId));
    }
}