package com.mini.credit.config;

import com.mini.credit.entity.referentiel.Role;
import com.mini.credit.entity.referentiel.Permission;
import com.mini.credit.enums.security.PermissionCode;
import com.mini.credit.enums.security.RoleCode;
import org.junit.jupiter.api.Test;
import com.mini.credit.repository.referentiel.PermissionRepository;
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
    void roleCode_shouldContainOnlyOfficialRoles() {
        assertThat(RoleCode.valueOf("COO")).isEqualTo(RoleCode.COO);
        assertThat(RoleCode.valueOf("GERANT_GENERAL")).isEqualTo(RoleCode.GERANT_GENERAL);
        assertThat(RoleCode.valueOf("CHEF_BUREAU")).isEqualTo(RoleCode.CHEF_BUREAU);
        assertThat(RoleCode.valueOf("GESTIONNAIRE")).isEqualTo(RoleCode.GESTIONNAIRE);
        assertThat(RoleCode.valueOf("RCI")).isEqualTo(RoleCode.RCI);
        assertThat(java.util.Arrays.stream(RoleCode.values()).map(Enum::name))
                .doesNotContain("AGENT_BUREAU", "RESPONSABLE");
    }

    @Test
    void createRoles_shouldSeedOnlyActiveOfficialRoles() throws Exception {
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
                        RoleCode.CHEF_BUREAU,
                        RoleCode.GESTIONNAIRE,
                        RoleCode.RCI,
                        RoleCode.AGENT_TERRAIN
                );

                assertThat(roles.keySet())
                    .containsExactlyInAnyOrder(
                        RoleCode.ADMIN,
                        RoleCode.CHEF_BUREAU,
                        RoleCode.GESTIONNAIRE,
                        RoleCode.AGENT_TERRAIN,
                        RoleCode.CAISSIER,
                        RoleCode.CONTROLEUR,
                        RoleCode.COO,
                        RoleCode.RCI,
                        RoleCode.GERANT_GENERAL,
                        RoleCode.MEMBER
                    );
    }

                @Test
                void rolePermissionSeederNeReferencePasRolesSupprimes() throws Exception {
                Map<RoleCode, Set<PermissionCode>> matrix = buildMatrix();

                assertThat(matrix.keySet())
                    .extracting(Enum::name)
                    .doesNotContain("AGENT_BUREAU", "RESPONSABLE");
                assertThat(matrix.keySet()).containsExactlyInAnyOrder(
                    RoleCode.ADMIN,
                    RoleCode.CHEF_BUREAU,
                    RoleCode.GESTIONNAIRE,
                    RoleCode.AGENT_TERRAIN,
                    RoleCode.CAISSIER,
                    RoleCode.CONTROLEUR,
                    RoleCode.COO,
                    RoleCode.RCI,
                    RoleCode.GERANT_GENERAL,
                    RoleCode.MEMBER
                );
                }

    @Test
    void createPermissions_shouldSeedAllPermissionCodes_withoutOrphans() throws Exception {
        RolePermissionSeeder seeder = new RolePermissionSeeder();
        PermissionRepository permissionRepository = mock(PermissionRepository.class);

        when(permissionRepository.findByCode(any(PermissionCode.class))).thenReturn(Optional.empty());
        when(permissionRepository.save(any(Permission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Method method = RolePermissionSeeder.class.getDeclaredMethod("createPermissions", PermissionRepository.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<PermissionCode, Permission> permissions = (Map<PermissionCode, Permission>) method.invoke(seeder, permissionRepository);

        assertThat(permissions.keySet()).containsExactlyInAnyOrder(PermissionCode.values());
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
    void chefBureauRole_receivesFinalClosePermissionWithoutControlValidation() throws Exception {
        Map<RoleCode, Set<PermissionCode>> matrix = buildMatrix();

        assertThat(matrix.get(RoleCode.CHEF_BUREAU))
                .contains(PermissionCode.SESSION_CAISSE_FINAL_CLOSE)
                .doesNotContain(PermissionCode.SESSION_CAISSE_CONTROL_VALIDATE)
                .doesNotContain(PermissionCode.CONTROLEUR_SESSION_CAISSE_VALIDATE)
                .doesNotContain(PermissionCode.CONTROLEUR_CREDITS_VALIDATE);
    }

            @Test
            void cooAndGerantGeneral_shouldHaveAuditAndReportReadExportBundle() throws Exception {
            Map<RoleCode, Set<PermissionCode>> matrix = buildMatrix();

            assertThat(matrix.get(RoleCode.COO))
                .contains(
                    PermissionCode.AUDIT_LOG_READ,
                    PermissionCode.RAPPORT_CAISSE_AUDIT_READ,
                    PermissionCode.RAPPORT_CAISSE_EXPORT
                );

            assertThat(matrix.get(RoleCode.GERANT_GENERAL))
                .contains(
                    PermissionCode.AUDIT_LOG_READ,
                    PermissionCode.RAPPORT_CAISSE_AUDIT_READ,
                    PermissionCode.RAPPORT_CAISSE_EXPORT
                );
            }

            @Test
            void admin_shouldKeepCreditDisburseAndSessionOpenOverride() throws Exception {
            Map<RoleCode, Set<PermissionCode>> matrix = buildMatrix();

            assertThat(matrix.get(RoleCode.ADMIN))
                .contains(PermissionCode.CREDIT_DISBURSE)
                .contains(PermissionCode.SESSION_CAISSE_OPEN_OVERRIDE);
            }

            @Test
            void agentTerrain_shouldOwnFicheCreate_andRciShouldBeReadOnlyOnFiche() throws Exception {
            Map<RoleCode, Set<PermissionCode>> matrix = buildMatrix();

            assertThat(matrix.get(RoleCode.AGENT_TERRAIN))
                .contains(PermissionCode.FICHE_JOURNALIERE_CREATE)
                .contains(PermissionCode.FICHE_JOURNALIERE_READ)
                .contains(PermissionCode.FICHE_JOURNALIERE_EDIT)
                .doesNotContain(PermissionCode.FICHE_JOURNALIERE_DELETE);

            assertThat(matrix.get(RoleCode.RCI))
                .contains(PermissionCode.FICHE_JOURNALIERE_READ)
                .doesNotContain(PermissionCode.FICHE_JOURNALIERE_CREATE)
                .doesNotContain(PermissionCode.FICHE_JOURNALIERE_EDIT)
                .doesNotContain(PermissionCode.FICHE_JOURNALIERE_DELETE);
            }

    @SuppressWarnings("unchecked")
    private Map<RoleCode, Set<PermissionCode>> buildMatrix() throws Exception {
        RolePermissionSeeder seeder = new RolePermissionSeeder();
        Method method = RolePermissionSeeder.class.getDeclaredMethod("buildPermissionMatrix");
        method.setAccessible(true);
        return (Map<RoleCode, Set<PermissionCode>>) method.invoke(seeder);
    }
}
