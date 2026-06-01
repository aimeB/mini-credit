package com.mini.credit.dto.auth;

import com.mini.credit.entity.referentiel.Utilisateur;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String type = "Bearer";
    private Long id;
    private String username;
    private String email;
    private String nomComplet;
    private String role;

    public static AuthResponse from(String token, Utilisateur utilisateur) {
        return AuthResponse.builder()
                .token(token)
                .id(utilisateur.getId())
                .username(utilisateur.getUsername())
                .email(utilisateur.getEmail())
                .nomComplet(utilisateur.getNomComplet())
                .role(utilisateur.getRole() != null ? utilisateur.getRole().getCode().name() : "MEMBER")
                .build();
    }
}
