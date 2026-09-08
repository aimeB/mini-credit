package com.mini.credit.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CollecteTerrainController — Sécurité des endpoints")
class CollecteTerrainControllerSecurityTest {

    private PreAuthorize getPreAuthorize(String methodName, Class<?>... paramTypes) throws NoSuchMethodException {
        Method method = CollecteTerrainController.class.getDeclaredMethod(methodName, paramTypes);
        return method.getAnnotation(PreAuthorize.class);
    }

    @Test
    void list_shouldAllowExpectedRoles() throws Exception {
        String expr = getPreAuthorize("list", String.class, java.time.LocalDate.class, java.time.LocalDate.class, Long.class, Long.class, Long.class, int.class, int.class).value();
        assertThat(expr).contains("ADMIN").contains("AGENT_TERRAIN").contains("CAISSIER").contains("GESTIONNAIRE").contains("CONTROLEUR").contains("CHEF_BUREAU").contains("RCI");
    }

    @Test
    void today_andLineCrud_shouldBeAgentOnly() throws Exception {
        assertThat(getPreAuthorize("getToday").value()).contains("AGENT_TERRAIN");
        assertThat(getPreAuthorize("addLigne", Long.class, com.mini.credit.dto.referentiel.CreateCollecteMembreLigneRequest.class).value()).contains("AGENT_TERRAIN");
        assertThat(getPreAuthorize("updateLigne", Long.class, Long.class, com.mini.credit.dto.referentiel.UpdateCollecteMembreLigneRequest.class).value()).contains("AGENT_TERRAIN");
        assertThat(getPreAuthorize("deleteLigne", Long.class, Long.class).value()).contains("AGENT_TERRAIN");
        assertThat(getPreAuthorize("soumettre", Long.class, com.mini.credit.dto.referentiel.CreateCollecteTerrainRequest.class).value()).contains("AGENT_TERRAIN");
        assertThat(getPreAuthorize("confirmerBilletage", Long.class, com.mini.credit.dto.referentiel.ConfirmerBilletageRequest.class).value()).contains("CAISSIER");
    }

    @Test
    void getById_shouldAllowReadRoles() throws Exception {
        String expr = getPreAuthorize("getById", Long.class).value();
        assertThat(expr)
            .contains("ADMIN")
            .contains("AGENT_TERRAIN")
            .contains("CAISSIER")
            .contains("CONTROLEUR")
            .contains("CHEF_BUREAU")
            .contains("GESTIONNAIRE")
            .contains("RCI");
    }

    @Test
    void valider_rejeter_shouldBeAdminOrControleurOnly() throws Exception {
        String validerExpr = getPreAuthorize("valider", Long.class, com.mini.credit.dto.referentiel.ValidateCollecteTerrainRequest.class).value();
        String rejeterExpr = getPreAuthorize("rejeter", Long.class, com.mini.credit.dto.referentiel.ValidateCollecteTerrainRequest.class).value();

        assertThat(validerExpr).contains("ADMIN").contains("CONTROLEUR");
        assertThat(rejeterExpr).contains("ADMIN").contains("CONTROLEUR");
        assertThat(validerExpr).doesNotContain("CAISSIER").doesNotContain("GESTIONNAIRE").doesNotContain("AGENT_TERRAIN").doesNotContain("CHEF_BUREAU");
        assertThat(rejeterExpr).doesNotContain("CAISSIER").doesNotContain("GESTIONNAIRE").doesNotContain("AGENT_TERRAIN").doesNotContain("CHEF_BUREAU");
    }

    @Test
    void recap_shouldAllowReadRoles() throws Exception {
        String expr = getPreAuthorize("recap", Long.class).value();
        assertThat(expr).contains("ADMIN").contains("CONTROLEUR").contains("AGENT_TERRAIN").contains("CAISSIER").contains("CHEF_BUREAU").contains("GESTIONNAIRE").contains("RCI");
    }
}
