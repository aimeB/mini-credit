package com.mini.credit.controller;

import com.mini.credit.dto.credit.EcheanceCreditResponse;
import com.mini.credit.service.EcheanceCreditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/credits")
@RequiredArgsConstructor
@Tag(name = "Echeance Credit", description = "Credit payment schedule endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class EcheanceCreditController {

    private final EcheanceCreditService echeanceCreditService;

    @GetMapping("/{creditId}/echeances")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'GESTIONNAIRE', 'AGENT_TERRAIN') or (hasRole('MEMBER') and @echeanceCreditService.isCurrentUserCredit(#creditId))")
    @Operation(summary = "Get payment schedules", description = "Retrieve all payment schedules for a credit")
    public List<EcheanceCreditResponse> getByCreditId(@PathVariable Long creditId) {
        return echeanceCreditService.getByCreditId(creditId);
    }
}

