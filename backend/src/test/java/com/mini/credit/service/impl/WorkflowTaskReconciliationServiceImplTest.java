package com.mini.credit.service.impl;

import com.mini.credit.dto.workflow.WorkflowTaskReconcileResultDTO;
import com.mini.credit.entity.credit.DemandeCredit;
import com.mini.credit.entity.agence.Agence;
import com.mini.credit.entity.caisse.Caisse;
import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.workflow.WorkflowTask;
import com.mini.credit.enums.StatutDemandeCredit;
import com.mini.credit.enums.StatutSessionCaisse;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.enums.workflow.WorkflowTaskModule;
import com.mini.credit.enums.workflow.WorkflowTaskStatus;
import com.mini.credit.repository.credit.DemandeCreditRepository;
import com.mini.credit.repository.caisse.SessionCaisseRepository;
import com.mini.credit.repository.workflow.WorkflowTaskRepository;
import com.mini.credit.service.WorkflowTaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowTaskReconciliationServiceImplTest {

    @Mock
    private SessionCaisseRepository sessionCaisseRepository;

    @Mock
    private DemandeCreditRepository demandeCreditRepository;

    @Mock
    private WorkflowTaskRepository workflowTaskRepository;

    @Mock
    private WorkflowTaskService workflowTaskService;

    @InjectMocks
    private WorkflowTaskReconciliationServiceImpl service;

    @Test
    void reconcile_shouldCreateControleurTaskForPreCloturee_withoutDuplicateOnReplay() {
        SessionCaisse preCloturee = session(101L, StatutSessionCaisse.PRE_CLOTUREE, null);

        when(sessionCaisseRepository.findAll()).thenReturn(List.of(preCloturee));
        when(workflowTaskRepository.findByModuleAndEntityTypeAndEntityIdAndRoleDestinataireAndStatutIn(
                eq(WorkflowTaskModule.CAISSE), eq("SESSION_CAISSE"), eq(101L), eq(RoleCode.CONTROLEUR), anyCollection()))
                .thenReturn(List.of())
                .thenReturn(List.of(new com.mini.credit.entity.workflow.WorkflowTask()));

        WorkflowTaskReconcileResultDTO first = service.reconcileCaisseSessions();
        WorkflowTaskReconcileResultDTO second = service.reconcileCaisseSessions();

        assertThat(first.getCreatedControleurTasks()).isEqualTo(1);
        assertThat(second.getCreatedControleurTasks()).isEqualTo(0);
        verify(workflowTaskService).onSessionPreCloturee(101L, "CAI-SESSION-101", 7L, 3L);
    }

    @Test
    void reconcile_shouldCloseActiveTasksForClotureeSession() {
        SessionCaisse cloturee = session(102L, StatutSessionCaisse.CLOTUREE, LocalDateTime.now());

        when(sessionCaisseRepository.findAll()).thenReturn(List.of(cloturee));
        when(workflowTaskRepository.findByModuleAndEntityTypeAndEntityIdAndStatutIn(
                eq(WorkflowTaskModule.CAISSE), eq("SESSION_CAISSE"), eq(102L), anyCollection()))
                .thenReturn(List.of(new com.mini.credit.entity.workflow.WorkflowTask()));

        WorkflowTaskReconcileResultDTO result = service.reconcileCaisseSessions();

        assertThat(result.getClosedTasksForCloture()).isEqualTo(1);
        verify(workflowTaskService).onSessionCloturee(102L, "CAI-SESSION-102", 7L, 3L);
    }

    @Test
    void reconcile_shouldCreateChefBureauTaskOnlyWhenFinalClosureExpected() {
        SessionCaisse valideeControleNoClosure = session(103L, StatutSessionCaisse.VALIDEE_CONTROLE, null);
        SessionCaisse valideeControleAlreadyClosed = session(104L, StatutSessionCaisse.VALIDEE_CONTROLE, LocalDateTime.now());

        when(sessionCaisseRepository.findAll()).thenReturn(List.of(valideeControleNoClosure, valideeControleAlreadyClosed));
        when(workflowTaskRepository.findByModuleAndEntityTypeAndEntityIdAndRoleDestinataireAndStatutIn(
                eq(WorkflowTaskModule.CAISSE), eq("SESSION_CAISSE"), eq(103L), eq(RoleCode.CHEF_BUREAU), anyCollection()))
                .thenReturn(List.of());

        WorkflowTaskReconcileResultDTO result = service.reconcileCaisseSessions();

        assertThat(result.getCreatedChefBureauTasks()).isEqualTo(1);
        verify(workflowTaskService).onSessionControleValide(103L, "CAI-SESSION-103", 7L, 3L);
        verify(workflowTaskService, never()).onSessionControleValide(104L, "CAI-SESSION-104", 7L, 3L);
    }

    @Test
    void reconcile_shouldIgnoreClotureeSessionWithoutActiveTasks() {
        SessionCaisse cloturee = session(105L, StatutSessionCaisse.CLOTUREE, LocalDateTime.now());

        when(sessionCaisseRepository.findAll()).thenReturn(List.of(cloturee));
        when(workflowTaskRepository.findByModuleAndEntityTypeAndEntityIdAndStatutIn(
                eq(WorkflowTaskModule.CAISSE), eq("SESSION_CAISSE"), eq(105L), anyCollection()))
                .thenReturn(List.of());

        WorkflowTaskReconcileResultDTO result = service.reconcileCaisseSessions();

        assertThat(result.getClosedTasksForCloture()).isZero();
        verify(workflowTaskService, never()).onSessionCloturee(105L, "CAI-SESSION-105", 7L, 3L);
    }

        @Test
        void reconcileSubmittedCreditDemands_shouldCreateGestionnaireTaskWhenMissing() {
        DemandeCredit demande = demande(201L, StatutDemandeCredit.SOUMISE, "DCR-201");

        when(demandeCreditRepository.findByStatut(StatutDemandeCredit.SOUMISE)).thenReturn(List.of(demande));
        when(workflowTaskRepository.findByModuleAndEntityTypeAndEntityIdAndRoleDestinataireAndStatutIn(
            eq(WorkflowTaskModule.CREDIT), eq("DEMANDE_CREDIT"), eq(201L), eq(RoleCode.GESTIONNAIRE), anyCollection()))
            .thenReturn(List.of());

        WorkflowTaskReconcileResultDTO result = service.reconcileSubmittedCreditDemands();

        assertThat(result.getScannedSubmittedCreditDemands()).isEqualTo(1);
        assertThat(result.getCreatedGestionnaireTasks()).isEqualTo(1);
        assertThat(result.getMigratedLegacyGestionnaireTasks()).isZero();
        verify(workflowTaskService).onDemandeCreditSoumise(201L, "DCR-201", 7L, 3L);
        }

        @Test
        void reconcileSubmittedCreditDemands_shouldCreateGestionnaireTaskWithoutRuntimeLegacyLookup() {
        DemandeCredit demande = demande(202L, StatutDemandeCredit.SOUMISE, "DCR-202");

        when(demandeCreditRepository.findByStatut(StatutDemandeCredit.SOUMISE)).thenReturn(List.of(demande));
        when(workflowTaskRepository.findByModuleAndEntityTypeAndEntityIdAndRoleDestinataireAndStatutIn(
            eq(WorkflowTaskModule.CREDIT), eq("DEMANDE_CREDIT"), eq(202L), eq(RoleCode.GESTIONNAIRE), anyCollection()))
            .thenReturn(List.of());

        WorkflowTaskReconcileResultDTO result = service.reconcileSubmittedCreditDemands();

        assertThat(result.getCreatedGestionnaireTasks()).isEqualTo(1);
        assertThat(result.getMigratedLegacyGestionnaireTasks()).isZero();
        verify(workflowTaskService).onDemandeCreditSoumise(202L, "DCR-202", 7L, 3L);
        }

        @Test
        void reconcileSubmittedCreditDemands_shouldSkipWhenGestionnaireTaskAlreadyExists() {
        DemandeCredit demande = demande(203L, StatutDemandeCredit.SOUMISE, "DCR-203");
        WorkflowTask gestionnaireTask = new WorkflowTask();
        gestionnaireTask.setId(2030L);
        gestionnaireTask.setRoleDestinataire(RoleCode.GESTIONNAIRE);

        when(demandeCreditRepository.findByStatut(StatutDemandeCredit.SOUMISE)).thenReturn(List.of(demande));
        when(workflowTaskRepository.findByModuleAndEntityTypeAndEntityIdAndRoleDestinataireAndStatutIn(
            eq(WorkflowTaskModule.CREDIT), eq("DEMANDE_CREDIT"), eq(203L), eq(RoleCode.GESTIONNAIRE), anyCollection()))
            .thenReturn(List.of(gestionnaireTask));
        WorkflowTaskReconcileResultDTO result = service.reconcileSubmittedCreditDemands();

        assertThat(result.getCreatedGestionnaireTasks()).isZero();
        assertThat(result.getMigratedLegacyGestionnaireTasks()).isZero();
        verify(workflowTaskService, never()).onDemandeCreditSoumise(203L, "DCR-203", 7L, 3L);
        }

    private SessionCaisse session(Long id, StatutSessionCaisse statut, LocalDateTime dateCloture) {
        Agence agence = new Agence();
        agence.setId(7L);

        Site site = new Site();
        site.setId(3L);

        Caisse caisse = new Caisse();
        caisse.setId(200L + id);
        caisse.setCodeCaisse("CAI");
        caisse.setAgence(agence);
        caisse.setSite(site);

        SessionCaisse session = new SessionCaisse();
        session.setId(id);
        session.setCaisse(caisse);
        session.setStatut(statut);
        session.setDateCloture(dateCloture);
        return session;
    }

    private DemandeCredit demande(Long id, StatutDemandeCredit statut, String numeroDemande) {
        Agence agence = new Agence();
        agence.setId(7L);

        Site site = new Site();
        site.setId(3L);
        site.setAgence(agence);

        DemandeCredit demande = new DemandeCredit();
        demande.setId(id);
        demande.setNumeroDemande(numeroDemande);
        demande.setStatut(statut);
        demande.setSite(site);
        return demande;
    }
}
