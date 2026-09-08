package com.mini.credit.controller;

import com.mini.credit.controller.audit.AuditController;
import com.mini.credit.dto.caisse.CloseSessionCaisseRequest;
import com.mini.credit.dto.audit.CaisseHistoryDiagnosticReportDTO;
import com.mini.credit.dto.caisse.CaisseCreateRequest;
import com.mini.credit.dto.caisse.CaisseResponse;
import com.mini.credit.dto.caisse.OperationCaisseRequest;
import com.mini.credit.dto.caisse.OperationCaisseResponse;
import com.mini.credit.dto.caisse.RequalificationNatureFinancementRequest;
import com.mini.credit.dto.caisse.SessionCaisseOpenRequest;
import com.mini.credit.dto.caisse.SessionCaisseResponse;
import com.mini.credit.dto.caisse.SessionCaisseValidationRequest;
import com.mini.credit.dto.document.TicketDuplicataRequest;
import com.mini.credit.dto.document.TicketPrintRequest;
import com.mini.credit.dto.document.TicketRecuResponse;
import com.mini.credit.dto.document.TicketVerificationResponse;
import com.mini.credit.dto.credit.CreditResponse;
import com.mini.credit.dto.credit.DecaissementCreditRequest;
import com.mini.credit.dto.caisse.EcartCaisseDTO;
import com.mini.credit.entity.audit.AuditLog;
import com.mini.credit.repository.audit.AuditLogRepository;
import com.mini.credit.service.CreditService;
import com.mini.credit.service.CaisseService;
import com.mini.credit.service.EcartCaisseService;
import com.mini.credit.service.OperationCaisseService;
import com.mini.credit.service.SessionCaisseAnomalieService;
import com.mini.credit.service.SessionCaisseService;
import com.mini.credit.service.TicketRecuService;
import com.mini.credit.service.audit.AuditService;
import com.mini.credit.service.audit.AuditAccessService;
import com.mini.credit.service.audit.CaisseHistoryDiagnosticService;
import com.mini.credit.service.security.ScopeService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(FinePermissionsSecurityIntegrationTest.TestSecurityConfig.class)
@DisplayName("Sécurité permissions fines - intégration")
class FinePermissionsSecurityIntegrationTest {

    @Autowired
    private SessionCaisseController sessionCaisseController;

    @Autowired
    private CaisseSessionWorkflowController caisseSessionWorkflowController;

    @Autowired
    private OperationCaisseController operationCaisseController;

    @Autowired
    private EcartCaisseController ecartCaisseController;

    @Autowired
    private AuditController auditController;

    @Autowired
    private CreditController creditController;

    @Autowired
    private CaisseController caisseController;

    @Autowired
    private TicketRecuController ticketRecuController;

    @Autowired
    private SessionCaisseService sessionCaisseService;

    @Autowired
    private SessionCaisseAnomalieService sessionCaisseAnomalieService;

    @Autowired
    private OperationCaisseService operationCaisseService;

    @Autowired
    private EcartCaisseService ecartCaisseService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private AuditAccessService auditAccessService;

    @Autowired
    private CaisseHistoryDiagnosticService caisseHistoryDiagnosticService;

    @Autowired
    private CreditService creditService;

    @Autowired
    private CaisseService caisseService;

    @Autowired
    private TicketRecuService ticketRecuService;

    @Autowired
    private ScopeService scopeService;

