package com.mini.credit.dto.auth;

import com.mini.credit.entity.referentiel.Role;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.security.RoleCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuthResponse - rôles officiels")
class AuthResponseRoleTest {

    @Test
    void chefBureau_shouldBeExposedInAuthResponse() {
        Utilisateur utilisateur = Utilisateur.builder()
                .username("chef.bureau")
                .email("chef@test.local")
                .nomComplet("Chef Bureau")
                .role(Role.builder().code(RoleCode.CHEF_BUREAU).build())
                .build();
        utilisateur.setId(12L);

        AuthResponse response = AuthResponse.from("jwt-token", utilisateur);

        assertThat(response.getRole()).isEqualTo("CHEF_BUREAU");
    }

    @Test
    void gestionnaire_shouldBeExposedInAuthResponse() {
        Utilisateur utilisateur = Utilisateur.builder()
                .username("gestionnaire.gest")
                .email("gestionnaire@test.local")
                .nomComplet("Gestionnaire Gest")
                .role(Role.builder().code(RoleCode.GESTIONNAIRE).build())
                .build();
        utilisateur.setId(13L);

        AuthResponse response = AuthResponse.from("jwt-token", utilisateur);

        assertThat(response.getRole()).isEqualTo("GESTIONNAIRE");
    }
}