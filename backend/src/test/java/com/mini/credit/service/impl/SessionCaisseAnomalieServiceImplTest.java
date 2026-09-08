package com.mini.credit.service.impl;

import com.mini.credit.dto.caisse.DemanderAnnulationSessionRequest;
import com.mini.credit.dto.caisse.ReouvrirSessionControleeRequest;
import com.mini.credit.dto.caisse.SessionCaisseResponse;
import com.mini.credit.dto.caisse.ValiderAnnulationSessionRequest;
import com.mini.credit.entity.caisse.Caisse;
import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.entity.caisse.SessionCaisseAnomalie;
import com.mini.credit.entity.referentiel.Role;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.StatutDossierAnomalieSession;
import com.mini.credit.enums.StatutSessionCaisse;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.mapper.CashMapper;
import com.mini.credit.repository.caisse.OperationCaisseRepository;
import com.mini.credit.repository.caisse.SessionCaisseAnomalieRepository;
import com.mini.credit.repository.caisse.SessionCaisseRepository;
import com.mini.credit.repository.epargne.OperationEpargneRepository;
import com.mini.credit.repository.utilisateur.UtilisateurRepository;
import com.mini.credit.repository.workflow.WorkflowTaskRepository;
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

import java.util.Optional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SessionCaisseAnomalieServiceImplTest {

    @Mock private SessionCaisseRepository sessionCaisseRepository;
    @Mock private SessionCaisseAnomalieRepository anomalieRepository;
    @Mock private OperationCaisseRepository operationCaisseRepository;
    @Mock private OperationEpargneRepository operationEpargneRepository;
    @Mock private WorkflowTaskRepository workflowTaskRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private AuditService auditService;
    @Mock private CashMapper cashMapper;

    @InjectMocks private SessionCaisseAnomalieServiceImpl service;

    private Utilisateur caissier;
    private Utilisateur chef;
    private Utilisateur admin;
    private SessionCaisse session;

    @BeforeEach
    void setUp() {
        caissier = user(10L, "caissier", RoleCode.CAISSIER);
        chef = user(20L, "chef", RoleCode.CHEF_BUREAU);
        admin = user(30L, "admin", RoleCode.ADMIN);

        Caisse caisse = Caisse.builder().actif(true).build();
        caisse.setId(1L);

        session = new SessionCaisse();
        session.setId(99L);
        session.setCaisse(caisse);
        session.setUtilisateur(caissier);
        session.setStatut(StatutSessionCaisse.OUVERTE);
        session.setDateComptable(LocalDate.of(2026, 7, 28));
        session.setDateOuverture(LocalDateTime.of(2026, 7, 28, 18, 38));

        when(utilisateurRepository.findByUsernameWithValidationContext("caissier")).thenReturn(Optional.of(caissier));
        when(utilisateurRepository.findByUsernameWithValidationContext("chef")).thenReturn(Optional.of(chef));
        when(utilisateurRepository.findByUsernameWithValidationContext("admin")).thenReturn(Optional.of(admin));

        when(sessionCaisseRepository.findById(99L)).thenReturn(Optional.of(session));
        when(anomalieRepository.save(any(SessionCaisseAnomalie.class))).thenAnswer(inv -> {
            SessionCaisseAnomalie a = inv.getArgument(0);
            if (a.getId() == null) {
                a.setId(500L);
            }
            return a;
        });
        when(sessionCaisseRepository.save(any(SessionCaisse.class))).thenAnswer(inv -> inv.getArgument(0));
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
    void demanderAnnulation_openSansMouvement_creeDemande() {
        authenticate(caissier);
        when(operationCaisseRepository.countBySessionCaisseId(99L)).thenReturn(0L);
        when(anomalieRepository.findFirstBySessionIdAndStatutDossierOrderByDateDemandeDesc(99L, StatutDossierAnomalieSession.DEMANDEE))
                .thenReturn(Optional.empty());

        DemanderAnnulationSessionRequest req = new DemanderAnnulationSessionRequest();
        req.setMotif("Ouverture par erreur");

        var response = service.demanderAnnulation(99L, req);

        assertThat(response.getSessionId()).isEqualTo(99L);
        assertThat(response.getStatutDossier()).isEqualTo(StatutDossierAnomalieSession.DEMANDEE);
    }

    @Test
    void demanderAnnulation_avecMouvement_refuse() {
        authenticate(caissier);
        when(operationCaisseRepository.countBySessionCaisseId(99L)).thenReturn(1L);

        DemanderAnnulationSessionRequest req = new DemanderAnnulationSessionRequest();
        req.setMotif("Erreur");

        assertThatThrownBy(() -> service.demanderAnnulation(99L, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("SESSION_AVEC_MOUVEMENTS_ANNULATION_SIMPLE_INTERDITE");
    }

    @Test
    void reouvrirControlee_precloturee_parChef_repasseOuverte() {
        authenticate(chef);
        session.setStatut(StatutSessionCaisse.PRE_CLOTUREE);

        ReouvrirSessionControleeRequest req = new ReouvrirSessionControleeRequest();
        req.setMotif("Pré-clôture déclenchée trop tôt");

        SessionCaisseResponse response = service.reouvrirControlee(99L, req);

        assertThat(response.getStatut()).isEqualTo(StatutSessionCaisse.OUVERTE);
        verify(sessionCaisseRepository).save(any(SessionCaisse.class));
    }

    @Test
    void annulerAdministrativement_clotureeSansMouvement_parChef_ok() {
        authenticate(chef);
        session.setStatut(StatutSessionCaisse.CLOTUREE);
        when(operationCaisseRepository.countBySessionCaisseId(99L)).thenReturn(0L);

        DemanderAnnulationSessionRequest req = new DemanderAnnulationSessionRequest();
        req.setMotif("Clôture automatique par erreur");

        SessionCaisseResponse response = service.annulerAdministrativement(99L, req);

        assertThat(response.getStatut()).isEqualTo(StatutSessionCaisse.ANNULEE_ADMINISTRATIVEMENT);

        ArgumentCaptor<SessionCaisse> captor = ArgumentCaptor.forClass(SessionCaisse.class);
        verify(sessionCaisseRepository).save(captor.capture());
        assertThat(captor.getValue().getMotifAnnulation()).isEqualTo("Clôture automatique par erreur");
    }

        @Test
        void annulerSessionTest_ouverteSansMouvement_parAdmin_ok() {
        authenticate(admin);
        when(operationCaisseRepository.countBySessionCaisseId(99L)).thenReturn(0L);
        when(operationEpargneRepository.countBySessionCaisseId(99L)).thenReturn(0L);
        when(sessionCaisseRepository.existsLaterSessionForCaisse(99L, 1L, session.getDateComptable(), session.getDateOuverture()))
            .thenReturn(false);
        when(anomalieRepository.findFirstBySessionIdAndStatutDossierOrderByDateDemandeDesc(99L, StatutDossierAnomalieSession.DEMANDEE))
            .thenReturn(Optional.empty());
        when(workflowTaskRepository.findByModuleAndEntityTypeAndEntityIdAndStatutIn(any(), any(), any(), any()))
            .thenReturn(List.of());

        DemanderAnnulationSessionRequest req = new DemanderAnnulationSessionRequest();
        req.setMotif("Ouverture de recette test à 0");
        req.setCommentaire("Annulation contrôlée sans mouvement");

        SessionCaisseResponse response = service.annulerSessionTest(99L, req);

        assertThat(response.getStatut()).isEqualTo(StatutSessionCaisse.ANNULEE);

        ArgumentCaptor<SessionCaisse> captor = ArgumentCaptor.forClass(SessionCaisse.class);
        verify(sessionCaisseRepository).save(captor.capture());
        assertThat(captor.getValue().getStatutCorrection()).isEqualTo("ANNULEE_RECETTE_TEST");
        assertThat(captor.getValue().getMotifAnnulation()).isEqualTo("Ouverture de recette test à 0");
        }

        @Test
        void annulerSessionTest_avecMouvementCaisse_refuseRectification() {
        authenticate(admin);
        when(operationCaisseRepository.countBySessionCaisseId(99L)).thenReturn(1L);

        DemanderAnnulationSessionRequest req = new DemanderAnnulationSessionRequest();
        req.setMotif("Ouverture de recette test à 0");

        assertThatThrownBy(() -> service.annulerSessionTest(99L, req))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("SESSION_AVEC_MOUVEMENTS_RECTIFICATION_REQUISE");
        }

        @Test
        void annulerSessionTest_avecMouvementEpargne_refuseRectification() {
        authenticate(admin);
        when(operationCaisseRepository.countBySessionCaisseId(99L)).thenReturn(0L);
        when(operationEpargneRepository.countBySessionCaisseId(99L)).thenReturn(1L);

        DemanderAnnulationSessionRequest req = new DemanderAnnulationSessionRequest();
        req.setMotif("Ouverture de recette test à 0");

        assertThatThrownBy(() -> service.annulerSessionTest(99L, req))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("SESSION_AVEC_MOUVEMENTS_RECTIFICATION_REQUISE");
        }

        @Test
        void annulerSessionTest_avecSessionSuivante_refuse() {
        authenticate(admin);
        when(operationCaisseRepository.countBySessionCaisseId(99L)).thenReturn(0L);
        when(operationEpargneRepository.countBySessionCaisseId(99L)).thenReturn(0L);
        when(sessionCaisseRepository.existsLaterSessionForCaisse(99L, 1L, session.getDateComptable(), session.getDateOuverture()))
            .thenReturn(true);

        DemanderAnnulationSessionRequest req = new DemanderAnnulationSessionRequest();
        req.setMotif("Ouverture de recette test à 0");

        assertThatThrownBy(() -> service.annulerSessionTest(99L, req))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("SESSION_AVEC_SESSION_SUIVANTE_ANNULATION_TEST_REFUSEE");
        }

        @Test
        void annulerSessionTest_parChef_refusePermission() {
        authenticate(chef);

        DemanderAnnulationSessionRequest req = new DemanderAnnulationSessionRequest();
        req.setMotif("Ouverture de recette test à 0");

        assertThatThrownBy(() -> service.annulerSessionTest(99L, req))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("PERMISSION_INSUFFISANTE");
        }

    private void authenticate(Utilisateur user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getUsername(), null, user.getAuthorities())
        );
    }

    private Utilisateur user(Long id, String username, RoleCode roleCode) {
        Utilisateur u = Utilisateur.builder()
                .username(username)
                .nomComplet(username)
                .role(Role.builder().code(roleCode).build())
                .motDePasseHash("x")
                .build();
        u.setId(id);
        return u;
    }
}
