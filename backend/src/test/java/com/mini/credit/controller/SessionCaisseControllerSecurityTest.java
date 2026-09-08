package com.mini.credit.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SessionCaisseController - Sécurité")
class SessionCaisseControllerSecurityTest {

    private String preAuthorizeValue(Class<?> controllerClass, String methodName, Class<?>... paramTypes)
            throws NoSuchMethodException {
        Method method = controllerClass.getDeclaredMethod(methodName, paramTypes);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertThat(preAuthorize).isNotNull();
        return preAuthorize.value();
    }

    @Test
    void getActive_isProtectedByPreAuthorize() throws Exception {
        Method method = SessionCaisseController.class.getDeclaredMethod("getSessionActive");
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'CAISSIER', 'CONTROLEUR', 'RCI')");
    }

    @Test
    void ouverture_requiresDedicatedPermission() throws Exception {
        String expr = preAuthorizeValue(SessionCaisseController.class,
                "ouvrir", com.mini.credit.dto.caisse.SessionCaisseOpenRequest.class);

        assertThat(expr).isEqualTo("hasAnyRole('ADMIN', 'CAISSIER', 'CHEF_BUREAU')");
    }

    @Test
    void preCloture_requiresDedicatedPermission() throws Exception {
        String expr = preAuthorizeValue(SessionCaisseController.class,
                "cloturer", Long.class, com.mini.credit.dto.caisse.SessionCaisseCloseRequest.class);

        assertThat(expr).isEqualTo("hasAnyRole('ADMIN', 'CAISSIER', 'CHEF_BUREAU')");
    }

    @Test
    void validationControle_notGrantedByCaissierRoleExpression() throws Exception {
        String expr = preAuthorizeValue(CaisseSessionWorkflowController.class,
                "validerControle", Long.class, com.mini.credit.dto.caisse.SessionCaisseValidationRequest.class);

        assertThat(expr).contains("SESSION_CAISSE_CONTROL_VALIDATE");
        assertThat(expr).doesNotContain("CAISSIER");
    }

    @Test
    void validationControle_isAlignedForControleurFlow() throws Exception {
        String expr = preAuthorizeValue(CaisseSessionWorkflowController.class,
                "cloturerFinale", Long.class, com.mini.credit.dto.caisse.SessionCaisseValidationRequest.class);

        assertThat(expr).isEqualTo("hasAuthority('SESSION_CAISSE_FINAL_CLOSE')");
    }

    @Test
    void caisseInitialization_requiresCaisseCreatePermission() throws Exception {
        String expr = preAuthorizeValue(CaisseController.class,
                "create", com.mini.credit.dto.caisse.CaisseCreateRequest.class);

        assertThat(expr).isEqualTo("hasRole('ADMIN')");
    }

    @Test
    void initialiserMaCaisse_isDedicatedToCaissierOnly() throws Exception {
        String expr = preAuthorizeValue(CaisseController.class, "initialiserMaCaisse");

        assertThat(expr).isEqualTo("hasRole('CAISSIER')");
    }
}
