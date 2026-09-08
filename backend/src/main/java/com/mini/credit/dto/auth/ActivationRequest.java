package com.mini.credit.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request d'activation de compte
 * Envoyé par le membre avec son code d'activation + nouveau password
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivationRequest {
    
    /**
     * Code d'activation reçu de l'agent
     */
    private String activationCode;
    
    /**
     * Nouveau password choisi par le membre
     * Doit respecter les règles de sécurité
     */
    private String newPassword;
    
    /**
     * Confirmation du password
     */
    private String confirmPassword;
}
