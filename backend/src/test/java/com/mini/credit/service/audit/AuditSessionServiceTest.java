package com.mini.credit.service.audit;

import com.mini.credit.entity.audit.AuditLog;
import com.mini.credit.entity.referentiel.Role;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.mapper.AuditLogMapper;
import com.mini.credit.repository.audit.AuditLogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditSessionServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private AuditAccessService auditAccessService;

    @Mock
    private AuditLogMapper auditLogMapper;

    @InjectMocks
    private AuditServiceImpl auditService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void logWithValues_shouldCaptureUserRoleAndHttpContext_whenAvailable() {
        Utilisateur user = Utilisateur.builder()
                .username("caissier.audit")
                .role(Role.builder().code(RoleCode.CAISSIER).build())
                .motDePasseHash("x")
                .build();
        user.setId(77L);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.10");
        request.addHeader("X-Forwarded-For", "10.10.10.5");
        request.addHeader("User-Agent", "JUnit-Agent");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        auditService.logWithValues(
                AuditAction.OUVERTURE_SESSION,
                "SessionCaisse",
                12L,
                true,
                "Ouverture session",
                "{\"ancienStatut\":null}",
                "{\"nouveauStatut\":\"OUVERTE\"}",
                null
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog log = captor.getValue();
        assertThat(log.getUserId()).isEqualTo(77L);
        assertThat(log.getUsername()).isEqualTo("caissier.audit");
        assertThat(log.getRoleCode()).isEqualTo(RoleCode.CAISSIER);
        assertThat(log.getIpAddress()).isEqualTo("10.10.10.5");
        assertThat(log.getUserAgent()).isEqualTo("JUnit-Agent");
    }

    @Test
    void logWithValues_shouldNotFail_whenNoRequestContext() {
        auditService.logWithValues(
                AuditAction.REFUS_TRANSITION,
                "SessionCaisse",
                13L,
                false,
                "Transition refusée",
                null,
                null,
                "Etat invalide"
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog log = captor.getValue();
        assertThat(log.getIpAddress()).isNull();
        assertThat(log.getAction()).isEqualTo(AuditAction.REFUS_TRANSITION);
        assertThat(log.getSuccess()).isFalse();
    }
}
