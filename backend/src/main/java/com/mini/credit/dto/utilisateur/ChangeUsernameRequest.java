package com.mini.credit.dto.utilisateur;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour changer le username d'un utilisateur
 * Utilisé par les membres pour personnaliser leur username
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeUsernameRequest {
    
    /**
     * Nouveau username désiré
     * Doit être unique dans le système
     */
    @NotBlank(message = "Le username ne peut pas être vide")
    @Size(min = 3, max = 50, message = "Le username doit avoir entre 3 et 50 caractères")
    private String newUsername;
}
