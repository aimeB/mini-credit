package com.mini.credit.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DemandeCreditController - sécurité workflow 3N")
class DemandeCreditControllerTest {

    private String preAuthorizeValue(String methodName, Class<?>... paramTypes) throws NoSuchMethodException {
        Method method = DemandeCreditController.class.getDeclaredMethod(methodName, paramTypes);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertThat(preAuthorize).isNotNull();
        return preAuthorize.value();
    }

    @Test
    void preAnalyse_shouldBeLimitedToAdminAndAgentBureau() throws Exception {
        String expr = preAuthorizeValue("preAnalyser", Long.class, String.class);
        assertThat(expr).contains("ADMIN");
        assertThat(expr).contains("AGENT_BUREAU");
        assertThat(expr).doesNotContain("CAISSIER");
    }

    @Test
    void controleEndpoints_shouldAllowControleur() throws Exception {
        String risqueExpr = preAuthorizeValue("controlerRisque", Long.class, String.class);
        String garantieExpr = preAuthorizeValue("controlerGarantie", Long.class, String.class);
        String observationExpr = preAuthorizeValue("enregistrerObservationRisque", Long.class, String.class);
        String validerExpr = preAuthorizeValue("validerAnalyseRisque", Long.class, String.class);

        assertThat(risqueExpr).contains("CONTROLEUR");
        assertThat(garantieExpr).contains("CONTROLEUR");
        assertThat(observationExpr).contains("CONTROLEUR");
        assertThat(validerExpr).contains("CONTROLEUR");
    }

    @Test
    void controleEndpoints_shouldExposeLegacyAndCurrentPaths() throws Exception {
        Method risqueMethod = DemandeCreditController.class.getDeclaredMethod("controlerRisque", Long.class, String.class);
        Method garantieMethod = DemandeCreditController.class.getDeclaredMethod("controlerGarantie", Long.class, String.class);
        Method observationMethod = DemandeCreditController.class.getDeclaredMethod("enregistrerObservationRisque", Long.class, String.class);
        Method validationMethod = DemandeCreditController.class.getDeclaredMethod("validerAnalyseRisque", Long.class, String.class);

        PostMapping risquePostMapping = risqueMethod.getAnnotation(PostMapping.class);
        PostMapping garantiePostMapping = garantieMethod.getAnnotation(PostMapping.class);
        PostMapping observationPostMapping = observationMethod.getAnnotation(PostMapping.class);
        PostMapping validationPostMapping = validationMethod.getAnnotation(PostMapping.class);

        assertThat(risquePostMapping).isNotNull();
        assertThat(garantiePostMapping).isNotNull();
        assertThat(observationPostMapping).isNotNull();
        assertThat(validationPostMapping).isNotNull();
        assertThat(risquePostMapping.value()).contains("/{id}/controle-risque", "/{id}/controler-risque");
        assertThat(garantiePostMapping.value()).contains("/{id}/controle-garantie", "/{id}/controler-garantie");
        assertThat(observationPostMapping.value()).contains("/{id}/analyse-risque/observation");
        assertThat(validationPostMapping.value()).contains("/{id}/analyse-risque/valider");
    }

    @Test
    void analyse_shouldAllowControleurAndExcludeAgentBureau() throws Exception {
        String expr = preAuthorizeValue("ajouterAnalyse", Long.class, com.mini.credit.dto.credit.AnalyseRisqueRequest.class);

        assertThat(expr).contains("CONTROLEUR");
        assertThat(expr).doesNotContain("AGENT_BUREAU");
    }

    @Test
    void getAll_shouldAllowControleur() throws Exception {
        String expr = preAuthorizeValue("getAll", int.class, int.class);

        assertThat(expr).contains("CONTROLEUR");
    }

    @Test
    void getById_shouldUseScopeForControleurAccess() throws Exception {
        String expr = preAuthorizeValue("getById", Long.class);

        assertThat(expr).contains("scopeService.canReadDemandeCredit");
    }

    @Test
    void approvalAndDisbursementMustStayOutsideControleurInCreditController() throws Exception {
        Method approve = CreditController.class.getDeclaredMethod(
                "approuver",
                Long.class,
                com.mini.credit.dto.credit.ApprobationCreditRequest.class
        );
        Method remboursement = CreditController.class.getDeclaredMethod(
            "rembourser",
            Long.class,
            com.mini.credit.dto.credit.RemboursementRequest.class
        );
        Method disburse = CreditController.class.getDeclaredMethod(
                "decaisser",
                Long.class,
                com.mini.credit.dto.credit.DecaissementCreditRequest.class
        );

        PreAuthorize approveAuth = approve.getAnnotation(PreAuthorize.class);
        PreAuthorize remboursementAuth = remboursement.getAnnotation(PreAuthorize.class);
        PreAuthorize disburseAuth = disburse.getAnnotation(PreAuthorize.class);

        assertThat(approveAuth).isNotNull();
        assertThat(remboursementAuth).isNotNull();
        assertThat(disburseAuth).isNotNull();
        assertThat(approveAuth.value()).contains("CREDIT_APPROVE");
        assertThat(approveAuth.value()).doesNotContain("CONTROLEUR");
        assertThat(remboursementAuth.value()).doesNotContain("CONTROLEUR");
        assertThat(disburseAuth.value()).contains("CAISSIER");
        assertThat(disburseAuth.value()).doesNotContain("CONTROLEUR");
    }
}
