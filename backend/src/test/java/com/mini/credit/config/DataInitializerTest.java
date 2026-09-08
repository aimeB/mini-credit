package com.mini.credit.config;

import com.mini.credit.enums.security.RoleCode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DataInitializerTest {

    @Test
    void dataInitializerNeCreePasRolesSupprimes() throws Exception {
        Field field = DataInitializer.class.getDeclaredField("OFFICIAL_BOOTSTRAP_ROLES");
        field.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<RoleCode> roles = (List<RoleCode>) field.get(null);

        assertThat(roles)
                .containsExactly(
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
        assertThat(roles).extracting(Enum::name).doesNotContain("AGENT_BUREAU", "RESPONSABLE");
    }
}