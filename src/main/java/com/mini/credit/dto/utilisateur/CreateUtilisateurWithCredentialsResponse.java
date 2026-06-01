package com.mini.credit.dto.utilisateur;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wrapper contenant l'utilisateur créé et son mot de passe temporaire
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUtilisateurWithCredentialsResponse {
    
    private String username;
    private String email;
    private String temporaryPassword;
    private String message;
}
