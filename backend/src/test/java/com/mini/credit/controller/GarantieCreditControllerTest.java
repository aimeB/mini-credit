package com.mini.credit.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GarantieCreditWorkflowController - RBAC ciblé 3N")
class GarantieCreditControllerTest {

    private String preAuthorizeValue(Class<?> controllerClass, String methodName, Class<?>... paramTypes) throws Exception {
        Method method = controllerClass.getDeclaredMethod(methodName, paramTypes);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertThat(preAuthorize).isNotNull();
        return preAuthorize.value();
    }

    @Test
    void controleur_shouldBeAllowedToVerifierBloquerValiderGarantie() throws Exception {
        assertThat(preAuthorizeValue(GarantieCreditWorkflowController.class, "verifier", Long.class, com.mini.credit.dto.garantie.VerifierGarantieCreditRequest.class))
                .contains("CONTROLEUR");
        assertThat(preAuthorizeValue(GarantieCreditWorkflowController.class, "bloquerEpargne", Long.class, com.mini.credit.dto.garantie.BloquerGarantieEpargneRequest.class))
                .contains("CONTROLEUR");
        assertThat(preAuthorizeValue(GarantieCreditWorkflowController.class, "valider", Long.class, com.mini.credit.dto.garantie.ValiderGarantieRequest.class))
                .contains("CONTROLEUR");
    }

    @Test
    void chefBureau_shouldReadButNotBloquer() throws Exception {
        assertThat(preAuthorizeValue(GarantieCreditWorkflowController.class, "getGarantie", Long.class))
                .contains("CHEF_BUREAU");
        assertThat(preAuthorizeValue(GarantieCreditWorkflowController.class, "bloquerEpargne", Long.class, com.mini.credit.dto.garantie.BloquerGarantieEpargneRequest.class))
                .doesNotContain("CHEF_BUREAU");
    }

    @Test
    void caissier_shouldReadButNotValider() throws Exception {
        assertThat(preAuthorizeValue(GarantieCreditWorkflowController.class, "getGarantie", Long.class))
                .contains("CAISSIER");
        assertThat(preAuthorizeValue(GarantieCreditWorkflowController.class, "valider", Long.class, com.mini.credit.dto.garantie.ValiderGarantieRequest.class))
                .doesNotContain("CAISSIER");
    }

    @Test
    void agentTerrain_shouldNotValidateGarantie() throws Exception {
        assertThat(preAuthorizeValue(GarantieCreditWorkflowController.class, "valider", Long.class, com.mini.credit.dto.garantie.ValiderGarantieRequest.class))
                .doesNotContain("AGENT_TERRAIN");
    }

    @Test
    void controleur_shouldNotApproveCredit() throws Exception {
        assertThat(preAuthorizeValue(CreditController.class, "approuver", Long.class, com.mini.credit.dto.credit.ApprobationCreditRequest.class))
                .contains("CREDIT_APPROVE")
                .doesNotContain("CONTROLEUR");
    }
}
