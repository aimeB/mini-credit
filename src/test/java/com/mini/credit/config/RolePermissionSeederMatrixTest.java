package com.mini.credit.config;

import com.mini.credit.enums.security.PermissionCode;
import com.mini.credit.enums.security.RoleCode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RolePermissionSeederMatrixTest {

    @Test
    void caissierRole_containsCaisseCreatePermission() throws Exception {
        Map<RoleCode, Set<PermissionCode>> matrix = buildMatrix();

        assertThat(matrix.get(RoleCode.CAISSIER)).contains(PermissionCode.CAISSE_CREATE);
    }

    @Test
    void controleurRole_doesNotContainCaisseCreatePermission() throws Exception {
        Map<RoleCode, Set<PermissionCode>> matrix = buildMatrix();

        assertThat(matrix.get(RoleCode.CONTROLEUR)).doesNotContain(PermissionCode.CAISSE_CREATE);
    }

    @Test
    void chefBureauRole_receivesSessionValidationPermissionsForSupervision() throws Exception {
        Map<RoleCode, Set<PermissionCode>> matrix = buildMatrix();

        assertThat(matrix.get(RoleCode.CHEF_BUREAU))
                .contains(PermissionCode.SESSION_CAISSE_CONTROL_VALIDATE)
                .contains(PermissionCode.SESSION_CAISSE_FINAL_CLOSE)
                .doesNotContain(PermissionCode.CONTROLEUR_SESSION_CAISSE_VALIDATE)
                .doesNotContain(PermissionCode.CONTROLEUR_CREDITS_VALIDATE);
    }

    @SuppressWarnings("unchecked")
    private Map<RoleCode, Set<PermissionCode>> buildMatrix() throws Exception {
        RolePermissionSeeder seeder = new RolePermissionSeeder();
        Method method = RolePermissionSeeder.class.getDeclaredMethod("buildPermissionMatrix");
        method.setAccessible(true);
        return (Map<RoleCode, Set<PermissionCode>>) method.invoke(seeder);
    }
}
