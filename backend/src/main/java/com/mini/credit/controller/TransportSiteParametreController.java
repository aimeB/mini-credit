package com.mini.credit.controller;

import com.mini.credit.annotation.Auditable;
import com.mini.credit.dto.referentiel.TransportSiteParametreRequest;
import com.mini.credit.dto.referentiel.TransportSiteParametreResponse;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.service.TransportSiteParametreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/transport-site-parametres")
@RequiredArgsConstructor
public class TransportSiteParametreController {

    private final TransportSiteParametreService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'COO', 'GERANT_GENERAL', 'RCI', 'CAISSIER')")
    public List<TransportSiteParametreResponse> getAll() {
        return service.getAll();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'COO', 'GERANT_GENERAL')")
    @Auditable(action = AuditAction.MODIFICATION_OPERATION, entityType = "TransportSiteParametre", captureParameters = true, captureResult = true, reason = "Création/modification paramètre transport site")
    public TransportSiteParametreResponse save(@RequestBody @Valid TransportSiteParametreRequest request) {
        return service.save(request);
    }

    @PostMapping("/{id}/desactiver")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'COO', 'GERANT_GENERAL')")
    @Auditable(action = AuditAction.MODIFICATION_OPERATION, entityType = "TransportSiteParametre", entityIdExpression = "#id", captureParameters = true, captureResult = true, reason = "Désactivation paramètre transport site")
    public TransportSiteParametreResponse deactivate(@PathVariable Long id, @RequestParam String commentaire) {
        return service.deactivate(id, commentaire);
    }
}