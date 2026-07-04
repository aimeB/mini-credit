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

    @Test
    void agentBureau_shouldRemainTechnicalEquivalentOfGestionnaire3n() {
        assertThat(RoleCode.AGENT_BUREAU.getDescription()).contains("Gestionnaire 3N");
        assertThat(RoleCode.AGENT_BUREAU).isNotEqualTo(RoleCode.AGENT_TERRAIN);
    }

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
                PermissionCode.CREDIT_APPROVE,
            PermissionCode.AUDIT_READ,
            PermissionCode.GARANTIE_READ
        );
        assertThat(responsable).doesNotContain(
            PermissionCode.SESSION_CAISSE_CONTROL_VALIDATE,
            PermissionCode.SESSION_CAISSE_FINAL_CLOSE,
            PermissionCode.DEPENSE_CAISSE_VALIDATE,
            PermissionCode.SESSION_CAISSE_ADMIN_CANCEL,
            PermissionCode.SESSION_CAISSE_REOPEN_CONTROLLED
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
            PermissionCode.RAPPORT_CAISSE_EXPORT,
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
                PermissionCode.CONTROLEUR_SESSION_CAISSE_READ,
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
        assertThat(rci).contains(PermissionCode.FICHE_JOURNALIERE_READ);
        assertThat(rci).doesNotContain(
            PermissionCode.OPERATION_CAISSE_CREATE,
            PermissionCode.CREDIT_DISBURSE,
            PermissionCode.DEPENSE_CAISSE_PAY,
            PermissionCode.CREDIT_APPROVE,
            PermissionCode.FICHE_JOURNALIERE_CREATE,
            PermissionCode.FICHE_JOURNALIERE_EDIT,
            PermissionCode.FICHE_JOURNALIERE_DELETE
        );
        assertThat(RoleCode.RCI).isNotEqualTo(RoleCode.CHEF_BUREAU);
    }

        @Test
        void sessionCaisseExceptionalPermissions_shouldBeAdminAndChefBureauOnlyWithoutResponsable() throws Exception {
        Map<RoleCode, Set<PermissionCode>> matrix = buildMatrix();

        assertThat(matrix.get(RoleCode.ADMIN)).contains(
            PermissionCode.SESSION_CAISSE_ADMIN_CANCEL,
            PermissionCode.SESSION_CAISSE_REOPEN_CONTROLLED,
            PermissionCode.SESSION_CAISSE_OPEN_OVERRIDE
        );

        assertThat(matrix.get(RoleCode.CHEF_BUREAU)).contains(
            PermissionCode.SESSION_CAISSE_ADMIN_CANCEL,
            PermissionCode.SESSION_CAISSE_REOPEN_CONTROLLED
        );

        assertThat(matrix.get(RoleCode.RESPONSABLE)).doesNotContain(
            PermissionCode.SESSION_CAISSE_ADMIN_CANCEL,
            PermissionCode.SESSION_CAISSE_REOPEN_CONTROLLED,
            PermissionCode.SESSION_CAISSE_OPEN_OVERRIDE
        );
        }

    @Test
    void sensitivePermissionsFreeze_shouldRemainUnchangedInRbac2bPatch() throws Exception {
        Map<RoleCode, Set<PermissionCode>> matrix = buildMatrix();

        assertThat(matrix.get(RoleCode.ADMIN)).contains(
                PermissionCode.USER_PASSWORD_RESET,
                PermissionCode.AUDIT_LOG_EXPORT,
                PermissionCode.AUDIT_SECURITY_READ,
                PermissionCode.SESSION_CAISSE_ADMIN_CANCEL,
                PermissionCode.SESSION_CAISSE_REOPEN_CONTROLLED,
                PermissionCode.CREDIT_APPROVE,
                PermissionCode.GARANTIE_VALIDATE,
                PermissionCode.DEPENSE_CAISSE_CANCEL,
                PermissionCode.SESSION_CAISSE_OPEN_OVERRIDE,
                PermissionCode.CREDIT_DISBURSE
        );

        assertThat(matrix.get(RoleCode.RCI)).contains(PermissionCode.AUDIT_LOG_EXPORT, PermissionCode.AUDIT_SECURITY_READ);
        assertThat(matrix.get(RoleCode.CHEF_BUREAU)).contains(
                PermissionCode.USER_PASSWORD_RESET,
                PermissionCode.SESSION_CAISSE_ADMIN_CANCEL,
                PermissionCode.SESSION_CAISSE_REOPEN_CONTROLLED,
                PermissionCode.CREDIT_APPROVE
        );
        assertThat(matrix.get(RoleCode.RESPONSABLE)).contains(
                PermissionCode.USER_PASSWORD_RESET,
                PermissionCode.CREDIT_APPROVE,
                PermissionCode.GARANTIE_VALIDATE
        );
        assertThat(matrix.get(RoleCode.RESPONSABLE)).doesNotContain(
            PermissionCode.SESSION_CAISSE_ADMIN_CANCEL,
            PermissionCode.SESSION_CAISSE_REOPEN_CONTROLLED,
            PermissionCode.SESSION_CAISSE_OPEN_OVERRIDE
        );

        // ADMIN-only permissions frozen in this patch
        assertThat(matrix.get(RoleCode.CHEF_BUREAU)).doesNotContain(PermissionCode.SESSION_CAISSE_OPEN_OVERRIDE);
        assertThat(matrix.get(RoleCode.CONTROLEUR)).doesNotContain(PermissionCode.SESSION_CAISSE_OPEN_OVERRIDE);
        assertThat(matrix.get(RoleCode.CAISSIER)).doesNotContain(PermissionCode.SESSION_CAISSE_OPEN_OVERRIDE);
        assertThat(matrix.get(RoleCode.RCI)).doesNotContain(PermissionCode.SESSION_CAISSE_OPEN_OVERRIDE);

        assertThat(matrix.get(RoleCode.CHEF_BUREAU)).doesNotContain(PermissionCode.DEPENSE_CAISSE_CANCEL);
        assertThat(matrix.get(RoleCode.CONTROLEUR)).doesNotContain(PermissionCode.DEPENSE_CAISSE_CANCEL);
        assertThat(matrix.get(RoleCode.CAISSIER)).doesNotContain(PermissionCode.DEPENSE_CAISSE_CANCEL);
        assertThat(matrix.get(RoleCode.RCI)).doesNotContain(PermissionCode.DEPENSE_CAISSE_CANCEL);

        assertThat(matrix.get(RoleCode.CAISSIER)).contains(PermissionCode.CREDIT_DISBURSE);
    }
}