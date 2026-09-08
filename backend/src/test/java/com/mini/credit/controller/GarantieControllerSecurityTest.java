package com.mini.credit.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GarantieController - Sécurité")
class GarantieControllerSecurityTest {

    private String preAuthorizeValue(String methodName, Class<?>... paramTypes) throws NoSuchMethodException {
        Method method = GarantieController.class.getDeclaredMethod(methodName, paramTypes);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertThat(preAuthorize).isNotNull();
        return preAuthorize.value();
    }

    @Test
    void controleurReadEndpoints_shouldUseGarantieControlPermission() throws Exception {
        assertThat(preAuthorizeValue("getAllGaranties")).contains("GARANTIE_CONTROL");
        assertThat(preAuthorizeValue("getGarantieById", Long.class)).contains("GARANTIE_CONTROL");
        assertThat(preAuthorizeValue("getGarantiesByCreditId", Long.class)).contains("GARANTIE_CONTROL");
        assertThat(preAuthorizeValue("getGarantiesByType", com.mini.credit.enums.TypeGarantie.class)).contains("GARANTIE_CONTROL");
        assertThat(preAuthorizeValue("getGarantiesByStatut", com.mini.credit.enums.StatutGarantie.class)).contains("GARANTIE_CONTROL");
    }

    @Test
    void writeEndpoints_shouldNotGrantControleurPermissionByDefault() throws Exception {
        assertThat(preAuthorizeValue("createGarantie", com.mini.credit.dto.garantie.GarantieCreateRequest.class))
                .doesNotContain("GARANTIE_CONTROL");
        assertThat(preAuthorizeValue("updateGarantie", Long.class, com.mini.credit.dto.garantie.GarantieCreateRequest.class))
                .doesNotContain("GARANTIE_CONTROL");
    }
}