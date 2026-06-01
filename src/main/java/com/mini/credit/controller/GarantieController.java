package com.mini.credit.controller;

import com.mini.credit.dto.garantie.GarantieCreateRequest;
import com.mini.credit.dto.garantie.GarantieResponse;
import com.mini.credit.enums.StatutGarantie;
import com.mini.credit.enums.TypeGarantie;
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
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Garantie", description = "Guarantee/Collateral management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class GarantieController {

    private final GarantieService garantieService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESPONSABLE') or hasRole('AGENT_BUREAU')")
    @Operation(summary = "Get all guarantees", description = "Retrieve list of all guarantees")
    public ResponseEntity<List<GarantieResponse>> getAllGaranties() {
        return ResponseEntity.ok(garantieService.getAllGaranties());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESPONSABLE') or hasRole('AGENT_BUREAU')")
    @Operation(summary = "Get guarantee by ID", description = "Retrieve guarantee details")
    public ResponseEntity<GarantieResponse> getGarantieById(@PathVariable Long id) {
        return ResponseEntity.ok(garantieService.getGarantieById(id));
    }

    @GetMapping("/credit/{creditId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESPONSABLE') or hasRole('AGENT_BUREAU')")
    @Operation(summary = "Get guarantees by credit ID", description = "Retrieve all guarantees for a specific credit")
    public ResponseEntity<List<GarantieResponse>> getGarantiesByCreditId(@PathVariable Long creditId) {
        return ResponseEntity.ok(garantieService.getGarantiesByCreditId(creditId));
    }

    @GetMapping("/membre/{membreId}")
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESPONSABLE') or hasRole('AGENT_BUREAU') or hasRole('MEMBER')")
    @Operation(summary = "Get guarantees by member ID", description = "Retrieve all guarantees for a specific member")
    public ResponseEntity<List<GarantieResponse>> getGarantiesByMembreId(@PathVariable Long membreId) {
        return ResponseEntity.ok(garantieService.getGarantiesByMembreId(membreId));
    }

    @GetMapping("/demande/{demandeCreditId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESPONSABLE') or hasRole('AGENT_BUREAU')")
    @Operation(summary = "Get guarantees by credit request ID", description = "Retrieve all guarantees for a specific credit request")
    public ResponseEntity<List<GarantieResponse>> getGarantiesByDemandeCreditId(@PathVariable Long demandeCreditId) {
        return ResponseEntity.ok(garantieService.getGarantiesByDemandeCreditId(demandeCreditId));
    }

    @GetMapping("/type/{type}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESPONSABLE') or hasRole('AGENT_BUREAU')")
    @Operation(summary = "Get guarantees by type", description = "Retrieve guarantees by type")
    public ResponseEntity<List<GarantieResponse>> getGarantiesByType(@PathVariable TypeGarantie type) {
        return ResponseEntity.ok(garantieService.getGarantiesByType(type));
    }

    @GetMapping("/statut/{statut}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESPONSABLE') or hasRole('AGENT_BUREAU')")
    @Operation(summary = "Get guarantees by status", description = "Retrieve guarantees by status")
    public ResponseEntity<List<GarantieResponse>> getGarantiesByStatut(@PathVariable StatutGarantie statut) {
        return ResponseEntity.ok(garantieService.getGarantiesByStatut(statut));
    }

    @GetMapping("/date-range")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESPONSABLE') or hasRole('AGENT_BUREAU')")
    @Operation(summary = "Get guarantees by date range", description = "Retrieve guarantees by date range")
    public ResponseEntity<List<GarantieResponse>> getGarantiesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        return ResponseEntity.ok(garantieService.getGarantiesByDateRange(debut, fin));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESPONSABLE') or hasRole('AGENT_BUREAU')")
    @Operation(summary = "Create guarantee", description = "Create a new guarantee")
    public ResponseEntity<GarantieResponse> createGarantie(@Valid @RequestBody GarantieCreateRequest request) {
        GarantieResponse response = garantieService.createGarantie(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESPONSABLE') or hasRole('AGENT_BUREAU')")
    @Operation(summary = "Update guarantee", description = "Update an existing guarantee")
    public ResponseEntity<GarantieResponse> updateGarantie(@PathVariable Long id, @Valid @RequestBody GarantieCreateRequest request) {
        return ResponseEntity.ok(garantieService.updateGarantie(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete guarantee", description = "Delete a guarantee")
    public ResponseEntity<Void> deleteGarantie(@PathVariable Long id) {
        garantieService.deleteGarantie(id);
        return ResponseEntity.noContent().build();
    }
}
