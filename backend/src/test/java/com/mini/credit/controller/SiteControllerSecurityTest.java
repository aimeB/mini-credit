package com.mini.credit.controller;

import com.mini.credit.dto.referentiel.CreateSiteRequest;
import com.mini.credit.dto.referentiel.UpdateSiteRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de sécurité pour SiteController.
 *
 * Vérifie par réflexion que chaque endpoint est protégé
 * par la bonne annotation @PreAuthorize.
 *
 * Règles métier :
 * - POST   /api/sites       → ADMIN uniquement
 * - PUT    /api/sites/{id}  → ADMIN uniquement
 * - GET    /api/sites       → ADMIN ou CHEF_BUREAU
 * - GET    /api/sites/{id}  → ADMIN ou CHEF_BUREAU
 */
@DisplayName("SiteController — Sécurité des endpoints")
class SiteControllerSecurityTest {

    // ──────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────

    private PreAuthorize getPreAuthorize(String methodName, Class<?>... paramTypes)
            throws NoSuchMethodException {
        Method method = SiteController.class.getDeclaredMethod(methodName, paramTypes);
        return method.getAnnotation(PreAuthorize.class);
    }

    // ──────────────────────────────────────────────────────────────────────
    // POST — création
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("admin_shouldCreateSite — POST /api/sites est sécurisé et autorise ADMIN")
    void admin_shouldCreateSite() throws NoSuchMethodException {
        PreAuthorize annotation = getPreAuthorize("create", CreateSiteRequest.class);
        assertThat(annotation)
                .as("L'endpoint POST /api/sites doit être annoté @PreAuthorize")
                .isNotNull();
        assertThat(annotation.value())
                .as("L'endpoint POST doit autoriser le rôle ADMIN")
                .contains("ADMIN");
    }

    @Test
    @DisplayName("nonAdmin_shouldNotCreateSite — POST /api/sites exclut les non-ADMIN")
    void nonAdmin_shouldNotCreateSite() throws NoSuchMethodException {
        PreAuthorize annotation = getPreAuthorize("create", CreateSiteRequest.class);
        assertThat(annotation)
                .as("L'endpoint POST /api/sites doit être protégé par @PreAuthorize")
                .isNotNull();
        String expression = annotation.value();
        assertThat(expression).doesNotContain("AGENT_TERRAIN");
        assertThat(expression).doesNotContain("GESTIONNAIRE");
        assertThat(expression).doesNotContain("CAISSIER");
        assertThat(expression).doesNotContain("MEMBER");
        assertThat(expression).contains("ADMIN");
    }

    // ──────────────────────────────────────────────────────────────────────
    // PUT — modification
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /api/sites/{id} — réservé à ADMIN")
    void update_shouldRequireAdminRole() throws NoSuchMethodException {
        PreAuthorize annotation = getPreAuthorize("update", Long.class, UpdateSiteRequest.class);
        assertThat(annotation)
                .as("L'endpoint PUT /api/sites/{id} doit être annoté @PreAuthorize")
                .isNotNull();
        assertThat(annotation.value()).contains("ADMIN");
    }

    @Test
    @DisplayName("PUT /api/sites/{id} — exclut les rôles non-ADMIN")
    void update_shouldExcludeNonAdmin() throws NoSuchMethodException {
        String expression = getPreAuthorize("update", Long.class, UpdateSiteRequest.class).value();
        assertThat(expression).doesNotContain("CHEF_BUREAU");
        assertThat(expression).doesNotContain("AGENT_TERRAIN");
        assertThat(expression).doesNotContain("CAISSIER");
    }

    // ──────────────────────────────────────────────────────────────────────
    // DELETE — désactivation (soft delete)
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/sites/{id} — réservé à ADMIN")
    void deactivate_shouldRequireAdminRole() throws NoSuchMethodException {
        PreAuthorize annotation = getPreAuthorize("deactivate", Long.class);
        assertThat(annotation)
                .as("L'endpoint DELETE /api/sites/{id} doit être annoté @PreAuthorize")
                .isNotNull();
        assertThat(annotation.value()).contains("ADMIN");
    }

    @Test
    @DisplayName("DELETE /api/sites/{id} — exclut les rôles non-ADMIN")
    void deactivate_shouldExcludeNonAdmin() throws NoSuchMethodException {
        String expression = getPreAuthorize("deactivate", Long.class).value();
        assertThat(expression).doesNotContain("CHEF_BUREAU");
        assertThat(expression).doesNotContain("AGENT_TERRAIN");
        assertThat(expression).doesNotContain("CAISSIER");
    }

    // ──────────────────────────────────────────────────────────────────────
    // GET — lecture
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/sites — accessible à ADMIN et CHEF_BUREAU")
    void getAll_shouldAllowAdminAndChefBureau() throws NoSuchMethodException {
        PreAuthorize annotation = getPreAuthorize("getAll");
        assertThat(annotation)
                .as("L'endpoint GET /api/sites doit être annoté @PreAuthorize")
                .isNotNull();
        assertThat(annotation.value())
                .contains("ADMIN")
            .contains("CHEF_BUREAU");
    }

    @Test
    @DisplayName("GET /api/sites/{id} — accessible à ADMIN et CHEF_BUREAU")
    void getById_shouldAllowAdminAndChefBureau() throws NoSuchMethodException {
        PreAuthorize annotation = getPreAuthorize("getById", Long.class);
        assertThat(annotation)
                .as("L'endpoint GET /api/sites/{id} doit être annoté @PreAuthorize")
                .isNotNull();
        assertThat(annotation.value())
                .contains("ADMIN")
            .contains("CHEF_BUREAU");
    }
}
