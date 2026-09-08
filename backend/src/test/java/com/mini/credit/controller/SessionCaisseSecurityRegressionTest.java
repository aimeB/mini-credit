package com.mini.credit.controller;

import com.mini.credit.controller.audit.AuditController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SessionCaisse - Régression sécurité")
class SessionCaisseSecurityRegressionTest {

    private String preAuthorizeValue(Class<?> controllerClass, String methodName, Class<?>... paramTypes)
            throws NoSuchMethodException {
        Method method = controllerClass.getDeclaredMethod(methodName, paramTypes);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertThat(preAuthorize).isNotNull();
        return preAuthorize.value();
    }

    @Test
    void activeEndpoint_shouldRemainProtected() throws Exception {
        Method method = SessionCaisseController.class.getDeclaredMethod("getSessionActive");
        assertThat(method.getAnnotation(PreAuthorize.class)).isNull();
    }

    @Test
    void validerControle_shouldRemainPermissionBased() throws Exception {
        String expr = preAuthorizeValue(
            CaisseSessionWorkflowController.class,
                "validerControle",
                Long.class,
                com.mini.credit.dto.caisse.SessionCaisseValidationRequest.class
        );

        assertThat(expr).contains("SESSION_CAISSE_CONTROL_VALIDATE");
        assertThat(expr).doesNotContain("CAISSIER");
    }

    @Test
    void auditEntityEndpoint_shouldAllowOperationalRoles() throws Exception {
        String expr = preAuthorizeValue(
                AuditController.class,
                "getEntityAuditTrail",
                String.class,
                Long.class,
                int.class,
                int.class
        );

        assertThat(expr).contains("AUDIT_READ");
        assertThat(expr).contains("CONTROLEUR_AUDIT_READ");
        assertThat(expr).contains("OPERATION_CAISSE_READ");
    }
}
