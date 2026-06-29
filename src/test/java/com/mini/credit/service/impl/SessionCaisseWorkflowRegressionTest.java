package com.mini.credit.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini.credit.dto.caisse.SessionCaisseCloseRequest;
import com.mini.credit.entity.caisse.Caisse;
import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.entity.referentiel.Role;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.StatutSessionCaisse;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionCaisseWorkflowRegressionTest {

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

    private Utilisateur controleur;

    @BeforeEach
    void setUp() throws Exception {
        controleur = Utilisateur.builder()
                .username("ctrl")
                .nomComplet("Controleur")
            .role(Role.builder().code(RoleCode.CHEF_BUREAU).build())
                .motDePasseHash("x")
                .build();
        controleur.setId(55L);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(controleur, null, controleur.getAuthorities())
        );

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void preCloture_shouldBeForbidden_whenSessionAlreadyCloturee() {
        SessionCaisse session = session(1L, StatutSessionCaisse.CLOTUREE);
        SessionCaisseCloseRequest request = new SessionCaisseCloseRequest();
        request.setDateCloture(LocalDateTime.now());
        request.setSoldePhysique(new BigDecimal("1000"));

        when(sessionCaisseRepository.findById(1L)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.preCloturer(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("n'est pas ouverte");
    }

    @Test
    void validationControle_shouldBeForbidden_whenAlreadyValidated() {
        SessionCaisse session = session(2L, StatutSessionCaisse.VALIDEE_CONTROLE);
        when(sessionCaisseRepository.findById(2L)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.validerControle(2L, "double validation"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("pré-clôturées");
    }

    @Test
    void clotureFinale_shouldBeForbidden_whenAlreadyCloturee() {
        SessionCaisse session = session(3L, StatutSessionCaisse.CLOTUREE);
        when(sessionCaisseRepository.findById(3L)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.cloturerFinale(3L, "double clôture"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("nécessite une session validée en contrôle");
    }

    private SessionCaisse session(Long id, StatutSessionCaisse statut) {
        Caisse caisse = Caisse.builder().actif(true).build();
        caisse.setId(10L);

        Utilisateur caissier = Utilisateur.builder()
                .username("cashier")
                .nomComplet("Cashier")
                .role(Role.builder().code(RoleCode.CAISSIER).build())
                .motDePasseHash("x")
                .build();
        caissier.setId(99L);

        SessionCaisse session = new SessionCaisse();
        session.setId(id);
        session.setCaisse(caisse);
        session.setUtilisateur(caissier);
        session.setStatut(statut);
        session.setDateOuverture(LocalDateTime.now().minusHours(3));
        session.setSoldeOuverture(new BigDecimal("1000"));
        session.setTotalEntrees(BigDecimal.ZERO);
        session.setTotalSorties(BigDecimal.ZERO);
        return session;
    }
}
