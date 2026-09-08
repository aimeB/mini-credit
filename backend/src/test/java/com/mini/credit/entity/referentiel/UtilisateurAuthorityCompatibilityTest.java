package com.mini.credit.entity.referentiel;

import com.mini.credit.enums.security.PermissionCode;
import com.mini.credit.enums.security.RoleCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Utilisateur - autorités rôles officiels")
class UtilisateurAuthorityRoleTest {

    @Test
        void chefBureau_shouldExposeChefBureauRoleAuthority() {
        Permission auditRead = Permission.builder().code(PermissionCode.AUDIT_READ).build();
                Role role = Role.builder().code(RoleCode.CHEF_BUREAU).build();
        role.setPermissions(Set.of(RolePermission.builder().role(role).permission(auditRead).build()));

        Utilisateur utilisateur = Utilisateur.builder()
                                .username("chef.bureau")
                .motDePasseHash("hash")
                                .nomComplet("Chef Bureau")
                .role(role)
                .build();

        Set<String> authorities = utilisateur.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(java.util.stream.Collectors.toSet());

        assertThat(authorities)
                        .contains("ROLE_CHEF_BUREAU", "AUDIT_READ");
    }

    @Test
        void gestionnaire_shouldExposeGestionnaireRoleAuthority() {
                Role role = Role.builder().code(RoleCode.GESTIONNAIRE).build();

        Utilisateur utilisateur = Utilisateur.builder()
                .username("gestionnaire.gest")
                .motDePasseHash("hash")
                .nomComplet("Gestionnaire Gest")
                .role(role)
                .build();

        Set<String> authorities = utilisateur.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(java.util.stream.Collectors.toSet());

        assertThat(authorities)
                        .contains("ROLE_GESTIONNAIRE");
    }
}