package com.mini.credit.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response après activation réussie
 * Contient le token JWT pour l'auto-login
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivationResponse {
    
    /**
     * Message de succès
     */
    private String message;
    
    /**
     * JWT Token pour auto-connexion
     */
    private String token;
    
    /**
     * Type de token (Bearer)
     */
    @Builder.Default
    private String tokenType = "Bearer";
    
    /**
     * Nom d'utilisateur créé
     */
    private String username;
    
    /**
     * Durée du token en secondes
     */
    private Long expiresIn;
    
    public static ActivationResponse of(String message, String token, String username, Long expiresIn) {
        return builder()
            .message(message)
            .token(token)
            .username(username)
            .expiresIn(expiresIn)
            .tokenType("Bearer")
            .build();
    }
}
