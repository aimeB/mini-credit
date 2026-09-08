package com.mini.credit.controller;

import com.mini.credit.dto.rapport.revenus.RapportRevenusResponse;
import com.mini.credit.service.RapportRevenusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/rapports/revenus")
@RequiredArgsConstructor
@Tag(name = "Rapport des revenus", description = "Rapport consultatif des revenus réels par antenne et catégorie")
@SecurityRequirement(name = "bearer-jwt")
public class RapportRevenusController {

    private final RapportRevenusService rapportRevenusService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERANT_GENERAL', 'COO', 'RCI', 'CHEF_BUREAU', 'CONTROLEUR')")
    @Operation(summary = "Rapport des revenus", description = "Agrège uniquement les revenus réels sans compter les simples mouvements de caisse")
    public RapportRevenusResponse getRapportRevenus(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(required = false) Long antenneId,
            @RequestParam(required = false) Long agenceId,
            @RequestParam(required = false) String categorie,
            @RequestParam(required = false) String source
    ) {
        Long bureauId = agenceId != null ? agenceId : antenneId;
        return rapportRevenusService.getRapportRevenus(dateDebut, dateFin, bureauId, categorie, source);
    }
}
