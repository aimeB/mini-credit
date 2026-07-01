package com.mini.credit.config;

import com.mini.credit.entity.referentiel.Role;
import com.mini.credit.enums.security.PermissionCode;
import com.mini.credit.enums.security.RoleCode;
import org.junit.jupiter.api.Test;
import com.mini.credit.repository.referentiel.RoleRepository;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RolePermissionSeederMatrixTest {

    @Test
    void roleCode_shouldContainCooGerantGeneralAndLegacyRoles() {
        assertThat(RoleCode.valueOf("COO")).isEqualTo(RoleCode.COO);
        assertThat(RoleCode.valueOf("GERANT_GENERAL")).isEqualTo(RoleCode.GERANT_GENERAL);
        assertThat(RoleCode.valueOf("RESPONSABLE")).isEqualTo(RoleCode.RESPONSABLE);
        assertThat(RoleCode.valueOf("CHEF_BUREAU")).isEqualTo(RoleCode.CHEF_BUREAU);
        assertThat(RoleCode.valueOf("AGENT_BUREAU")).isEqualTo(RoleCode.AGENT_BUREAU);
        assertThat(RoleCode.valueOf("RCI")).isEqualTo(RoleCode.RCI);
    }

    @Test
    void createRoles_shouldSeedCooGerantGeneral_andKeepLegacyAnd3nRoles() throws Exception {
        RolePermissionSeeder seeder = new RolePermissionSeeder();
        RoleRepository roleRepository = mock(RoleRepository.class);

        when(roleRepository.findByCode(any(RoleCode.class))).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Method method = RolePermissionSeeder.class.getDeclaredMethod("createRoles", RoleRepository.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<RoleCode, Role> roles = (Map<RoleCode, Role>) method.invoke(seeder, roleRepository);

        assertThat(roles)
                .containsKeys(
                        RoleCode.COO,
                        RoleCode.GERANT_GENERAL,
                        RoleCode.RESPONSABLE,
                        RoleCode.CHEF_BUREAU,
                        RoleCode.RCI,
                        RoleCode.AGENT_BUREAU
                );
    }

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
