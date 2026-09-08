package com.mini.credit.controller;

import com.mini.credit.dto.CreateAgenceRequest;
import com.mini.credit.dto.UpdateAgenceRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de sécurité pour AgenceController.
 *
 * Vérifie par réflexion que chaque endpoint est protégé
 * par la bonne annotation @PreAuthorize.
 *
 * Règles métier :
 * - POST   /api/agences       → ADMIN uniquement
 * - PUT    /api/agences/{id}  → ADMIN uniquement
 * - DELETE /api/agences/{id}  → ADMIN uniquement
 * - GET    /api/agences       → ADMIN ou CHEF_BUREAU
 * - GET    /api/agences/actives → ADMIN ou CHEF_BUREAU
 * - GET    /api/agences/{id}  → ADMIN ou CHEF_BUREAU
 */
@DisplayName("AgenceController — Sécurité des endpoints")
class AgenceControllerSecurityTest {

    // ──────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────

    private PreAuthorize getPreAuthorize(String methodName, Class<?>... paramTypes)
            throws NoSuchMethodException {
        Method method = AgenceController.class.getDeclaredMethod(methodName, paramTypes);
        return method.getAnnotation(PreAuthorize.class);
    }

    // ──────────────────────────────────────────────────────────────────────
    // POST — création
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("admin_shouldCreateAgence — POST /api/agences est sécurisé et autorise ADMIN")
    void admin_shouldCreateAgence() throws NoSuchMethodException {
        PreAuthorize annotation = getPreAuthorize("create", CreateAgenceRequest.class);
        assertThat(annotation)
                .as("L'endpoint POST /api/agences doit être annoté @PreAuthorize")
                .isNotNull();
        assertThat(annotation.value())
                .as("L'endpoint POST doit autoriser le rôle ADMIN")
                .contains("ADMIN");
    }

    @Test
    @DisplayName("nonAdmin_shouldNotCreateAgence — POST /api/agences exclut les non-ADMIN")
    void nonAdmin_shouldNotCreateAgence() throws NoSuchMethodException {
        PreAuthorize annotation = getPreAuthorize("create", CreateAgenceRequest.class);
        assertThat(annotation)
                .as("L'endpoint POST /api/agences doit être protégé par @PreAuthorize")
                .isNotNull();
        // La valeur ne doit pas permettre les rôles opérationnels
        String expression = annotation.value();
        assertThat(expression).doesNotContain("AGENT_TERRAIN");
        assertThat(expression).doesNotContain("GESTIONNAIRE");
        assertThat(expression).doesNotContain("CAISSIER");
        assertThat(expression).doesNotContain("MEMBER");
        // Doit être hasRole('ADMIN') ou hasAnyRole(...'ADMIN'...)
        assertThat(expression).contains("ADMIN");
    }

    // ──────────────────────────────────────────────────────────────────────
    // PUT — modification
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /api/agences/{id} — réservé à ADMIN")
    void update_shouldRequireAdminRole() throws NoSuchMethodException {
        PreAuthorize annotation = getPreAuthorize("update", Long.class, UpdateAgenceRequest.class);
        assertThat(annotation)
                .as("L'endpoint PUT /api/agences/{id} doit être annoté @PreAuthorize")
                .isNotNull();
        assertThat(annotation.value()).contains("ADMIN");
    }

    @Test
    @DisplayName("PUT /api/agences/{id} — exclut les rôles non-ADMIN")
    void update_shouldExcludeNonAdmin() throws NoSuchMethodException {
        String expression = getPreAuthorize("update", Long.class, UpdateAgenceRequest.class).value();
        assertThat(expression).doesNotContain("CHEF_BUREAU");
        assertThat(expression).doesNotContain("AGENT_TERRAIN");
        assertThat(expression).doesNotContain("CAISSIER");
    }

    // ──────────────────────────────────────────────────────────────────────
    // DELETE — désactivation (soft delete)
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/agences/{id} — réservé à ADMIN")
    void deactivate_shouldRequireAdminRole() throws NoSuchMethodException {
        PreAuthorize annotation = getPreAuthorize("deactivate", Long.class);
        assertThat(annotation)
                .as("L'endpoint DELETE /api/agences/{id} doit être annoté @PreAuthorize")
                .isNotNull();
        assertThat(annotation.value()).contains("ADMIN");
    }

    // ──────────────────────────────────────────────────────────────────────
    // GET — lecture
    // ──────────────────────────────────────────────────────────────────────

    @Test
        @DisplayName("GET /api/agences — accessible à ADMIN et CHEF_BUREAU")
    void getAll_shouldAllowAdminAndChefBureau() throws NoSuchMethodException {
        PreAuthorize annotation = getPreAuthorize("getAll");
        assertThat(annotation)
                .as("L'endpoint GET /api/agences doit être annoté @PreAuthorize")
                .isNotNull();
        assertThat(annotation.value())
                .contains("ADMIN")
                .contains("CHEF_BUREAU");
    }

    @Test
        @DisplayName("GET /api/agences/actives — accessible à ADMIN et CHEF_BUREAU")
    void getActives_shouldAllowAdminAndChefBureau() throws NoSuchMethodException {
        PreAuthorize annotation = getPreAuthorize("getActives");
        assertThat(annotation)
                .as("L'endpoint GET /api/agences/actives doit être annoté @PreAuthorize")
                .isNotNull();
        assertThat(annotation.value())
                .contains("ADMIN")
                .contains("CHEF_BUREAU");
    }

    @Test
        @DisplayName("GET /api/agences/{id} — accessible à ADMIN et CHEF_BUREAU")
    void getById_shouldAllowAdminAndChefBureau() throws NoSuchMethodException {
        PreAuthorize annotation = getPreAuthorize("getById", Long.class);
        assertThat(annotation)
                .as("L'endpoint GET /api/agences/{id} doit être annoté @PreAuthorize")
                .isNotNull();
        assertThat(annotation.value())
                .contains("ADMIN")
                .contains("CHEF_BUREAU");
    }
}
