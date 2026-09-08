package com.mini.credit.service.impl;

import com.mini.credit.dto.workflow.WorkflowTaskReconcileResultDTO;
import com.mini.credit.entity.credit.DemandeCredit;
import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.entity.workflow.WorkflowTask;
import com.mini.credit.enums.StatutDemandeCredit;
import com.mini.credit.enums.StatutSessionCaisse;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.enums.workflow.WorkflowTaskModule;
import com.mini.credit.enums.workflow.WorkflowTaskStatus;
import com.mini.credit.repository.credit.DemandeCreditRepository;
import com.mini.credit.repository.caisse.SessionCaisseRepository;
import com.mini.credit.repository.workflow.WorkflowTaskRepository;
import com.mini.credit.service.WorkflowTaskReconciliationService;
import com.mini.credit.service.WorkflowTaskService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkflowTaskReconciliationServiceImpl implements WorkflowTaskReconciliationService {

    private static final String ENTITY_TYPE_SESSION_CAISSE = "SESSION_CAISSE";
        private static final String ENTITY_TYPE_DEMANDE_CREDIT = "DEMANDE_CREDIT";
    private static final Set<WorkflowTaskStatus> ACTIVE_STATUSES =
            EnumSet.of(WorkflowTaskStatus.A_FAIRE, WorkflowTaskStatus.EN_COURS);

    private final SessionCaisseRepository sessionCaisseRepository;
        private final DemandeCreditRepository demandeCreditRepository;
    private final WorkflowTaskRepository workflowTaskRepository;
    private final WorkflowTaskService workflowTaskService;

    @Override
    public WorkflowTaskReconcileResultDTO reconcileCaisseSessions() {
        long createdControleurTasks = 0;
        long createdChefBureauTasks = 0;
        long closedTasksForCloture = 0;

        List<SessionCaisse> sessions = sessionCaisseRepository.findAll();
        for (SessionCaisse session : sessions) {
            StatutSessionCaisse statut = session.getStatut();
            if (statut == null) {
                continue;
            }

            if (statut == StatutSessionCaisse.PRE_CLOTUREE) {
                if (!hasActiveRoleTask(session.getId(), RoleCode.CONTROLEUR)) {
                    workflowTaskService.onSessionPreCloturee(
                            session.getId(),
                            resolveWorkflowReference(session),
                            resolveAntenneId(session),
                            resolveSiteId(session)
                    );
                    createdControleurTasks++;
                }
                continue;
            }

            if (statut == StatutSessionCaisse.VALIDEE_CONTROLE) {
                if (isFinalClosureExpected(session) && !hasActiveRoleTask(session.getId(), RoleCode.CHEF_BUREAU)) {
                    workflowTaskService.onSessionControleValide(
                            session.getId(),
                            resolveWorkflowReference(session),
                            resolveAntenneId(session),
                            resolveSiteId(session)
                    );
                    createdChefBureauTasks++;
                }
                continue;
            }

            if (statut == StatutSessionCaisse.CLOTUREE && hasAnyActiveTask(session.getId())) {
                workflowTaskService.onSessionCloturee(
                        session.getId(),
                        resolveWorkflowReference(session),
                        resolveAntenneId(session),
                        resolveSiteId(session)
                );
                closedTasksForCloture++;
            }
        }

        return WorkflowTaskReconcileResultDTO.builder()
                .scannedSessions(sessions.size())
                .createdControleurTasks(createdControleurTasks)
                .createdChefBureauTasks(createdChefBureauTasks)
                .closedTasksForCloture(closedTasksForCloture)
                .build();
    }

    @Override
    public WorkflowTaskReconcileResultDTO reconcileSubmittedCreditDemands() {
        long scannedSubmittedCreditDemands = 0;
        long createdGestionnaireTasks = 0;

        List<DemandeCredit> demandesSoumises = demandeCreditRepository.findByStatut(StatutDemandeCredit.SOUMISE);
        for (DemandeCredit demande : demandesSoumises) {
            scannedSubmittedCreditDemands++;

            List<WorkflowTask> activeTasks = findActiveCreditGestionnaireTasks(demande.getId());
            boolean hasGestionnaireTask = activeTasks.stream().anyMatch(task -> task.getRoleDestinataire() == RoleCode.GESTIONNAIRE);
            if (hasGestionnaireTask) {
                continue;
            }

            workflowTaskService.onDemandeCreditSoumise(
                    demande.getId(),
                    resolveWorkflowReference(demande),
                    resolveAntenneId(demande),
                    resolveSiteId(demande)
            );
            createdGestionnaireTasks++;
        }

        return WorkflowTaskReconcileResultDTO.builder()
                .scannedSubmittedCreditDemands(scannedSubmittedCreditDemands)
                .createdGestionnaireTasks(createdGestionnaireTasks)
                .migratedLegacyGestionnaireTasks(0)
                .build();
    }

    private boolean hasActiveRoleTask(Long sessionId, RoleCode roleCode) {
        return !workflowTaskRepository.findByModuleAndEntityTypeAndEntityIdAndRoleDestinataireAndStatutIn(
                WorkflowTaskModule.CAISSE,
                ENTITY_TYPE_SESSION_CAISSE,
                sessionId,
                roleCode,
                ACTIVE_STATUSES
        ).isEmpty();
    }

    private boolean hasAnyActiveTask(Long sessionId) {
        return !workflowTaskRepository.findByModuleAndEntityTypeAndEntityIdAndStatutIn(
                WorkflowTaskModule.CAISSE,
                ENTITY_TYPE_SESSION_CAISSE,
                sessionId,
                ACTIVE_STATUSES
        ).isEmpty();
    }

    private List<WorkflowTask> findActiveCreditGestionnaireTasks(Long demandeId) {
        List<WorkflowTask> gestionnaireTasks = workflowTaskRepository.findByModuleAndEntityTypeAndEntityIdAndRoleDestinataireAndStatutIn(
                WorkflowTaskModule.CREDIT,
                ENTITY_TYPE_DEMANDE_CREDIT,
                demandeId,
                RoleCode.GESTIONNAIRE,
                ACTIVE_STATUSES
        );
        return gestionnaireTasks;
    }

    private boolean isFinalClosureExpected(SessionCaisse session) {
        return session.getDateCloture() == null;
    }

    private Long resolveAntenneId(SessionCaisse session) {
        if (session == null || session.getCaisse() == null || session.getCaisse().getAgence() == null) {
            return null;
        }
        return session.getCaisse().getAgence().getId();
    }

    private Long resolveSiteId(SessionCaisse session) {
        if (session == null || session.getCaisse() == null || session.getCaisse().getSite() == null) {
            return null;
        }
        return session.getCaisse().getSite().getId();
    }

    private String resolveWorkflowReference(SessionCaisse session) {
        if (session == null) {
            return "SESSION-UNKNOWN";
        }
        String codeCaisse = session.getCaisse() != null ? session.getCaisse().getCodeCaisse() : "CAISSE";
        return codeCaisse + "-SESSION-" + session.getId();
    }

    private Long resolveAntenneId(DemandeCredit demande) {
        if (demande == null) {
            return null;
        }
        if (demande.getSite() != null && demande.getSite().getAgence() != null) {
            return demande.getSite().getAgence().getId();
        }
        if (demande.getMembre() != null && demande.getMembre().getSite() != null && demande.getMembre().getSite().getAgence() != null) {
            return demande.getMembre().getSite().getAgence().getId();
        }
        return null;
    }

    private Long resolveSiteId(DemandeCredit demande) {
        if (demande == null) {
            return null;
        }
        if (demande.getSite() != null) {
            return demande.getSite().getId();
        }
        if (demande.getMembre() != null && demande.getMembre().getSite() != null) {
            return demande.getMembre().getSite().getId();
        }
        return null;
    }

    private String resolveWorkflowReference(DemandeCredit demande) {
        if (demande == null) {
            return "DEMANDE-UNKNOWN";
        }
        if (demande.getNumeroDemande() != null && !demande.getNumeroDemande().isBlank()) {
            return demande.getNumeroDemande();
        }
        return "DEMANDE-" + demande.getId();
    }
}
