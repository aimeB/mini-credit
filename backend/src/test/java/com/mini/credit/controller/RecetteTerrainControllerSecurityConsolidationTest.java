package com.mini.credit.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RecetteTerrainController — Consolidation legacy")
class RecetteTerrainControllerSecurityConsolidationTest {

    private PreAuthorize getPreAuthorize(String methodName, Class<?>... paramTypes) throws NoSuchMethodException {
        Method method = RecetteTerrainController.class.getDeclaredMethod(methodName, paramTypes);
        return method.getAnnotation(PreAuthorize.class);
    }

    @Test
    void legacyWriteEndpoints_shouldBeAdminOnly() throws Exception {
        assertThat(getPreAuthorize("create", com.mini.credit.dto.referentiel.CreateRecetteTerrainRequest.class).value())
            .isEqualTo("hasRole('ADMIN')");
        assertThat(getPreAuthorize("update", Long.class, com.mini.credit.dto.referentiel.UpdateRecetteTerrainRequest.class).value())
            .isEqualTo("hasRole('ADMIN')");
        assertThat(getPreAuthorize("delete", Long.class).value())
            .isEqualTo("hasRole('ADMIN')");
        assertThat(getPreAuthorize("soumettre", Long.class).value())
            .isEqualTo("hasRole('ADMIN')");
    }

    @Test
    void legacyReadEndpoints_shouldRemainBackofficeReadable() throws Exception {
        String listExpr = getPreAuthorize("getRecettePaginated", org.springframework.data.domain.Pageable.class, String.class, Long.class, Long.class, java.time.LocalDate.class, java.time.LocalDate.class).value();
        assertThat(listExpr).contains("ADMIN").contains("CHEF_BUREAU").contains("CONTROLEUR").contains("AGENT_TERRAIN");
    }
}
