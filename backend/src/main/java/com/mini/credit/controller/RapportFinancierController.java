package com.mini.credit.controller;

import com.mini.credit.dto.rapport.RapportFinancierDTO;
import com.mini.credit.enums.TypeRapport;
import com.mini.credit.service.RapportFinancierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * PHASE 12: Controller Rapports Financiers
 *
 * Endpoints pour consultation et génération de rapports financiers
 */
@RestController
@RequestMapping("/api/rapports-financiers")
@RequiredArgsConstructor
@Tag(name = "Rapports Financiers", description = "PHASE 12 - Génération et consultation rapports financiers")
@SecurityRequirement(name = "bearerAuth")
public class RapportFinancierController {

    private final RapportFinancierService rapportFinancierService;

    /**
     * POST /api/rapports-financiers/generer
     * Génère rapport financier manuel pour période spécifique
     */
    @PostMapping("/generer")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Générer rapport", description = "Génère un rapport financier pour type et période spécifiques")
    public ResponseEntity<RapportFinancierDTO> genererRapport(
            @RequestParam @NotNull TypeRapport typeRapport,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @NotNull LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @NotNull LocalDate dateFin) {
        RapportFinancierDTO rapport = rapportFinancierService.genererRapportPersonnalise(typeRapport, dateDebut, dateFin);
        return ResponseEntity.status(HttpStatus.CREATED).body(rapport);
    }

    /**
     * GET /api/rapports-financiers/par-type
     * Récupère rapports par type
     */
    @GetMapping("/par-type")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTIONNAIRE')")
    @Operation(summary = "Rapports par type", description = "Lister tous les rapports d'un type spécifique")
    public ResponseEntity<List<RapportFinancierDTO>> getByTypeRapport(
            @RequestParam @NotNull TypeRapport typeRapport) {
        List<RapportFinancierDTO> rapports = rapportFinancierService.getByTypeRapport(typeRapport);
        return ResponseEntity.ok(rapports);
    }

    /**
     * GET /api/rapports-financiers/en-attente-validation
     * Récupère rapports en attente de validation
     */
    @GetMapping("/en-attente-validation")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "En attente de validation", description = "Lister rapports à valider")
    public ResponseEntity<List<RapportFinancierDTO>> getEnAttenteValidation() {
        List<RapportFinancierDTO> rapports = rapportFinancierService.getEnAttenteValidation();
        return ResponseEntity.ok(rapports);
    }

    /**
     * POST /api/rapports-financiers/{id}/valider
     * Valide un rapport
     */
    @PostMapping("/{id}/valider")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Valider rapport", description = "Valider et approuver un rapport financier")
    public ResponseEntity<RapportFinancierDTO> validerRapport(
            @PathVariable @NotNull Long id) {
        RapportFinancierDTO rapport = rapportFinancierService.validerRapport(id);
        return ResponseEntity.ok(rapport);
    }

    /**
     * POST /api/rapports-financiers/{id}/archiver
     * Archive un rapport
     */
    @PostMapping("/{id}/archiver")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Archiver rapport", description = "Archiver un rapport validé")
    public ResponseEntity<RapportFinancierDTO> archiverRapport(
            @PathVariable @NotNull Long id) {
        RapportFinancierDTO rapport = rapportFinancierService.archiverRapport(id);
        return ResponseEntity.ok(rapport);
    }

    /**
     * GET /api/rapports-financiers/{id}
     * Récupère rapport spécifique
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTIONNAIRE')")
    @Operation(summary = "Détails rapport", description = "Récupère détails complets d'un rapport")
    public ResponseEntity<RapportFinancierDTO> getRapport(
            @PathVariable @NotNull Long id) {
        RapportFinancierDTO rapport = rapportFinancierService.getById(id);
        return ResponseEntity.ok(rapport);
    }

    /**
     * GET /api/rapports-financiers/recents
     * Récupère derniers rapports
     */
    @GetMapping("/recents")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTIONNAIRE')")
    @Operation(summary = "Rapports récents", description = "Lister les derniers rapports non archivés")
    public ResponseEntity<List<RapportFinancierDTO>> getRapportsRecents() {
        List<RapportFinancierDTO> rapports = rapportFinancierService.getRapportRecents();
        return ResponseEntity.ok(rapports);
    }
}
