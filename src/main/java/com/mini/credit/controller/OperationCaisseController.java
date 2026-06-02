package com.mini.credit.controller;

import com.mini.credit.annotation.Auditable;
import com.mini.credit.dto.caisse.OperationCaisseResponse;
import com.mini.credit.dto.caisse.OperationCaisseRequest;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.service.OperationCaisseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/operations-caisse")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Operation Caisse", description = "Caisse operation management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class OperationCaisseController {

    private final OperationCaisseService operationCaisseService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE', 'CAISSIER')")
    @Operation(summary = "Register caisse operation", description = "Register a new caisse operation")
    @Auditable(action = AuditAction.OPERATION_CAISSE_CREATED, entityType = "OperationCaisse", captureParameters = true, captureResult = true)
    public OperationCaisseResponse enregistrer(@Valid @RequestBody OperationCaisseRequest request) {
        return operationCaisseService.enregistrer(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE', 'CAISSIER')")
    @Operation(summary = "Get all caisse operations", description = "Retrieve paginated list of all caisse operations")
    public Page<OperationCaisseResponse> getAll(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        // PHASE 3B: Limiter size max à 100 pour éviter les abus
        if (size > 100) size = 100;
        Pageable pageable = PageRequest.of(page, size);
        return operationCaisseService.getAll(pageable);
    }

    @GetMapping("/session/{sessionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE', 'CAISSIER')")
    @Operation(summary = "Get operations by session", description = "Retrieve caisse operations for a specific session")
    public List<OperationCaisseResponse> getBySession(@PathVariable Long sessionId) {
        return operationCaisseService.getBySession(sessionId);
    }

    @GetMapping("/caisse/{caisseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE', 'CAISSIER')")
    @Operation(summary = "Get operations by caisse", description = "Retrieve caisse operations for a specific caisse")
    public List<OperationCaisseResponse> getByCaisse(@PathVariable Long caisseId) {
        return operationCaisseService.getByCaisse(caisseId);
    }


}
