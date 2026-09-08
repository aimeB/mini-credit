package com.mini.credit.service.impl;

import com.mini.credit.dto.workflow.WorkflowTaskDashboardDTO;
import com.mini.credit.dto.workflow.WorkflowTaskItemDTO;
import com.mini.credit.entity.caisse.Caisse;
import com.mini.credit.entity.caisse.DepenseCaisse;
import com.mini.credit.entity.caisse.RecetteJournaliereTerrain;
import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.entity.credit.Credit;
import com.mini.credit.entity.credit.DemandeCredit;
import com.mini.credit.entity.epargne.DemandeRetraitEpargne;
import com.mini.credit.entity.agence.Agence;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.Role;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.referentiel.CollecteJournaliereTerrain;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.entity.workflow.WorkflowTask;
import com.mini.credit.enums.DepenseCaisseStatus;
import com.mini.credit.enums.RecetteStatut;
import com.mini.credit.enums.StatutCredit;
import com.mini.credit.enums.StatutDemandeRetrait;
import com.mini.credit.enums.StatutDemandeCredit;
import com.mini.credit.enums.StatutRecetteJournaliere;
import com.mini.credit.enums.StatutSessionCaisse;
import com.mini.credit.enums.security.PermissionCode;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.enums.workflow.WorkflowTaskModule;
import com.mini.credit.enums.workflow.WorkflowTaskPriority;
import com.mini.credit.enums.workflow.WorkflowTaskStatus;
import com.mini.credit.enums.workflow.WorkflowTaskTypeAction;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.mapper.WorkflowTaskMapper;
import com.mini.credit.repository.caisse.DepenseCaisseRepository;
import com.mini.credit.repository.caisse.RecetteJournaliereTerrainRepository;
import com.mini.credit.repository.caisse.SessionCaisseRepository;
import com.mini.credit.repository.collecteTerrain.CollecteJournaliereTerrainRepository;
import com.mini.credit.repository.credit.DemandeCreditRepository;
import com.mini.credit.repository.epargne.DemandeRetraitEpargneRepository;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkflowTaskServiceImplTest {

    @Mock
    private WorkflowTaskRepository workflowTaskRepository;

    @Mock
    private DemandeCreditRepository demandeCreditRepository;

    @Mock
    private DepenseCaisseRepository depenseCaisseRepository;

    @Mock
    private DemandeRetraitEpargneRepository demandeRetraitEpargneRepository;

    @Mock
    private RecetteJournaliereTerrainRepository recetteJournaliereTerrainRepository;

    @Mock
    private CollecteJournaliereTerrainRepository collecteJournaliereTerrainRepository;

    @Mock
    private SessionCaisseRepository sessionCaisseRepository;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private WorkflowTaskMapper workflowTaskMapper;

    @InjectMocks
    private WorkflowTaskServiceImpl service;

    private Utilisateur controleur;
    private Utilisateur caissier;

    @BeforeEach
    void setUp() {
        controleur = user(11L, "controleur", RoleCode.CONTROLEUR, 7L);
        caissier = user(12L, "caissier", RoleCode.CAISSIER, 7L);
        when(workflowTaskMapper.toDto(any(WorkflowTask.class))).thenAnswer(invocation -> {
            WorkflowTask task = invocation.getArgument(0);
            return WorkflowTaskItemDTO.builder()
                    .id(task.getId())
                    .typeAction(task.getTypeAction())
                    .module(task.getModule())
                    .referenceMetier(task.getReferenceMetier())
                    .entityType(task.getEntityType())
                    .entityId(task.getEntityId())
                    .titre(task.getTitre())
                    .priorite(task.getPriorite())
                    .statut(task.getStatut())
                    .roleDestinataire(task.getRoleDestinataire())
                    .dateCreation(task.getDateCreation())
                    .completedAt(task.getCompletedAt())
                    .build();
        });
        when(demandeCreditRepository.findByStatut(any())).thenReturn(List.of());
        when(depenseCaisseRepository.findAllByOrderByDateDemandeDesc()).thenReturn(List.of());
        when(demandeRetraitEpargneRepository.findByStatut(any())).thenReturn(List.of());
        when(recetteJournaliereTerrainRepository.findByStatutOrderByDateJourDesc(any())).thenReturn(List.of());
        when(collecteJournaliereTerrainRepository.findByStatutAndAntenneId(any(), anyLong())).thenReturn(List.of());
        when(sessionCaisseRepository.findAllByOrderByDateComptableDescDateOuvertureDescIdDesc()).thenReturn(List.of());
        when(workflowTaskRepository.findByUtilisateurDestinataireIdAndStatutInOrderByDateCreationDesc(anyLong(), anyCollection()))
            .thenReturn(List.of());
        when(workflowTaskRepository.findByRoleDestinataireAndAntenneIdAndStatutInOrderByDateCreationDesc(any(), anyLong(), anyCollection()))
            .thenReturn(List.of());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void onSessionPreCloturee_shouldCreateTaskWithoutDuplicateWhenTransitionReplayed() {
        authenticate(controleur, PermissionCode.TASK_READ_OWN.name(), PermissionCode.TASK_COMPLETE.name());

        when(workflowTaskRepository.findByActiveKey(any())).thenReturn(Optional.empty(), Optional.of(activeTask()));
        when(workflowTaskRepository.save(any())).thenAnswer(invocation -> {
            WorkflowTask task = invocation.getArgument(0);
            task.setId(100L);
            return task;
        });

        service.onSessionPreCloturee(501L, "CAI-SESSION-501", 7L, 3L);
        service.onSessionPreCloturee(501L, "CAI-SESSION-501", 7L, 3L);

        verify(workflowTaskRepository).save(any(WorkflowTask.class));
    }

    @Test
    void onSessionControleValide_shouldCreateChefBureauClosureTask() {
        authenticate(controleur, PermissionCode.TASK_COMPLETE.name());

        when(workflowTaskRepository.findByModuleAndEntityTypeAndEntityIdAndRoleDestinataireAndStatutIn(
                eq(WorkflowTaskModule.CAISSE), eq("SESSION_CAISSE"), eq(909L), eq(RoleCode.CONTROLEUR), anyCollection()))
                .thenReturn(List.of());
        when(workflowTaskRepository.findByActiveKey(any())).thenReturn(Optional.empty());
        when(workflowTaskRepository.save(any())).thenAnswer(invocation -> {
            WorkflowTask task = invocation.getArgument(0);
            task.setId(9090L);
            return task;
        });

        service.onSessionControleValide(909L, "CAI-SESSION-909", 7L, 3L);

        ArgumentCaptor<WorkflowTask> captor = ArgumentCaptor.forClass(WorkflowTask.class);
        verify(workflowTaskRepository).save(captor.capture());
        assertThat(captor.getValue().getRoleDestinataire()).isEqualTo(RoleCode.CHEF_BUREAU);
        assertThat(captor.getValue().getTypeAction()).isEqualTo(WorkflowTaskTypeAction.CLOTURER_SESSION_CAISSE);
    }

    @Test
    void onSessionCloturee_shouldNotGenerateTaskWhenNoActiveTasks() {
        authenticate(controleur, PermissionCode.TASK_COMPLETE.name());
        when(workflowTaskRepository.findByModuleAndEntityTypeAndEntityIdAndStatutIn(
                eq(WorkflowTaskModule.CAISSE), eq("SESSION_CAISSE"), eq(808L), anyCollection()))
                .thenReturn(List.of());

        service.onSessionCloturee(808L, "CAI-SESSION-808", 7L, 3L);

        verify(workflowTaskRepository, never()).save(any());
    }

    @Test
    void chefBureauShouldReceiveDepenseCaisseToValidateFromBusinessStatus() {
        Utilisateur chef = user(13L, "chef", RoleCode.CHEF_BUREAU, 7L);
        DepenseCaisse depense = depense(701L, DepenseCaisseStatus.EN_ATTENTE_VALIDATION, 7L, 17L);
        when(depenseCaisseRepository.findAllByOrderByDateDemandeDesc()).thenReturn(List.of(depense));
        when(workflowTaskRepository.findByModuleAndEntityTypeAndEntityIdAndRoleDestinataireAndStatutIn(
                eq(WorkflowTaskModule.CAISSE), eq("DEPENSE_CAISSE"), eq(701L), eq(RoleCode.CAISSIER), anyCollection()))
                .thenReturn(List.of());
        when(workflowTaskRepository.findByActiveKey(any())).thenReturn(Optional.empty());
        when(workflowTaskRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        authenticate(chef, PermissionCode.TASK_READ_ANTENNE.name());
        service.getMyActions("A_FAIRE");

        ArgumentCaptor<WorkflowTask> captor = ArgumentCaptor.forClass(WorkflowTask.class);
        verify(workflowTaskRepository).save(captor.capture());
        assertThat(captor.getValue().getTypeAction()).isEqualTo(WorkflowTaskTypeAction.DEPENSE_CAISSE_VALIDATE);
        assertThat(captor.getValue().getRoleDestinataire()).isEqualTo(RoleCode.CHEF_BUREAU);
        assertThat(captor.getValue().getReferenceMetier()).isEqualTo("DEPENSE-701");
    }

    @Test
    void caissierShouldReceiveDepenseCaisseToPayFromBusinessStatus() {
        DepenseCaisse depense = depense(702L, DepenseCaisseStatus.VALIDEE, 7L, 17L);
        when(depenseCaisseRepository.findAllByOrderByDateDemandeDesc()).thenReturn(List.of(depense));
        when(workflowTaskRepository.findByModuleAndEntityTypeAndEntityIdAndRoleDestinataireAndStatutIn(
                eq(WorkflowTaskModule.CAISSE), eq("DEPENSE_CAISSE"), eq(702L), eq(RoleCode.CHEF_BUREAU), anyCollection()))
                .thenReturn(List.of());
        when(workflowTaskRepository.findByActiveKey(any())).thenReturn(Optional.empty());
        when(workflowTaskRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        authenticate(caissier, PermissionCode.TASK_READ_ANTENNE.name());
        service.getMyActions("A_FAIRE");

        ArgumentCaptor<WorkflowTask> captor = ArgumentCaptor.forClass(WorkflowTask.class);
        verify(workflowTaskRepository).save(captor.capture());
        assertThat(captor.getValue().getTypeAction()).isEqualTo(WorkflowTaskTypeAction.DEPENSE_CAISSE_PAY);
        assertThat(captor.getValue().getRoleDestinataire()).isEqualTo(RoleCode.CAISSIER);
        assertThat(captor.getValue().getReferenceMetier()).isEqualTo("DEPENSE-702");
    }

    @Test
    void caissierShouldSeeDepensePayTaskInActionsAndDashboardWithOwnReadPermission() {
        WorkflowTask payTask = WorkflowTask.builder()
                .typeAction(WorkflowTaskTypeAction.DEPENSE_CAISSE_PAY)
                .module(WorkflowTaskModule.CAISSE)
                .referenceMetier("DEPENSE-703")
                .entityType("DEPENSE_CAISSE")
                .entityId(703L)
                .titre("Dépense à payer")
                .roleDestinataire(RoleCode.CAISSIER)
                .antenneId(7L)
                .siteId(17L)
                .priorite(WorkflowTaskPriority.HAUTE)
                .statut(WorkflowTaskStatus.A_FAIRE)
                .build();
        payTask.setId(7030L);
        payTask.setDateCreation(LocalDateTime.now());

        when(workflowTaskRepository.findByRoleDestinataireAndAntenneIdAndStatutInOrderByDateCreationDesc(
                eq(RoleCode.CAISSIER), eq(7L), anyCollection()))
                .thenReturn(List.of(payTask));

        authenticate(caissier, PermissionCode.TASK_READ_OWN.name());

        List<WorkflowTaskItemDTO> actions = service.getMyActions("A_FAIRE");
        WorkflowTaskDashboardDTO dashboard = service.getDashboard();

        assertThat(actions).hasSize(1);
        assertThat(actions.get(0).getTypeAction()).isEqualTo(WorkflowTaskTypeAction.DEPENSE_CAISSE_PAY);
        assertThat(actions.get(0).getRoleDestinataire()).isEqualTo(RoleCode.CAISSIER);
        assertThat(dashboard.getTotalAFaire()).isEqualTo(1);
    }

    @Test
    void depenseValideeAlreadyPaidShouldNotCreatePayTask() {
        DepenseCaisse depense = depense(704L, DepenseCaisseStatus.VALIDEE, 7L, 17L);
        depense.setPayePar(caissier);
        depense.setDatePaiement(LocalDateTime.now());
        when(depenseCaisseRepository.findAllByOrderByDateDemandeDesc()).thenReturn(List.of(depense));

        authenticate(caissier, PermissionCode.TASK_READ_OWN.name());

        service.getMyActions("A_FAIRE");

        verify(workflowTaskRepository, never()).save(any());
    }

    @Test
    void chefBureauShouldNeverSeeCaissierTaskEvenWithSupervisionPermission() {
        Utilisateur chef = user(14L, "chef-bureau", RoleCode.CHEF_BUREAU, 7L);
        WorkflowTask caissierTask = WorkflowTask.builder()
                .typeAction(WorkflowTaskTypeAction.DEPENSE_CAISSE_PAY)
                .module(WorkflowTaskModule.CAISSE)
                .referenceMetier("DEPENSE-800")
                .entityType("DEPENSE_CAISSE")
                .entityId(800L)
                .titre("Dépense à payer")
                .roleDestinataire(RoleCode.CAISSIER)
                .antenneId(7L)
                .siteId(17L)
                .priorite(WorkflowTaskPriority.HAUTE)
                .statut(WorkflowTaskStatus.A_FAIRE)
                .build();
        caissierTask.setId(8000L);
        caissierTask.setDateCreation(LocalDateTime.now());

        when(workflowTaskRepository.findByStatutInOrderByDateCreationDesc(anyCollection()))
                .thenReturn(List.of(caissierTask));

        authenticate(chef, PermissionCode.TASK_SUPERVISE.name());

        assertThat(service.getMyActions("A_FAIRE")).isEmpty();
        assertThat(service.getDashboard().getTasks()).isEmpty();
    }

        @Test
        void gerantGeneralShouldSeeBusinessTasksOnlyInSupervision() {
        Utilisateur gerant = user(21L, "gerant", RoleCode.GERANT_GENERAL, 7L);
        WorkflowTask caissierTask = WorkflowTask.builder()
            .typeAction(WorkflowTaskTypeAction.DEPENSE_CAISSE_PAY)
            .module(WorkflowTaskModule.CAISSE)
            .referenceMetier("DEPENSE-801")
            .entityType("DEPENSE_CAISSE")
            .entityId(801L)
            .titre("Dépense à payer")
            .roleDestinataire(RoleCode.CAISSIER)
            .antenneId(7L)
            .siteId(17L)
            .priorite(WorkflowTaskPriority.HAUTE)
            .statut(WorkflowTaskStatus.A_FAIRE)
            .build();
        caissierTask.setId(8010L);
        caissierTask.setDateCreation(LocalDateTime.now());

        when(workflowTaskRepository.findByStatutInOrderByDateCreationDesc(anyCollection()))
            .thenReturn(List.of(caissierTask));

        authenticate(gerant, PermissionCode.TASK_SUPERVISE.name());

        assertThat(service.getMyActions("A_FAIRE")).isEmpty();
        assertThat(service.getMyActionCount()).isZero();
        assertThat(service.getSupervisionActions("A_FAIRE"))
            .extracting(WorkflowTaskItemDTO::getRoleDestinataire)
            .containsExactly(RoleCode.CAISSIER);
        }

        @Test
        void adminWithCompletePermissionShouldNotTerminateTaskForAnotherRole() {
        Utilisateur admin = user(22L, "admin", RoleCode.ADMIN, 7L);
        WorkflowTask caissierTask = workflowTaskFor(RoleCode.CAISSIER, 7L);
        caissierTask.setId(8020L);

        authenticate(admin, PermissionCode.TASK_COMPLETE.name(), PermissionCode.TASK_SUPERVISE.name());
        when(workflowTaskRepository.findById(8020L)).thenReturn(Optional.of(caissierTask));

        assertThatThrownBy(() -> service.complete(8020L, "Clôture admin"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Seul le rôle destinataire");
        verify(workflowTaskRepository, never()).save(any());
        }

    @Test
    void directAssignedTaskMustStillMatchConnectedRole() {
        Utilisateur chef = user(15L, "chef-direct", RoleCode.CHEF_BUREAU, 7L);
        WorkflowTask caissierTask = workflowTaskFor(RoleCode.CAISSIER, 7L);
        caissierTask.setUtilisateurDestinataire(chef);
        caissierTask.setId(8100L);

        when(workflowTaskRepository.findByUtilisateurDestinataireIdAndStatutInOrderByDateCreationDesc(eq(15L), anyCollection()))
                .thenReturn(List.of(caissierTask));

        authenticate(chef, PermissionCode.TASK_READ_OWN.name());

        assertThat(service.getMyActions("A_FAIRE")).isEmpty();
    }

    @Test
    void caissierShouldReceiveOpenedSessionToPreCloseFromBusinessStatus() {
        SessionCaisse session = session(820L, StatutSessionCaisse.OUVERTE, 7L, 17L);
        when(sessionCaisseRepository.findAllByOrderByDateComptableDescDateOuvertureDescIdDesc()).thenReturn(List.of(session));
        when(workflowTaskRepository.findByActiveKey(any())).thenReturn(Optional.empty());
        when(workflowTaskRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        authenticate(caissier, PermissionCode.TASK_READ_ANTENNE.name());
        service.getMyActions("A_FAIRE");

        ArgumentCaptor<WorkflowTask> captor = ArgumentCaptor.forClass(WorkflowTask.class);
        verify(workflowTaskRepository).save(captor.capture());
        assertThat(captor.getValue().getTypeAction()).isEqualTo(WorkflowTaskTypeAction.PRE_CLOTURER_SESSION_CAISSE);
        assertThat(captor.getValue().getRoleDestinataire()).isEqualTo(RoleCode.CAISSIER);
        assertThat(captor.getValue().getReferenceMetier()).isEqualTo("CAI-SESSION-820");
    }

    @Test
    void onRetraitDemande_shouldCreateControleurTaskWithoutDuplicateWhenTransitionReplayed() {
        authenticate(controleur, PermissionCode.TASK_READ_OWN.name());

        when(workflowTaskRepository.findByActiveKey(any())).thenReturn(Optional.empty(), Optional.of(activeTask()));
        when(workflowTaskRepository.save(any())).thenAnswer(invocation -> {
            WorkflowTask task = invocation.getArgument(0);
            task.setId(1200L);
            return task;
        });

        service.onRetraitDemande(120L, "RETRAIT-120", 7L, 3L);
        service.onRetraitDemande(120L, "RETRAIT-120", 7L, 3L);

        ArgumentCaptor<WorkflowTask> captor = ArgumentCaptor.forClass(WorkflowTask.class);
        verify(workflowTaskRepository).save(captor.capture());
        assertThat(captor.getValue().getModule()).isEqualTo(WorkflowTaskModule.EPARGNE);
        assertThat(captor.getValue().getEntityType()).isEqualTo("RETRAIT_EPARGNE");
        assertThat(captor.getValue().getTypeAction()).isEqualTo(WorkflowTaskTypeAction.VALIDER_RETRAIT_EPARGNE);
        assertThat(captor.getValue().getRoleDestinataire()).isEqualTo(RoleCode.CONTROLEUR);
        assertThat(captor.getValue().getPriorite()).isEqualTo(WorkflowTaskPriority.NORMALE);
    }

        @Test
        void onRecetteSoumise_shouldCreateControleurTaskWithoutDuplicateWhenTransitionReplayed() {
        authenticate(controleur, PermissionCode.TASK_READ_OWN.name());

        when(workflowTaskRepository.findByActiveKey(any())).thenReturn(Optional.empty(), Optional.of(activeTask()));
        when(workflowTaskRepository.save(any())).thenAnswer(invocation -> {
            WorkflowTask task = invocation.getArgument(0);
            task.setId(1300L);
            return task;
        });

        service.onRecetteSoumise(130L, "RECETTE-130", 7L, 3L);
        service.onRecetteSoumise(130L, "RECETTE-130", 7L, 3L);

        ArgumentCaptor<WorkflowTask> captor = ArgumentCaptor.forClass(WorkflowTask.class);
        verify(workflowTaskRepository).save(captor.capture());
        assertThat(captor.getValue().getModule()).isEqualTo(WorkflowTaskModule.RECETTE);
        assertThat(captor.getValue().getEntityType()).isEqualTo("RECETTE_TERRAIN");
        assertThat(captor.getValue().getTypeAction()).isEqualTo(WorkflowTaskTypeAction.CONTROLER_RECETTE_TERRAIN);
        assertThat(captor.getValue().getRoleDestinataire()).isEqualTo(RoleCode.CONTROLEUR);
        assertThat(captor.getValue().getPriorite()).isEqualTo(WorkflowTaskPriority.NORMALE);
        }

        @Test
        void onRecetteValidee_shouldCloseControleurTaskWithoutCreatingNewTask() {
        authenticate(controleur, PermissionCode.TASK_COMPLETE.name());

        WorkflowTask controleurTask = WorkflowTask.builder()
            .module(WorkflowTaskModule.RECETTE)
            .entityType("RECETTE_TERRAIN")
            .entityId(131L)
            .roleDestinataire(RoleCode.CONTROLEUR)
            .statut(WorkflowTaskStatus.EN_COURS)
            .build();
        controleurTask.setId(1310L);

        when(workflowTaskRepository.findByModuleAndEntityTypeAndEntityIdAndRoleDestinataireAndStatutIn(
            eq(WorkflowTaskModule.RECETTE), eq("RECETTE_TERRAIN"), eq(131L), eq(RoleCode.CONTROLEUR), anyCollection()))
            .thenReturn(List.of(controleurTask));
        when(workflowTaskRepository.saveAll(anyCollection())).thenAnswer(invocation -> invocation.getArgument(0));

        service.onRecetteValidee(131L, "RECETTE-131", 7L, 3L);

        verify(workflowTaskRepository).saveAll(anyCollection());
        verify(workflowTaskRepository, never()).save(any());
        }

        @Test
        void onRecetteRejetee_shouldCloseControleurTaskWithoutCreatingNewTask() {
        authenticate(controleur, PermissionCode.TASK_COMPLETE.name());

        WorkflowTask controleurTask = WorkflowTask.builder()
            .module(WorkflowTaskModule.RECETTE)
            .entityType("RECETTE_TERRAIN")
            .entityId(132L)
            .roleDestinataire(RoleCode.CONTROLEUR)
            .statut(WorkflowTaskStatus.A_FAIRE)
            .build();
        controleurTask.setId(1320L);

        when(workflowTaskRepository.findByModuleAndEntityTypeAndEntityIdAndRoleDestinataireAndStatutIn(
            eq(WorkflowTaskModule.RECETTE), eq("RECETTE_TERRAIN"), eq(132L), eq(RoleCode.CONTROLEUR), anyCollection()))
            .thenReturn(List.of(controleurTask));
        when(workflowTaskRepository.saveAll(anyCollection())).thenAnswer(invocation -> invocation.getArgument(0));

        service.onRecetteRejetee(132L, "RECETTE-132", 7L, 3L);

        verify(workflowTaskRepository).saveAll(anyCollection());
        verify(workflowTaskRepository, never()).save(any());
        }

        @Test
        void controleurShouldSeeRecetteTaskOfAntenne_andCaissierShouldNotSeeIt() {
        WorkflowTask recetteTask = WorkflowTask.builder()
            .typeAction(WorkflowTaskTypeAction.CONTROLER_RECETTE_TERRAIN)
            .module(WorkflowTaskModule.RECETTE)
            .referenceMetier("RECETTE-777")
            .entityType("RECETTE_TERRAIN")
            .entityId(777L)
            .titre("Recette journalière à contrôler")
            .roleDestinataire(RoleCode.CONTROLEUR)
            .antenneId(7L)
            .siteId(3L)
            .priorite(WorkflowTaskPriority.NORMALE)
            .statut(WorkflowTaskStatus.A_FAIRE)
            .build();
        recetteTask.setId(7770L);
        recetteTask.setDateCreation(LocalDateTime.now());

        when(workflowTaskRepository.findByUtilisateurDestinataireIdAndStatutInOrderByDateCreationDesc(anyLong(), anyCollection()))
            .thenReturn(List.of());
        when(workflowTaskRepository.findByRoleDestinataireAndAntenneIdAndStatutInOrderByDateCreationDesc(
            eq(RoleCode.CONTROLEUR), eq(7L), anyCollection()))
            .thenReturn(List.of(recetteTask));

        authenticate(controleur, PermissionCode.TASK_READ_ANTENNE.name());
        List<WorkflowTaskItemDTO> controleurTasks = service.getMyActions("A_FAIRE");

        authenticate(caissier, PermissionCode.TASK_READ_OWN.name());
        List<WorkflowTaskItemDTO> caissierTasks = service.getMyActions("A_FAIRE");

        assertThat(controleurTasks).hasSize(1);
        assertThat(controleurTasks.get(0).getModule()).isEqualTo(WorkflowTaskModule.RECETTE);
        assertThat(caissierTasks).isEmpty();
        }

        @Test
        void controleurShouldSeeCaissePreClotureeTask_andCaissierShouldNotSeeIt() {
        WorkflowTask caisseTask = WorkflowTask.builder()
            .typeAction(WorkflowTaskTypeAction.CONTROLER_SESSION_CAISSE)
            .module(WorkflowTaskModule.CAISSE)
            .referenceMetier("CAI-SESSION-3")
            .entityType("SESSION_CAISSE")
            .entityId(3L)
            .titre("Session caisse à contrôler")
            .roleDestinataire(RoleCode.CONTROLEUR)
            .antenneId(7L)
            .siteId(3L)
            .priorite(WorkflowTaskPriority.NORMALE)
            .statut(WorkflowTaskStatus.A_FAIRE)
            .build();
        caisseTask.setId(3070L);
        caisseTask.setDateCreation(LocalDateTime.now());

        when(workflowTaskRepository.findByUtilisateurDestinataireIdAndStatutInOrderByDateCreationDesc(anyLong(), anyCollection()))
            .thenReturn(List.of());
        when(workflowTaskRepository.findByRoleDestinataireAndAntenneIdAndStatutInOrderByDateCreationDesc(
            eq(RoleCode.CONTROLEUR), eq(7L), anyCollection()))
            .thenReturn(List.of(caisseTask));

        authenticate(controleur, PermissionCode.TASK_READ_ANTENNE.name());
        List<WorkflowTaskItemDTO> controleurTasks = service.getMyActions("A_FAIRE");

        authenticate(caissier, PermissionCode.TASK_READ_OWN.name());
        List<WorkflowTaskItemDTO> caissierTasks = service.getMyActions("A_FAIRE");

        assertThat(controleurTasks).hasSize(1);
        assertThat(controleurTasks.get(0).getModule()).isEqualTo(WorkflowTaskModule.CAISSE);
        assertThat(controleurTasks.get(0).getEntityId()).isEqualTo(3L);
        assertThat(caissierTasks).isEmpty();
        }

        @Test
        void onDemandeCreditSoumise_shouldCreateGestionnaireTaskWithoutDuplicateWhenTransitionReplayed() {
        Utilisateur gestionnaire = user(13L, "gestionnaire", RoleCode.GESTIONNAIRE, 7L);
        authenticate(gestionnaire, PermissionCode.TASK_READ_OWN.name());

        when(workflowTaskRepository.findByActiveKey(any())).thenReturn(Optional.empty(), Optional.of(activeTask()));
        when(workflowTaskRepository.save(any())).thenAnswer(invocation -> {
            WorkflowTask task = invocation.getArgument(0);
            task.setId(801L);
            return task;
        });

        service.onDemandeCreditSoumise(801L, "DCR-801", 7L, 3L);
        service.onDemandeCreditSoumise(801L, "DCR-801", 7L, 3L);

        ArgumentCaptor<WorkflowTask> captor = ArgumentCaptor.forClass(WorkflowTask.class);
        verify(workflowTaskRepository).save(captor.capture());
        assertThat(captor.getValue().getModule()).isEqualTo(WorkflowTaskModule.CREDIT);
        assertThat(captor.getValue().getEntityType()).isEqualTo("DEMANDE_CREDIT");
        assertThat(captor.getValue().getTypeAction()).isEqualTo(WorkflowTaskTypeAction.TRAITER_DEMANDE_CREDIT);
        assertThat(captor.getValue().getTitre()).isEqualTo("Crédit à pré-analyser");
        assertThat(captor.getValue().getRoleDestinataire()).isEqualTo(RoleCode.GESTIONNAIRE);
        }

        @Test
        void onDemandeCreditValidationChef_shouldCloseControleurAndCreateChefBureauTask() {
        authenticate(controleur, PermissionCode.TASK_COMPLETE.name());

        WorkflowTask controleurTask = WorkflowTask.builder()
            .module(WorkflowTaskModule.CREDIT)
            .entityType("DEMANDE_CREDIT")
            .entityId(802L)
            .roleDestinataire(RoleCode.CONTROLEUR)
            .statut(WorkflowTaskStatus.A_FAIRE)
            .build();
        controleurTask.setId(8020L);

        when(workflowTaskRepository.findByModuleAndEntityTypeAndEntityIdAndRoleDestinataireAndStatutIn(
            eq(WorkflowTaskModule.CREDIT), eq("DEMANDE_CREDIT"), eq(802L), eq(RoleCode.CONTROLEUR), anyCollection()))
            .thenReturn(List.of(controleurTask));
        when(workflowTaskRepository.saveAll(anyCollection())).thenAnswer(invocation -> invocation.getArgument(0));
        when(workflowTaskRepository.findByActiveKey(any())).thenReturn(Optional.empty());
        when(workflowTaskRepository.save(any())).thenAnswer(invocation -> {
            WorkflowTask task = invocation.getArgument(0);
            task.setId(8021L);
            return task;
        });

        service.onDemandeCreditValidationChef(802L, "DCR-802", 7L, 3L);

        verify(workflowTaskRepository).saveAll(anyCollection());
        ArgumentCaptor<WorkflowTask> captor = ArgumentCaptor.forClass(WorkflowTask.class);
        verify(workflowTaskRepository).save(captor.capture());
        assertThat(captor.getValue().getModule()).isEqualTo(WorkflowTaskModule.CREDIT);
        assertThat(captor.getValue().getEntityType()).isEqualTo("DEMANDE_CREDIT");
        assertThat(captor.getValue().getTypeAction()).isEqualTo(WorkflowTaskTypeAction.APPROUVER_DEMANDE_CREDIT);
        assertThat(captor.getValue().getRoleDestinataire()).isEqualTo(RoleCode.CHEF_BUREAU);
        }

        @Test
        void onDemandeCreditApprouvee_shouldCloseChefBureauAndCreateCaissierTask() {
        Utilisateur chef = user(14L, "chef", RoleCode.CHEF_BUREAU, 7L);
        authenticate(chef, PermissionCode.TASK_COMPLETE.name());

        WorkflowTask chefTask = WorkflowTask.builder()
            .module(WorkflowTaskModule.CREDIT)
            .entityType("DEMANDE_CREDIT")
            .entityId(803L)
            .roleDestinataire(RoleCode.CHEF_BUREAU)
            .statut(WorkflowTaskStatus.EN_COURS)
            .build();
        chefTask.setId(8030L);

        when(workflowTaskRepository.findByModuleAndEntityTypeAndEntityIdAndRoleDestinataireAndStatutIn(
            eq(WorkflowTaskModule.CREDIT), eq("DEMANDE_CREDIT"), eq(803L), eq(RoleCode.CHEF_BUREAU), anyCollection()))
            .thenReturn(List.of(chefTask));
        when(workflowTaskRepository.saveAll(anyCollection())).thenAnswer(invocation -> invocation.getArgument(0));
        when(workflowTaskRepository.findByActiveKey(any())).thenReturn(Optional.empty());
        when(workflowTaskRepository.save(any())).thenAnswer(invocation -> {
            WorkflowTask task = invocation.getArgument(0);
            task.setId(8031L);
            return task;
        });

        service.onDemandeCreditApprouvee(803L, 903L, "CR-903", 7L, 3L);

        verify(workflowTaskRepository).saveAll(anyCollection());
        ArgumentCaptor<WorkflowTask> captor = ArgumentCaptor.forClass(WorkflowTask.class);
        verify(workflowTaskRepository).save(captor.capture());
        assertThat(captor.getValue().getModule()).isEqualTo(WorkflowTaskModule.CREDIT);
        assertThat(captor.getValue().getEntityType()).isEqualTo("CREDIT");
        assertThat(captor.getValue().getTypeAction()).isEqualTo(WorkflowTaskTypeAction.DECAISSER_CREDIT);
        assertThat(captor.getValue().getRoleDestinataire()).isEqualTo(RoleCode.CAISSIER);
        }

        @Test
        void onCreditDecaisse_shouldCloseCaissierTaskWithoutCreatingNewTask() {
        authenticate(caissier, PermissionCode.TASK_COMPLETE.name());

        WorkflowTask caissierTask = WorkflowTask.builder()
            .module(WorkflowTaskModule.CREDIT)
            .entityType("CREDIT")
            .entityId(804L)
            .roleDestinataire(RoleCode.CAISSIER)
            .statut(WorkflowTaskStatus.A_FAIRE)
            .build();
        caissierTask.setId(8040L);

        when(workflowTaskRepository.findByModuleAndEntityTypeAndEntityIdAndRoleDestinataireAndStatutIn(
            eq(WorkflowTaskModule.CREDIT), eq("CREDIT"), eq(804L), eq(RoleCode.CAISSIER), anyCollection()))
            .thenReturn(List.of(caissierTask));
        when(workflowTaskRepository.saveAll(anyCollection())).thenAnswer(invocation -> invocation.getArgument(0));

        service.onCreditDecaisse(804L, "CR-804", 7L, 3L);

        verify(workflowTaskRepository).saveAll(anyCollection());
        verify(workflowTaskRepository, never()).save(any());
        }

        @Test
        void onDemandeCreditRejetee_shouldCloseAllActiveCreditTasks() {
        authenticate(controleur, PermissionCode.TASK_COMPLETE.name());

        WorkflowTask controleurTask = WorkflowTask.builder()
            .module(WorkflowTaskModule.CREDIT)
            .entityType("DEMANDE_CREDIT")
            .entityId(805L)
            .roleDestinataire(RoleCode.CONTROLEUR)
            .statut(WorkflowTaskStatus.A_FAIRE)
            .build();
        controleurTask.setId(8050L);

        when(workflowTaskRepository.findByModuleAndEntityTypeAndEntityIdAndStatutIn(
            eq(WorkflowTaskModule.CREDIT), eq("DEMANDE_CREDIT"), eq(805L), anyCollection()))
            .thenReturn(List.of(controleurTask));
        when(workflowTaskRepository.saveAll(anyCollection())).thenAnswer(invocation -> invocation.getArgument(0));

        service.onDemandeCreditRejetee(805L, "DCR-805", 7L, 3L);

        verify(workflowTaskRepository).saveAll(anyCollection());
        verify(workflowTaskRepository, never()).save(any());
        }

        @Test
        void gestionnaireShouldOnlyQueryGestionnaireTasksOfAntenne() {
        Utilisateur gestionnaire = user(15L, "gestionnaire", RoleCode.GESTIONNAIRE, 7L);

        when(workflowTaskRepository.findByUtilisateurDestinataireIdAndStatutInOrderByDateCreationDesc(anyLong(), anyCollection()))
            .thenReturn(List.of());
        when(workflowTaskRepository.findByRoleDestinataireAndAntenneIdAndStatutInOrderByDateCreationDesc(
            eq(RoleCode.GESTIONNAIRE), eq(7L), anyCollection()))
            .thenReturn(List.of());

        authenticate(gestionnaire, PermissionCode.TASK_READ_ANTENNE.name());
        List<WorkflowTaskItemDTO> gestionnaireTasks = service.getMyActions("A_FAIRE");

        authenticate(caissier, PermissionCode.TASK_READ_OWN.name());
        List<WorkflowTaskItemDTO> caissierTasks = service.getMyActions("A_FAIRE");

        assertThat(gestionnaireTasks).isEmpty();
        assertThat(caissierTasks).isEmpty();
        }

        @Test
        void dashboardGestionnaireShouldCountSubmittedCreditDemandAsTodo() {
        Utilisateur gestionnaire = user(17L, "gestionnaire.dashboard", RoleCode.GESTIONNAIRE, 7L);
        DemandeCredit soumise = demandeCredit(901L, "DCR-901", StatutDemandeCredit.SOUMISE, 7L, 3L);
        WorkflowTask visibleTask = creditTask(
            9010L,
            WorkflowTaskTypeAction.TRAITER_DEMANDE_CREDIT,
            RoleCode.GESTIONNAIRE,
            "DEMANDE_CREDIT",
            901L,
            "Crédit à pré-analyser",
            WorkflowTaskStatus.A_FAIRE,
            7L,
            3L
        );

        when(demandeCreditRepository.findByStatut(StatutDemandeCredit.SOUMISE)).thenReturn(List.of(soumise));
        when(workflowTaskRepository.findByModuleAndEntityTypeAndEntityIdAndRoleDestinataireAndStatutIn(
            eq(WorkflowTaskModule.CREDIT), eq("DEMANDE_CREDIT"), eq(901L), eq(RoleCode.GESTIONNAIRE), anyCollection()))
            .thenReturn(List.of());
        when(workflowTaskRepository.findByActiveKey(any())).thenReturn(Optional.empty());
        when(workflowTaskRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(workflowTaskRepository.findByRoleDestinataireAndAntenneIdAndStatutInOrderByDateCreationDesc(
            eq(RoleCode.GESTIONNAIRE), eq(7L), anyCollection()))
            .thenReturn(List.of(visibleTask));

        authenticate(gestionnaire, PermissionCode.TASK_READ_ANTENNE.name());
        WorkflowTaskDashboardDTO dashboard = service.getDashboard();

        assertThat(dashboard.getTotalAFaire()).isEqualTo(1);
        assertThat(dashboard.getTasks()).extracting(WorkflowTaskItemDTO::getTypeAction)
            .containsExactly(WorkflowTaskTypeAction.TRAITER_DEMANDE_CREDIT);
        ArgumentCaptor<WorkflowTask> captor = ArgumentCaptor.forClass(WorkflowTask.class);
        verify(workflowTaskRepository).save(captor.capture());
        assertThat(captor.getValue().getRoleDestinataire()).isEqualTo(RoleCode.GESTIONNAIRE);
        assertThat(captor.getValue().getTypeAction()).isEqualTo(WorkflowTaskTypeAction.TRAITER_DEMANDE_CREDIT);
        }

        @Test
        void gestionnaireDashboardShouldNotExposeCreditApprovalActions() {
        Utilisateur gestionnaire = user(18L, "gestionnaire.no.approval", RoleCode.GESTIONNAIRE, 7L);
        when(demandeCreditRepository.findByStatut(StatutDemandeCredit.SOUMISE)).thenReturn(List.of());
        when(workflowTaskRepository.findByRoleDestinataireAndAntenneIdAndStatutInOrderByDateCreationDesc(
            eq(RoleCode.GESTIONNAIRE), eq(7L), anyCollection()))
            .thenReturn(List.of());

        authenticate(gestionnaire, PermissionCode.TASK_READ_ANTENNE.name());
        WorkflowTaskDashboardDTO dashboard = service.getDashboard();

        assertThat(dashboard.getTotalAFaire()).isZero();
        verify(demandeCreditRepository, never()).findByStatut(StatutDemandeCredit.VALIDATION_CHEF);
        verify(workflowTaskRepository, never()).findByRoleDestinataireAndAntenneIdAndStatutInOrderByDateCreationDesc(
            eq(RoleCode.CHEF_BUREAU), eq(7L), anyCollection());
        }

        @Test
        void controleurShouldReceiveAnalysedCreditDemandToControl() {
        DemandeCredit demande = demandeCredit(902L, "DCR-902", StatutDemandeCredit.ANALYSE_TERRAIN_VALIDEE, 7L, 3L);

        when(demandeCreditRepository.findByStatut(StatutDemandeCredit.ANALYSE_TERRAIN_VALIDEE)).thenReturn(List.of(demande));
        when(workflowTaskRepository.findByModuleAndEntityTypeAndEntityIdAndRoleDestinataireAndStatutIn(
            eq(WorkflowTaskModule.CREDIT), eq("DEMANDE_CREDIT"), eq(902L), eq(RoleCode.CONTROLEUR), anyCollection()))
            .thenReturn(List.of());
        when(workflowTaskRepository.findByActiveKey(any())).thenReturn(Optional.empty());
        when(workflowTaskRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        authenticate(controleur, PermissionCode.TASK_READ_ANTENNE.name());
        service.getMyActions("A_FAIRE");

        ArgumentCaptor<WorkflowTask> captor = ArgumentCaptor.forClass(WorkflowTask.class);
        verify(workflowTaskRepository).save(captor.capture());
        assertThat(captor.getValue().getRoleDestinataire()).isEqualTo(RoleCode.CONTROLEUR);
        assertThat(captor.getValue().getTypeAction()).isEqualTo(WorkflowTaskTypeAction.CONTROLER_DEMANDE_CREDIT);
        }

        @Test
        void caissierShouldReceiveCollecteSoumiseWithoutConfirmedBilletage() {
        CollecteJournaliereTerrain collecte = collecte(1401L, RecetteStatut.SOUMISE, false, 7L, 17L);

        when(collecteJournaliereTerrainRepository.findByStatutAndAntenneId(RecetteStatut.SOUMISE, 7L))
            .thenReturn(List.of(collecte));
        when(workflowTaskRepository.findByModuleAndEntityTypeAndEntityIdAndRoleDestinataireAndStatutIn(
            eq(WorkflowTaskModule.RECETTE), eq("COLLECTE_TERRAIN"), eq(1401L), eq(RoleCode.CONTROLEUR), anyCollection()))
            .thenReturn(List.of());
        when(workflowTaskRepository.findByModuleAndEntityTypeAndEntityIdAndRoleDestinataireAndStatutIn(
            eq(WorkflowTaskModule.RECETTE), eq("COLLECTE_TERRAIN"), eq(1401L), eq(RoleCode.CAISSIER), anyCollection()))
            .thenReturn(List.of());
        when(workflowTaskRepository.findByActiveKey(any())).thenReturn(Optional.empty());
        when(workflowTaskRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        authenticate(caissier, PermissionCode.TASK_READ_ANTENNE.name());
        service.getMyActions("A_FAIRE");

        ArgumentCaptor<WorkflowTask> captor = ArgumentCaptor.forClass(WorkflowTask.class);
        verify(workflowTaskRepository).save(captor.capture());
        assertThat(captor.getValue().getModule()).isEqualTo(WorkflowTaskModule.RECETTE);
        assertThat(captor.getValue().getEntityType()).isEqualTo("COLLECTE_TERRAIN");
        assertThat(captor.getValue().getEntityId()).isEqualTo(1401L);
        assertThat(captor.getValue().getTypeAction()).isEqualTo(WorkflowTaskTypeAction.EFFECTUER_BILLETAGE);
        assertThat(captor.getValue().getRoleDestinataire()).isEqualTo(RoleCode.CAISSIER);
        assertThat(captor.getValue().getReferenceMetier()).isEqualTo("COLLECTE-1401");
        }

        @Test
        void controleurShouldReceiveCollecteSoumiseAfterConfirmedBilletage() {
        CollecteJournaliereTerrain collecte = collecte(1404L, RecetteStatut.SOUMISE, true, 7L, 17L);

        when(collecteJournaliereTerrainRepository.findByStatutAndAntenneId(RecetteStatut.SOUMISE, 7L))
            .thenReturn(List.of(collecte));
        when(workflowTaskRepository.findByModuleAndEntityTypeAndEntityIdAndRoleDestinataireAndStatutIn(
            eq(WorkflowTaskModule.RECETTE), eq("COLLECTE_TERRAIN"), eq(1404L), eq(RoleCode.CAISSIER), anyCollection()))
            .thenReturn(List.of());
        when(workflowTaskRepository.findByModuleAndEntityTypeAndEntityIdAndRoleDestinataireAndStatutIn(
            eq(WorkflowTaskModule.RECETTE), eq("COLLECTE_TERRAIN"), eq(1404L), eq(RoleCode.CONTROLEUR), anyCollection()))
            .thenReturn(List.of());
        when(workflowTaskRepository.findByActiveKey(any())).thenReturn(Optional.empty());
        when(workflowTaskRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        authenticate(controleur, PermissionCode.TASK_READ_ANTENNE.name());
        service.getMyActions("A_FAIRE");

        ArgumentCaptor<WorkflowTask> captor = ArgumentCaptor.forClass(WorkflowTask.class);
        verify(workflowTaskRepository).save(captor.capture());
        assertThat(captor.getValue().getTypeAction()).isEqualTo(WorkflowTaskTypeAction.CONTROLER_RECETTE_TERRAIN);
        assertThat(captor.getValue().getRoleDestinataire()).isEqualTo(RoleCode.CONTROLEUR);
        assertThat(captor.getValue().getEntityType()).isEqualTo("COLLECTE_TERRAIN");
        }

        @Test
        void caissierDashboardShouldCountCollecteSoumiseBeforeBilletage() {
        CollecteJournaliereTerrain collecte = collecte(1403L, RecetteStatut.SOUMISE, false, 7L, 17L);
        WorkflowTask visibleTask = WorkflowTask.builder()
            .typeAction(WorkflowTaskTypeAction.EFFECTUER_BILLETAGE)
            .module(WorkflowTaskModule.RECETTE)
            .referenceMetier("COLLECTE-1403")
            .entityType("COLLECTE_TERRAIN")
            .entityId(1403L)
            .titre("Collecte à billeter")
            .roleDestinataire(RoleCode.CAISSIER)
            .antenneId(7L)
            .siteId(17L)
            .priorite(WorkflowTaskPriority.NORMALE)
            .statut(WorkflowTaskStatus.A_FAIRE)
            .build();
        visibleTask.setId(14030L);
        visibleTask.setDateCreation(LocalDateTime.now());

        when(collecteJournaliereTerrainRepository.findByStatutAndAntenneId(RecetteStatut.SOUMISE, 7L))
            .thenReturn(List.of(collecte));
        when(workflowTaskRepository.findByModuleAndEntityTypeAndEntityIdAndRoleDestinataireAndStatutIn(
            eq(WorkflowTaskModule.RECETTE), eq("COLLECTE_TERRAIN"), eq(1403L), eq(RoleCode.CONTROLEUR), anyCollection()))
            .thenReturn(List.of());
        when(workflowTaskRepository.findByModuleAndEntityTypeAndEntityIdAndRoleDestinataireAndStatutIn(
            eq(WorkflowTaskModule.RECETTE), eq("COLLECTE_TERRAIN"), eq(1403L), eq(RoleCode.CAISSIER), anyCollection()))
            .thenReturn(List.of());
        when(workflowTaskRepository.findByActiveKey(any())).thenReturn(Optional.empty());
        when(workflowTaskRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(workflowTaskRepository.findByRoleDestinataireAndAntenneIdAndStatutInOrderByDateCreationDesc(
            eq(RoleCode.CAISSIER), eq(7L), anyCollection()))
            .thenReturn(List.of(visibleTask));

        authenticate(caissier, PermissionCode.TASK_READ_ANTENNE.name());
        WorkflowTaskDashboardDTO dashboard = service.getDashboard();

        assertThat(dashboard.getTotalAFaire()).isEqualTo(1);
        assertThat(dashboard.getTasks()).extracting(WorkflowTaskItemDTO::getEntityType)
            .containsExactly("COLLECTE_TERRAIN");
        }

        @Test
        void collecteValideeShouldCloseActiveCollecteTask() {
        CollecteJournaliereTerrain collecte = collecte(1402L, RecetteStatut.VALIDEE, true, 7L, 17L);
        WorkflowTask activeTask = WorkflowTask.builder()
            .module(WorkflowTaskModule.RECETTE)
            .entityType("COLLECTE_TERRAIN")
            .entityId(1402L)
            .roleDestinataire(RoleCode.CONTROLEUR)
            .statut(WorkflowTaskStatus.A_FAIRE)
            .build();
        activeTask.setId(14020L);

        when(collecteJournaliereTerrainRepository.findByStatutAndAntenneId(RecetteStatut.VALIDEE, 7L))
            .thenReturn(List.of(collecte));
        when(workflowTaskRepository.findByModuleAndEntityTypeAndEntityIdAndStatutIn(
            eq(WorkflowTaskModule.RECETTE), eq("COLLECTE_TERRAIN"), eq(1402L), anyCollection()))
            .thenReturn(List.of(activeTask));
        when(workflowTaskRepository.saveAll(anyCollection())).thenAnswer(invocation -> invocation.getArgument(0));

        authenticate(controleur, PermissionCode.TASK_READ_ANTENNE.name());
        service.getMyActions("A_FAIRE");

        verify(workflowTaskRepository).saveAll(anyCollection());
        verify(workflowTaskRepository, never()).save(any());
        assertThat(activeTask.getStatut()).isEqualTo(WorkflowTaskStatus.TERMINEE);
        assertThat(activeTask.getCompletedAt()).isNotNull();
        }

        @Test
        void chefBureauShouldReceiveCreditDemandReadyToApprove() {
        Utilisateur chef = user(19L, "chef.approval", RoleCode.CHEF_BUREAU, 7L);
        DemandeCredit demande = demandeCredit(903L, "DCR-903", StatutDemandeCredit.VALIDATION_CHEF, 7L, 3L);

        when(demandeCreditRepository.findByStatut(StatutDemandeCredit.VALIDATION_CHEF)).thenReturn(List.of(demande));
        when(workflowTaskRepository.findByModuleAndEntityTypeAndEntityIdAndRoleDestinataireAndStatutIn(
            eq(WorkflowTaskModule.CREDIT), eq("DEMANDE_CREDIT"), eq(903L), eq(RoleCode.CHEF_BUREAU), anyCollection()))
            .thenReturn(List.of());
        when(workflowTaskRepository.findByActiveKey(any())).thenReturn(Optional.empty());
        when(workflowTaskRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        authenticate(chef, PermissionCode.TASK_READ_ANTENNE.name());
        service.getMyActions("A_FAIRE");

        ArgumentCaptor<WorkflowTask> captor = ArgumentCaptor.forClass(WorkflowTask.class);
        verify(workflowTaskRepository).save(captor.capture());
        assertThat(captor.getValue().getRoleDestinataire()).isEqualTo(RoleCode.CHEF_BUREAU);
        assertThat(captor.getValue().getTypeAction()).isEqualTo(WorkflowTaskTypeAction.APPROUVER_DEMANDE_CREDIT);
        }

        @Test
        void caissierShouldReceiveApprovedCreditToDisburse() {
        DemandeCredit demande = demandeCredit(904L, "DCR-904", StatutDemandeCredit.APPROUVEE, 7L, 3L);
        Credit credit = Credit.builder().demandeCredit(demande).site(demande.getSite()).build();
        credit.setId(1904L);
        credit.setStatut(StatutCredit.APPROUVE);
        demande.setCredit(credit);

        when(demandeCreditRepository.findByStatut(StatutDemandeCredit.APPROUVEE)).thenReturn(List.of(demande));
        when(workflowTaskRepository.findByModuleAndEntityTypeAndEntityIdAndRoleDestinataireAndStatutIn(
            eq(WorkflowTaskModule.CREDIT), eq("CREDIT"), eq(1904L), eq(RoleCode.CAISSIER), anyCollection()))
            .thenReturn(List.of());
        when(workflowTaskRepository.findByModuleAndEntityTypeAndEntityIdAndRoleDestinataireAndStatutIn(
            eq(WorkflowTaskModule.CREDIT), eq("DEMANDE_CREDIT"), eq(904L), eq(RoleCode.CHEF_BUREAU), anyCollection()))
            .thenReturn(List.of());
        when(workflowTaskRepository.findByActiveKey(any())).thenReturn(Optional.empty());
        when(workflowTaskRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        authenticate(caissier, PermissionCode.TASK_READ_ANTENNE.name());
        service.getMyActions("A_FAIRE");

        ArgumentCaptor<WorkflowTask> captor = ArgumentCaptor.forClass(WorkflowTask.class);
        verify(workflowTaskRepository).save(captor.capture());
        assertThat(captor.getValue().getRoleDestinataire()).isEqualTo(RoleCode.CAISSIER);
        assertThat(captor.getValue().getTypeAction()).isEqualTo(WorkflowTaskTypeAction.DECAISSER_CREDIT);
        assertThat(captor.getValue().getEntityType()).isEqualTo("CREDIT");
        assertThat(captor.getValue().getEntityId()).isEqualTo(1904L);
        }

        @Test
        void gestionnaireCountShouldOnlyQueryGestionnaireTasks() {
        Utilisateur gestionnaire = user(16L, "gestionnaire.count", RoleCode.GESTIONNAIRE, 7L);

        when(workflowTaskRepository.findByUtilisateurDestinataireIdAndStatutInOrderByDateCreationDesc(anyLong(), anyCollection()))
            .thenReturn(List.of());
        when(workflowTaskRepository.findByRoleDestinataireAndAntenneIdAndStatutInOrderByDateCreationDesc(
            eq(RoleCode.GESTIONNAIRE), eq(7L), anyCollection()))
            .thenReturn(List.of());
        authenticate(gestionnaire, PermissionCode.TASK_READ_ANTENNE.name());

        assertThat(service.getMyActionCount()).isZero();
        }

    @Test
    void onRetraitApprouve_shouldCloseControleurAndCreateCaissierTask() {
        authenticate(controleur, PermissionCode.TASK_COMPLETE.name());

        WorkflowTask controleurTask = WorkflowTask.builder()
                .module(WorkflowTaskModule.EPARGNE)
                .entityType("RETRAIT_EPARGNE")
                .entityId(121L)
                .roleDestinataire(RoleCode.CONTROLEUR)
                .statut(WorkflowTaskStatus.A_FAIRE)
                .build();
        controleurTask.setId(1210L);

        when(workflowTaskRepository.findByModuleAndEntityTypeAndEntityIdAndRoleDestinataireAndStatutIn(
                eq(WorkflowTaskModule.EPARGNE), eq("RETRAIT_EPARGNE"), eq(121L), eq(RoleCode.CONTROLEUR), anyCollection()))
                .thenReturn(List.of(controleurTask));
        when(workflowTaskRepository.saveAll(anyCollection())).thenAnswer(invocation -> invocation.getArgument(0));
        when(workflowTaskRepository.findByActiveKey(any())).thenReturn(Optional.empty());
        when(workflowTaskRepository.save(any())).thenAnswer(invocation -> {
            WorkflowTask task = invocation.getArgument(0);
            task.setId(1211L);
            return task;
        });

        service.onRetraitApprouve(121L, "RETRAIT-121", 7L, 3L);

        verify(workflowTaskRepository).saveAll(anyCollection());

        ArgumentCaptor<WorkflowTask> captor = ArgumentCaptor.forClass(WorkflowTask.class);
        verify(workflowTaskRepository).save(captor.capture());
        assertThat(captor.getValue().getModule()).isEqualTo(WorkflowTaskModule.EPARGNE);
        assertThat(captor.getValue().getEntityType()).isEqualTo("RETRAIT_EPARGNE");
        assertThat(captor.getValue().getTypeAction()).isEqualTo(WorkflowTaskTypeAction.PAYER_RETRAIT_EPARGNE);
        assertThat(captor.getValue().getRoleDestinataire()).isEqualTo(RoleCode.CAISSIER);
    }

    @Test
    void onRetraitRejete_shouldCloseControleurTaskWithoutCreatingNewTask() {
        authenticate(controleur, PermissionCode.TASK_COMPLETE.name());

        WorkflowTask controleurTask = WorkflowTask.builder()
                .module(WorkflowTaskModule.EPARGNE)
                .entityType("RETRAIT_EPARGNE")
                .entityId(122L)
                .roleDestinataire(RoleCode.CONTROLEUR)
                .statut(WorkflowTaskStatus.EN_COURS)
                .build();
        controleurTask.setId(1220L);

        when(workflowTaskRepository.findByModuleAndEntityTypeAndEntityIdAndRoleDestinataireAndStatutIn(
                eq(WorkflowTaskModule.EPARGNE), eq("RETRAIT_EPARGNE"), eq(122L), eq(RoleCode.CONTROLEUR), anyCollection()))
                .thenReturn(List.of(controleurTask));
        when(workflowTaskRepository.saveAll(anyCollection())).thenAnswer(invocation -> invocation.getArgument(0));

        service.onRetraitRejete(122L, "RETRAIT-122", 7L, 3L);

        verify(workflowTaskRepository).saveAll(anyCollection());
        verify(workflowTaskRepository, never()).save(any());
    }

    @Test
    void onRetraitPaye_shouldCloseAllActiveTasksForRetraitEntity() {
        authenticate(caissier, PermissionCode.TASK_COMPLETE.name());

        WorkflowTask caissierTask = WorkflowTask.builder()
                .module(WorkflowTaskModule.EPARGNE)
                .entityType("RETRAIT_EPARGNE")
                .entityId(123L)
                .roleDestinataire(RoleCode.CAISSIER)
                .statut(WorkflowTaskStatus.A_FAIRE)
                .build();
        caissierTask.setId(1230L);

        when(workflowTaskRepository.findByModuleAndEntityTypeAndEntityIdAndStatutIn(
                eq(WorkflowTaskModule.EPARGNE), eq("RETRAIT_EPARGNE"), eq(123L), anyCollection()))
                .thenReturn(List.of(caissierTask));
        when(workflowTaskRepository.saveAll(anyCollection())).thenAnswer(invocation -> invocation.getArgument(0));

        service.onRetraitPaye(123L, "RETRAIT-123", 7L, 3L);

        verify(workflowTaskRepository).saveAll(anyCollection());
        verify(workflowTaskRepository, never()).save(any());
    }

    @Test
    void controleurShouldSeeControlTask_andCaissierShouldNotSeeIt() {
        WorkflowTask task = workflowTaskFor(RoleCode.CONTROLEUR, 7L);

        when(workflowTaskRepository.findByUtilisateurDestinataireIdAndStatutInOrderByDateCreationDesc(anyLong(), anyCollection()))
                .thenReturn(List.of());
        when(workflowTaskRepository.findByRoleDestinataireAndAntenneIdAndStatutInOrderByDateCreationDesc(
                eq(RoleCode.CONTROLEUR), eq(7L), anyCollection()))
                .thenReturn(List.of(task));

        authenticate(controleur, PermissionCode.TASK_READ_ANTENNE.name());
        List<WorkflowTaskItemDTO> controleurTasks = service.getMyActions("A_FAIRE");

        authenticate(caissier, PermissionCode.TASK_READ_OWN.name());
        List<WorkflowTaskItemDTO> caissierTasks = service.getMyActions("A_FAIRE");

        assertThat(controleurTasks).hasSize(1);
        assertThat(caissierTasks).isEmpty();
    }

    @Test
    void complete_shouldReduceCountByIgnoringTerminee() {
        WorkflowTask todoTask = workflowTaskFor(RoleCode.CONTROLEUR, 7L);
        todoTask.setId(500L);
        todoTask.setStatut(WorkflowTaskStatus.A_FAIRE);

        WorkflowTask doneTask = workflowTaskFor(RoleCode.CONTROLEUR, 7L);
        doneTask.setId(501L);
        doneTask.setStatut(WorkflowTaskStatus.TERMINEE);

        when(workflowTaskRepository.findByUtilisateurDestinataireIdAndStatutInOrderByDateCreationDesc(anyLong(), anyCollection()))
                .thenReturn(List.of());
        when(workflowTaskRepository.findByRoleDestinataireAndAntenneIdAndStatutInOrderByDateCreationDesc(
                eq(RoleCode.CONTROLEUR), eq(7L), anyCollection()))
                .thenReturn(List.of(todoTask));

        authenticate(controleur, PermissionCode.TASK_READ_ANTENNE.name());
        long count = service.getMyActionCount();

        assertThat(count).isEqualTo(1);
        assertThat(doneTask.getStatut()).isEqualTo(WorkflowTaskStatus.TERMINEE);
    }

    @Test
    void complete_shouldMarkTaskDoneAndRemoveActiveKey() {
        Utilisateur user = user(20L, "chef", RoleCode.CHEF_BUREAU, 9L);
        WorkflowTask task = workflowTaskFor(RoleCode.CHEF_BUREAU, 9L);
        task.setId(777L);
        task.setUtilisateurDestinataire(user);
        task.setActiveKey("CAISSE|CLOTURER_SESSION_CAISSE|SESSION_CAISSE|777|CHEF_BUREAU|9|ACTIVE");

        authenticate(user, PermissionCode.TASK_COMPLETE.name(), PermissionCode.TASK_READ_OWN.name());
        when(workflowTaskRepository.findById(777L)).thenReturn(Optional.of(task));
        when(workflowTaskRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        WorkflowTaskItemDTO result = service.complete(777L, "Clôture traitée");

        assertThat(result.getStatut()).isEqualTo(WorkflowTaskStatus.TERMINEE);
        assertThat(result.getCompletedAt()).isNotNull();

        ArgumentCaptor<WorkflowTask> captor = ArgumentCaptor.forClass(WorkflowTask.class);
        verify(workflowTaskRepository).save(captor.capture());
        assertThat(captor.getValue().getActiveKey()).isNull();
        verify(auditService).logAction(any(), any(), eq("WorkflowTask"), eq(777L), eq(true), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void markAsViewed_shouldAuditViewEvent() {
        WorkflowTask task = workflowTaskFor(RoleCode.CONTROLEUR, 7L);
        task.setId(321L);
        task.setStatut(WorkflowTaskStatus.A_FAIRE);

        authenticate(controleur, PermissionCode.TASK_READ_OWN.name());
        when(workflowTaskRepository.findById(321L)).thenReturn(Optional.of(task));
        when(workflowTaskRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        WorkflowTaskItemDTO dto = service.markAsViewed(321L, "Pris en charge");

        assertThat(dto.getStatut()).isEqualTo(WorkflowTaskStatus.EN_COURS);
        verify(auditService).logAction(any(), any(), eq("WorkflowTask"), eq(321L), eq(true), any(), eq("Tâche marquée en cours | Pris en charge"), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void chefBureauShouldUseChefBureauRoleForVisibility() {
        Utilisateur chefBureau = user(30L, "chef", RoleCode.CHEF_BUREAU, 5L);
        when(workflowTaskRepository.findByUtilisateurDestinataireIdAndStatutInOrderByDateCreationDesc(anyLong(), anyCollection()))
                .thenReturn(List.of());
        when(workflowTaskRepository.findByRoleDestinataireAndAntenneIdAndStatutInOrderByDateCreationDesc(
                any(), eq(5L), anyCollection()))
                .thenReturn(List.of());

        authenticate(chefBureau, PermissionCode.TASK_READ_ANTENNE.name());
        service.getMyActions("A_FAIRE");

        verify(workflowTaskRepository, atLeastOnce()).findByRoleDestinataireAndAntenneIdAndStatutInOrderByDateCreationDesc(
            eq(RoleCode.CHEF_BUREAU), eq(5L), anyCollection());
    }

    @Test
    void mesActionsCountExclutSupervision() {
        Utilisateur gerant = user(31L, "gerant.count", RoleCode.GERANT_GENERAL, 7L);
        WorkflowTask caissierTask = workflowTaskFor(RoleCode.CAISSIER, 7L);
        caissierTask.setId(3100L);

        when(workflowTaskRepository.findByStatutInOrderByDateCreationDesc(anyCollection()))
                .thenReturn(List.of(caissierTask));

        authenticate(gerant, PermissionCode.TASK_SUPERVISE.name());

        assertThat(service.getSupervisionActions("A_FAIRE")).hasSize(1);
        assertThat(service.getMyActionCount()).isZero();
    }

    @Test
    void superviseurVoitTacheSansPouvoirTerminer() {
        Utilisateur admin = user(32L, "admin.supervision", RoleCode.ADMIN, 7L);
        WorkflowTask caissierTask = workflowTaskFor(RoleCode.CAISSIER, 7L);
        caissierTask.setId(3200L);

        when(workflowTaskRepository.findByStatutInOrderByDateCreationDesc(anyCollection()))
                .thenReturn(List.of(caissierTask));
        when(workflowTaskRepository.findById(3200L)).thenReturn(Optional.of(caissierTask));

        authenticate(admin, PermissionCode.TASK_SUPERVISE.name(), PermissionCode.TASK_COMPLETE.name());

        assertThat(service.getSupervisionActions("A_FAIRE"))
                .extracting(WorkflowTaskItemDTO::getRoleDestinataire)
                .containsExactly(RoleCode.CAISSIER);
        assertThatThrownBy(() -> service.complete(3200L, "clôture supervision"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Seul le rôle destinataire");
        verify(workflowTaskRepository, never()).save(caissierTask);
    }

    @Test
    void roleDestinatairePeutTerminer() {
        WorkflowTask controleurTask = workflowTaskFor(RoleCode.CONTROLEUR, 7L);
        controleurTask.setId(3300L);

        when(workflowTaskRepository.findById(3300L)).thenReturn(Optional.of(controleurTask));
        when(workflowTaskRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        authenticate(controleur, PermissionCode.TASK_READ_OWN.name(), PermissionCode.TASK_COMPLETE.name());

        WorkflowTaskItemDTO result = service.complete(3300L, "terminée par destinataire");

        assertThat(result.getStatut()).isEqualTo(WorkflowTaskStatus.TERMINEE);
        assertThat(controleurTask.getCompletedBy()).isEqualTo(controleur);
        verify(workflowTaskRepository).save(controleurTask);
    }

    @Test
    void autreRoleNePeutPasTerminer() {
        WorkflowTask controleurTask = workflowTaskFor(RoleCode.CONTROLEUR, 7L);
        controleurTask.setId(3400L);

        when(workflowTaskRepository.findById(3400L)).thenReturn(Optional.of(controleurTask));

        authenticate(caissier, PermissionCode.TASK_READ_OWN.name(), PermissionCode.TASK_COMPLETE.name());

        assertThatThrownBy(() -> service.complete(3400L, "tentative autre rôle"))
                .isInstanceOf(BusinessException.class);
        verify(workflowTaskRepository, never()).save(controleurTask);
    }

    @Test
    void tachePreAnalyseVaAuGestionnaire() {
        when(workflowTaskRepository.findByActiveKey(any())).thenReturn(Optional.empty());
        when(workflowTaskRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.onDemandeCreditSoumise(3500L, "DCR-3500", 7L, 3L);

        ArgumentCaptor<WorkflowTask> captor = ArgumentCaptor.forClass(WorkflowTask.class);
        verify(workflowTaskRepository).save(captor.capture());
        assertThat(captor.getValue().getTypeAction()).isEqualTo(WorkflowTaskTypeAction.TRAITER_DEMANDE_CREDIT);
        assertThat(captor.getValue().getRoleDestinataire()).isEqualTo(RoleCode.GESTIONNAIRE);
    }

    @Test
    void tacheApprobationVaAuChefBureau() {
        when(workflowTaskRepository.findByModuleAndEntityTypeAndEntityIdAndRoleDestinataireAndStatutIn(
                any(), any(), anyLong(), any(), anyCollection()))
                .thenReturn(List.of());
        when(workflowTaskRepository.findByActiveKey(any())).thenReturn(Optional.empty());
        when(workflowTaskRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.onDemandeCreditValidationChef(3600L, "DCR-3600", 7L, 3L);

        ArgumentCaptor<WorkflowTask> captor = ArgumentCaptor.forClass(WorkflowTask.class);
        verify(workflowTaskRepository).save(captor.capture());
        assertThat(captor.getValue().getTypeAction()).isEqualTo(WorkflowTaskTypeAction.APPROUVER_DEMANDE_CREDIT);
        assertThat(captor.getValue().getRoleDestinataire()).isEqualTo(RoleCode.CHEF_BUREAU);
    }

    @Test
    void tacheDecaissementVaAuCaissier() {
        when(workflowTaskRepository.findByModuleAndEntityTypeAndEntityIdAndRoleDestinataireAndStatutIn(
                any(), any(), anyLong(), any(), anyCollection()))
                .thenReturn(List.of());
        when(workflowTaskRepository.findByActiveKey(any())).thenReturn(Optional.empty());
        when(workflowTaskRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.onDemandeCreditApprouvee(3700L, 4700L, "CR-4700", 7L, 3L);

        ArgumentCaptor<WorkflowTask> captor = ArgumentCaptor.forClass(WorkflowTask.class);
        verify(workflowTaskRepository).save(captor.capture());
        assertThat(captor.getValue().getTypeAction()).isEqualTo(WorkflowTaskTypeAction.DECAISSER_CREDIT);
        assertThat(captor.getValue().getRoleDestinataire()).isEqualTo(RoleCode.CAISSIER);
    }

    @Test
    void workflowTaskNeCreeJamaisTacheVersRoleSupprime() {
        when(workflowTaskRepository.findByModuleAndEntityTypeAndEntityIdAndRoleDestinataireAndStatutIn(
                any(), any(), anyLong(), any(), anyCollection()))
                .thenReturn(List.of());
        when(workflowTaskRepository.findByActiveKey(any())).thenReturn(Optional.empty());
        when(workflowTaskRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.onDemandeCreditSoumise(3800L, "DCR-3800", 7L, 3L);
        service.onDemandeCreditValidationChef(3801L, "DCR-3801", 7L, 3L);
        service.onDemandeCreditApprouvee(3801L, 4801L, "CR-4801", 7L, 3L);

        ArgumentCaptor<WorkflowTask> captor = ArgumentCaptor.forClass(WorkflowTask.class);
        verify(workflowTaskRepository, org.mockito.Mockito.atLeast(3)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(task -> task.getRoleDestinataire().name())
                .doesNotContain("AGENT_BUREAU", "RESPONSABLE");
    }

    private WorkflowTask activeTask() {
        WorkflowTask task = new WorkflowTask();
        task.setStatut(WorkflowTaskStatus.A_FAIRE);
        return task;
    }

    private void authenticate(Utilisateur user, String... authorities) {
        List<SimpleGrantedAuthority> auth = java.util.Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new)
                .toList();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(user, "n/a", auth)
        );
    }

    private Utilisateur user(Long id, String username, RoleCode roleCode, Long agenceId) {
        Agence agence = Agence.builder().build();
        agence.setId(agenceId);
        Site site = Site.builder().agence(agence).build();
        site.setId(agenceId + 10);
        Role role = Role.builder().code(roleCode).build();
        Utilisateur user = Utilisateur.builder()
                .username(username)
                .nomComplet(username)
                .motDePasseHash("x")
                .role(role)
                .site(site)
                .build();
        user.setId(id);
        return user;
    }

    private WorkflowTask workflowTaskFor(RoleCode roleCode, Long antenneId) {
        WorkflowTask task = WorkflowTask.builder()
                .typeAction(WorkflowTaskTypeAction.CONTROLER_SESSION_CAISSE)
                .module(WorkflowTaskModule.CAISSE)
                .referenceMetier("CAI-SESSION-88")
                .entityType("SESSION_CAISSE")
                .entityId(88L)
                .titre("Session caisse à contrôler")
                .roleDestinataire(roleCode)
                .antenneId(antenneId)
                .siteId(3L)
                .priorite(WorkflowTaskPriority.HAUTE)
                .statut(WorkflowTaskStatus.A_FAIRE)
                .build();
        task.setId(88L);
        task.setDateCreation(LocalDateTime.now());
        return task;
    }

    private DepenseCaisse depense(Long id, DepenseCaisseStatus statut, Long agenceId, Long siteId) {
        Agence agence = Agence.builder().build();
        agence.setId(agenceId);
        Site site = Site.builder().agence(agence).build();
        site.setId(siteId);
        Caisse caisse = Caisse.builder().agence(agence).site(site).build();
        caisse.setId(30L);
        DepenseCaisse depense = DepenseCaisse.builder()
                .caisse(caisse)
                .site(site)
                .statut(statut)
                .build();
        depense.setId(id);
        return depense;
    }

    private SessionCaisse session(Long id, StatutSessionCaisse statut, Long agenceId, Long siteId) {
        Agence agence = Agence.builder().build();
        agence.setId(agenceId);
        Site site = Site.builder().agence(agence).build();
        site.setId(siteId);
        Caisse caisse = Caisse.builder().agence(agence).site(site).build();
        caisse.setId(40L);
        SessionCaisse session = SessionCaisse.builder()
                .caisse(caisse)
                .utilisateur(caissier)
                .statut(statut)
                .build();
        session.setId(id);
        return session;
    }

    private CollecteJournaliereTerrain collecte(Long id, RecetteStatut statut, boolean billetageConfirme, Long antenneId, Long siteId) {
        Site site = Site.builder().build();
        site.setId(siteId);
        CollecteJournaliereTerrain collecte = CollecteJournaliereTerrain.builder()
                .site(site)
                .antenneId(antenneId)
                .statut(statut)
                .billetageConfirme(billetageConfirme)
                .build();
        collecte.setId(id);
        return collecte;
    }

    private WorkflowTask creditTask(
            Long id,
            WorkflowTaskTypeAction typeAction,
            RoleCode roleCode,
            String entityType,
            Long entityId,
            String titre,
            WorkflowTaskStatus statut,
            Long antenneId,
            Long siteId
    ) {
        WorkflowTask task = WorkflowTask.builder()
                .typeAction(typeAction)
                .module(WorkflowTaskModule.CREDIT)
                .referenceMetier("DCR-" + entityId)
                .entityType(entityType)
                .entityId(entityId)
                .titre(titre)
                .roleDestinataire(roleCode)
                .antenneId(antenneId)
                .siteId(siteId)
                .priorite(WorkflowTaskPriority.NORMALE)
                .statut(statut)
                .build();
        task.setId(id);
        task.setDateCreation(LocalDateTime.now());
        return task;
    }

    private DemandeCredit demandeCredit(Long id, String numero, StatutDemandeCredit statut, Long agenceId, Long siteId) {
        Agence agence = Agence.builder().build();
        agence.setId(agenceId);
        Site site = Site.builder().agence(agence).build();
        site.setId(siteId);
        DemandeCredit demande = DemandeCredit.builder()
                .numeroDemande(numero)
                .site(site)
                .statut(statut)
                .build();
        demande.setId(id);
        return demande;
    }
}
