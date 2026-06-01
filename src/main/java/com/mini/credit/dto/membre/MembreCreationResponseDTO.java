package com.mini.credit.dto.membre;

import com.mini.credit.dto.utilisateur.UtilisateurCredentialsDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Réponse lors de la création d'un membre avec credentials temporaires
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembreCreationResponseDTO {
    
    private MembreResponse membre;
    private UtilisateurCredentialsDTO credentials;
    private String message;
}
