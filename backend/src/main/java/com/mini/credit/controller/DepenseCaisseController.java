package com.mini.credit.controller;

import com.mini.credit.annotation.Auditable;
import com.mini.credit.dto.caisse.*;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.service.DepenseCaisseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/depenses-caisse")
@RequiredArgsConstructor
@Tag(name = "Dépense Caisse", description = "Workflow des dépenses caisse")
@SecurityRequirement(name = "bearer-jwt")
public class DepenseCaisseController {

    private final DepenseCaisseService depenseCaisseService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CAISSIER', 'CHEF_BUREAU') or hasAuthority('DEPENSE_CAISSE_CREATE')")
    @Operation(summary = "Créer une dépense caisse")
    public DepenseCaisseResponse creer(@RequestBody @Valid DepenseCaisseCreateRequest request) {
        return depenseCaisseService.creer(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CAISSIER', 'CHEF_BUREAU', 'CONTROLEUR', 'RCI') or hasAuthority('DEPENSE_CAISSE_READ')")
    @Operation(summary = "Lister les dépenses caisse")
    public List<DepenseCaisseResponse> getAll(
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) Long caisseId,
            @RequestParam(required = false) Long siteId,
            @RequestParam(required = false) Long sessionCaisseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin
    ) {
        return depenseCaisseService.getAll(statut, caisseId, siteId, sessionCaisseId, dateDebut, dateFin);
    }

    @GetMapping("/beneficiaires-salaire")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAISSIER', 'CHEF_BUREAU') or hasAuthority('DEPENSE_CAISSE_CREATE')")
    @Operation(summary = "Lister les bénéficiaires possibles pour une dépense salaire")
    public List<DepenseCaisseBeneficiaireSalaireResponse> getBeneficiairesSalaire(@RequestParam Long caisseId) {
        return depenseCaisseService.getBeneficiairesSalaire(caisseId);
    }

    @GetMapping("/paie-preview")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAISSIER', 'CHEF_BUREAU') or hasAuthority('DEPENSE_CAISSE_CREATE')")
    @Operation(summary = "Prévisualiser la paie d'un employé pour une période")
    public PaieEmployePreviewResponse getPaiePreview(@RequestParam Long employeId, @RequestParam String periodePaie) {
        return depenseCaisseService.getPaiePreview(employeId, periodePaie);
    }

    @PatchMapping("/{id}/rattachement-paie")
    @PreAuthorize("hasAnyAuthority('DEPENSE_CAISSE_UPDATE', 'DEPENSE_CAISSE_CREATE')")
    @Operation(summary = "Rattacher une dépense salaire historique à une paie")
    @Auditable(action = AuditAction.MODIFICATION_OPERATION, entityType = "DepenseCaisse", entityIdExpression = "#id", captureParameters = true, captureResult = true, reason = "Rattachement paie dépense caisse")
    public DepenseCaisseResponse rattacherPaie(@PathVariable Long id, @RequestBody @Valid DepenseCaisseRattachementPaieRequest request) {
        return depenseCaisseService.rattacherPaie(id, request);
    }

    @PatchMapping("/{id}/rattachement-transport")
    @PreAuthorize("hasAnyAuthority('DEPENSE_CAISSE_UPDATE', 'DEPENSE_CAISSE_CREATE')")
    @Operation(summary = "Rattacher une dépense transport historique à un agent terrain/site/période")
    @Auditable(action = AuditAction.MODIFICATION_OPERATION, entityType = "DepenseCaisse", entityIdExpression = "#id", captureParameters = true, captureResult = true, reason = "Rattachement transport dépense caisse")
    public DepenseCaisseResponse rattacherTransport(@PathVariable Long id, @RequestBody @Valid DepenseCaisseRattachementTransportRequest request) {
        return depenseCaisseService.rattacherTransport(id, request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAISSIER', 'CHEF_BUREAU', 'CONTROLEUR', 'RCI') or hasAuthority('DEPENSE_CAISSE_READ')")
    @Operation(summary = "Consulter une dépense caisse")
    public DepenseCaisseResponse getById(@PathVariable Long id) {
        return depenseCaisseService.getById(id);
    }

    @PostMapping("/{id}/soumettre")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAISSIER', 'CHEF_BUREAU') or hasAuthority('DEPENSE_CAISSE_SUBMIT')")
    @Operation(summary = "Soumettre une dépense caisse")
    public DepenseCaisseResponse soumettre(@PathVariable Long id, @RequestBody(required = false) DepenseCaisseSubmitRequest request) {
        return depenseCaisseService.soumettre(id, request);
    }

    @PostMapping("/{id}/valider")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'CONTROLEUR') or hasAuthority('DEPENSE_CAISSE_VALIDATE')")
    @Operation(summary = "Valider une dépense caisse")
    public DepenseCaisseResponse valider(@PathVariable Long id, @RequestBody(required = false) DepenseCaisseValidateRequest request) {
        return depenseCaisseService.valider(id, request);
    }

    @PostMapping("/{id}/rejeter")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'CONTROLEUR') or hasAuthority('DEPENSE_CAISSE_REJECT')")
    @Operation(summary = "Rejeter une dépense caisse")
    public DepenseCaisseResponse rejeter(@PathVariable Long id, @RequestBody @Valid DepenseCaisseRejectRequest request) {
        return depenseCaisseService.rejeter(id, request);
    }

    @PostMapping("/{id}/payer")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAISSIER') or hasAuthority('DEPENSE_CAISSE_PAY')")
    @Operation(summary = "Payer une dépense caisse")
    public DepenseCaisseResponse payer(@PathVariable Long id, @RequestBody(required = false) DepenseCaissePayRequest request) {
        return depenseCaisseService.payer(id, request);
    }

    @PostMapping("/{id}/annuler")
    @PreAuthorize("hasAnyAuthority('DEPENSE_CAISSE_CANCEL')")
    @Operation(summary = "Annuler une dépense caisse")
    public DepenseCaisseResponse annuler(@PathVariable Long id, @RequestParam(required = false) String commentaire) {
        return depenseCaisseService.annuler(id, commentaire);
    }
}