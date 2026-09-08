package com.mini.credit.controller;

import com.mini.credit.controller.audit.AuditController;
import com.mini.credit.dto.audit.CaisseHistoryDiagnosticReportDTO;
import com.mini.credit.repository.audit.AuditLogRepository;
import com.mini.credit.service.audit.AuditAccessService;
import com.mini.credit.service.audit.CaisseHistoryDiagnosticService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(AuditControllerCaisseHistorySecurityTest.TestSecurityConfig.class)
@DisplayName("AuditController - Diagnostic historique caisse - sécurité")
class AuditControllerCaisseHistorySecurityTest {

    @Autowired
    private AuditController auditController;

    @Autowired
    private CaisseHistoryDiagnosticService caisseHistoryDiagnosticService;

    @BeforeEach
    void setUp() {
        reset(caisseHistoryDiagnosticService);
        when(caisseHistoryDiagnosticService.generateDiagnosticReport())
                .thenReturn(CaisseHistoryDiagnosticReportDTO.builder()
                        .readOnlyMode(true)
                        .correctionsApplied(false)
                        .build());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void adminRole_isAuthorized() {
        runAsRole("admin", "ADMIN");
        assertThatCode(() -> auditController.getCaisseHistoryDiagnostic()).doesNotThrowAnyException();
    }

    @Test
    void rciRole_isAuthorized() {
        runAsRole("rci", "RCI");
        assertThatCode(() -> auditController.getCaisseHistoryDiagnostic()).doesNotThrowAnyException();
    }

    @Test
    void caissierRole_isDenied() {
        runAsRole("caissier", "CAISSIER");
        assertThatThrownBy(() -> auditController.getCaisseHistoryDiagnostic())
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void agentTerrainRole_isDenied() {
        runAsRole("terrain", "AGENT_TERRAIN");
        assertThatThrownBy(() -> auditController.getCaisseHistoryDiagnostic())
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void gestionnaireRole_isDenied() {
        runAsRole("gestionnaire", "GESTIONNAIRE");
        assertThatThrownBy(() -> auditController.getCaisseHistoryDiagnostic())
                .isInstanceOf(AccessDeniedException.class);
    }

    private void runAsRole(String username, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        username,
                        "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                )
        );
    }

    @Configuration
    @EnableMethodSecurity
    static class TestSecurityConfig {

        @Bean
        AuditLogRepository auditLogRepository() {
            return mock(AuditLogRepository.class);
        }

        @Bean
        AuditAccessService auditAccessService() {
            return mock(AuditAccessService.class);
        }

        @Bean
        CaisseHistoryDiagnosticService caisseHistoryDiagnosticService() {
            return mock(CaisseHistoryDiagnosticService.class);
        }

        @Bean
        AuditController auditController(
                AuditLogRepository auditLogRepository,
                AuditAccessService auditAccessService,
                CaisseHistoryDiagnosticService caisseHistoryDiagnosticService
        ) {
            return new AuditController(auditLogRepository, auditAccessService, caisseHistoryDiagnosticService);
        }
    }
}