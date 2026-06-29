package com.mini.credit.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini.credit.dto.caisse.SessionCaisseCloseRequest;
import com.mini.credit.dto.caisse.SessionCaisseResponse;
import com.mini.credit.entity.agence.Agence;
import com.mini.credit.entity.caisse.Caisse;
import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.entity.referentiel.Role;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.StatutSessionCaisse;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.mapper.CashMapper;
import com.mini.credit.repository.caisse.CaisseRepository;
import com.mini.credit.repository.caisse.EcartCaisseRepository;
import com.mini.credit.repository.caisse.OperationCaisseRepository;
import com.mini.credit.repository.caisse.SessionCaisseAnomalieRepository;
import com.mini.credit.repository.caisse.SessionCaisseRepository;
import com.mini.credit.repository.utilisateur.UtilisateurRepository;
import com.mini.credit.service.EcartThresholdConfigService;
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
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SessionCaisseAuditTest {

    @Mock private SessionCaisseRepository sessionCaisseRepository;
    @Mock private CaisseRepository caisseRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private OperationCaisseRepository operationCaisseRepository;
    @Mock private SessionCaisseAnomalieRepository sessionCaisseAnomalieRepository;
    @Mock private EcartCaisseRepository ecartCaisseRepository;
    @Mock private CashMapper cashMapper;
    @Mock private EcartThresholdConfigService ecartThresholdConfigService;
    @Mock private AuditService auditService;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks private SessionCaisseServiceImpl service;

    private SessionCaisse session;

    @BeforeEach
    void setUp() throws Exception {
        Agence agence = Agence.builder().codeAgence("AG-1").nomAgence("Agence Test").build();
        agence.setId(1L);
        Site site = Site.builder().codeSite("SITE-1").nomSite("Site Test").zone("Z1").agence(agence).actif(true).build();
        site.setId(2L);

        Caisse caisse = Caisse.builder().actif(true).agence(agence).build();
        caisse.setId(5L);

        Utilisateur caissier = Utilisateur.builder()
                .username("cashier")
                .nomComplet("Cashier")
                .role(Role.builder().code(RoleCode.CAISSIER).build())
            .site(site)
                .motDePasseHash("x")
                .build();
        caissier.setId(10L);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(caissier, null, caissier.getAuthorities())
        );

        session = new SessionCaisse();
        session.setId(20L);
        session.setCaisse(caisse);
        session.setUtilisateur(caissier);
        session.setStatut(StatutSessionCaisse.OUVERTE);
        session.setDateOuverture(LocalDateTime.now().minusHours(2));
        session.setSoldeOuverture(new BigDecimal("1000"));
        session.setTotalEntrees(BigDecimal.ZERO);
        session.setTotalSorties(BigDecimal.ZERO);

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(cashMapper.toResponse(any(SessionCaisse.class))).thenAnswer(inv -> {
            SessionCaisse s = inv.getArgument(0);
            return SessionCaisseResponse.builder().id(s.getId()).statut(s.getStatut()).build();
        });
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void preCloturer_shouldLogAuditActionPreCloture_onSuccess() {
        SessionCaisseCloseRequest request = new SessionCaisseCloseRequest();
        request.setDateCloture(LocalDateTime.now());
        request.setSoldePhysique(new BigDecimal("1000"));
        request.setObservation("RAS");

        when(sessionCaisseRepository.findById(20L)).thenReturn(Optional.of(session));
        when(operationCaisseRepository.countBySessionCaisseIdAndSourceAndRecetteIdIsNull(eq(20L), any())).thenReturn(0L);
        when(ecartCaisseRepository.findBySessionCaisseIdOrderByDateCreationAsc(20L)).thenReturn(Collections.emptyList());
        when(sessionCaisseRepository.save(any(SessionCaisse.class))).thenAnswer(inv -> inv.getArgument(0));

        SessionCaisseResponse response = service.preCloturer(20L, request);

        assertThat(response.getStatut()).isEqualTo(StatutSessionCaisse.PRE_CLOTUREE);
        verify(auditService).logWithValues(
                eq(AuditAction.PRE_CLOTURE),
                eq("SessionCaisse"),
                eq(20L),
                eq(true),
                any(),
                any(),
                any(),
                eq(null)
        );
    }

    @Test
    void preCloturer_shouldLogRefusTransition_whenStatusInvalid() {
        SessionCaisseCloseRequest request = new SessionCaisseCloseRequest();
        request.setDateCloture(LocalDateTime.now());
        request.setSoldePhysique(new BigDecimal("1000"));
        request.setObservation("Refus attendu");

        session.setStatut(StatutSessionCaisse.CLOTUREE);
        when(sessionCaisseRepository.findById(20L)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.preCloturer(20L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("n'est pas ouverte");

        verify(auditService).logWithValues(
                eq(AuditAction.REFUS_TRANSITION),
                eq("SessionCaisse"),
                eq(20L),
                eq(false),
                any(),
                any(),
                any(),
                any()
        );
    }
}
