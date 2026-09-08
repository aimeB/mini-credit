package com.mini.credit.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini.credit.entity.agence.Agence;
import com.mini.credit.dto.caisse.SessionCaisseCloseRequest;
import com.mini.credit.dto.caisse.SessionCaisseOpenRequest;
import com.mini.credit.dto.caisse.SessionCaisseResponse;
import com.mini.credit.entity.caisse.Caisse;
import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.entity.referentiel.Role;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.SourceOperationCaisse;
import com.mini.credit.enums.StatutSessionCaisse;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.enums.security.PermissionCode;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.mapper.CashMapper;
import com.mini.credit.repository.caisse.CaisseRepository;
import com.mini.credit.repository.caisse.DepenseCaisseRepository;
import com.mini.credit.repository.caisse.EcartCaisseRepository;
import com.mini.credit.repository.caisse.OperationCaisseRepository;
import com.mini.credit.repository.caisse.SessionCaisseAnomalieRepository;
import com.mini.credit.repository.caisse.SessionCaisseRepository;
import com.mini.credit.repository.utilisateur.UtilisateurRepository;
import com.mini.credit.service.EcartThresholdConfigService;
import com.mini.credit.service.WorkflowTaskService;
import com.mini.credit.service.audit.AuditService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SessionCaisseServiceImplTest {

    @Mock private SessionCaisseRepository sessionCaisseRepository;
    @Mock private CaisseRepository caisseRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private OperationCaisseRepository operationCaisseRepository;
    @Mock private SessionCaisseAnomalieRepository sessionCaisseAnomalieRepository;
    @Mock private EcartCaisseRepository ecartCaisseRepository;
    @Mock private DepenseCaisseRepository depenseCaisseRepository;
    @Mock private CashMapper cashMapper;
    @Mock private EcartThresholdConfigService ecartThresholdConfigService;
    @Mock private WorkflowTaskService workflowTaskService;
    @Mock private AuditService auditService;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks private SessionCaisseServiceImpl service;

    private Caisse caisse;
    private Utilisateur caissier;

    @BeforeEach
    void setUp() {
        Agence agence = agence(1L);
        caisse = Caisse.builder().actif(true).agence(agence).build();
        caisse.setId(11L);

        caissier = utilisateur(100L, RoleCode.CAISSIER, agence);
        authenticate(caissier);

        when(utilisateurRepository.findByIdWithValidationContext(anyLong())).thenReturn(Optional.empty());
        when(utilisateurRepository.findByUsernameWithValidationContext(anyString())).thenReturn(Optional.empty());

        when(cashMapper.toResponse(any(SessionCaisse.class))).thenAnswer(invocation -> {
            SessionCaisse s = invocation.getArgument(0);
            return SessionCaisseResponse.builder()
                    .id(s.getId())
                    .statut(s.getStatut())
                    .build();
        });
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void ouvertureSession_ok_ifNoSessionOpen() {
        SessionCaisseOpenRequest request = openRequest();

        when(caisseRepository.findById(11L)).thenReturn(Optional.of(caisse));
        when(sessionCaisseRepository.existsByCaisseIdAndStatutIn(eq(11L), any(List.class))).thenReturn(false);
        when(sessionCaisseRepository.existsByUtilisateurIdAndStatutIn(eq(100L), any(List.class))).thenReturn(false);
        when(sessionCaisseRepository.findByCaisseIdAndDateComptableBeforeAndStatutInOrderByDateComptableAscDateOuvertureAsc(eq(11L), any(LocalDate.class), any(List.class)))
                .thenReturn(Collections.emptyList());
        when(sessionCaisseRepository.existsByCaisseIdAndDateComptableAndStatutIn(eq(11L), any(LocalDate.class), any(List.class))).thenReturn(false);
        when(sessionCaisseRepository.findClosedSessionsForOpening(eq(11L), any(LocalDate.class), any(LocalDateTime.class))).thenReturn(Collections.emptyList());
        when(sessionCaisseRepository.save(any(SessionCaisse.class))).thenAnswer(inv -> {
            SessionCaisse s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        SessionCaisseResponse response = service.ouvrir(request);

        assertThat(response.getStatut()).isEqualTo(StatutSessionCaisse.OUVERTE);
    }

    @Test
    void ouverturePremiereSession_soldeManuel500000_autoriseePourCaissier() {
        SessionCaisseOpenRequest request = openRequest();
        request.setSoldeOuverture(new BigDecimal("500000.00"));

        when(caisseRepository.findById(11L)).thenReturn(Optional.of(caisse));
        when(sessionCaisseRepository.existsByCaisseIdAndStatutIn(eq(11L), any(List.class))).thenReturn(false);
        when(sessionCaisseRepository.existsByUtilisateurIdAndStatutIn(eq(100L), any(List.class))).thenReturn(false);
        when(sessionCaisseRepository.findByCaisseIdAndDateComptableBeforeAndStatutInOrderByDateComptableAscDateOuvertureAsc(eq(11L), any(LocalDate.class), any(List.class)))
                .thenReturn(Collections.emptyList());
        when(sessionCaisseRepository.existsByCaisseIdAndDateComptableAndStatutIn(eq(11L), any(LocalDate.class), any(List.class))).thenReturn(false);
        when(sessionCaisseRepository.findClosedSessionsForOpening(eq(11L), any(LocalDate.class), any(LocalDateTime.class))).thenReturn(Collections.emptyList());
        when(sessionCaisseRepository.save(any(SessionCaisse.class))).thenAnswer(inv -> {
            SessionCaisse s = inv.getArgument(0);
            s.setId(101L);
            return s;
        });

        service.ouvrir(request);

        ArgumentCaptor<SessionCaisse> captor = ArgumentCaptor.forClass(SessionCaisse.class);
        verify(sessionCaisseRepository).save(captor.capture());
        assertThat(captor.getValue().getSoldeOuverture()).isEqualByComparingTo("500000.00");
    }

    @Test
    void premiereSessionOuvreAZeroSiAucuneSessionPrecedente() {
        SessionCaisseOpenRequest request = openRequest();
        request.setSoldeOuverture(BigDecimal.ZERO);

        when(caisseRepository.findById(11L)).thenReturn(Optional.of(caisse));
        when(sessionCaisseRepository.existsByCaisseIdAndStatutIn(eq(11L), any(List.class))).thenReturn(false);
        when(sessionCaisseRepository.existsByUtilisateurIdAndStatutIn(eq(100L), any(List.class))).thenReturn(false);
        when(sessionCaisseRepository.findByCaisseIdAndDateComptableBeforeAndStatutInOrderByDateComptableAscDateOuvertureAsc(eq(11L), any(LocalDate.class), any(List.class)))
                .thenReturn(Collections.emptyList());
        when(sessionCaisseRepository.existsByCaisseIdAndDateComptableAndStatutIn(eq(11L), any(LocalDate.class), any(List.class))).thenReturn(false);
        when(sessionCaisseRepository.findClosedSessionsForOpening(eq(11L), any(LocalDate.class), any(LocalDateTime.class))).thenReturn(Collections.emptyList());
        when(sessionCaisseRepository.save(any(SessionCaisse.class))).thenAnswer(inv -> inv.getArgument(0));

        service.ouvrir(request);

        ArgumentCaptor<SessionCaisse> captor = ArgumentCaptor.forClass(SessionCaisse.class);
        verify(sessionCaisseRepository).save(captor.capture());
        assertThat(captor.getValue().getSoldeOuverture()).isEqualByComparingTo("0.00");
        assertThat(captor.getValue().getSoldeTheorique()).isEqualByComparingTo("0.00");
    }

    @Test
    void nouvelleSessionReprendSoldeCloturePrecedente() {
        SessionCaisseOpenRequest request = openRequest();
        request.setSoldeOuverture(null);

        SessionCaisse previousClosed = session(88L, StatutSessionCaisse.CLOTUREE);
        previousClosed.setSoldeTheorique(new BigDecimal("450000.00"));
        previousClosed.setSoldePhysique(new BigDecimal("450000.00"));

        when(caisseRepository.findById(11L)).thenReturn(Optional.of(caisse));
        when(sessionCaisseRepository.existsByCaisseIdAndStatutIn(eq(11L), any(List.class))).thenReturn(false);
        when(sessionCaisseRepository.existsByUtilisateurIdAndStatutIn(eq(100L), any(List.class))).thenReturn(false);
        when(sessionCaisseRepository.findByCaisseIdAndDateComptableBeforeAndStatutInOrderByDateComptableAscDateOuvertureAsc(eq(11L), any(LocalDate.class), any(List.class)))
                .thenReturn(Collections.emptyList());
        when(sessionCaisseRepository.existsByCaisseIdAndDateComptableAndStatutIn(eq(11L), any(LocalDate.class), any(List.class))).thenReturn(false);
        when(sessionCaisseRepository.findClosedSessionsForOpening(eq(11L), any(LocalDate.class), any(LocalDateTime.class))).thenReturn(List.of(previousClosed));
        when(sessionCaisseRepository.save(any(SessionCaisse.class))).thenAnswer(inv -> inv.getArgument(0));

        service.ouvrir(request);

        ArgumentCaptor<SessionCaisse> captor = ArgumentCaptor.forClass(SessionCaisse.class);
        verify(sessionCaisseRepository).save(captor.capture());
        assertThat(captor.getValue().getSoldeOuverture()).isEqualByComparingTo("450000.00");
        assertThat(captor.getValue().getSoldeTheorique()).isEqualByComparingTo("450000.00");
    }

    @Test
    void soldeOuvertureFrontendZeroIgnoreSiSessionPrecedenteExiste() {
        SessionCaisseOpenRequest request = openRequest();
        request.setSoldeOuverture(BigDecimal.ZERO);

        SessionCaisse previousClosed = session(99L, StatutSessionCaisse.CLOTUREE);
        previousClosed.setSoldeTheorique(new BigDecimal("1048000.00"));
        previousClosed.setSoldePhysique(new BigDecimal("1048000.00"));

        when(caisseRepository.findById(11L)).thenReturn(Optional.of(caisse));
        when(sessionCaisseRepository.existsByCaisseIdAndStatutIn(eq(11L), any(List.class))).thenReturn(false);
        when(sessionCaisseRepository.existsByUtilisateurIdAndStatutIn(eq(100L), any(List.class))).thenReturn(false);
        when(sessionCaisseRepository.findByCaisseIdAndDateComptableBeforeAndStatutInOrderByDateComptableAscDateOuvertureAsc(eq(11L), any(LocalDate.class), any(List.class)))
                .thenReturn(Collections.emptyList());
        when(sessionCaisseRepository.existsByCaisseIdAndDateComptableAndStatutIn(eq(11L), any(LocalDate.class), any(List.class))).thenReturn(false);
        when(sessionCaisseRepository.findClosedSessionsForOpening(eq(11L), any(LocalDate.class), any(LocalDateTime.class))).thenReturn(List.of(previousClosed));
        when(sessionCaisseRepository.save(any(SessionCaisse.class))).thenAnswer(inv -> inv.getArgument(0));

        service.ouvrir(request);

        ArgumentCaptor<SessionCaisse> captor = ArgumentCaptor.forClass(SessionCaisse.class);
        verify(sessionCaisseRepository).save(captor.capture());
        assertThat(captor.getValue().getSoldeOuverture()).isEqualByComparingTo("1048000.00");
        assertThat(captor.getValue().getSoldeTheorique()).isEqualByComparingTo("1048000.00");
    }

    @Test
    void soldeOuvertureFrontendDifferentIgnoreMemeAvecPermission() {
        SessionCaisseOpenRequest request = openRequest();
        request.setSoldeOuverture(new BigDecimal("500000.00"));
        request.setObservation("Ajustement validé pour incident de comptage.");

        SessionCaisse previousClosed = session(120L, StatutSessionCaisse.CLOTUREE);
        previousClosed.setSoldeTheorique(new BigDecimal("450000.00"));
        previousClosed.setSoldePhysique(new BigDecimal("450000.00"));

        authenticateWithPermission(caissier, PermissionCode.SESSION_CAISSE_OPEN_OVERRIDE.name());

        when(caisseRepository.findById(11L)).thenReturn(Optional.of(caisse));
        when(sessionCaisseRepository.existsByCaisseIdAndStatutIn(eq(11L), any(List.class))).thenReturn(false);
        when(sessionCaisseRepository.existsByUtilisateurIdAndStatutIn(eq(100L), any(List.class))).thenReturn(false);
        when(sessionCaisseRepository.findByCaisseIdAndDateComptableBeforeAndStatutInOrderByDateComptableAscDateOuvertureAsc(eq(11L), any(LocalDate.class), any(List.class)))
                .thenReturn(Collections.emptyList());
        when(sessionCaisseRepository.existsByCaisseIdAndDateComptableAndStatutIn(eq(11L), any(LocalDate.class), any(List.class))).thenReturn(false);
        when(sessionCaisseRepository.findClosedSessionsForOpening(eq(11L), any(LocalDate.class), any(LocalDateTime.class))).thenReturn(List.of(previousClosed));
        when(sessionCaisseRepository.save(any(SessionCaisse.class))).thenAnswer(inv -> inv.getArgument(0));

        service.ouvrir(request);

        ArgumentCaptor<SessionCaisse> captor = ArgumentCaptor.forClass(SessionCaisse.class);
        verify(sessionCaisseRepository).save(captor.capture());
        assertThat(captor.getValue().getSoldeOuverture()).isEqualByComparingTo("450000.00");
        assertThat(captor.getValue().getObservation()).isEqualTo("Ajustement validé pour incident de comptage.");
    }

    @Test
    void plusieursSessionsMemeJourReportentSoldeEnCascade() {
        LocalDateTime ouvertureSession2 = LocalDateTime.now().withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime ouvertureSession3 = ouvertureSession2.plusHours(2);
        SessionCaisseOpenRequest requestSession2 = openRequest(ouvertureSession2);
        SessionCaisseOpenRequest requestSession3 = openRequest(ouvertureSession3);
        requestSession2.setSoldeOuverture(BigDecimal.ZERO);
        requestSession3.setSoldeOuverture(BigDecimal.ZERO);

        SessionCaisse session1Cloturee = session(201L, StatutSessionCaisse.CLOTUREE);
        session1Cloturee.setDateComptable(LocalDate.now());
        session1Cloturee.setDateCloture(ouvertureSession2.minusMinutes(15));
        session1Cloturee.setSoldeTheorique(new BigDecimal("1048000.00"));
        session1Cloturee.setSoldePhysique(new BigDecimal("1048000.00"));

        SessionCaisse session2Cloturee = session(202L, StatutSessionCaisse.CLOTUREE);
        session2Cloturee.setDateComptable(LocalDate.now());
        session2Cloturee.setDateCloture(ouvertureSession3.minusMinutes(10));
        session2Cloturee.setSoldeTheorique(new BigDecimal("1248000.00"));
        session2Cloturee.setSoldePhysique(new BigDecimal("1248000.00"));

        when(caisseRepository.findById(11L)).thenReturn(Optional.of(caisse));
        when(sessionCaisseRepository.existsByCaisseIdAndStatutIn(eq(11L), any(List.class))).thenReturn(false);
        when(sessionCaisseRepository.existsByUtilisateurIdAndStatutIn(eq(100L), any(List.class))).thenReturn(false);
        when(sessionCaisseRepository.findByCaisseIdAndDateComptableBeforeAndStatutInOrderByDateComptableAscDateOuvertureAsc(eq(11L), any(LocalDate.class), any(List.class)))
                .thenReturn(Collections.emptyList());
        when(sessionCaisseRepository.existsByCaisseIdAndDateComptableAndStatutIn(eq(11L), any(LocalDate.class), any(List.class))).thenReturn(false);
        when(sessionCaisseRepository.findClosedSessionsForOpening(eq(11L), any(LocalDate.class), any(LocalDateTime.class)))
                .thenReturn(List.of(session1Cloturee), List.of(session2Cloturee));
        when(sessionCaisseRepository.save(any(SessionCaisse.class))).thenAnswer(inv -> inv.getArgument(0));

        service.ouvrir(requestSession2);
        service.ouvrir(requestSession3);

        ArgumentCaptor<SessionCaisse> captor = ArgumentCaptor.forClass(SessionCaisse.class);
        verify(sessionCaisseRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getSoldeOuverture()).isEqualByComparingTo("1048000.00");
        assertThat(captor.getAllValues().get(0).getSoldeTheorique()).isEqualByComparingTo("1048000.00");
        assertThat(captor.getAllValues().get(1).getSoldeOuverture()).isEqualByComparingTo("1248000.00");
        assertThat(captor.getAllValues().get(1).getSoldeTheorique()).isEqualByComparingTo("1248000.00");
    }

    @Test
    void derniereSessionClotureeMemeJourEstSourceDuReport() {
        LocalDateTime dateOuverture = LocalDateTime.now().withHour(16).withMinute(0).withSecond(0).withNano(0);
        SessionCaisseOpenRequest request = openRequest(dateOuverture);
        request.setSoldeOuverture(BigDecimal.ZERO);

        SessionCaisse sessionPlusAncienne = session(211L, StatutSessionCaisse.CLOTUREE);
        sessionPlusAncienne.setDateComptable(LocalDate.now());
        sessionPlusAncienne.setDateCloture(dateOuverture.minusHours(4));
        sessionPlusAncienne.setSoldePhysique(new BigDecimal("1048000.00"));

        SessionCaisse derniereSession = session(212L, StatutSessionCaisse.CLOTUREE);
        derniereSession.setDateComptable(LocalDate.now());
        derniereSession.setDateCloture(dateOuverture.minusMinutes(20));
        derniereSession.setSoldePhysique(new BigDecimal("1248000.00"));

        when(caisseRepository.findById(11L)).thenReturn(Optional.of(caisse));
        when(sessionCaisseRepository.existsByCaisseIdAndStatutIn(eq(11L), any(List.class))).thenReturn(false);
        when(sessionCaisseRepository.existsByUtilisateurIdAndStatutIn(eq(100L), any(List.class))).thenReturn(false);
        when(sessionCaisseRepository.findByCaisseIdAndDateComptableBeforeAndStatutInOrderByDateComptableAscDateOuvertureAsc(eq(11L), any(LocalDate.class), any(List.class)))
                .thenReturn(Collections.emptyList());
        when(sessionCaisseRepository.existsByCaisseIdAndDateComptableAndStatutIn(eq(11L), any(LocalDate.class), any(List.class))).thenReturn(false);
        when(sessionCaisseRepository.findClosedSessionsForOpening(eq(11L), eq(LocalDate.now()), eq(dateOuverture)))
                .thenReturn(List.of(derniereSession, sessionPlusAncienne));
        when(sessionCaisseRepository.save(any(SessionCaisse.class))).thenAnswer(inv -> inv.getArgument(0));

        service.ouvrir(request);

        ArgumentCaptor<SessionCaisse> captor = ArgumentCaptor.forClass(SessionCaisse.class);
        verify(sessionCaisseRepository).save(captor.capture());
        assertThat(captor.getValue().getSoldeOuverture()).isEqualByComparingTo("1248000.00");
    }

    @Test
    void sessionOuverteMemeJourBloqueNouvelleOuverture() {
        SessionCaisseOpenRequest request = openRequest();

        when(caisseRepository.findById(11L)).thenReturn(Optional.of(caisse));
        when(sessionCaisseRepository.existsByCaisseIdAndStatutIn(eq(11L), any(List.class))).thenReturn(true);

        assertThatThrownBy(() -> service.ouvrir(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("session active existe déjà");
    }

    @Test
    void historiqueSessionsMemeJourOrdonneCorrectement() {
        LocalDate today = LocalDate.now();
        SessionCaisse session3 = session(303L, StatutSessionCaisse.CLOTUREE);
        session3.setDateComptable(today);
        session3.setDateOuverture(today.atTime(15, 0));
        SessionCaisse session2 = session(302L, StatutSessionCaisse.CLOTUREE);
        session2.setDateComptable(today);
        session2.setDateOuverture(today.atTime(11, 0));
        SessionCaisse session1 = session(301L, StatutSessionCaisse.CLOTUREE);
        session1.setDateComptable(today);
        session1.setDateOuverture(today.atTime(8, 0));

        when(sessionCaisseRepository.findAllByOrderByDateComptableDescDateOuvertureDescIdDesc())
                .thenReturn(List.of(session3, session2, session1));

        List<SessionCaisseResponse> responses = service.getAll();

        assertThat(responses).extracting(SessionCaisseResponse::getId).containsExactly(303L, 302L, 301L);
    }

    @Test
    void nouvelleSessionRefuseeSiSessionPrecedenteOuverte() {
        assertOuvertureRefuseeSiSessionPrecedenteNonCloturee(StatutSessionCaisse.OUVERTE);
    }

    @Test
    void nouvelleSessionRefuseeSiSessionPrecedentePreCloturee() {
        assertOuvertureRefuseeSiSessionPrecedenteNonCloturee(StatutSessionCaisse.PRE_CLOTUREE);
    }

    @Test
    void nouvelleSessionRefuseeSiSessionPrecedenteValideeControleNonCloturee() {
        assertOuvertureRefuseeSiSessionPrecedenteNonCloturee(StatutSessionCaisse.VALIDEE_CONTROLE);
    }

    @Test
    void reportSoldeTraceAudit() throws Exception {
        SessionCaisseOpenRequest request = openRequest();
        request.setSoldeOuverture(BigDecimal.ZERO);

        SessionCaisse previousClosed = session(130L, StatutSessionCaisse.CLOTUREE);
        previousClosed.setSoldeTheorique(new BigDecimal("1048000.00"));
        previousClosed.setSoldePhysique(new BigDecimal("1048000.00"));

        when(caisseRepository.findById(11L)).thenReturn(Optional.of(caisse));
        when(sessionCaisseRepository.existsByCaisseIdAndStatutIn(eq(11L), any(List.class))).thenReturn(false);
        when(sessionCaisseRepository.existsByUtilisateurIdAndStatutIn(eq(100L), any(List.class))).thenReturn(false);
        when(sessionCaisseRepository.findByCaisseIdAndDateComptableBeforeAndStatutInOrderByDateComptableAscDateOuvertureAsc(eq(11L), any(LocalDate.class), any(List.class)))
                .thenReturn(Collections.emptyList());
        when(sessionCaisseRepository.existsByCaisseIdAndDateComptableAndStatutIn(eq(11L), any(LocalDate.class), any(List.class))).thenReturn(false);
        when(sessionCaisseRepository.findClosedSessionsForOpening(eq(11L), any(LocalDate.class), any(LocalDateTime.class))).thenReturn(List.of(previousClosed));
        when(sessionCaisseRepository.save(any(SessionCaisse.class))).thenAnswer(inv -> {
            SessionCaisse s = inv.getArgument(0);
            s.setId(131L);
            return s;
        });
        when(objectMapper.writeValueAsString(any())).thenAnswer(inv -> inv.getArgument(0, Object.class).toString());

        service.ouvrir(request);

        verify(auditService).logWithValues(
                eq(AuditAction.OUVERTURE_SESSION),
                eq("SessionCaisse"),
                eq(131L),
                eq(true),
                anyString(),
                any(),
                contains("reportSoldeDepuisSessionPrecedente=true"),
                any()
        );
    }

    @Test
    void ancienneSessionResteConsultable() {
        SessionCaisse ancienneSession = session(140L, StatutSessionCaisse.CLOTUREE);
        ancienneSession.setSoldeTheorique(new BigDecimal("1048000.00"));
        ancienneSession.setSoldePhysique(new BigDecimal("1048000.00"));
        when(sessionCaisseRepository.findById(140L)).thenReturn(Optional.of(ancienneSession));
        when(cashMapper.toResponse(ancienneSession)).thenReturn(SessionCaisseResponse.builder()
                .id(140L)
                .statut(StatutSessionCaisse.CLOTUREE)
                .soldeOuverture(ancienneSession.getSoldeOuverture())
                .soldeTheorique(ancienneSession.getSoldeTheorique())
                .soldePhysique(ancienneSession.getSoldePhysique())
                .build());
        when(operationCaisseRepository.findBySessionCaisseIdOrderByDateOperationDesc(140L)).thenReturn(Collections.emptyList());

        SessionCaisseResponse response = service.getById(140L);

        assertThat(response.getId()).isEqualTo(140L);
        assertThat(response.getStatut()).isEqualTo(StatutSessionCaisse.CLOTUREE);
        assertThat(response.getSoldePhysique()).isEqualByComparingTo("1048000.00");
    }

    @Test
    void getOpeningContext_shouldIgnoreClosedSessionAsActive() {
        when(caisseRepository.findById(11L)).thenReturn(Optional.of(caisse));
        when(sessionCaisseRepository.findFirstByCaisseIdAndStatutInOrderByDateOuvertureDesc(eq(11L), any(List.class)))
                .thenReturn(Optional.empty());
        when(sessionCaisseRepository.findClosedSessionsForOpening(eq(11L), any(LocalDate.class), any(LocalDateTime.class))).thenReturn(Collections.emptyList());

        var context = service.getOpeningContext(11L, LocalDate.now());

        assertThat(context.isSessionExistante()).isFalse();
        assertThat(context.getSessionExistanteId()).isNull();
    }

    @Test
    void getOpeningContext_shouldExposePreClotureeSessionAsActive() {
        SessionCaisse sessionActive = session(111L, StatutSessionCaisse.PRE_CLOTUREE);
        sessionActive.setDateComptable(LocalDate.now());

        when(caisseRepository.findById(11L)).thenReturn(Optional.of(caisse));
        when(sessionCaisseRepository.findFirstByCaisseIdAndStatutInOrderByDateOuvertureDesc(eq(11L), any(List.class)))
                .thenReturn(Optional.of(sessionActive));
        when(sessionCaisseRepository.findClosedSessionsForOpening(eq(11L), any(LocalDate.class), any(LocalDateTime.class))).thenReturn(Collections.emptyList());

        var context = service.getOpeningContext(11L, LocalDate.now());

        assertThat(context.isSessionExistante()).isTrue();
        assertThat(context.getSessionExistanteId()).isEqualTo(111L);
        assertThat(context.getSessionExistanteStatut()).isEqualTo(StatutSessionCaisse.PRE_CLOTUREE.name());
    }

    @Test
    void ouvertureSession_doubleOpenOnSameCaisse_isForbidden() {
        SessionCaisseOpenRequest request = openRequest();

        when(caisseRepository.findById(11L)).thenReturn(Optional.of(caisse));
        when(sessionCaisseRepository.existsByCaisseIdAndStatutIn(eq(11L), any(List.class))).thenReturn(true);

        assertThatThrownBy(() -> service.ouvrir(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("session active existe déjà");
    }

    @Test
    void preCloture_fromOuverte_setsPreCloturee() {
        SessionCaisse session = session(10L, StatutSessionCaisse.OUVERTE);
        SessionCaisseCloseRequest request = closeRequest(new BigDecimal("1000.00"));

        when(sessionCaisseRepository.findById(10L)).thenReturn(Optional.of(session));
        when(depenseCaisseRepository.existsPendingForSessionClosure(anyLong(), anyLong(), any(), any(), any())).thenReturn(false);
        when(operationCaisseRepository.countBySessionCaisseIdAndSourceAndRecetteIdIsNull(
                10L, SourceOperationCaisse.RECETTE_JOURNALIERE)).thenReturn(0L);
        when(ecartCaisseRepository.findBySessionCaisseIdOrderByDateCreationAsc(10L)).thenReturn(Collections.emptyList());
        when(sessionCaisseRepository.save(any(SessionCaisse.class))).thenAnswer(inv -> inv.getArgument(0));

        SessionCaisseResponse response = service.preCloturer(10L, request);

        assertThat(response.getStatut()).isEqualTo(StatutSessionCaisse.PRE_CLOTUREE);
    }

    @Test
    void preCloture_refused_whenPendingDepenseValidationExists() {
        SessionCaisse session = session(15L, StatutSessionCaisse.OUVERTE);
        SessionCaisseCloseRequest request = closeRequest(new BigDecimal("1000.00"));

        when(sessionCaisseRepository.findById(15L)).thenReturn(Optional.of(session));
        when(depenseCaisseRepository.existsPendingForSessionClosure(anyLong(), anyLong(), any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.preCloturer(15L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("dépenses non autorisées")
                .hasMessageContaining("validées ou rejetées");
    }

    @Test
    void validationControle_fromPreCloturee_setsValideeControle() {
        SessionCaisse session = session(20L, StatutSessionCaisse.PRE_CLOTUREE);
        session.setUtilisateur(utilisateur(200L, RoleCode.CAISSIER, agence(1L)));

        Utilisateur validateur = utilisateur(300L, RoleCode.ADMIN, agence(1L));
        authenticate(validateur);

        when(sessionCaisseRepository.findById(20L)).thenReturn(Optional.of(session));
        when(sessionCaisseRepository.save(any(SessionCaisse.class))).thenAnswer(inv -> inv.getArgument(0));

        SessionCaisseResponse response = service.validerControle(20L, "OK");

        assertThat(response.getStatut()).isEqualTo(StatutSessionCaisse.VALIDEE_CONTROLE);
    }

    @Test
    void validationControle_fromPreCloturee_allowsControleur() {
        SessionCaisse session = session(21L, StatutSessionCaisse.PRE_CLOTUREE);
        session.setUtilisateur(utilisateur(201L, RoleCode.CAISSIER, agence(1L)));

        Utilisateur validateur = utilisateur(301L, RoleCode.CONTROLEUR, agence(1L));
        authenticate(validateur);

        when(sessionCaisseRepository.findById(21L)).thenReturn(Optional.of(session));
        when(sessionCaisseRepository.save(any(SessionCaisse.class))).thenAnswer(inv -> inv.getArgument(0));

        SessionCaisseResponse response = service.validerControle(21L, "Contrôle validé");

        assertThat(response.getStatut()).isEqualTo(StatutSessionCaisse.VALIDEE_CONTROLE);
    }

    @Test
    void clotureFinale_fromValideeControle_setsCloturee() {
        SessionCaisse session = session(30L, StatutSessionCaisse.VALIDEE_CONTROLE);
        authenticate(utilisateur(301L, RoleCode.CHEF_BUREAU, agence(1L)));
        when(sessionCaisseRepository.findById(30L)).thenReturn(Optional.of(session));
        when(sessionCaisseRepository.save(any(SessionCaisse.class))).thenAnswer(inv -> inv.getArgument(0));

        SessionCaisseResponse response = service.cloturerFinale(30L, "Clôture finale validée");

        assertThat(response.getStatut()).isEqualTo(StatutSessionCaisse.CLOTUREE);
        ArgumentCaptor<SessionCaisse> captor = ArgumentCaptor.forClass(SessionCaisse.class);
        verify(sessionCaisseRepository).save(captor.capture());
        assertThat(captor.getValue().getDateCloture()).isNotNull();
        verify(auditService).logWithValues(
                eq(AuditAction.CLOTURE_FINALE),
                eq("SessionCaisse"),
                eq(30L),
                eq(true),
                contains("Clôture finale effectuée"),
                any(),
                any(),
                any()
        );
    }

    @Test
    void clotureFinale_refusesGestionnaireCaissierAndControleur() {
        SessionCaisse session = session(31L, StatutSessionCaisse.VALIDEE_CONTROLE);
        when(sessionCaisseRepository.findById(31L)).thenReturn(Optional.of(session));

        for (RoleCode roleCode : List.of(RoleCode.GESTIONNAIRE, RoleCode.CAISSIER, RoleCode.CONTROLEUR)) {
            authenticate(utilisateur(400L + roleCode.ordinal(), roleCode, agence(1L)));

            assertThatThrownBy(() -> service.cloturerFinale(31L, "tentative non autorisée"))
                    .as(roleCode.name())
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Seuls ADMIN et CHEF_BUREAU");
        }
    }

    @Test
    void clotureFinale_fromOuverte_isForbidden() {
        SessionCaisse session = session(40L, StatutSessionCaisse.OUVERTE);
        authenticate(utilisateur(302L, RoleCode.CHEF_BUREAU, agence(1L)));
        when(sessionCaisseRepository.findById(40L)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.cloturerFinale(40L, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("nécessite une session validée en contrôle");
    }

    @Test
    void clotureFinale_fromPreClotureeWithoutValidation_isForbidden() {
        SessionCaisse session = session(50L, StatutSessionCaisse.PRE_CLOTUREE);
        authenticate(utilisateur(303L, RoleCode.CHEF_BUREAU, agence(1L)));
        when(sessionCaisseRepository.findById(50L)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.cloturerFinale(50L, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("nécessite une session validée en contrôle");
    }

    private void assertOuvertureRefuseeSiSessionPrecedenteNonCloturee(StatutSessionCaisse statutBloquant) {
        SessionCaisseOpenRequest request = openRequest();
        SessionCaisse sessionBloquante = session(200L + statutBloquant.ordinal(), statutBloquant);
        sessionBloquante.setDateComptable(LocalDate.now().minusDays(1));

        when(caisseRepository.findById(11L)).thenReturn(Optional.of(caisse));
        when(sessionCaisseRepository.existsByCaisseIdAndStatutIn(eq(11L), any(List.class))).thenReturn(false);
        when(sessionCaisseRepository.existsByUtilisateurIdAndStatutIn(eq(100L), any(List.class))).thenReturn(false);
        when(sessionCaisseRepository.findByCaisseIdAndDateComptableBeforeAndStatutInOrderByDateComptableAscDateOuvertureAsc(eq(11L), any(LocalDate.class), any(List.class)))
                .thenReturn(List.of(sessionBloquante));

        assertThatThrownBy(() -> service.ouvrir(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("session ancienne ouverte")
                .hasMessageContaining("Clôturez ou régularisez-la");
    }

    private SessionCaisseOpenRequest openRequest() {
        return openRequest(LocalDateTime.now());
    }

    private SessionCaisseOpenRequest openRequest(LocalDateTime dateOuverture) {
        SessionCaisseOpenRequest request = new SessionCaisseOpenRequest();
        request.setCaisseId(11L);
        request.setDateOuverture(dateOuverture);
        return request;
    }

    private SessionCaisseCloseRequest closeRequest(BigDecimal soldePhysique) {
        SessionCaisseCloseRequest request = new SessionCaisseCloseRequest();
        request.setDateCloture(LocalDateTime.now());
        request.setSoldePhysique(soldePhysique);
        request.setObservation("Pré-clôture avec solde physique");
        return request;
    }

    private SessionCaisse session(Long id, StatutSessionCaisse statut) {
        SessionCaisse session = new SessionCaisse();
        session.setId(id);
        session.setCaisse(caisse);
        session.setUtilisateur(caissier);
        session.setStatut(statut);
        session.setDateOuverture(LocalDateTime.now().minusHours(3));
        session.setSoldeOuverture(new BigDecimal("1000.00"));
        session.setTotalEntrees(BigDecimal.ZERO);
        session.setTotalSorties(BigDecimal.ZERO);
        return session;
    }

    private Agence agence(Long id) {
        Agence agence = Agence.builder().nomAgence("Agence " + id).codeAgence("AG" + id).actif(true).build();
        agence.setId(id);
        return agence;
    }

    private Utilisateur utilisateur(Long id, RoleCode roleCode, Agence agence) {
        Utilisateur u = Utilisateur.builder()
                .username(roleCode.name().toLowerCase())
                .nomComplet(roleCode.name())
                .motDePasseHash("hash")
                .role(Role.builder().code(roleCode).libelle(roleCode.name()).build())
                .site(Site.builder().nomSite("Site " + id).agence(agence).build())
                .build();
        u.setId(id);
        return u;
    }

    private void authenticate(Utilisateur utilisateur) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(utilisateur, null, utilisateur.getAuthorities())
        );
    }

    private void authenticateWithPermission(Utilisateur utilisateur, String permission) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        utilisateur,
                        null,
                        List.of(new SimpleGrantedAuthority(permission))
                )
        );
    }
}
