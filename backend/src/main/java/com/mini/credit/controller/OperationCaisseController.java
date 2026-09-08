package com.mini.credit.controller;

import com.mini.credit.annotation.Auditable;
import com.mini.credit.dto.caisse.JournalCaisseFilterRequest;
import com.mini.credit.dto.caisse.JournalCaisseResponse;
import com.mini.credit.dto.caisse.OperationCaisseResponse;
import com.mini.credit.dto.caisse.OperationCaisseRequest;
import com.mini.credit.dto.caisse.RequalificationNatureFinancementRequest;
import com.mini.credit.enums.CategorieOperationCaisse;
import com.mini.credit.enums.SourceOperationCaisse;
import com.mini.credit.enums.TypeOperationCaisse;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.enums.security.AuditModule;
import com.mini.credit.service.OperationCaisseService;
import com.mini.credit.service.audit.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/operations-caisse")
@RequiredArgsConstructor
@Tag(name = "Operation Caisse", description = "Caisse operation management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class OperationCaisseController {

    private final OperationCaisseService operationCaisseService;
    private final AuditService auditService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU') or (hasAuthority('OPERATION_CAISSE_CREATE') and !hasAnyRole('CAISSIER', 'RCI'))")
    @Operation(summary = "Register caisse operation", description = "Register a new caisse operation")
    @Auditable(action = AuditAction.OPERATION_CAISSE_CREATED, entityType = "OperationCaisse", captureParameters = true, captureResult = true)
    public OperationCaisseResponse enregistrer(@Valid @RequestBody OperationCaisseRequest request) {
        return operationCaisseService.enregistrer(request);
    }

    @PatchMapping("/{id}/nature-financement")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU') or (hasAuthority('OPERATION_CAISSE_UPDATE') and !hasAnyRole('CAISSIER', 'RCI'))")
    @Operation(summary = "Requalifier la nature de financement", description = "Requalifie uniquement la nature de financement d'un approvisionnement caisse existant")
    public OperationCaisseResponse requalifierNatureFinancement(
            @PathVariable Long id,
            @Valid @RequestBody RequalificationNatureFinancementRequest request
    ) {
        return operationCaisseService.requalifierNatureFinancement(id, request);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('OPERATION_CAISSE_READ')")
    @Operation(summary = "Get all caisse operations", description = "Retrieve paginated list of all caisse operations")
    public Page<OperationCaisseResponse> getAll(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        // PHASE 3B: Limiter size max Ã  100 pour Ã©viter les abus
        if (size > 100) size = 100;
        Pageable pageable = PageRequest.of(page, size);
        return operationCaisseService.getAll(pageable);
    }

    @GetMapping("/session/{sessionId}")
    @PreAuthorize("hasAuthority('OPERATION_CAISSE_READ')")
    @Operation(summary = "Get operations by session", description = "Retrieve caisse operations for a specific session")
    public List<OperationCaisseResponse> getBySession(@PathVariable Long sessionId) {
        return operationCaisseService.getBySession(sessionId);
    }

    @GetMapping("/caisse/{caisseId}")
    @PreAuthorize("hasAuthority('OPERATION_CAISSE_READ')")
    @Operation(summary = "Get operations by caisse", description = "Retrieve caisse operations for a specific caisse")
    public List<OperationCaisseResponse> getByCaisse(@PathVariable Long caisseId) {
        return operationCaisseService.getByCaisse(caisseId);
    }

    @GetMapping("/journal")
    @PreAuthorize("hasAnyAuthority('JOURNAL_CAISSE_READ', 'OPERATION_CAISSE_READ')")
    @Operation(summary = "Journal caisse filtré", description = "Retrieve filtered paginated cash journal")
    public Page<JournalCaisseResponse> getJournal(
            @RequestParam(required = false) Long sessionCaisseId,
            @RequestParam(required = false) Long caisseId,
            @RequestParam(required = false) Long siteId,
            @RequestParam(required = false) Long utilisateurId,
            @RequestParam(required = false) TypeOperationCaisse typeOperation,
            @RequestParam(required = false) CategorieOperationCaisse categorie,
            @RequestParam(required = false) SourceOperationCaisse source,
            @RequestParam(required = false) LocalDate dateDebut,
            @RequestParam(required = false) LocalDate dateFin,
            @RequestParam(required = false) String referenceMetier,
            @RequestParam(required = false) Long recetteId,
            @RequestParam(required = false) Long depenseCaisseId,
            @RequestParam(required = false) Long creditId,
            @RequestParam(required = false) Long retraitEpargneId,
            @RequestParam(required = false) Long operationEpargneId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "dateOperation,desc") String sort
    ) {
        if (size > 200) {
            size = 200;
        }

        JournalCaisseFilterRequest filter = new JournalCaisseFilterRequest();
        filter.setSessionCaisseId(sessionCaisseId);
        filter.setCaisseId(caisseId);
        filter.setSiteId(siteId);
        filter.setUtilisateurId(utilisateurId);
        filter.setTypeOperation(typeOperation);
        filter.setCategorie(categorie);
        filter.setSource(source);
        filter.setDateDebut(dateDebut != null ? dateDebut.atStartOfDay() : null);
        filter.setDateFin(dateFin != null ? dateFin.atTime(LocalTime.MAX) : null);
        filter.setReferenceMetier(referenceMetier);
        filter.setRecetteId(recetteId);
        filter.setDepenseCaisseId(depenseCaisseId);
        filter.setCreditId(creditId);
        filter.setRetraitEpargneId(retraitEpargneId);
        filter.setOperationEpargneId(operationEpargneId);

        String[] sortParts = sort.split(",");
        String sortField = sortParts.length > 0 ? sortParts[0] : "dateOperation";
        Sort.Direction direction = sortParts.length > 1 && "asc".equalsIgnoreCase(sortParts[1])
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
        Page<JournalCaisseResponse> result = operationCaisseService.getJournal(filter, pageable);
        auditService.logBusinessEvent(
            AuditAction.RAPPORT_GENERE,
            AuditModule.JOURNAL_CAISSE,
            "JournalCaisse",
            sessionCaisseId,
            true,
            "Consultation journal caisse",
            referenceMetier
        );
        return result;
    }


}

