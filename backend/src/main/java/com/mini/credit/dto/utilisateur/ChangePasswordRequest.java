package com.mini.credit.dto.utilisateur;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la demande de changement de mot de passe
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {

    @NotBlank(message = "Le mot de passe actuel est requis")
    private String currentPassword;

    /**
     * Alias API demandé: oldPassword.
     * Si fourni, sera utilisé à la place de currentPassword.
     */
    private String oldPassword;
    
    @NotBlank(message = "Le nouveau mot de passe est requis")
    private String newPassword;
    
    @NotBlank(message = "La confirmation du mot de passe est requise")
    private String confirmPassword;

    public String resolveCurrentPassword() {
        if (oldPassword != null && !oldPassword.isBlank()) {
            return oldPassword;
        }
        return currentPassword;
    }
}
