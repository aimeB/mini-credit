package com.mini.credit.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OperationCaisseController - Sécurité")
class OperationCaisseControllerSecurityTest {

    private String preAuthorizeValue(String methodName, Class<?>... paramTypes) throws NoSuchMethodException {
        Method method = OperationCaisseController.class.getDeclaredMethod(methodName, paramTypes);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertThat(preAuthorize).isNotNull();
        return preAuthorize.value();
    }

    @Test
    void create_shouldRequireOperationCreatePermission() throws Exception {
        assertThat(preAuthorizeValue("enregistrer", com.mini.credit.dto.caisse.OperationCaisseRequest.class))
                .isEqualTo("hasAnyRole('ADMIN', 'CHEF_BUREAU') or (hasAuthority('OPERATION_CAISSE_CREATE') and !hasAnyRole('CAISSIER', 'RCI'))");
    }

    @Test
    void update_shouldDenyRciAndCaissierOnGenericModification() throws Exception {
        assertThat(preAuthorizeValue(
                "requalifierNatureFinancement",
                Long.class,
                com.mini.credit.dto.caisse.RequalificationNatureFinancementRequest.class
        )).isEqualTo("hasAnyRole('ADMIN', 'CHEF_BUREAU') or (hasAuthority('OPERATION_CAISSE_UPDATE') and !hasAnyRole('CAISSIER', 'RCI'))");
    }

    @Test
    void readEndpoints_shouldRequireOperationReadPermission() throws Exception {
        assertThat(preAuthorizeValue("getAll", int.class, int.class))
                .isEqualTo("hasAuthority('OPERATION_CAISSE_READ')");
        assertThat(preAuthorizeValue("getBySession", Long.class))
                .isEqualTo("hasAuthority('OPERATION_CAISSE_READ')");
        assertThat(preAuthorizeValue("getByCaisse", Long.class))
                .isEqualTo("hasAuthority('OPERATION_CAISSE_READ')");
    }
}