package com.mini.credit.controller;

import com.mini.credit.entity.agence.Agence;
import com.mini.credit.entity.employe.Employe;
import com.mini.credit.entity.referentiel.Role;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.entity.workflow.WorkflowTask;
import com.mini.credit.enums.security.PermissionCode;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.enums.workflow.WorkflowTaskModule;
import com.mini.credit.enums.workflow.WorkflowTaskPriority;
import com.mini.credit.enums.workflow.WorkflowTaskStatus;
import com.mini.credit.enums.workflow.WorkflowTaskTypeAction;
import com.mini.credit.repository.referentiel.RoleRepository;
import com.mini.credit.repository.utilisateur.UtilisateurRepository;
import com.mini.credit.repository.workflow.WorkflowTaskRepository;
import com.mini.credit.service.WorkflowTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest
@ActiveProfiles("test")
@WebAppConfiguration
@Transactional
@DisplayName("WorkflowTask API - Intégration sécurité runtime")
class WorkflowTaskApiSecurityIntegrationTest {

    private static final String ENTITY_TYPE_SESSION_CAISSE = "SESSION_CAISSE";
    private static final Set<WorkflowTaskStatus> ACTIVE_STATUSES =
            EnumSet.of(WorkflowTaskStatus.A_FAIRE, WorkflowTaskStatus.EN_COURS);

    @Autowired
        private WebApplicationContext context;

        private MockMvc mockMvc;

    @Autowired
    private WorkflowTaskRepository workflowTaskRepository;

    @Autowired
    private WorkflowTaskService workflowTaskService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

        @BeforeEach
        void initMockMvc() {
                mockMvc = webAppContextSetup(context)
                                .apply(springSecurity())
                                .build();
        }

