package com.mini.credit.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OperationEpargneController — Sécurité")
class OperationEpargneControllerSecurityTest {

    private PreAuthorize getPreAuthorize(String methodName, Class<?>... paramTypes) throws NoSuchMethodException {
        Method method = OperationEpargneController.class.getDeclaredMethod(methodName, paramTypes);
        return method.getAnnotation(PreAuthorize.class);
    }

    @Test
    void post_shouldExcludeAgentTerrain() throws Exception {
        String expr = getPreAuthorize("enregistrer", com.mini.credit.dto.epargne.OperationEpargneRequest.class).value();
        assertThat(expr).doesNotContain("AGENT_TERRAIN");
    }

    @Test
    void getByCompteAndMembre_shouldAllowScopedAgentRead() throws Exception {
        String compteExpr = getPreAuthorize("getByCompte", Long.class).value();
        String membreExpr = getPreAuthorize("getByMembre", Long.class).value();
        String listExpr = getPreAuthorize("getAll", int.class, int.class).value();

        assertThat(compteExpr).contains("AGENT_TERRAIN");
        assertThat(membreExpr).contains("AGENT_TERRAIN");
        assertThat(compteExpr).contains("@scopeService.canReadCompteEpargne");
        assertThat(membreExpr).contains("@scopeService.canReadMembreEpargne");
        assertThat(compteExpr).contains("CONTROLEUR_EPARGNE_OPERATION_READ");
        assertThat(membreExpr).contains("CONTROLEUR_EPARGNE_OPERATION_READ");
        assertThat(listExpr).contains("CONTROLEUR_EPARGNE_OPERATION_READ");
    }
}
