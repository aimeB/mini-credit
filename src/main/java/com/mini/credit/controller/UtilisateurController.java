package com.mini.credit.controller;

import com.mini.credit.dto.utilisateur.UtilisateurDTO;
import com.mini.credit.dto.utilisateur.CreateUtilisateurRequest;
import com.mini.credit.dto.utilisateur.UpdateUtilisateurRequest;
import com.mini.credit.dto.utilisateur.ChangePasswordRequest;
import com.mini.credit.dto.utilisateur.ChangeUsernameRequest;
import com.mini.credit.dto.utilisateur.ResetPasswordRequest;
import com.mini.credit.service.UtilisateurService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/utilisateurs")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UtilisateurDTO>> getAll() {
        return ResponseEntity.ok(utilisateurService.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UtilisateurDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(utilisateurService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UtilisateurDTO> create(@RequestBody CreateUtilisateurRequest request) {
        UtilisateurDTO created = utilisateurService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UtilisateurDTO> update(
            @PathVariable Long id,
            @RequestBody UpdateUtilisateurRequest request) {
        UtilisateurDTO updated = utilisateurService.update(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        utilisateurService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/change-password")
    @PreAuthorize("@scopeService.canAccessCurrentUser(#id)")
    public ResponseEntity<String> changePassword(
            @PathVariable Long id,
            @RequestBody ChangePasswordRequest request) {
        utilisateurService.changePassword(id, request);
        return ResponseEntity.ok("Mot de passe changé avec succès");
    }

    /**
     * Réinitialiser le mot de passe sans l'ancien
     * Utilisé par l'utilisateur qui a oublié son mot de passe
     * Seulement accessible pour son propre compte
     */
    @PostMapping("/{id}/reset-password-self")
    @PreAuthorize("@scopeService.canAccessCurrentUser(#id)")
    public ResponseEntity<Map<String, String>> resetPasswordSelf(
            @PathVariable Long id,
            @RequestBody ResetPasswordRequest request) {
        utilisateurService.resetPasswordSelf(id, request);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Mot de passe réinitialisé avec succès");
        response.put("success", "true");
        return ResponseEntity.ok(response);
    }

    /**
     * Permet à un membre de changer son username
     * Endpoint protégé - seul l'utilisateur lui-même peut changer son username
     */
    @PostMapping("/{id}/change-username")
    @PreAuthorize("@scopeService.canAccessCurrentUser(#id)")
    public ResponseEntity<UtilisateurDTO> changeUsername(
            @PathVariable Long id,
            @RequestBody ChangeUsernameRequest request) {
        UtilisateurDTO updated = utilisateurService.changeUsername(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Vérifie si un username est disponible
     * Endpoint public - utilisé lors de la saisie dans le formulaire
     */
    @GetMapping("/check-username/{username}")
    public ResponseEntity<Map<String, Object>> checkUsernameAvailability(@PathVariable String username) {
        boolean exists = utilisateurService.usernameExists(username);
        Map<String, Object> response = new HashMap<>();
        response.put("available", !exists);
        response.put("username", username.toLowerCase());
        return ResponseEntity.ok(response);
    }
}
