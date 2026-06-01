package com.mini.credit.controller;

import com.mini.credit.dto.credit.PenaliteCreditResponse;
import com.mini.credit.service.PenaliteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/penalites")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Penalite", description = "Credit penalties management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class PenaliteController {

    private final PenaliteService penaliteService;

    @GetMapping("/credit/{creditId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE', 'AGENT_BUREAU', 'AGENT_TERRAIN') or (hasRole('MEMBER') and @penaliteService.isCurrentUserCredit(#creditId))")
    @Operation(summary = "Calculate penalties", description = "Calculate penalties for a credit")
    public PenaliteCreditResponse calculer(
            @PathVariable Long creditId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateReference
    ) {
        return penaliteService.calculerPenalitesCredit(creditId, dateReference);
    }

    @PostMapping("/credit/{creditId}/appliquer")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE')")
    @Operation(summary = "Apply penalties", description = "Apply calculated penalties to a credit")
    public void appliquer(
            @PathVariable Long creditId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateReference
    ) {
        penaliteService.appliquerPenalitesCredit(creditId, dateReference);
    }
}
