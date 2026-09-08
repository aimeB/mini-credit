package com.mini.credit.service.audit;

import com.mini.credit.dto.audit.CaisseHistoryDiagnosticAnomalyDTO;
import com.mini.credit.dto.audit.CaisseHistoryDiagnosticReportDTO;
import com.mini.credit.entity.agence.Agence;
import com.mini.credit.entity.caisse.Caisse;
import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.entity.referentiel.Role;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.SourceOperationCaisse;
import com.mini.credit.enums.StatutSessionCaisse;
import com.mini.credit.enums.TypeOperationCaisse;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.repository.caisse.CaisseRepository;
import com.mini.credit.repository.caisse.OperationCaisseRepository;
import com.mini.credit.repository.caisse.SessionCaisseRepository;
import com.mini.credit.repository.projection.OperationCaisseDiagnosticProjection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaisseHistoryDiagnosticServiceTest {

    @Mock
    private SessionCaisseRepository sessionCaisseRepository;

    @Mock
    private CaisseRepository caisseRepository;

    @Mock
    private OperationCaisseRepository operationCaisseRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private CaisseHistoryDiagnosticService service;

    @BeforeEach
    void setUp() {
        authenticateAdmin();

        lenient().when(sessionCaisseRepository.findAll()).thenReturn(Collections.emptyList());
        lenient().when(caisseRepository.findAll()).thenReturn(Collections.emptyList());
        lenient().when(operationCaisseRepository.count()).thenReturn(0L);
        lenient().when(operationCaisseRepository.findOperationsWithoutSessionReference()).thenReturn(Collections.emptyList());
        lenient().when(operationCaisseRepository.findOperationsWithMissingSessionReference()).thenReturn(Collections.emptyList());
        lenient().when(operationCaisseRepository.findAdjustmentOperationsWithoutObservation()).thenReturn(Collections.emptyList());
        lenient().when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class))).thenReturn(1L);
        lenient().when(operationCaisseRepository.sumBySessionAndType(any(Long.class), eq(TypeOperationCaisse.ENTREE)))
                .thenReturn(BigDecimal.ZERO);
        lenient().when(operationCaisseRepository.sumBySessionAndType(any(Long.class), eq(TypeOperationCaisse.SORTIE)))
                .thenReturn(BigDecimal.ZERO);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void dryRun_doesNotModifyDataAndKeepsReadOnlyFlags() {
        CaisseHistoryDiagnosticReportDTO report = service.generateDiagnosticReport();

        assertThat(report.isReadOnlyMode()).isTrue();
        assertThat(report.isCorrectionsApplied()).isFalse();
        assertThat(report.getGeneratedBy()).isEqualTo("admin-diagnostic");
        verify(sessionCaisseRepository, never()).save(any());
        verify(caisseRepository, never()).save(any());
        verify(operationCaisseRepository, never()).save(any());
    }

    @Test
    void emitsEmptyDatasetWarningWhenNoSessionNoCaisseAndNoOperationExist() {
        CaisseHistoryDiagnosticReportDTO report = service.generateDiagnosticReport();

        assertThat(report.getAnomalies())
                .extracting(CaisseHistoryDiagnosticAnomalyDTO::getAnomalyType)
                .contains("EMPTY_DATASET_WARNING");
        assertThat(report.getAnomalies())
                .filteredOn(anomaly -> "EMPTY_DATASET_WARNING".equals(anomaly.getAnomalyType()))
                .singleElement()
                .satisfies(anomaly -> {
                    assertThat(anomaly.getSeverity()).isEqualTo("WARNING");
                    assertThat(anomaly.getRecommendation()).isEqualTo(
                            "Aucune caisse, session ou opération caisse n’a été trouvée. Le diagnostic est techniquement exécuté, mais il ne permet pas de conclure sur la conformité métier des données caisse."
                    );
                });
        assertThat(report.getAnomalyCount()).isEqualTo(report.getAnomalies().size());
        assertThat(report.getAnomaliesByType()).containsEntry("EMPTY_DATASET_WARNING", 1L);
    }

    @Test
    void detectsIncorrectTotalEntrees() {
        SessionCaisse session = session(10L, StatutSessionCaisse.OUVERTE);
        session.setTotalEntrees(new BigDecimal("50.00"));
        session.setSoldeTheorique(new BigDecimal("175.00"));
        when(sessionCaisseRepository.findAll()).thenReturn(List.of(session));
        when(caisseRepository.findAll()).thenReturn(List.of(session.getCaisse()));
        when(operationCaisseRepository.count()).thenReturn(1L);
        when(operationCaisseRepository.sumBySessionAndType(10L, TypeOperationCaisse.ENTREE))
                .thenReturn(new BigDecimal("75.00"));

        CaisseHistoryDiagnosticReportDTO report = service.generateDiagnosticReport();

        assertThat(report.getAnomalies())
                .extracting(CaisseHistoryDiagnosticAnomalyDTO::getAnomalyType)
                .contains("TOTAL_ENTREES_INCORRECT");
        assertThat(report.getAnomalies())
            .extracting(CaisseHistoryDiagnosticAnomalyDTO::getAnomalyType)
            .doesNotContain("EMPTY_DATASET_WARNING");
    }

        @Test
        void doesNotEmitEmptyDatasetWarningWhenDataExists() {
        SessionCaisse session = session(80L, StatutSessionCaisse.OUVERTE);
        when(sessionCaisseRepository.findAll()).thenReturn(List.of(session));
        when(caisseRepository.findAll()).thenReturn(List.of(session.getCaisse()));
        when(operationCaisseRepository.count()).thenReturn(1L);

        CaisseHistoryDiagnosticReportDTO report = service.generateDiagnosticReport();

        assertThat(report.getAnomalies())
            .extracting(CaisseHistoryDiagnosticAnomalyDTO::getAnomalyType)
            .doesNotContain("EMPTY_DATASET_WARNING");
        }

    @Test
    void detectsIncorrectTotalSorties() {
        SessionCaisse session = session(20L, StatutSessionCaisse.OUVERTE);
        session.setTotalSorties(new BigDecimal("10.00"));
        session.setSoldeTheorique(new BigDecimal("70.00"));
        when(sessionCaisseRepository.findAll()).thenReturn(List.of(session));
        when(operationCaisseRepository.sumBySessionAndType(20L, TypeOperationCaisse.SORTIE))
                .thenReturn(new BigDecimal("30.00"));

        CaisseHistoryDiagnosticReportDTO report = service.generateDiagnosticReport();

        assertThat(report.getAnomalies())
                .extracting(CaisseHistoryDiagnosticAnomalyDTO::getAnomalyType)
                .contains("TOTAL_SORTIES_INCORRECT");
    }

    @Test
    void detectsIncorrectSoldeTheorique() {
        SessionCaisse session = session(30L, StatutSessionCaisse.OUVERTE);
        session.setTotalEntrees(new BigDecimal("25.00"));
        session.setTotalSorties(new BigDecimal("10.00"));
        session.setSoldeTheorique(new BigDecimal("200.00"));
        when(sessionCaisseRepository.findAll()).thenReturn(List.of(session));
        when(operationCaisseRepository.sumBySessionAndType(30L, TypeOperationCaisse.ENTREE))
                .thenReturn(new BigDecimal("25.00"));
        when(operationCaisseRepository.sumBySessionAndType(30L, TypeOperationCaisse.SORTIE))
                .thenReturn(new BigDecimal("10.00"));

        CaisseHistoryDiagnosticReportDTO report = service.generateDiagnosticReport();

        assertThat(report.getAnomalies())
                .extracting(CaisseHistoryDiagnosticAnomalyDTO::getAnomalyType)
                .contains("SOLDE_THEORIQUE_INCORRECT");
    }

    @Test
    void detectsOperationWithoutSession() {
        when(operationCaisseRepository.findOperationsWithoutSessionReference())
                .thenReturn(List.of(projection(901L, null, 11L, 5L)));

        CaisseHistoryDiagnosticReportDTO report = service.generateDiagnosticReport();

        assertThat(report.getAnomalies())
                .extracting(CaisseHistoryDiagnosticAnomalyDTO::getAnomalyType)
                .contains("OPERATION_WITHOUT_SESSION");
    }

    @Test
    void detectsOperationWithMissingSessionReference() {
        when(operationCaisseRepository.findOperationsWithMissingSessionReference())
                .thenReturn(List.of(projection(902L, 777L, 11L, 5L)));

        CaisseHistoryDiagnosticReportDTO report = service.generateDiagnosticReport();

        assertThat(report.getAnomalies())
                .extracting(CaisseHistoryDiagnosticAnomalyDTO::getAnomalyType)
                .contains("OPERATION_WITH_MISSING_SESSION_REFERENCE");
    }

    @Test
    void detectsDuplicateActiveSessions() {
        SessionCaisse first = session(40L, StatutSessionCaisse.OUVERTE);
        SessionCaisse second = session(41L, StatutSessionCaisse.PRE_CLOTUREE);
        when(sessionCaisseRepository.findAll()).thenReturn(List.of(first, second));

        CaisseHistoryDiagnosticReportDTO report = service.generateDiagnosticReport();

        assertThat(report.getAnomalies())
                .extracting(CaisseHistoryDiagnosticAnomalyDTO::getAnomalyType)
                .contains("MULTIPLE_ACTIVE_SESSIONS_ON_CAISSE");
    }

    @Test
    void detectsMultipleActiveCaissesOnSameSite() {
        Caisse first = caisse(51L, 7L, true);
        Caisse second = caisse(52L, 7L, true);
        when(caisseRepository.findAll()).thenReturn(List.of(first, second));

        CaisseHistoryDiagnosticReportDTO report = service.generateDiagnosticReport();

        assertThat(report.getAnomalies())
                .extracting(CaisseHistoryDiagnosticAnomalyDTO::getAnomalyType)
                .contains("MULTIPLE_ACTIVE_CAISSES_ON_SITE");
    }

    @Test
    void detectsClosedSessionWithoutDateCloture() {
        SessionCaisse session = session(60L, StatutSessionCaisse.CLOTUREE);
        session.setDateCloture(null);
        session.setDateControle(LocalDateTime.now().minusMinutes(30));
        session.setControleValidePar(utilisateur(333L, RoleCode.CONTROLEUR));
        when(sessionCaisseRepository.findAll()).thenReturn(List.of(session));

        CaisseHistoryDiagnosticReportDTO report = service.generateDiagnosticReport();

        assertThat(report.getAnomalies())
                .extracting(CaisseHistoryDiagnosticAnomalyDTO::getAnomalyType)
                .contains("CLOSED_SESSION_WITHOUT_DATE_CLOTURE");
    }

    @Test
    void detectsAdjustmentWithoutObservationAndSessionEcartWithoutObservation() {
        SessionCaisse session = session(70L, StatutSessionCaisse.PRE_CLOTUREE);
        session.setEcartCaisse(new BigDecimal("15.00"));
        session.setObservation("   ");
        when(sessionCaisseRepository.findAll()).thenReturn(List.of(session));
        when(operationCaisseRepository.findAdjustmentOperationsWithoutObservation())
                .thenReturn(List.of(projection(903L, 70L, 11L, 5L)));

        CaisseHistoryDiagnosticReportDTO report = service.generateDiagnosticReport();

        assertThat(report.getAnomalies())
                .extracting(CaisseHistoryDiagnosticAnomalyDTO::getAnomalyType)
                .contains("ADJUSTMENT_OPERATION_WITHOUT_OBSERVATION", "SESSION_WITH_ECART_WITHOUT_OBSERVATION");
    }

    @Test
    void emitsModelLimitationWhenOperationObservationColumnIsMissing() {
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class))).thenReturn(0L);

        CaisseHistoryDiagnosticReportDTO report = service.generateDiagnosticReport();

        assertThat(report.getAnomalies())
                .extracting(CaisseHistoryDiagnosticAnomalyDTO::getAnomalyType)
                .contains("MODEL_LIMITATION_OPERATION_OBSERVATION_COLUMN_MISSING");
    }

    private void authenticateAdmin() {
        Utilisateur admin = utilisateur(1L, RoleCode.ADMIN);
        admin.setUsername("admin-diagnostic");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities())
        );
    }

    private SessionCaisse session(Long sessionId, StatutSessionCaisse statut) {
        SessionCaisse session = new SessionCaisse();
        session.setId(sessionId);
        session.setCaisse(caisse(11L, 5L, true));
        session.setUtilisateur(utilisateur(44L, RoleCode.CAISSIER));
        session.setStatut(statut);
        session.setDateOuverture(LocalDateTime.now().minusHours(4));
        session.setDateCloture(LocalDateTime.now().minusHours(1));
        session.setSoldeOuverture(new BigDecimal("100.00"));
        session.setTotalEntrees(BigDecimal.ZERO);
        session.setTotalSorties(BigDecimal.ZERO);
        session.setSoldeTheorique(new BigDecimal("100.00"));
        return session;
    }

    private Caisse caisse(Long caisseId, Long siteId, boolean actif) {
        Site site = Site.builder()
                .codeSite("SITE-" + siteId)
                .nomSite("Site " + siteId)
                .zone("Zone test")
                .agence(Agence.builder().nomAgence("Agence Test").codeAgence("AG-" + siteId).build())
                .build();
        site.setId(siteId);

        Caisse caisse = Caisse.builder()
                .codeCaisse("CAISSE-" + caisseId)
                .libelle("Caisse " + caisseId)
                .site(site)
                .actif(actif)
                .build();
        caisse.setId(caisseId);
        return caisse;
    }

    private Utilisateur utilisateur(Long id, RoleCode roleCode) {
        Utilisateur utilisateur = Utilisateur.builder()
                .username(roleCode.name().toLowerCase())
                .nomComplet(roleCode.name())
                .motDePasseHash("hash")
                .role(Role.builder().code(roleCode).libelle(roleCode.name()).build())
                .build();
        utilisateur.setId(id);
        return utilisateur;
    }

    private OperationCaisseDiagnosticProjection projection(Long operationId, Long sessionId, Long caisseId, Long siteId) {
        return new OperationCaisseDiagnosticProjection() {
            @Override
            public Long getOperationId() {
                return operationId;
            }

            @Override
            public Long getSessionId() {
                return sessionId;
            }

            @Override
            public Long getCaisseId() {
                return caisseId;
            }

            @Override
            public Long getSiteId() {
                return siteId;
            }
        };
    }
}