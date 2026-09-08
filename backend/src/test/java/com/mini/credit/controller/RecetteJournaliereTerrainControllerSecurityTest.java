package com.mini.credit.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RecetteJournaliereTerrainController — Sécurité")
class RecetteJournaliereTerrainControllerSecurityTest {

    private PreAuthorize getPreAuthorize(String methodName, Class<?>... paramTypes) throws NoSuchMethodException {
        Method method = RecetteJournaliereTerrainController.class.getDeclaredMethod(methodName, paramTypes);
        return method.getAnnotation(PreAuthorize.class);
    }

    @Test
    void create_shouldExcludeAgentTerrain() throws Exception {
        String expr = getPreAuthorize(
            "creerRecette",
            Long.class,
            Long.class,
            java.time.LocalDate.class,
            String.class,
            java.math.BigDecimal.class,
            String.class,
            String.class
        ).value();

        assertThat(expr).contains("ADMIN").contains("CONTROLEUR").contains("GESTIONNAIRE");
        assertThat(expr).doesNotContain("AGENT_TERRAIN");
    }
}
