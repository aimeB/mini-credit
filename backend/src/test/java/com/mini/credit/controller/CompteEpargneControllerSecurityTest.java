package com.mini.credit.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CompteEpargneController — Sécurité")
class CompteEpargneControllerSecurityTest {

    private PreAuthorize getPreAuthorize(String methodName, Class<?>... paramTypes) throws NoSuchMethodException {
        Method method = CompteEpargneController.class.getDeclaredMethod(methodName, paramTypes);
        return method.getAnnotation(PreAuthorize.class);
    }

    @Test
    void scopedEndpoints_shouldBeRoleSpecific() throws Exception {
        String mesMembresExpr = getPreAuthorize("getMesMembresComptes").value();
        String meExpr = getPreAuthorize("getMesComptes").value();

        assertThat(mesMembresExpr).contains("AGENT_TERRAIN");
        assertThat(meExpr).contains("MEMBER");
    }

    @Test
    void lectureEndpoints_shouldAllowControleurReadOnly() throws Exception {
        String listExpr = getPreAuthorize("getAll").value();
        String byIdExpr = getPreAuthorize("getById", Long.class).value();
        String byMembreExpr = getPreAuthorize("getByMembre", Long.class).value();

        assertThat(listExpr).contains("CONTROLEUR_EPARGNE_READ");
        assertThat(byIdExpr).contains("scopeService.canReadCompteEpargne");
        assertThat(byMembreExpr).contains("scopeService.canReadMembreEpargne");
    }

    @Test
    void writeEndpoints_shouldStayAdminOnly() throws Exception {
        String createExpr = getPreAuthorize("create", com.mini.credit.dto.epargne.CompteEpargneCreateRequest.class).value();
        String repairExpr = getPreAuthorize("repairForMember", Long.class).value();

        assertThat(createExpr).contains("ADMIN").doesNotContain("CONTROLEUR");
        assertThat(repairExpr).contains("ADMIN").doesNotContain("CONTROLEUR");
    }
}
