package com.mini.credit.dto.membre;

import com.mini.credit.dto.auth.ActivationCodeDTO;
import com.mini.credit.dto.utilisateur.UtilisateurCredentialsDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response lors de la création d'un membre
 * Contient le code d'activation unique et les credentials de l'utilisateur
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembreActivationResponseDTO {
    
    /**
     * Le membre créé
     */
    private MembreResponse membre;
    
    /**
     * Code d'activation pour le membre
     */
    private ActivationCodeDTO activationCode;
    
    /**
     * Credentials de l'utilisateur créé (username, email, etc.)
     */
    private UtilisateurCredentialsDTO credentials;
    
    /**
     * Message de succès
     */
    private String message;
}
