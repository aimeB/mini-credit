package com.mini.credit.controller;

import com.mini.credit.dto.utilisateur.UtilisateurDTO;
import com.mini.credit.dto.utilisateur.CreateUtilisateurRequest;
import com.mini.credit.dto.utilisateur.UpdateUtilisateurRequest;
import com.mini.credit.dto.utilisateur.ChangePasswordRequest;
import com.mini.credit.dto.utilisateur.ChangeUsernameRequest;
import com.mini.credit.dto.utilisateur.ResetPasswordRequest;
import com.mini.credit.dto.utilisateur.AdminPasswordResetRequest;
import com.mini.credit.dto.utilisateur.ResetPasswordResponse;
import com.mini.credit.annotation.Auditable;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.service.UtilisateurService;
import jakarta.validation.Valid;
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
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU')")
    public ResponseEntity<List<UtilisateurDTO>> getAll() {
        return ResponseEntity.ok(utilisateurService.getAll());
    }

    @GetMapping("/par-role/{role}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'CAISSIER', 'CONTROLEUR')")
    public ResponseEntity<List<UtilisateurDTO>> getByRole(@PathVariable String role) {
        RoleCode roleCode = RoleCode.valueOf(role.toUpperCase());
        return ResponseEntity.ok(utilisateurService.getByRole(roleCode));
    }

    /**
     * Utilisateurs disponibles pour crÃ©er un profil Agent Terrain.
     * Filtre : actif + rÃ´le AGENT_TERRAIN + employÃ© fonction AGENT_TERRAIN + pas encore agent.
     */
    @GetMapping("/disponibles-agent-terrain")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_BUREAU')")
    public ResponseEntity<List<UtilisateurDTO>> getDisponiblesAgentTerrain() {
        return ResponseEntity.ok(utilisateurService.getDisponiblesAgentTerrain());
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
        return ResponseEntity.ok("Mot de passe changÃ© avec succÃ¨s");
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('USER_PASSWORD_RESET')")
    @Auditable(action = AuditAction.USER_PASSWORD_RESET, entityType = "Utilisateur", captureParameters = true, captureResult = true)
    public ResponseEntity<ResetPasswordResponse> resetPasswordByAdmin(
            @PathVariable Long id,
            @Valid @RequestBody AdminPasswordResetRequest request) {
        return ResponseEntity.ok(utilisateurService.resetPasswordByAdmin(id, request));
    }

    /**
     * RÃ©initialiser le mot de passe sans l'ancien
     * UtilisÃ© par l'utilisateur qui a oubliÃ© son mot de passe
     * Seulement accessible pour son propre compte
     */
    @PostMapping("/{id}/reset-password-self")
    @PreAuthorize("@scopeService.canAccessCurrentUser(#id)")
    public ResponseEntity<Map<String, String>> resetPasswordSelf(
            @PathVariable Long id,
            @RequestBody ResetPasswordRequest request) {
        utilisateurService.resetPasswordSelf(id, request);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Mot de passe rÃ©initialisÃ© avec succÃ¨s");
        response.put("success", "true");
        return ResponseEntity.ok(response);
    }

    /**
     * Permet Ã  un membre de changer son username
     * Endpoint protÃ©gÃ© - seul l'utilisateur lui-mÃªme peut changer son username
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
     * VÃ©rifie si un username est disponible
     * Endpoint public - utilisÃ© lors de la saisie dans le formulaire
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

