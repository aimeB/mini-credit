package com.mini.credit.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DemandeRetraitEpargneController — Sécurité")
class DemandeRetraitEpargneControllerSecurityTest {

    private PreAuthorize getPreAuthorize(String methodName, Class<?>... paramTypes) throws NoSuchMethodException {
        Method method = DemandeRetraitEpargneController.class.getDeclaredMethod(methodName, paramTypes);
        return method.getAnnotation(PreAuthorize.class);
    }

    @Test
    void create_shouldExcludeAgentTerrain() throws Exception {
        String expr = getPreAuthorize("creerDemande", Long.class, java.math.BigDecimal.class, String.class).value();
        assertThat(expr).contains("MEMBER");
        assertThat(expr).doesNotContain("AGENT_TERRAIN");
    }

    @Test
    void decaisser_shouldUseOperationCaisseCreatePermission() throws Exception {
        String expr = getPreAuthorize("decaisserRetrait", Long.class).value();

        assertThat(expr).isEqualTo("hasAuthority('OPERATION_CAISSE_CREATE')");
        assertThat(expr).doesNotContain("CAISSIER");
    }
}