    @BeforeEach
    void setUp() {
        reset(sessionCaisseService, sessionCaisseAnomalieService, operationCaisseService, ecartCaisseService, auditLogRepository, auditAccessService, caisseHistoryDiagnosticService, creditService, scopeService, caisseService, ticketRecuService);

        when(sessionCaisseService.ouvrir(any())).thenReturn(SessionCaisseResponse.builder().id(1L).build());
        when(sessionCaisseService.preCloturer(anyLong(), any())).thenReturn(SessionCaisseResponse.builder().id(1L).build());
        when(sessionCaisseService.validerControle(anyLong(), any())).thenReturn(SessionCaisseResponse.builder().id(1L).build());
        when(sessionCaisseService.cloturerFinale(anyLong(), any())).thenReturn(SessionCaisseResponse.builder().id(1L).build());
        when(sessionCaisseService.getById(anyLong())).thenReturn(SessionCaisseResponse.builder().id(1L).build());

        when(operationCaisseService.enregistrer(any())).thenReturn(OperationCaisseResponse.builder().id(1L).build());
        when(operationCaisseService.requalifierNatureFinancement(anyLong(), any())).thenReturn(OperationCaisseResponse.builder().id(1L).build());
        when(operationCaisseService.getJournal(any(), any(org.springframework.data.domain.Pageable.class)))
            .thenReturn(org.springframework.data.domain.Page.empty());
        when(ecartCaisseService.getAll()).thenReturn(List.of(EcartCaisseDTO.builder().id(1L).build()));
        when(auditLogRepository.findByEntityType(anyString(), any(org.springframework.data.domain.Pageable.class)))
            .thenReturn(org.springframework.data.domain.Page.empty());
        when(auditAccessService.getEntityAuditTrailScoped(anyString(), anyLong(), anyInt(), anyInt()))
            .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(new AuditLog())));
        when(caisseHistoryDiagnosticService.generateDiagnosticReport())
            .thenReturn(CaisseHistoryDiagnosticReportDTO.builder().readOnlyMode(true).correctionsApplied(false).build());
        when(creditService.decaisserCredit(anyLong(), any())).thenReturn(CreditResponse.builder().id(1L).build());
        when(caisseService.create(any())).thenReturn(CaisseResponse.builder().id(1L).codeCaisse("CAI-TST").build());
        when(ticketRecuService.getById(anyLong())).thenReturn(TicketRecuResponse.builder().id(1L).build());
        when(ticketRecuService.marquerImpression(anyLong(), any())).thenReturn(TicketRecuResponse.builder().id(1L).build());
        when(ticketRecuService.genererDuplicata(anyLong(), any())).thenReturn(TicketRecuResponse.builder().id(1L).build());
        when(ticketRecuService.getPrintableHtml(anyLong(), any(boolean.class))).thenReturn("<html></html>");
        when(ticketRecuService.verify(anyString())).thenReturn(TicketVerificationResponse.builder().valide(true).build());
        when(ticketRecuService.canReadTicket(anyLong())).thenReturn(true);
        when(ticketRecuService.canReadMembreTickets(anyLong())).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void caissierPermissions_shouldOpenAndPreCloseButNotValidateOrFinalize() {
        runAsRole("caissier", "CAISSIER");
        assertThatCode(() -> sessionCaisseController.ouvrir(openRequest())).doesNotThrowAnyException();

        runAs("caissier", "SESSION_CAISSE_PRE_CLOSE");
        assertThatCode(() -> caisseSessionWorkflowController.cloturer(1L, closeWorkflowRequest())).doesNotThrowAnyException();

        runAs("caissier", "SESSION_CAISSE_PRE_CLOSE");
        assertThatThrownBy(() -> caisseSessionWorkflowController.validerControle(1L, new SessionCaisseValidationRequest()))
            .isInstanceOf(AccessDeniedException.class);

        runAs("caissier", "SESSION_CAISSE_PRE_CLOSE");
        assertThatThrownBy(() -> caisseSessionWorkflowController.cloturerFinale(1L, new SessionCaisseValidationRequest()))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void controleurPermissions_shouldValidateButNotFinalizeOrDisburseCredit() {
        runAs("controleur", "SESSION_CAISSE_CONTROL_VALIDATE");
        assertThatCode(() -> caisseSessionWorkflowController.validerControle(1L, new SessionCaisseValidationRequest()))
            .doesNotThrowAnyException();

        runAs("controleur", "SESSION_CAISSE_CONTROL_VALIDATE");
        assertThatThrownBy(() -> caisseSessionWorkflowController.cloturerFinale(1L, new SessionCaisseValidationRequest()))
            .isInstanceOf(AccessDeniedException.class);

        runAs("controleur", "CONTROLEUR_CREDITS_VALIDATE");
        assertThatThrownBy(() -> creditController.decaisser(1L, new DecaissementCreditRequest()))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void chefBureauPermissions_shouldFinalizeButNotValidateControl() {
        runAs("chef", "SESSION_CAISSE_FINAL_CLOSE");
        assertThatCode(() -> caisseSessionWorkflowController.cloturerFinale(1L, new SessionCaisseValidationRequest()))
            .doesNotThrowAnyException();

        runAs("chef", "SESSION_CAISSE_FINAL_CLOSE");
        assertThatThrownBy(() -> caisseSessionWorkflowController.validerControle(1L, new SessionCaisseValidationRequest()))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void chefBureauPermissions_shouldReadButNotValidateControl() {
        runAs("chef", "CAISSE_READ");
        assertThatCode(() -> caisseSessionWorkflowController.getById(1L)).doesNotThrowAnyException();

        runAs("chef", "AUDIT_READ");
        assertThatCode(() -> auditController.getEntityAuditTrail("SessionCaisse", 1L, 0, 10)).doesNotThrowAnyException();

        runAs("chef", "CAISSE_READ", "AUDIT_READ");
        assertThatThrownBy(() -> caisseSessionWorkflowController.validerControle(1L, new SessionCaisseValidationRequest()))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rciPermissions_shouldReadAuditAndCashButNotCreateOperations() {
        runAs("rci", "CAISSE_READ");
        assertThatCode(() -> caisseSessionWorkflowController.getById(1L)).doesNotThrowAnyException();

        runAs("rci", "AUDIT_READ", "OPERATION_CAISSE_READ");
        assertThatCode(() -> auditController.getEntityAuditTrail("SessionCaisse", 1L, 0, 10)).doesNotThrowAnyException();

        runAs("rci", "OPERATION_CAISSE_READ");
        assertThatThrownBy(() -> operationCaisseController.enregistrer(new OperationCaisseRequest()))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rciNePeutPasCreerOperationCaisseGenerique() {
        runAs("rci", "ROLE_RCI", "OPERATION_CAISSE_CREATE", "OPERATION_CAISSE_READ");

        assertThatThrownBy(() -> operationCaisseController.enregistrer(new OperationCaisseRequest()))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rciNePeutPasModifierOperationCaisseGenerique() {
        runAs("rci", "ROLE_RCI", "OPERATION_CAISSE_UPDATE", "OPERATION_CAISSE_READ");

        assertThatThrownBy(() -> operationCaisseController.requalifierNatureFinancement(1L, new RequalificationNatureFinancementRequest()))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rciPeutConsulterJournalCaisse() {
        runAs("rci", "ROLE_RCI", "JOURNAL_CAISSE_READ");

        assertThatCode(() -> operationCaisseController.getJournal(
            null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, 0, 20, "dateOperation,desc"
        )).doesNotThrowAnyException();
    }

    @Test
    void rciPeutConsulterRapportsEcarts() {
        runAsRole("rci", "RCI");

        assertThatCode(() -> ecartCaisseController.getAll()).doesNotThrowAnyException();
    }

    @Test
    void caissierNePeutPasCreerOperationLibreGenerique() {
        runAs("caissier", "ROLE_CAISSIER", "OPERATION_CAISSE_CREATE");

        assertThatThrownBy(() -> operationCaisseController.enregistrer(new OperationCaisseRequest()))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void agentTerrain_shouldBeDeniedOnCashAndAuditEndpoints() {
        runAsRole("terrain", "AGENT_TERRAIN");
        assertThatThrownBy(() -> caisseSessionWorkflowController.getById(1L))
            .isInstanceOf(AccessDeniedException.class);

        runAsRole("terrain", "AGENT_TERRAIN");
        assertThatThrownBy(() -> auditController.getEntityAuditTrail("SessionCaisse", 1L, 0, 10))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void missingPermission_shouldReturn403EquivalentAtMethodSecurityLayer() {
        runAs("reader", "OPERATION_CAISSE_READ");
        assertThatThrownBy(() -> operationCaisseController.enregistrer(new OperationCaisseRequest()))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void adminRole_canCallCreateCaisseEndpoint() {
        runAsRole("admin", "ADMIN");
        assertThatCode(() -> caisseController.create(caisseCreateRequest()))
                .doesNotThrowAnyException();
    }

    @Test
    void caissierWithoutCaisseCreate_isDeniedOnCreateCaisseEndpoint() {
        runAs("caissier", "CAISSE_READ");
        assertThatThrownBy(() -> caisseController.create(caisseCreateRequest()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void controleurPermissions_doNotGrantCaisseCreateImplicitly() {
        runAs("controleur", "CAISSE_READ", "SESSION_CAISSE_CONTROL_VALIDATE");
        assertThatThrownBy(() -> caisseController.create(caisseCreateRequest()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rciLitTicketMaisNePeutPasImprimerOuGenererDuplicata() {
        runAs("rci", "ROLE_RCI", "TICKET_RECU_READ", "TICKET_RECU_VERIFY");
        assertThatCode(() -> ticketRecuController.getById(1L)).doesNotThrowAnyException();
        assertThatCode(() -> ticketRecuController.verify("ABCD-2345")).doesNotThrowAnyException();

        assertThatThrownBy(() -> ticketRecuController.marquerImpression(1L, new TicketPrintRequest()))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> ticketRecuController.genererDuplicata(1L, duplicataRequest()))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void caissierImprimeTicketAutoriseMaisNeCreePasOperationLibre() {
        runAs("caissier", "ROLE_CAISSIER", "TICKET_RECU_READ", "TICKET_RECU_PRINT", "OPERATION_CAISSE_CREATE");

        assertThatCode(() -> ticketRecuController.marquerImpression(1L, new TicketPrintRequest()))
            .doesNotThrowAnyException();
        assertThatCode(() -> ticketRecuController.getPrintableHtml(1L, false))
            .doesNotThrowAnyException();
        assertThatThrownBy(() -> operationCaisseController.enregistrer(new OperationCaisseRequest()))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void gestionnaireNePeutPasImprimerTicketSansPermissionPrint() {
        runAs("gestionnaire", "TICKET_RECU_READ", "TICKET_RECU_VERIFY");

        assertThatThrownBy(() -> ticketRecuController.marquerImpression(1L, new TicketPrintRequest()))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void memberNePeutPasLireTicketAutrui() {
        when(ticketRecuService.canReadTicket(1L)).thenReturn(false);
        runAs("member", "ROLE_MEMBER", "TICKET_RECU_READ", "TICKET_RECU_VERIFY");

        assertThatThrownBy(() -> ticketRecuController.getById(1L))
            .isInstanceOf(AccessDeniedException.class);
    }

    private void runAs(String username, String... authorities) {
        List<SimpleGrantedAuthority> grantedAuthorities = Arrays.stream(authorities)
            .map(SimpleGrantedAuthority::new)
            .toList();
        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(username, "n/a", grantedAuthorities)
        );
    }

    private void runAsRole(String username, String role) {
        runAs(username, "ROLE_" + role);
    }

    private SessionCaisseOpenRequest openRequest() {
        SessionCaisseOpenRequest request = new SessionCaisseOpenRequest();
        request.setCaisseId(1L);
        request.setDateOuverture(LocalDateTime.now());
        request.setSoldeOuverture(new BigDecimal("100.00"));
        return request;
    }

    private CaisseCreateRequest caisseCreateRequest() {
        CaisseCreateRequest request = new CaisseCreateRequest();
        request.setLibelle("Caisse test");
        request.setSiteId(1L);
        request.setDevise("CDF");
        request.setCaissierAffecteId(10L);
        return request;
    }

    private CloseSessionCaisseRequest closeWorkflowRequest() {
        CloseSessionCaisseRequest request = new CloseSessionCaisseRequest();
        request.setSoldePhysique(new BigDecimal("100.00"));
        request.setObservation("Cloture test");
        return request;
    }

    private TicketDuplicataRequest duplicataRequest() {
        TicketDuplicataRequest request = new TicketDuplicataRequest();
        request.setMotif("Perte ticket original");
        return request;
    }

    @Configuration
    @EnableMethodSecurity
    static class TestSecurityConfig {
        @Bean
        SessionCaisseService sessionCaisseService() {
            return mock(SessionCaisseService.class);
        }

        @Bean
        SessionCaisseAnomalieService sessionCaisseAnomalieService() {
            return mock(SessionCaisseAnomalieService.class);
        }

        @Bean
        OperationCaisseService operationCaisseService() {
            return mock(OperationCaisseService.class);
        }

        @Bean
        EcartCaisseService ecartCaisseService() {
            return mock(EcartCaisseService.class);
        }

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
        CreditService creditService() {
            return mock(CreditService.class);
        }

        @Bean
        CaisseService caisseService() {
            return mock(CaisseService.class);
        }

        @Bean
        TicketRecuService ticketRecuService() {
            return mock(TicketRecuService.class);
        }

        @Bean
        ScopeService scopeService() {
            return mock(ScopeService.class);
        }

        @Bean
        SessionCaisseController sessionCaisseController(SessionCaisseService sessionCaisseService,
                                                        SessionCaisseAnomalieService sessionCaisseAnomalieService) {
            return new SessionCaisseController(sessionCaisseService, sessionCaisseAnomalieService);
        }

        @Bean
        CaisseSessionWorkflowController caisseSessionWorkflowController(
                SessionCaisseService sessionCaisseService,
                SessionCaisseAnomalieService sessionCaisseAnomalieService
        ) {
            return new CaisseSessionWorkflowController(sessionCaisseService, sessionCaisseAnomalieService);
        }

        @Bean
        AuditService auditService() {
            return mock(AuditService.class);
        }

        @Bean
        OperationCaisseController operationCaisseController(OperationCaisseService operationCaisseService, AuditService auditService) {
            return new OperationCaisseController(operationCaisseService, auditService);
        }

        @Bean
        EcartCaisseController ecartCaisseController(EcartCaisseService ecartCaisseService) {
            return new EcartCaisseController(ecartCaisseService);
        }

        @Bean
        AuditController auditController(
                AuditLogRepository auditLogRepository,
                AuditAccessService auditAccessService,
                CaisseHistoryDiagnosticService caisseHistoryDiagnosticService
        ) {
            return new AuditController(auditLogRepository, auditAccessService, caisseHistoryDiagnosticService);
        }

        @Bean
        CreditController creditController(CreditService creditService, ScopeService scopeService) {
            return new CreditController(creditService, scopeService);
        }

        @Bean
        CaisseController caisseController(CaisseService caisseService, SessionCaisseService sessionCaisseService) {
            return new CaisseController(caisseService, sessionCaisseService);
        }

        @Bean
        TicketRecuController ticketRecuController(TicketRecuService ticketRecuService) {
            return new TicketRecuController(ticketRecuService);
        }
    }
}
