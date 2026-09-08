package com.mini.credit.controller;

import com.mini.credit.dto.caisse.CaisseCreateRequest;
import com.mini.credit.dto.caisse.CaisseResponse;
import com.mini.credit.dto.caisse.SessionCaisseResponse;
import com.mini.credit.service.CaisseService;
import com.mini.credit.service.SessionCaisseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/caisses")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Caisse", description = "Caisse management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class CaisseController {

    private final CaisseService caisseService;
    private final SessionCaisseService sessionCaisseService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create new caisse", description = "Create a new caisse")
    public CaisseResponse create(@Valid @RequestBody CaisseCreateRequest request) {
        return caisseService.create(request);
    }

    @PostMapping("/initialiser-ma-caisse")
    @PreAuthorize("hasRole('CAISSIER')")
    @Operation(summary = "Initialize current cashier caisse", description = "Create the first active caisse for the authenticated cashier's agency")
    public CaisseResponse initialiserMaCaisse() {
        return caisseService.initialiserMaCaisse();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'CAISSIER', 'CONTROLEUR')")
    @Operation(summary = "Get caisse by ID", description = "Retrieve caisse details")
    public CaisseResponse getById(@PathVariable Long id) {
        return caisseService.getById(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'CAISSIER', 'CONTROLEUR')")
    @Operation(summary = "Get all caisses", description = "Retrieve list of all caisses")
    public List<CaisseResponse> getAll() {
        return caisseService.getAll();
    }

    @GetMapping("/actives")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'CAISSIER', 'CONTROLEUR')")
    @Operation(summary = "Get active caisses", description = "Retrieve list of active caisses")
    public List<CaisseResponse> getActives() {
        return caisseService.getActives();
    }

    @GetMapping("/accessibles")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'CAISSIER', 'CONTROLEUR')")
    @Operation(summary = "Get accessible caisses", description = "Retrieve list of accessible caisses for current user")
    public List<CaisseResponse> getAccessibles() {
        return caisseService.getAccessibles();
    }

    @GetMapping("/{id}/sessions/ouvertes")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'CAISSIER', 'CONTROLEUR')")
    @Operation(summary = "Get open session by caisse", description = "Retrieve open session for a caisse")
    public SessionCaisseResponse getSessionOuverteByCaisse(@PathVariable Long id) {
        return sessionCaisseService.getSessionOuverteByCaisse(id);
    }
}

