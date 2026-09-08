package com.mini.credit.controller;

import com.mini.credit.controller.DemandeCreditPaiementController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CreditController — Sécurité")
class CreditControllerSecurityTest {

    private PreAuthorize getPreAuthorize(String methodName, Class<?>... paramTypes) throws NoSuchMethodException {
        Method method = CreditController.class.getDeclaredMethod(methodName, paramTypes);
        return method.getAnnotation(PreAuthorize.class);
    }

    @Test
    void sensitiveCreditActions_shouldExcludeAgentTerrain() throws Exception {
        String approbationExpr = getPreAuthorize("approuver", Long.class, com.mini.credit.dto.credit.ApprobationCreditRequest.class).value();
        String remboursementExpr = getPreAuthorize("rembourser", Long.class, com.mini.credit.dto.credit.RemboursementRequest.class).value();
        String decaissementExpr = getPreAuthorize("decaisser", Long.class, com.mini.credit.dto.credit.DecaissementCreditRequest.class).value();

        assertThat(approbationExpr).doesNotContain("AGENT_TERRAIN");
        assertThat(remboursementExpr).doesNotContain("AGENT_TERRAIN");
        assertThat(decaissementExpr).doesNotContain("AGENT_TERRAIN");
    }

    @Test
    void decaissement_shouldRemainStrictlyBoundToCreditDisbursePermission() throws Exception {
        String decaissementExpr = getPreAuthorize("decaisser", Long.class, com.mini.credit.dto.credit.DecaissementCreditRequest.class).value();

        assertThat(decaissementExpr).isEqualTo("hasRole('CAISSIER')");
        assertThat(decaissementExpr).doesNotContain("CONTROLEUR_CREDITS_VALIDATE");
    }

    @Test
    void creditsADecaisser_shouldBeVisibleToPageRolesWithoutGlobalCreditAccess() throws Exception {
        String creditsADecaisserExpr = getPreAuthorize("getCreditsADecaisser").value();
        String getAllExpr = getPreAuthorize("getAll", int.class, int.class).value();

        assertThat(creditsADecaisserExpr).isEqualTo("hasAnyRole('ADMIN', 'CHEF_BUREAU', 'GESTIONNAIRE', 'CAISSIER', 'CONTROLEUR')");
        assertThat(creditsADecaisserExpr).contains("GESTIONNAIRE");
        assertThat(creditsADecaisserExpr).contains("CAISSIER");
        assertThat(getAllExpr).doesNotContain("CAISSIER");
    }

    @Test
    void paiementInitialDemande_shouldRemainStrictlyBoundToCaissierRole() throws Exception {
        Method method = DemandeCreditPaiementController.class.getDeclaredMethod(
                "enregistrerPaiementInitial",
                Long.class,
                com.mini.credit.dto.credit.PaiementInitialDemandeCreditRequest.class
        );

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasRole('CAISSIER')");
        assertThat(preAuthorize.value()).doesNotContain("CONTROLEUR");
    }
}
