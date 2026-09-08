package com.mini.credit.dto.utilisateur;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la réinitialisation de mot de passe (sans besoin de l'ancien)
 * Utilisé quand l'utilisateur a oublié son mot de passe
 * L'authentification JWT suffit pour vérifier que c'est lui
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {
    
    @NotBlank(message = "Le nouveau mot de passe est requis")
    @Size(min = 8, message = "Le mot de passe doit avoir au moins 8 caractères")
    private String newPassword;
    
    @NotBlank(message = "La confirmation du mot de passe est requise")
    private String confirmPassword;
}
