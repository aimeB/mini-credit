package com.mini.credit.dto.utilisateur;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour les credentials temporaires d'un utilisateur nouvellement créé
 * Contient le username et mot de passe temporaire
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UtilisateurCredentialsDTO {
    
    private String username;
    private String temporaryPassword;
    private String email;
    private String message;
    
    /**
     * Crée une instance avec un message par défaut
     */
    public static UtilisateurCredentialsDTO of(String username, String temporaryPassword, String email) {
        return UtilisateurCredentialsDTO.builder()
                .username(username)
                .temporaryPassword(temporaryPassword)
                .email(email)
                .message("✅ Compte créé! Copier le mot de passe - il disparaîtra après 60 secondes")
                .build();
    }
}
