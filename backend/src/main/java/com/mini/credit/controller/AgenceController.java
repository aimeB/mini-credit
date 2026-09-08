package com.mini.credit.controller;

import com.mini.credit.dto.AgenceDTO;
import com.mini.credit.dto.CreateAgenceRequest;
import com.mini.credit.dto.UpdateAgenceRequest;
import com.mini.credit.entity.agence.Agence;
import com.mini.credit.repository.AgenceRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/agences")
@Tag(name = "Agence", description = "Gestion des agences â€” crÃ©ation manuelle E2E")
public class AgenceController {

    private final AgenceRepository agenceRepository;

    @GetMapping
    @Operation(summary = "Lister toutes les agences")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU')")
    public List<AgenceDTO> getAll() {
        return agenceRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    @GetMapping("/actives")
    @Operation(summary = "Lister les agences actives")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU')")
    public List<AgenceDTO> getActives() {
        return agenceRepository.findByActifTrue().stream()
                .map(this::toDTO)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "RÃ©cupÃ©rer une agence par ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU')")
    public ResponseEntity<AgenceDTO> getById(@PathVariable Long id) {
        return agenceRepository.findById(id)
                .map(a -> ResponseEntity.ok(toDTO(a)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "CrÃ©er une nouvelle agence")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AgenceDTO> create(@Valid @RequestBody CreateAgenceRequest request) {
        if (agenceRepository.existsByCodeAgenceIgnoreCase(request.getCodeAgence())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Une agence avec le code '" + request.getCodeAgence() + "' existe dÃ©jÃ ");
        }
        Agence agence = Agence.builder()
            .codeAgence(normalizeUpper(request.getCodeAgence()))
            .nomAgence(normalizeUpper(request.getNomAgence()))
                .adresse(request.getAdresse())
                .commune(request.getCommune())
                .quartier(request.getQuartier())
                .reference(request.getReference())
                .telephone(request.getTelephone())
                .ville(request.getVille())
                .actif(Boolean.TRUE.equals(request.getActif()))
                .description(request.getDescription())
                .build();
        Agence saved = agenceRepository.save(agence);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(saved));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier une agence existante")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AgenceDTO> update(@PathVariable Long id,
                                             @Valid @RequestBody UpdateAgenceRequest request) {
        Agence agence = agenceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Agence introuvable : id=" + id));
        agence.setNomAgence(normalizeUpper(request.getNomAgence()));
        agence.setAdresse(request.getAdresse());
        agence.setCommune(request.getCommune());
        agence.setQuartier(request.getQuartier());
        agence.setReference(request.getReference());
        agence.setTelephone(request.getTelephone());
        agence.setVille(request.getVille());
        agence.setActif(Boolean.TRUE.equals(request.getActif()));
        agence.setDescription(request.getDescription());
        Agence saved = agenceRepository.save(agence);
        return ResponseEntity.ok(toDTO(saved));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "DÃ©sactiver une agence (soft delete)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        Agence agence = agenceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Agence introuvable : id=" + id));
        agence.setActif(false);
        agenceRepository.save(agence);
        return ResponseEntity.noContent().build();
    }

    private AgenceDTO toDTO(Agence agence) {
        return AgenceDTO.builder()
                .id(agence.getId())
                .codeAgence(agence.getCodeAgence())
                .nomAgence(agence.getNomAgence())
                .adresse(agence.getAdresse())
                .commune(agence.getCommune())
                .quartier(agence.getQuartier())
                .reference(agence.getReference())
                .telephone(agence.getTelephone())
                .email(agence.getEmail())
                .ville(agence.getVille())
                .actif(agence.getActif())
                .description(agence.getDescription())
                .chefBureauId(agence.getChefBureau() != null ? agence.getChefBureau().getId() : null)
                .dateCreation(agence.getDateCreation())
                .dateModification(agence.getDateModification())
                .build();
    }

    private String normalizeUpper(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }
}