    @Test
    void controleur_shouldSeeOwnAntenneOnly_andCanViewAndCompleteWithPermissions() throws Exception {
        Utilisateur controleur = principal("controleur-api", RoleCode.CONTROLEUR, 10L);

        WorkflowTask inScope = saveRoleTask(RoleCode.CONTROLEUR, 10L, WorkflowTaskStatus.A_FAIRE,
                WorkflowTaskTypeAction.CONTROLER_SESSION_CAISSE, "CAI-CTRL-10");
        WorkflowTask outOfScope = saveRoleTask(RoleCode.CONTROLEUR, 11L, WorkflowTaskStatus.A_FAIRE,
                WorkflowTaskTypeAction.CONTROLER_SESSION_CAISSE, "CAI-CTRL-11");

        mockMvc.perform(get("/api/me/actions")
                        .param("statut", "A_FAIRE")
                        .with(auth(controleur, PermissionCode.TASK_READ_ANTENNE.name())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(inScope.getId().intValue())))
                .andExpect(jsonPath("$[*].id", not(hasItem(outOfScope.getId().intValue()))));

        mockMvc.perform(post("/api/actions/{id}/marquer-vue", inScope.getId())
                        .contentType(APPLICATION_JSON)
                        .content("{\"commentaire\":\"prise en charge\"}")
                        .with(auth(controleur, PermissionCode.TASK_READ_ANTENNE.name())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(inScope.getId().intValue()))
                .andExpect(jsonPath("$.statut").value("EN_COURS"));

        mockMvc.perform(post("/api/actions/{id}/terminer", inScope.getId())
                        .contentType(APPLICATION_JSON)
                        .content("{\"commentaire\":\"contrôle terminé\"}")
                        .with(auth(controleur,
                                PermissionCode.TASK_READ_ANTENNE.name(),
                                PermissionCode.TASK_COMPLETE.name())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(inScope.getId().intValue()))
                .andExpect(jsonPath("$.statut").value("TERMINEE"));
    }

    @Test
    void caissier_shouldSeeOnlyDirectTasks_andCannotCompleteWithoutTaskComplete() throws Exception {
        Role caissierRole = ensureRole(RoleCode.CAISSIER);
        Utilisateur caissierPersisted = saveUser("caissier-direct", caissierRole);
        Utilisateur caissierPrincipal = principal(caissierPersisted.getId(), "caissier-direct", RoleCode.CAISSIER, 10L);

        WorkflowTask directTask = saveDirectTask(caissierPersisted, 10L, WorkflowTaskStatus.A_FAIRE, "CAI-DIRECT-1");
        WorkflowTask controleurTask = saveRoleTask(RoleCode.CONTROLEUR, 10L, WorkflowTaskStatus.A_FAIRE,
                WorkflowTaskTypeAction.CONTROLER_SESSION_CAISSE, "CAI-CTRL-CAISSIER");

        mockMvc.perform(get("/api/me/actions")
                        .param("statut", "A_FAIRE")
                        .with(auth(caissierPrincipal, PermissionCode.TASK_READ_OWN.name())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(directTask.getId().intValue())))
                .andExpect(jsonPath("$[*].id", not(hasItem(controleurTask.getId().intValue()))));

        mockMvc.perform(post("/api/actions/{id}/terminer", directTask.getId())
                        .contentType(APPLICATION_JSON)
                        .content("{\"commentaire\":\"tentative sans droit\"}")
                        .with(auth(caissierPrincipal, PermissionCode.TASK_READ_OWN.name())))
                .andExpect(status().isForbidden());
    }

    @Test
    void chefBureau_shouldSeeAntenneTasksIncludingClosure_andNotOutsideScope() throws Exception {
        Utilisateur chef = principal("chef-api", RoleCode.CHEF_BUREAU, 20L);

        WorkflowTask inScope = saveRoleTask(RoleCode.CONTROLEUR, 20L, WorkflowTaskStatus.A_FAIRE,
                WorkflowTaskTypeAction.CONTROLER_SESSION_CAISSE, "CAI-CHEF-IN");
        WorkflowTask outOfScope = saveRoleTask(RoleCode.CONTROLEUR, 21L, WorkflowTaskStatus.A_FAIRE,
                WorkflowTaskTypeAction.CONTROLER_SESSION_CAISSE, "CAI-CHEF-OUT");

        workflowTaskService.onSessionControleValide(920L, "CAI-SESSION-920", 20L, 3L);
        WorkflowTask closureTask = workflowTaskRepository.findByActiveKey(
                "CAISSE|CLOTURER_SESSION_CAISSE|SESSION_CAISSE|920|CHEF_BUREAU|20|ACTIVE"
        ).orElseThrow();

        mockMvc.perform(get("/api/me/actions")
                        .param("statut", "A_FAIRE")
                        .with(auth(chef, PermissionCode.TASK_SUPERVISE.name())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(inScope.getId().intValue())))
                .andExpect(jsonPath("$[*].id", hasItem(closureTask.getId().intValue())))
                .andExpect(jsonPath("$[*].id", not(hasItem(outOfScope.getId().intValue()))));
    }

    @Test
    void rci_shouldConsultWithAuditPermission_butCannotCompleteOperationalTask() throws Exception {
        Utilisateur rci = principal("rci-api", RoleCode.RCI, 30L);
        WorkflowTask auditVisible = saveRoleTask(RoleCode.CONTROLEUR, 30L, WorkflowTaskStatus.A_FAIRE,
                WorkflowTaskTypeAction.CONTROLER_SESSION_CAISSE, "CAI-RCI-1");

        mockMvc.perform(get("/api/me/actions")
                        .param("statut", "A_FAIRE")
                        .with(auth(rci, PermissionCode.TASK_AUDIT.name())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(auditVisible.getId().intValue())));

        mockMvc.perform(post("/api/actions/{id}/terminer", auditVisible.getId())
                        .contentType(APPLICATION_JSON)
                        .content("{\"commentaire\":\"rci no-complete\"}")
                        .with(auth(rci, PermissionCode.TASK_AUDIT.name())))
                .andExpect(status().isForbidden());
    }

    @Test
    void gerantGeneral_andCoo_shouldHaveGlobalReadScopeWhenSupervisePermissionIsPresent() throws Exception {
        Utilisateur gerant = principal("gg-api", RoleCode.GERANT_GENERAL, 40L);
        Utilisateur coo = principal("coo-api", RoleCode.COO, 41L);
        WorkflowTask antenne40 = saveRoleTask(RoleCode.CONTROLEUR, 40L, WorkflowTaskStatus.A_FAIRE,
                WorkflowTaskTypeAction.CONTROLER_SESSION_CAISSE, "CAI-GLOBAL-40");
        WorkflowTask antenne41 = saveRoleTask(RoleCode.CHEF_BUREAU, 41L, WorkflowTaskStatus.A_FAIRE,
                WorkflowTaskTypeAction.CLOTURER_SESSION_CAISSE, "CAI-GLOBAL-41");

        mockMvc.perform(get("/api/me/actions")
                        .param("statut", "A_FAIRE")
                        .with(auth(gerant, PermissionCode.TASK_SUPERVISE.name())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(antenne40.getId().intValue())))
                .andExpect(jsonPath("$[*].id", hasItem(antenne41.getId().intValue())));

        mockMvc.perform(get("/api/me/actions")
                        .param("statut", "A_FAIRE")
                        .with(auth(coo, PermissionCode.TASK_SUPERVISE.name())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(antenne40.getId().intValue())))
                .andExpect(jsonPath("$[*].id", hasItem(antenne41.getId().intValue())));
    }

    @Test
    void userWithoutTaskPermissions_shouldBeForbiddenOnActionsEndpoints() throws Exception {
        Utilisateur user = principal("sans-task", RoleCode.CAISSIER, 50L);
        WorkflowTask anyTask = saveRoleTask(RoleCode.CONTROLEUR, 50L, WorkflowTaskStatus.A_FAIRE,
                WorkflowTaskTypeAction.CONTROLER_SESSION_CAISSE, "CAI-NO-PERM");

        mockMvc.perform(get("/api/me/actions").with(auth(user)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/me/actions/count").with(auth(user)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/actions/{id}/terminer", anyTask.getId())
                        .contentType(APPLICATION_JSON)
                        .content("{\"commentaire\":\"forbidden\"}")
                        .with(auth(user)))
                .andExpect(status().isForbidden());
    }

    @Test
    void preClotureeReplay_shouldKeepSingleActiveTask() {
        workflowTaskService.onSessionPreCloturee(3001L, "CAI-SESSION-3001", 60L, 3L);
        workflowTaskService.onSessionPreCloturee(3001L, "CAI-SESSION-3001", 60L, 3L);

        List<WorkflowTask> activeControleur = workflowTaskRepository
                .findByModuleAndEntityTypeAndEntityIdAndRoleDestinataireAndStatutIn(
                        WorkflowTaskModule.CAISSE,
                        ENTITY_TYPE_SESSION_CAISSE,
                        3001L,
                        RoleCode.CONTROLEUR,
                        ACTIVE_STATUSES
                );

        assertThat(activeControleur).hasSize(1);
    }

    @Test
    void fullCaisseCycle_shouldCreateTransitionAndCloseWithoutNewTasksOnCloturee() {
        Long entityId = 4001L;
        workflowTaskService.onSessionPreCloturee(entityId, "CAI-SESSION-4001", 70L, 3L);

        List<WorkflowTask> controleurActive = workflowTaskRepository
                .findByModuleAndEntityTypeAndEntityIdAndRoleDestinataireAndStatutIn(
                        WorkflowTaskModule.CAISSE,
                        ENTITY_TYPE_SESSION_CAISSE,
                        entityId,
                        RoleCode.CONTROLEUR,
                        ACTIVE_STATUSES
                );
        assertThat(controleurActive).hasSize(1);

        workflowTaskService.onSessionControleValide(entityId, "CAI-SESSION-4001", 70L, 3L);

        List<WorkflowTask> controleurAfterValidation = workflowTaskRepository
                .findByModuleAndEntityTypeAndEntityIdAndRoleDestinataireAndStatutIn(
                        WorkflowTaskModule.CAISSE,
                        ENTITY_TYPE_SESSION_CAISSE,
                        entityId,
                        RoleCode.CONTROLEUR,
                        ACTIVE_STATUSES
                );
        List<WorkflowTask> chefBureauActive = workflowTaskRepository
                .findByModuleAndEntityTypeAndEntityIdAndRoleDestinataireAndStatutIn(
                        WorkflowTaskModule.CAISSE,
                        ENTITY_TYPE_SESSION_CAISSE,
                        entityId,
                        RoleCode.CHEF_BUREAU,
                        ACTIVE_STATUSES
                );

        assertThat(controleurAfterValidation).isEmpty();
        assertThat(chefBureauActive).hasSize(1);

        long totalBeforeClose = workflowTaskRepository.count();
        workflowTaskService.onSessionCloturee(entityId, "CAI-SESSION-4001", 70L, 3L);
        long totalAfterClose = workflowTaskRepository.count();

        List<WorkflowTask> allActive = workflowTaskRepository
                .findByModuleAndEntityTypeAndEntityIdAndStatutIn(
                        WorkflowTaskModule.CAISSE,
                        ENTITY_TYPE_SESSION_CAISSE,
                        entityId,
                        ACTIVE_STATUSES
                );

        assertThat(allActive).isEmpty();
        assertThat(totalAfterClose).isEqualTo(totalBeforeClose);
    }

        @Test
        void dashboardGestionnaire_shouldReturnSubmittedCreditPreAnalysisCount() throws Exception {
                Utilisateur gestionnaire = principal("gestionnaire-dashboard-api", RoleCode.GESTIONNAIRE, 80L);

                workflowTaskService.onDemandeCreditSoumise(8101L, "DCR-8101", 80L, 3L);

                mockMvc.perform(get("/api/actions/dashboard")
                                                .with(auth(gestionnaire, PermissionCode.TASK_READ_ANTENNE.name())))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalAFaire").value(greaterThanOrEqualTo(1)))
                                .andExpect(jsonPath("$.countAFaire").value(greaterThanOrEqualTo(1)))
                                .andExpect(jsonPath("$.tasks[*].typeAction", hasItem("TRAITER_DEMANDE_CREDIT")))
                                .andExpect(jsonPath("$.tasks[*].roleDestinataire", hasItem("GESTIONNAIRE")));
        }

        @Test
        void dashboardChefBureau_shouldReturnCreditApprovalCount() throws Exception {
                Utilisateur chef = principal("chef-dashboard-api", RoleCode.CHEF_BUREAU, 81L);

                workflowTaskService.onDemandeCreditValidationChef(8102L, "DCR-8102", 81L, 3L);

                mockMvc.perform(get("/api/actions/dashboard")
                                                .with(auth(chef, PermissionCode.TASK_READ_ANTENNE.name())))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalAFaire").value(greaterThanOrEqualTo(1)))
                                .andExpect(jsonPath("$.tasks[*].typeAction", hasItem("APPROUVER_DEMANDE_CREDIT")))
                                .andExpect(jsonPath("$.tasks[*].roleDestinataire", hasItem("CHEF_BUREAU")));
        }

        @Test
        void dashboardControleur_shouldReturnRecetteValidationCount() throws Exception {
                Utilisateur controleur = principal("controleur-dashboard-api", RoleCode.CONTROLEUR, 82L);

                workflowTaskService.onRecetteSoumise(8201L, "RECETTE-8201", 82L, 3L);

                mockMvc.perform(get("/api/actions/dashboard")
                                                .with(auth(controleur, PermissionCode.TASK_READ_ANTENNE.name())))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalAFaire").value(greaterThanOrEqualTo(1)))
                                .andExpect(jsonPath("$.tasks[*].typeAction", hasItem("CONTROLER_RECETTE_TERRAIN")))
                                .andExpect(jsonPath("$.tasks[*].roleDestinataire", hasItem("CONTROLEUR")));
        }

        @Test
        void dashboardCaissier_shouldReturnApprovedCreditDisbursementCount() throws Exception {
                Utilisateur caissier = principal("caissier-dashboard-api", RoleCode.CAISSIER, 83L);

                workflowTaskService.onDemandeCreditApprouvee(8301L, 9301L, "CR-9301", 83L, 3L);

                mockMvc.perform(get("/api/actions/dashboard")
                                                .with(auth(caissier, PermissionCode.TASK_READ_ANTENNE.name())))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalAFaire").value(greaterThanOrEqualTo(1)))
                                .andExpect(jsonPath("$.tasks[*].typeAction", hasItem("DECAISSER_CREDIT")))
                                .andExpect(jsonPath("$.tasks[*].roleDestinataire", hasItem("CAISSIER")));
        }

        @Test
        void dashboardGestionnaire_shouldNotReturnChefBureauApprovalActions() throws Exception {
                Utilisateur gestionnaire = principal("gestionnaire-dashboard-scope-api", RoleCode.GESTIONNAIRE, 84L);

                workflowTaskService.onDemandeCreditValidationChef(8401L, "DCR-8401", 84L, 3L);

                mockMvc.perform(get("/api/actions/dashboard")
                                                .with(auth(gestionnaire, PermissionCode.TASK_READ_ANTENNE.name())))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.tasks[*].typeAction", not(hasItem("APPROUVER_DEMANDE_CREDIT"))))
                                .andExpect(jsonPath("$.tasks[*].roleDestinataire", not(hasItem("CHEF_BUREAU"))));
        }

    private RequestPostProcessor auth(Utilisateur principal, String... authorities) {
        List<GrantedAuthority> granted = Arrays.stream(authorities)
                .map(a -> (GrantedAuthority) new SimpleGrantedAuthority(a))
                .toList();
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(principal, "n/a", granted);
        return SecurityMockMvcRequestPostProcessors.authentication(authentication);
    }

    private WorkflowTask saveRoleTask(
            RoleCode roleDestinataire,
            Long antenneId,
            WorkflowTaskStatus statut,
            WorkflowTaskTypeAction action,
            String reference
    ) {
        WorkflowTask task = WorkflowTask.builder()
                .typeAction(action)
                .module(WorkflowTaskModule.CAISSE)
                .referenceMetier(reference)
                .entityType(ENTITY_TYPE_SESSION_CAISSE)
                .entityId(Math.abs(reference.hashCode()) + 0L)
                .titre("Tâche " + reference)
                .description("Test sécurité runtime")
                .roleDestinataire(roleDestinataire)
                .antenneId(antenneId)
                .siteId(3L)
                .priorite(WorkflowTaskPriority.NORMALE)
                .statut(statut)
                .activeKey("CAISSE|" + action.name() + "|" + ENTITY_TYPE_SESSION_CAISSE + "|" + Math.abs(reference.hashCode()) + "|" + roleDestinataire.name() + "|" + antenneId + "|ACTIVE")
                .build();
        task.setDateCreation(LocalDateTime.now());
        return workflowTaskRepository.save(task);
    }

    private WorkflowTask saveDirectTask(Utilisateur destinataire, Long antenneId, WorkflowTaskStatus statut, String reference) {
        WorkflowTask task = WorkflowTask.builder()
                .typeAction(WorkflowTaskTypeAction.CONTROLER_SESSION_CAISSE)
                .module(WorkflowTaskModule.CAISSE)
                .referenceMetier(reference)
                .entityType(ENTITY_TYPE_SESSION_CAISSE)
                .entityId(Math.abs(reference.hashCode()) + 0L)
                .titre("Tâche directe " + reference)
                .description("Affectation directe")
                .roleDestinataire(RoleCode.CAISSIER)
                .utilisateurDestinataire(destinataire)
                .antenneId(antenneId)
                .siteId(3L)
                .priorite(WorkflowTaskPriority.NORMALE)
                .statut(statut)
                .activeKey("CAISSE|CONTROLER_SESSION_CAISSE|" + ENTITY_TYPE_SESSION_CAISSE + "|" + Math.abs(reference.hashCode()) + "|CAISSIER|" + antenneId + "|ACTIVE")
                .build();
        task.setDateCreation(LocalDateTime.now());
        return workflowTaskRepository.save(task);
    }

    private Utilisateur saveUser(String username, Role role) {
        Utilisateur user = Utilisateur.builder()
                .username(username)
                .motDePasseHash("x")
                .nomComplet(username)
                .role(role)
                .isEnabled(true)
                .isLocked(false)
                .actif(true)
                .build();
        return utilisateurRepository.save(user);
    }

    private Role ensureRole(RoleCode code) {
        return roleRepository.findByCode(code)
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .code(code)
                        .libelle(code.name())
                        .isActive(true)
                        .build()));
    }

    private Utilisateur principal(String username, RoleCode roleCode, Long antenneId) {
        return principal(100000L + Math.abs(username.hashCode()), username, roleCode, antenneId);
    }

    private Utilisateur principal(Long id, String username, RoleCode roleCode, Long antenneId) {
        Role role = Role.builder().code(roleCode).libelle(roleCode.name()).isActive(true).build();
        Agence agence = Agence.builder().build();
        agence.setId(antenneId);
        Employe employe = Employe.builder().agence(agence).build();

        Utilisateur user = Utilisateur.builder()
                .username(username)
                .motDePasseHash("x")
                .nomComplet(username)
                .role(role)
                .employe(employe)
                .isEnabled(true)
                .isLocked(false)
                .actif(true)
                .build();
        user.setId(id);
        return user;
    }
}
