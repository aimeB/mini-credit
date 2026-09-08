package com.mini.credit.dto.auth;

import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.PosteEmploye;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    @Builder.Default
    private String type = "Bearer";
    private Long id;
    private String username;
    private String email;
    private String nomComplet;
    private String role;
    private String posteEmploye;
    private Long siteId;
    private String siteNom;
    private List<String> permissions;
    private Boolean passwordChangeRequired;

    private static Long resolveSiteId(Utilisateur utilisateur) {
        if (utilisateur.getSite() != null && utilisateur.getSite().getId() != null) {
            return utilisateur.getSite().getId();
        }
        if (utilisateur.getEmploye() != null
                && utilisateur.getEmploye().getSite() != null
                && utilisateur.getEmploye().getSite().getId() != null) {
            return utilisateur.getEmploye().getSite().getId();
        }
        return null;
    }

    private static String resolveSiteNom(Utilisateur utilisateur) {
        if (utilisateur.getSite() != null && utilisateur.getSite().getNomSite() != null) {
            return utilisateur.getSite().getNomSite();
        }
        if (utilisateur.getEmploye() != null
                && utilisateur.getEmploye().getSite() != null
                && utilisateur.getEmploye().getSite().getNomSite() != null) {
            return utilisateur.getEmploye().getSite().getNomSite();
        }
        return null;
    }

    private static String resolvePosteEmploye(Utilisateur utilisateur) {
        if (utilisateur.getEmploye() == null || utilisateur.getEmploye().getFonction() == null) {
            return null;
        }
        PosteEmploye fonction = utilisateur.getEmploye().getFonction();
        return fonction.name();
    }

    public static AuthResponse from(String token, Utilisateur utilisateur) {
        return AuthResponse.builder()
                .token(token)
                .id(utilisateur.getId())
                .username(utilisateur.getUsername())
                .email(utilisateur.getEmail())
                .nomComplet(utilisateur.getNomComplet())
                .role(utilisateur.getRole() != null ? utilisateur.getRole().getCode().name() : "MEMBER")
                .posteEmploye(resolvePosteEmploye(utilisateur))
                .siteId(resolveSiteId(utilisateur))
                .siteNom(resolveSiteNom(utilisateur))
                .permissions(utilisateur.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(authority -> !authority.startsWith("ROLE_"))
                    .toList())
                .passwordChangeRequired(Boolean.TRUE.equals(utilisateur.getPasswordChangeRequired()))
                .build();
    }

    public static AuthResponse forCurrentUser(Utilisateur utilisateur) {
        AuthResponse response = from("", utilisateur);
        response.setToken(null);
        response.setType("Bearer");
        return response;
    }
}
