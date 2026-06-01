package com.mini.credit.dto.auth;

import com.mini.credit.entity.referentiel.Utilisateur;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;

    @NotBlank(message = "Full name is required")
    private String nomComplet;

    private String role = "MEMBER"; // Par défaut

    public Utilisateur toEntity() {
        return Utilisateur.builder()
                .username(this.username)
                .email(this.email)
                .nomComplet(this.nomComplet)
                .actif(true)
                .isEnabled(true)
                .isLocked(false)
                .build();
    }
}
