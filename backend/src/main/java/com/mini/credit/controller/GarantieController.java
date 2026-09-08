package com.mini.credit.controller;

import com.mini.credit.annotation.Auditable;
import com.mini.credit.dto.garantie.GarantieCreateRequest;
import com.mini.credit.dto.garantie.GarantieResponse;
import com.mini.credit.enums.StatutGarantie;
import com.mini.credit.enums.TypeGarantie;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.service.GarantieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/garanties")
@RequiredArgsConstructor
@Tag(name = "Garantie", description = "Guarantee/Collateral management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class GarantieController {

    private final GarantieService garantieService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('CHEF_BUREAU') or hasRole('GESTIONNAIRE') or hasAuthority('GARANTIE_CONTROL')")
    @Operation(summary = "Get all guarantees", description = "Retrieve list of all guarantees")
    public ResponseEntity<List<GarantieResponse>> getAllGaranties() {
        return ResponseEntity.ok(garantieService.getAllGaranties());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CHEF_BUREAU') or hasRole('GESTIONNAIRE') or hasAuthority('GARANTIE_CONTROL')")
    @Operation(summary = "Get guarantee by ID", description = "Retrieve guarantee details")
    public ResponseEntity<GarantieResponse> getGarantieById(@PathVariable Long id) {
        return ResponseEntity.ok(garantieService.getGarantieById(id));
    }

    @GetMapping("/credit/{creditId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CHEF_BUREAU') or hasRole('GESTIONNAIRE') or hasAuthority('GARANTIE_CONTROL')")
    @Operation(summary = "Get guarantees by credit ID", description = "Retrieve all guarantees for a specific credit")
    public ResponseEntity<List<GarantieResponse>> getGarantiesByCreditId(@PathVariable Long creditId) {
        return ResponseEntity.ok(garantieService.getGarantiesByCreditId(creditId));
    }

    @GetMapping("/membre/{membreId}")
    @Transactional(readOnly = true)
    @PreAuthorize("@scopeService.canReadMembre(#membreId)")
    @Operation(summary = "Get guarantees by member ID", description = "Retrieve all guarantees for a specific member")
    public ResponseEntity<List<GarantieResponse>> getGarantiesByMembreId(@PathVariable Long membreId) {
        return ResponseEntity.ok(garantieService.getGarantiesByMembreId(membreId));
    }

    @GetMapping("/demande/{demandeCreditId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CHEF_BUREAU') or hasRole('GESTIONNAIRE')")
    @Operation(summary = "Get guarantees by credit request ID", description = "Retrieve all guarantees for a specific credit request")
    public ResponseEntity<List<GarantieResponse>> getGarantiesByDemandeCreditId(@PathVariable Long demandeCreditId) {
        return ResponseEntity.ok(garantieService.getGarantiesByDemandeCreditId(demandeCreditId));
    }

    @GetMapping("/type/{type}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CHEF_BUREAU') or hasRole('GESTIONNAIRE') or hasAuthority('GARANTIE_CONTROL')")
    @Operation(summary = "Get guarantees by type", description = "Retrieve guarantees by type")
    public ResponseEntity<List<GarantieResponse>> getGarantiesByType(@PathVariable TypeGarantie type) {
        return ResponseEntity.ok(garantieService.getGarantiesByType(type));
    }

    @GetMapping("/statut/{statut}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CHEF_BUREAU') or hasRole('GESTIONNAIRE') or hasAuthority('GARANTIE_CONTROL')")
    @Operation(summary = "Get guarantees by status", description = "Retrieve guarantees by status")
    public ResponseEntity<List<GarantieResponse>> getGarantiesByStatut(@PathVariable StatutGarantie statut) {
        return ResponseEntity.ok(garantieService.getGarantiesByStatut(statut));
    }

    @GetMapping("/date-range")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CHEF_BUREAU') or hasRole('GESTIONNAIRE') or hasAuthority('GARANTIE_CONTROL')")
    @Operation(summary = "Get guarantees by date range", description = "Retrieve guarantees by date range")
    public ResponseEntity<List<GarantieResponse>> getGarantiesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        return ResponseEntity.ok(garantieService.getGarantiesByDateRange(debut, fin));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create guarantee disabled", description = "Legacy free guarantee creation is disabled. Use the credit request guarantee workflow.")
    @Auditable(action = AuditAction.INVALID_OPERATION, entityType = "Garantie", reason = "Tentative de creation libre de garantie legacy")
    public ResponseEntity<GarantieResponse> createGarantie(@Valid @RequestBody GarantieCreateRequest request) {
        throw new BusinessException("Création libre de garantie désactivée. Les garanties crédit 3N doivent être traitées depuis le dossier de demande de crédit.");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RCI')")
    @Operation(summary = "Update guarantee disabled", description = "Legacy guarantee update is disabled to preserve historical data.")
    @Auditable(action = AuditAction.INVALID_OPERATION, entityType = "Garantie", entityIdExpression = "#id", reason = "Tentative de modification de garantie legacy")
    public ResponseEntity<GarantieResponse> updateGarantie(@PathVariable Long id, @Valid @RequestBody GarantieCreateRequest request) {
        throw new BusinessException("Modification libre de garantie désactivée. Les garanties crédit 3N doivent être contrôlées depuis le workflow de demande de crédit.");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete guarantee disabled", description = "Physical deletion is disabled to preserve guarantee history.")
    @Auditable(action = AuditAction.SUPPRESSION_REFUSEE, entityType = "Garantie", entityIdExpression = "#id", reason = "Suppression physique de garantie legacy refusee")
    public ResponseEntity<Void> deleteGarantie(@PathVariable Long id) {
        throw new BusinessException("Suppression physique désactivée. Les données historiques de garantie doivent être conservées.");
    }
}

