package com.mini.credit.controller;

import com.mini.credit.dto.auth.AuthResponse;
import com.mini.credit.dto.auth.LoginRequest;
import com.mini.credit.dto.auth.RegisterRequest;
import com.mini.credit.dto.utilisateur.ChangePasswordRequest;
import com.mini.credit.annotation.Auditable;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.service.UtilisateurService;
import com.mini.credit.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication endpoints")
@Slf4j
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UtilisateurService utilisateurService;

    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Authenticate user and return JWT token")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        log.info("User login attempt: {}", loginRequest.getUsername());
        AuthResponse response = authService.login(loginRequest, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Register a new user account")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        log.info("User registration: {}", registerRequest.getUsername());
        AuthResponse response = authService.register(registerRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Get current authenticated user information")
    public ResponseEntity<AuthResponse> getCurrentUser() {
        return ResponseEntity.ok(authService.getCurrentUserResponse());
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change own password", description = "Authenticated user changes own password")
    @Auditable(action = AuditAction.USER_PASSWORD_CHANGED, entityType = "Utilisateur", captureParameters = true)
    public ResponseEntity<?> changeOwnPassword(@Valid @RequestBody ChangePasswordRequest request) {
        utilisateurService.changeOwnPassword(request);
        return ResponseEntity.ok().body(java.util.Map.of("message", "Mot de passe changé avec succès"));
    }
}
