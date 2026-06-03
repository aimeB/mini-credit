package com.mini.credit.controller;

import com.mini.credit.dto.membre.MembreCreateRequest;
import com.mini.credit.dto.membre.MembreActivationResponseDTO;
import com.mini.credit.dto.membre.MembreResponse;
import com.mini.credit.dto.membre.MembreUpdateRequest;
import com.mini.credit.service.MembreService;
import com.mini.credit.service.security.ScopeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/membres")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Membre", description = "Member management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class MembreController {

    private final MembreService membreService;
    private final ScopeService scopeService;

    @PostMapping
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE', 'AGENT_TERRAIN')")
    @Operation(summary = "Create member", description = "Create a new member with activation code")
    public ResponseEntity<MembreActivationResponseDTO> create(@Valid @RequestBody MembreCreateRequest request) {
        MembreActivationResponseDTO response = membreService.createWithActivationCode(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE', 'AGENT_BUREAU')")
    @Operation(summary = "Update member", description = "Update member information")
    public MembreResponse update(@PathVariable Long id, @Valid @RequestBody MembreUpdateRequest request) {
        return membreService.update(id, request);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @PreAuthorize("@scopeService.canReadMembre(#id)")
    @Operation(summary = "Get member by ID", description = "Retrieve member details")
    public MembreResponse getById(@PathVariable Long id) {
        return membreService.getById(id);
    }

    @GetMapping
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE', 'AGENT_BUREAU', 'AGENT_TERRAIN')")
    @Operation(summary = "Get all members", description = "Retrieve paginated list of all members")
    public Page<MembreResponse> getAll(@RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "10") int size) {
        // PHASE 3B: Limiter size max à 100 pour éviter les abus
        if (size > 100) size = 100;
        Pageable pageable = PageRequest.of(page, size);
        return membreService.getAll(pageable);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('MEMBER')")
    @Transactional
    @Operation(summary = "Get current member profile", description = "Get current authenticated member's profile")
    public MembreResponse getCurrentMember() {
        return membreService.getCurrentMember();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete member", description = "Delete a member account")
    public void delete(@PathVariable Long id) {
        membreService.delete(id);
    }
}
