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
    void chefBureau_shouldCarryOfficialDecisionCapabilitiesWithoutLegacyMatrixEntry() throws Exception {
        Map<RoleCode, Set<PermissionCode>> matrix = buildMatrix();

        Set<PermissionCode> chefBureau = matrix.get(RoleCode.CHEF_BUREAU);

        assertThat(chefBureau).contains(
            PermissionCode.CAISSE_READ,
            PermissionCode.SESSION_CAISSE_FINAL_CLOSE,
            PermissionCode.CREDIT_APPROVE,
            PermissionCode.AUDIT_READ,
            PermissionCode.GARANTIE_READ
        );
        assertThat(chefBureau).doesNotContain(PermissionCode.SESSION_CAISSE_CONTROL_VALIDATE);
    }

    @Test
    void gestionnaire_shouldHaveTerrainAndPreAnalyseWithoutFinancialValidation() throws Exception {
        Set<PermissionCode> gestionnaire = buildMatrix().get(RoleCode.GESTIONNAIRE);

        assertThat(gestionnaire).contains(
            PermissionCode.MEMBRE_READ,
                PermissionCode.AGENT_TERRAIN_READ,
                PermissionCode.SITE_READ,
                PermissionCode.TERRAIN_ACTIVITY_READ,
                PermissionCode.PERFORMANCE_TERRAIN_READ,
                PermissionCode.CREDIT_PRE_ANALYSE,
                PermissionCode.RECLAMATION_READ,
                PermissionCode.RECLAMATION_COMMENT,
                PermissionCode.ANALYSE_RISQUE_CREATE,
                PermissionCode.ANALYSE_RISQUE_READ
        );

        assertThat(gestionnaire).doesNotContain(
                PermissionCode.CONTROLEUR_RECETTES_VALIDATE,
                PermissionCode.CONTROLEUR_RETRAITS_VALIDATE,
                PermissionCode.CREDIT_APPROVE,
                PermissionCode.CREDIT_DISBURSE,
                PermissionCode.SESSION_CAISSE_CLOSE,
                PermissionCode.SESSION_CAISSE_FINAL_CLOSE,
                PermissionCode.SESSION_CAISSE_CONTROL_VALIDATE,
                PermissionCode.GARANTIE_CONTROL,
                PermissionCode.GARANTIE_VALIDATE
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
    void controleur_shouldAccessCaisseConsultationControlAndEcartWithoutCashierPowers() throws Exception {
        Set<PermissionCode> perms = buildMatrix().get(RoleCode.CONTROLEUR);

        assertThat(perms).contains(
                PermissionCode.CAISSE_READ,
                PermissionCode.CONTROLEUR_SESSION_CAISSE_READ,
                PermissionCode.SESSION_CAISSE_CONTROL_VALIDATE,
                PermissionCode.SESSION_CAISSE_ANOMALIE_READ,
                PermissionCode.CONTROLEUR_ECART_READ,
                PermissionCode.CONTROLEUR_ECART_VALIDATE,
                PermissionCode.RAPPORT_CAISSE_READ,
                PermissionCode.RAPPORT_CAISSE_AUDIT_READ
        );

        assertThat(perms).doesNotContain(
                PermissionCode.SESSION_CAISSE_OPEN,
                PermissionCode.SESSION_CAISSE_CLOSE,
                PermissionCode.OPERATION_CAISSE_CREATE,
                PermissionCode.SESSION_CAISSE_ADMIN_CANCEL,
                PermissionCode.SESSION_CAISSE_REOPEN_CONTROLLED,
                PermissionCode.SESSION_CAISSE_OPEN_OVERRIDE
        );
    }

    @Test
    void chefBureau_shouldHaveCashierSubstitutionPermissions_andRciShouldRemainAuditOnly() throws Exception {
        Map<RoleCode, Set<PermissionCode>> matrix = buildMatrix();

        Set<PermissionCode> chefBureau = matrix.get(RoleCode.CHEF_BUREAU);
        assertThat(chefBureau).contains(
            PermissionCode.CAISSE_READ,
            PermissionCode.SESSION_CAISSE_OPEN,
            PermissionCode.SESSION_CAISSE_PRE_CLOSE,
            PermissionCode.SESSION_CAISSE_CLOSE,
            PermissionCode.OPERATION_CAISSE_CREATE,
            PermissionCode.OPERATION_CAISSE_READ,
            PermissionCode.CREDIT_APPROVE,
            PermissionCode.AUDIT_READ,
            PermissionCode.SESSION_CAISSE_FINAL_CLOSE
        );
        assertThat(chefBureau).doesNotContain(
            PermissionCode.SESSION_CAISSE_CONTROL_VALIDATE,
            PermissionCode.SESSION_CAISSE_OPEN_OVERRIDE,
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
        void sessionCaisseExceptionalPermissions_shouldBeAdminAndChefBureauOnly() throws Exception {
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