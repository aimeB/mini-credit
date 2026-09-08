package com.mini.credit.controller;

import com.mini.credit.dto.auth.ActivationRequest;
import com.mini.credit.dto.auth.AuthResponse;
import com.mini.credit.service.ActivationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur pour gérer l'activation des comptes
 * Endpoints publics (sans authentification)
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class ActivationController {

    private final ActivationService activationService;

    /**
     * Endpoint public: Activer un compte avec code + nouveau password
     * POSTé par le membre après réception du code
     * Retourne AuthResponse avec JWT token pour auto-login
     */
    @PostMapping("/activate")
    public ResponseEntity<AuthResponse> activateAccount(
            @RequestBody ActivationRequest request,
            HttpServletRequest httpRequest) {
        
        // Récupérer l'IP pour l'audit
        String ipAddress = getClientIpAddress(httpRequest);
        
        AuthResponse response = activationService.activateAccount(request, ipAddress);
        
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Extraire l'adresse IP du client
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
