package com.mini.credit.config;

import com.mini.credit.enums.security.PermissionCode;
import com.mini.credit.enums.security.RoleCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RolePermissionSeeder - Matrice sécurité")
class RolePermissionSeederSecurityTest {

    @SuppressWarnings("unchecked")
    private Map<RoleCode, Set<PermissionCode>> buildMatrix() throws Exception {
        RolePermissionSeeder seeder = new RolePermissionSeeder();
        Method method = RolePermissionSeeder.class.getDeclaredMethod("buildPermissionMatrix");
        method.setAccessible(true);
        return (Map<RoleCode, Set<PermissionCode>>) method.invoke(seeder);
    }

    @Test
    void responsable_shouldRemainLegacyCompatibleWithChefBureauCoreCapabilities() throws Exception {
        Map<RoleCode, Set<PermissionCode>> matrix = buildMatrix();

        Set<PermissionCode> chefBureau = matrix.get(RoleCode.CHEF_BUREAU);
        Set<PermissionCode> responsable = matrix.get(RoleCode.RESPONSABLE);

        assertThat(responsable).contains(
                PermissionCode.CAISSE_READ,
                PermissionCode.SESSION_CAISSE_CONTROL_VALIDATE,
                PermissionCode.SESSION_CAISSE_FINAL_CLOSE,
                PermissionCode.CREDIT_APPROVE,
            PermissionCode.AUDIT_READ,
            PermissionCode.GARANTIE_READ
        );
        assertThat(chefBureau).contains(
            PermissionCode.CAISSE_READ,
            PermissionCode.SESSION_CAISSE_CONTROL_VALIDATE,
            PermissionCode.SESSION_CAISSE_FINAL_CLOSE,
            PermissionCode.CREDIT_APPROVE,
            PermissionCode.AUDIT_READ,
            PermissionCode.GARANTIE_READ
        );
    }

    @Test
    void caissier_shouldHaveOpenPreCloseAndCashOperationsWithoutControllerFinalization() throws Exception {
        Set<PermissionCode> perms = buildMatrix().get(RoleCode.CAISSIER);

        assertThat(perms).contains(
                PermissionCode.CAISSE_READ,
                PermissionCode.SESSION_CAISSE_OPEN,
                PermissionCode.SESSION_CAISSE_PRE_CLOSE,
                PermissionCode.OPERATION_CAISSE_CREATE,
                PermissionCode.OPERATION_CAISSE_READ,
                PermissionCode.CREDIT_DISBURSE
        );
        assertThat(perms).doesNotContain(
                PermissionCode.SESSION_CAISSE_CONTROL_VALIDATE,
                PermissionCode.SESSION_CAISSE_FINAL_CLOSE,
                PermissionCode.GARANTIE_CONTROL
        );
    }

    @Test
    void controleur_shouldHaveFinalControlPermissionsWithoutCreditDisbursement() throws Exception {
        Set<PermissionCode> perms = buildMatrix().get(RoleCode.CONTROLEUR);

        assertThat(perms).contains(
                PermissionCode.CAISSE_READ,
                PermissionCode.OPERATION_CAISSE_READ,
                PermissionCode.SESSION_CAISSE_CONTROL_VALIDATE,
                PermissionCode.SESSION_CAISSE_FINAL_CLOSE,
                PermissionCode.CONTROLEUR_RECETTES_VALIDATE,
                PermissionCode.CONTROLEUR_RETRAITS_VALIDATE,
                PermissionCode.CONTROLEUR_CREDITS_VALIDATE,
                PermissionCode.GARANTIE_CONTROL
        );
        assertThat(perms).doesNotContain(PermissionCode.CREDIT_DISBURSE);
    }

    @Test
    void chefBureauAndRci_shouldRemainReadOrDecisionOriented() throws Exception {
        Map<RoleCode, Set<PermissionCode>> matrix = buildMatrix();

        Set<PermissionCode> chefBureau = matrix.get(RoleCode.CHEF_BUREAU);
        assertThat(chefBureau).contains(
            PermissionCode.CAISSE_READ,
            PermissionCode.OPERATION_CAISSE_READ,
            PermissionCode.CREDIT_APPROVE,
            PermissionCode.AUDIT_READ,
            PermissionCode.SESSION_CAISSE_CONTROL_VALIDATE,
            PermissionCode.SESSION_CAISSE_FINAL_CLOSE
        );
        assertThat(chefBureau).doesNotContain(
                PermissionCode.OPERATION_CAISSE_CREATE,
                PermissionCode.CONTROLEUR_CREDITS_VALIDATE
        );

        Set<PermissionCode> rci = matrix.get(RoleCode.RCI);
        assertThat(rci).contains(PermissionCode.CAISSE_READ, PermissionCode.OPERATION_CAISSE_READ, PermissionCode.AUDIT_READ);
        assertThat(rci).doesNotContain(PermissionCode.OPERATION_CAISSE_CREATE, PermissionCode.CREDIT_DISBURSE);
    }
}