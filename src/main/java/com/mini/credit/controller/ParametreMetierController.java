package com.mini.credit.controller;

import com.mini.credit.dto.parametrage.ParametreMetierDTO;
import com.mini.credit.enums.CategorieParametre;
import com.mini.credit.service.ParametreMetierService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST pour l'accès aux paramètres métier.
 *
 * PHASE 1: API d'administration uniquement (ADMIN role).
 * Endpoints pour consulter les paramètres en runtime.
 */
@RestController
@RequestMapping("/api/admin/parametres-metier")
@RequiredArgsConstructor
public class ParametreMetierController {

    private final ParametreMetierService parametreMetierService;

    /**
     * GET /api/admin/parametres-metier
     * Récupère tous les paramètres actifs.
     * Accès: ADMIN only
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ParametreMetierDTO>> getAllActifs() {
        List<ParametreMetierDTO> parametres = parametreMetierService.getAllActifs();
        return ResponseEntity.ok(parametres);
    }

    /**
     * GET /api/admin/parametres-metier/{cle}
     * Récupère un paramètre par sa clé.
     * Accès: ADMIN only
     */
    @GetMapping("/{cle}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ParametreMetierDTO> getByKey(@PathVariable String cle) {
        return parametreMetierService.getByKey(cle)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/admin/parametres-metier/categorie/{categorie}
     * Récupère tous les paramètres d'une catégorie.
     * Accès: ADMIN only
     */
    @GetMapping("/categorie/{categorie}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ParametreMetierDTO>> getByCategorie(
            @PathVariable CategorieParametre categorie) {
        List<ParametreMetierDTO> parametres = parametreMetierService.getByCategorie(categorie);
        return ResponseEntity.ok(parametres);
    }

    /**
     * POST /api/admin/parametres-metier/reload-cache
     * Recharge le cache des paramètres.
     * À utiliser après modification directe en base de données.
     * Accès: ADMIN only
     */
    @PostMapping("/reload-cache")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> reloadCache() {
        parametreMetierService.rechargerCache();
        return ResponseEntity.ok("Cache reloadé avec succès");
    }
}
