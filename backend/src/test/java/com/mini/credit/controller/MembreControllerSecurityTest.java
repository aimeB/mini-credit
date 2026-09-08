package com.mini.credit.controller;

import com.mini.credit.dto.membre.MembreResponse;
import com.mini.credit.service.MembreService;
import com.mini.credit.service.security.ScopeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(MembreControllerSecurityTest.TestConfig.class)
@DisplayName("MembreController — Sécurité endpoint search")
class MembreControllerSecurityTest {

    @Autowired
    private MembreController membreController;

    @Autowired
    private MembreService membreService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        reset(membreService);
    }

    private PreAuthorize getPreAuthorize(String methodName, Class<?>... paramTypes) throws NoSuchMethodException {
        Method method = MembreController.class.getDeclaredMethod(methodName, paramTypes);
        return method.getAnnotation(PreAuthorize.class);
    }

    @Test
    void search_shouldRequireMembreReadPermission() throws Exception {
        String expr = getPreAuthorize("search", String.class, Long.class, int.class, int.class).value();
        assertThat(expr)
            .contains("MEMBRE_READ");
    }

    @Test
    void getAll_shouldRequireMembreReadPermission() throws Exception {
        String expr = getPreAuthorize("getAll", int.class, int.class).value();
        assertThat(expr)
            .contains("MEMBRE_READ");
    }

    @Test
    void gestionnaireWithMembreRead_shouldCallSearchMembers() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
            "gestionnaire.gest",
            null,
            List.of(
                new SimpleGrantedAuthority("ROLE_GESTIONNAIRE"),
                new SimpleGrantedAuthority("MEMBRE_READ")
            )
        ));
        when(membreService.search(eq(null), eq(null), any()))
            .thenReturn(new PageImpl<>(List.of(MembreResponse.builder().id(1L).nomComplet("Membre Test").build()), PageRequest.of(0, 20), 1));

        assertThatCode(() -> membreController.search(null, null, 0, 20)).doesNotThrowAnyException();
    }

    @Test
    void gestionnaireWithMembreRead_shouldCallGetAllMembers() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
            "gestionnaire.gest",
            null,
            List.of(
                new SimpleGrantedAuthority("ROLE_GESTIONNAIRE"),
                new SimpleGrantedAuthority("MEMBRE_READ")
            )
        ));
        when(membreService.getAll(any()))
            .thenReturn(new PageImpl<>(List.of(MembreResponse.builder().id(2L).nomComplet("Membre Liste").build()), PageRequest.of(0, 10), 1));

        assertThatCode(() -> membreController.getAll(0, 10)).doesNotThrowAnyException();
    }

    @Configuration
    @EnableMethodSecurity
    static class TestConfig {
        @Bean
        MembreService membreService() {
            return mock(MembreService.class);
        }

        @Bean
        ScopeService scopeService() {
            return mock(ScopeService.class);
        }

        @Bean
        MembreController membreController(MembreService membreService, ScopeService scopeService) {
            return new MembreController(membreService, scopeService);
        }
    }
}
