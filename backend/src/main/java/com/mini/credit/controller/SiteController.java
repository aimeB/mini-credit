package com.mini.credit.controller;

import com.mini.credit.dto.referentiel.CreateSiteRequest;
import com.mini.credit.dto.referentiel.SiteDTO;
import com.mini.credit.dto.referentiel.SiteResponse;
import com.mini.credit.dto.referentiel.UpdateSiteRequest;
import com.mini.credit.entity.agence.Agence;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.repository.AgenceRepository;
import com.mini.credit.repository.referentiel.SiteRepository;
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
@RequestMapping("/api/sites")
@Tag(name = "Site", description = "Gestion des sites/antennes")
public class SiteController {

    private final SiteRepository siteRepository;
    private final AgenceRepository agenceRepository;

    @GetMapping
    @Operation(summary = "Lister tous les sites")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU')")
    public List<SiteResponse> getAll() {
        return siteRepository.findAll()
                .stream()
                .map(SiteResponse::fromEntity)
                .toList();
    }

    @GetMapping("/actifs")
    @Operation(summary = "Lister les sites actifs")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'CAISSIER', 'CONTROLEUR')")
    public List<SiteResponse> getActifs() {
        return siteRepository.findAllByActifTrue()
                .stream()
                .map(SiteResponse::fromEntity)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Recuperer un site par ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU')")
    public ResponseEntity<SiteDTO> getById(@PathVariable Long id) {
        return siteRepository.findById(id)
                .map(this::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Creer un nouveau site rattache a une agence")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SiteDTO> create(@Valid @RequestBody CreateSiteRequest request) {
        if (siteRepository.findByCodeSite(request.getCodeSite()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Un site avec le code '" + request.getCodeSite() + "' existe deja");
        }
        Agence agence = agenceRepository.findById(request.getAgenceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Agence introuvable : id=" + request.getAgenceId()));
        Site site = Site.builder()
                .codeSite(normalizeUpper(request.getCodeSite()))
                .nomSite(normalizeUpper(request.getNomSite()))
                .zone(request.getZone())
                .actif(true)
                .agence(agence)
                .build();
        Site saved = siteRepository.save(site);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(saved));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un site existant")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SiteDTO> update(@PathVariable Long id,
                                          @RequestBody UpdateSiteRequest request) {
        Site site = siteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Site introuvable : id=" + id));
        if (request.getNomSite() != null)    site.setNomSite(normalizeUpper(request.getNomSite()));
        if (request.getZone() != null)       site.setZone(request.getZone());
        if (request.getActif() != null)      site.setActif(request.getActif());
        if (request.getAgenceId() != null) {
            Agence agence = agenceRepository.findById(request.getAgenceId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Agence introuvable : id=" + request.getAgenceId()));
            site.setAgence(agence);
        }
        Site saved = siteRepository.save(site);
        return ResponseEntity.ok(toDTO(saved));
    }

    @GetMapping("/by-agence/{agenceId}")
    @Operation(summary = "Lister les sites rattaches a une agence")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU')")
    public List<SiteResponse> getByAgence(@PathVariable Long agenceId) {
        if (!agenceRepository.existsById(agenceId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Agence introuvable : id=" + agenceId);
        }
        return siteRepository.findByAgenceId(agenceId)
                .stream()
                .map(SiteResponse::fromEntity)
                .toList();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "DÃ©sactiver un site (soft delete)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        Site site = siteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Site introuvable : id=" + id));
        site.setActif(false);
        siteRepository.save(site);
        return ResponseEntity.noContent().build();
    }

    private SiteDTO toDTO(Site site) {
        return SiteDTO.builder()
                .id(site.getId())
                .codeSite(site.getCodeSite())
                .nomSite(site.getNomSite())
                .zone(site.getZone())
                .actif(site.getActif())
                .agenceId(site.getAgence() != null ? site.getAgence().getId() : null)
                .nomAgence(site.getAgence() != null ? site.getAgence().getNomAgence() : null)
                .villeAgence(site.getAgence() != null ? site.getAgence().getVille() : null)
                .communeAgence(site.getAgence() != null ? site.getAgence().getCommune() : null)
                .dateCreation(site.getDateCreation())
                .dateModification(site.getDateModification())
                .build();
    }

        private String normalizeUpper(String value) {
                return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
        }
}
