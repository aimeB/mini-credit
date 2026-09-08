package com.mini.credit.service.impl;

import com.mini.credit.dto.workflow.WorkflowTaskDashboardDTO;
import com.mini.credit.dto.workflow.WorkflowTaskItemDTO;
import com.mini.credit.entity.caisse.DepenseCaisse;
import com.mini.credit.entity.caisse.RecetteJournaliereTerrain;
import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.entity.credit.DemandeCredit;
import com.mini.credit.entity.epargne.DemandeRetraitEpargne;
import com.mini.credit.entity.referentiel.CollecteJournaliereTerrain;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.DepenseCaisseStatus;
import com.mini.credit.enums.RecetteStatut;
import com.mini.credit.enums.StatutCredit;
import com.mini.credit.enums.StatutDemandeRetrait;
import com.mini.credit.entity.workflow.WorkflowTask;
import com.mini.credit.enums.StatutDemandeCredit;
import com.mini.credit.enums.StatutRecetteJournaliere;
import com.mini.credit.enums.StatutSessionCaisse;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.enums.security.AuditModule;
import com.mini.credit.enums.security.AuditSeverity;
import com.mini.credit.enums.security.PermissionCode;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.enums.workflow.WorkflowTaskModule;
import com.mini.credit.enums.workflow.WorkflowTaskPriority;
import com.mini.credit.enums.workflow.WorkflowTaskStatus;
import com.mini.credit.enums.workflow.WorkflowTaskTypeAction;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.exception.ResourceNotFoundException;
import com.mini.credit.mapper.WorkflowTaskMapper;
import com.mini.credit.repository.caisse.RecetteJournaliereTerrainRepository;
import com.mini.credit.repository.caisse.SessionCaisseRepository;
import com.mini.credit.repository.collecteTerrain.CollecteJournaliereTerrainRepository;
import com.mini.credit.repository.credit.CreditRepository;
import com.mini.credit.repository.credit.DemandeCreditRepository;
import com.mini.credit.repository.caisse.DepenseCaisseRepository;
import com.mini.credit.repository.epargne.DemandeRetraitEpargneRepository;
import com.mini.credit.repository.utilisateur.UtilisateurRepository;
import com.mini.credit.repository.workflow.WorkflowTaskRepository;
import com.mini.credit.service.WorkflowTaskService;
import com.mini.credit.service.audit.AuditService;
import com.mini.credit.service.security.SecurityUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkflowTaskServiceImpl implements WorkflowTaskService {

	private static final Logger log = LoggerFactory.getLogger(WorkflowTaskServiceImpl.class);
	private static final String DIAG_PREFIX = "[DIAG-MES-ACTIONS-CREDIT]";

	private static final String ENTITY_TYPE_SESSION_CAISSE = "SESSION_CAISSE";
	private static final String ENTITY_TYPE_RECETTE_TERRAIN = "RECETTE_TERRAIN";
	private static final String ENTITY_TYPE_DEMANDE_CREDIT = "DEMANDE_CREDIT";
	private static final String ENTITY_TYPE_CREDIT = "CREDIT";
	private static final String ENTITY_TYPE_RETRAIT_EPARGNE = "RETRAIT_EPARGNE";
	private static final String ENTITY_TYPE_DEPENSE_CAISSE = "DEPENSE_CAISSE";
	private static final String ENTITY_TYPE_COLLECTE_TERRAIN = "COLLECTE_TERRAIN";
	private static final String CREDIT_PRE_ANALYSE_TITLE = "Crédit à pré-analyser";
	private static final Set<WorkflowTaskStatus> ACTIVE_STATUSES =
			EnumSet.of(WorkflowTaskStatus.A_FAIRE, WorkflowTaskStatus.EN_COURS);
	private final WorkflowTaskRepository workflowTaskRepository;
	private final CreditRepository creditRepository;
	private final DemandeCreditRepository demandeCreditRepository;
	private final DepenseCaisseRepository depenseCaisseRepository;
	private final DemandeRetraitEpargneRepository demandeRetraitEpargneRepository;
	private final RecetteJournaliereTerrainRepository recetteJournaliereTerrainRepository;
	private final CollecteJournaliereTerrainRepository collecteJournaliereTerrainRepository;
	private final SessionCaisseRepository sessionCaisseRepository;
	private final UtilisateurRepository utilisateurRepository;
	private final AuditService auditService;
	private final WorkflowTaskMapper workflowTaskMapper;

	@Override
	public List<WorkflowTaskItemDTO> getMyActions(String statut) {
		Utilisateur currentUser = getCurrentUserOrThrow();
		RoleCode roleCode = resolveEffectiveRole(currentUser);
		Long antenneId = resolveAntenneId(currentUser);
		log.warn("{} getMyActions START statut={} userId={} username={} role={} authorities={} antenneId={} siteId={}",
				DIAG_PREFIX,
				statut,
				currentUser.getId(),
				currentUser.getUsername(),
				roleCode,
				resolveCurrentAuthorities(),
				antenneId,
				resolveSiteId(currentUser));
		synchronizeWorkflowTasks(roleCode, antenneId);

		Set<WorkflowTaskStatus> statuses = resolveStatuses(statut);
		List<WorkflowTask> visibles = findVisibleTasks(currentUser, roleCode, antenneId, statuses);
		log.warn("{} getMyActions END userId={} role={} statuses={} returnedCount={} tasks={}",
				DIAG_PREFIX,
				currentUser.getId(),
				roleCode,
				statuses,
				visibles.size(),
				visibles.stream().map(this::formatTask).toList());
		return visibles.stream().map(workflowTaskMapper::toDto).toList();
	}

	@Override
	public List<WorkflowTaskItemDTO> getSupervisionActions(String statut) {
		Utilisateur currentUser = getCurrentUserOrThrow();
		RoleCode roleCode = resolveEffectiveRole(currentUser);
		Long antenneId = resolveAntenneId(currentUser);
		if (!canAccessWorkflowSupervision(roleCode)) {
			throw new BusinessException("Permission TASK_SUPERVISE ou TASK_AUDIT requise");
		}

		Set<WorkflowTaskStatus> statuses = resolveStatuses(statut);
		return findSupervisionTasks(roleCode, antenneId, statuses).stream()
				.map(workflowTaskMapper::toDto)
				.toList();
	}

	@Override
	public long getMyActionCount() {
		Utilisateur currentUser = getCurrentUserOrThrow();
		RoleCode roleCode = resolveEffectiveRole(currentUser);
		Long antenneId = resolveAntenneId(currentUser);
		log.warn("{} getMyActionCount START userId={} username={} role={} authorities={} antenneId={} siteId={}",
				DIAG_PREFIX,
				currentUser.getId(),
				currentUser.getUsername(),
				roleCode,
				resolveCurrentAuthorities(),
				antenneId,
				resolveSiteId(currentUser));
		synchronizeWorkflowTasks(roleCode, antenneId);

		List<WorkflowTask> visibles = findVisibleTasks(currentUser, roleCode, antenneId, Set.of(WorkflowTaskStatus.A_FAIRE));
		log.warn("{} getMyActionCount END userId={} role={} returnedCount={} tasks={}",
				DIAG_PREFIX,
				currentUser.getId(),
				roleCode,
				visibles.size(),
				visibles.stream().map(this::formatTask).toList());
		return visibles.size();
	}

	@Override
	public WorkflowTaskDashboardDTO getDashboard() {
		List<WorkflowTaskItemDTO> actions = getMyActions(null);
		List<WorkflowTaskItemDTO> top = actions.stream()
				.filter(action -> action.getStatut() == WorkflowTaskStatus.A_FAIRE || action.getStatut() == WorkflowTaskStatus.EN_COURS)
				.limit(5)
				.toList();
		long totalAFaire = actions.stream().filter(action -> action.getStatut() == WorkflowTaskStatus.A_FAIRE).count();
		long totalEnCours = actions.stream().filter(action -> action.getStatut() == WorkflowTaskStatus.EN_COURS).count();
		long totalTerminee = actions.stream().filter(action -> action.getStatut() == WorkflowTaskStatus.TERMINEE).count();
		long urgentCount = actions.stream()
				.filter(action -> action.getStatut() == WorkflowTaskStatus.A_FAIRE || action.getStatut() == WorkflowTaskStatus.EN_COURS)
				.filter(action -> action.getPriorite() == WorkflowTaskPriority.HAUTE || action.getPriorite() == WorkflowTaskPriority.CRITIQUE)
				.count();
		LocalDateTime now = LocalDateTime.now();
		long overdueCount = actions.stream()
				.filter(action -> action.getStatut() == WorkflowTaskStatus.A_FAIRE || action.getStatut() == WorkflowTaskStatus.EN_COURS)
				.filter(action -> action.getDateEcheance() != null && action.getDateEcheance().isBefore(now))
				.count();
		return WorkflowTaskDashboardDTO.builder()
				.totalAFaire(totalAFaire)
				.totalEnCours(totalEnCours)
				.totalTerminee(totalTerminee)
				.urgentCount(urgentCount)
				.overdueCount(overdueCount)
				.countAFaire(totalAFaire)
				.tasks(top)
				.build();
	}

	@Override
	public WorkflowTaskItemDTO markAsViewed(Long taskId, String commentaire) {
		if (!hasAnyAuthority(PermissionCode.TASK_READ_OWN.name(), PermissionCode.TASK_READ_ANTENNE.name())) {
			throw new BusinessException("Permission TASK_READ_* requise");
		}

		Utilisateur currentUser = getCurrentUserOrThrow();
		RoleCode roleCode = resolveEffectiveRole(currentUser);
		WorkflowTask task = getVisibleTaskOrThrow(taskId);
		assertCanMutateTask(task, roleCode);
		if (task.getStatut() == WorkflowTaskStatus.A_FAIRE) {
			task.setStatut(WorkflowTaskStatus.EN_COURS);
			task.setCommentaire(cleanText(commentaire));
			WorkflowTask saved = workflowTaskRepository.save(task);
			auditTaskAction(saved, "Tâche marquée en cours", cleanText(commentaire));
			return workflowTaskMapper.toDto(saved);
		}
		return workflowTaskMapper.toDto(task);
	}

	@Override
	public WorkflowTaskItemDTO complete(Long taskId, String commentaire) {
		if (!hasAuthority(PermissionCode.TASK_COMPLETE.name())) {
			throw new BusinessException("Permission TASK_COMPLETE requise");
		}

		Utilisateur user = getCurrentUserOrThrow();
		RoleCode roleCode = resolveEffectiveRole(user);
		WorkflowTask task = getVisibleTaskOrThrow(taskId);
		assertCanMutateTask(task, roleCode);
		task.setStatut(WorkflowTaskStatus.TERMINEE);
		task.setCompletedBy(user);
		task.setCompletedAt(LocalDateTime.now());
		task.setCommentaire(cleanText(commentaire));
		task.setActiveKey(null);
		WorkflowTask saved = workflowTaskRepository.save(task);

		auditService.logAction(
				AuditAction.MODIFICATION_OPERATION,
				AuditModule.SESSION_CAISSE,
				"WorkflowTask",
				saved.getId(),
				true,
				AuditSeverity.INFO,
				"Tâche terminée",
				saved.getReferenceMetier(),
				null,
				null,
				null,
				null,
				saved.getEntityId(),
				saved.getSiteId(),
				null
		);

		return workflowTaskMapper.toDto(saved);
	}

	@Override
	public void onSessionPreCloturee(Long sessionId, String referenceMetier, Long antenneId, Long siteId) {
		upsertRoleTask(
				WorkflowTaskTypeAction.CONTROLER_SESSION_CAISSE,
				WorkflowTaskModule.CAISSE,
				ENTITY_TYPE_SESSION_CAISSE,
				sessionId,
				referenceMetier,
				"Session caisse à contrôler",
				"La session est soumise au contrôle et attend la validation du contrôleur.",
				RoleCode.CONTROLEUR,
				antenneId,
				siteId,
				WorkflowTaskPriority.HAUTE
		);

		completeRoleTask(
				WorkflowTaskModule.CAISSE,
				ENTITY_TYPE_SESSION_CAISSE,
				sessionId,
				RoleCode.CHEF_BUREAU,
				"Remplacée par l'étape contrôle"
		);
	}

	@Override
	public void onSessionControleValide(Long sessionId, String referenceMetier, Long antenneId, Long siteId) {
		completeRoleTask(
				WorkflowTaskModule.CAISSE,
				ENTITY_TYPE_SESSION_CAISSE,
				sessionId,
				RoleCode.CONTROLEUR,
				"Contrôle validé"
		);

		upsertRoleTask(
				WorkflowTaskTypeAction.CLOTURER_SESSION_CAISSE,
				WorkflowTaskModule.CAISSE,
				ENTITY_TYPE_SESSION_CAISSE,
				sessionId,
				referenceMetier,
				"Session caisse à clôturer",
				"Le contrôle est validé. La session attend la clôture finale.",
				RoleCode.CHEF_BUREAU,
				antenneId,
				siteId,
				WorkflowTaskPriority.HAUTE
		);
	}

	@Override
	public void onSessionCloturee(Long sessionId, String referenceMetier, Long antenneId, Long siteId) {
		completeAllTasksForEntity(
				WorkflowTaskModule.CAISSE,
				ENTITY_TYPE_SESSION_CAISSE,
				sessionId,
				"Session clôturée"
		);
	}

	@Override
	public void onRecetteSoumise(Long recetteId, String referenceMetier, Long antenneId, Long siteId) {
		upsertRoleTask(
				WorkflowTaskTypeAction.CONTROLER_RECETTE_TERRAIN,
				WorkflowTaskModule.RECETTE,
				ENTITY_TYPE_RECETTE_TERRAIN,
				recetteId,
				referenceMetier,
				"Recette journalière à contrôler",
				"La recette est soumise au contrôle et attend la validation du contrôleur.",
				RoleCode.CONTROLEUR,
				antenneId,
				siteId,
				WorkflowTaskPriority.NORMALE
		);
	}

	@Override
	public void onRecetteValidee(Long recetteId, String referenceMetier, Long antenneId, Long siteId) {
		completeRoleTask(
				WorkflowTaskModule.RECETTE,
				ENTITY_TYPE_RECETTE_TERRAIN,
				recetteId,
				RoleCode.CONTROLEUR,
				"Recette validée"
		);
	}

	@Override
	public void onRecetteRejetee(Long recetteId, String referenceMetier, Long antenneId, Long siteId) {
		completeRoleTask(
				WorkflowTaskModule.RECETTE,
				ENTITY_TYPE_RECETTE_TERRAIN,
				recetteId,
				RoleCode.CONTROLEUR,
				"Recette rejetée"
		);
	}

	@Override
	public void onRecetteEcartConstate(Long recetteId, String referenceMetier, Long antenneId, Long siteId) {
		upsertRoleTask(
				WorkflowTaskTypeAction.CONTROLER_RECETTE_TERRAIN,
				WorkflowTaskModule.RECETTE,
				ENTITY_TYPE_RECETTE_TERRAIN,
				recetteId,
				referenceMetier,
				"Recette journalière à contrôler",
				"Un écart a été constaté sur la recette. Contrôle requis.",
				RoleCode.CONTROLEUR,
				antenneId,
				siteId,
				WorkflowTaskPriority.NORMALE
		);
	}

	@Override
	public void onCollecteSoumise(Long collecteId, String referenceMetier, Long antenneId, Long siteId) {
		completeRoleTask(
				WorkflowTaskModule.RECETTE,
				ENTITY_TYPE_COLLECTE_TERRAIN,
				collecteId,
				RoleCode.CONTROLEUR,
				"Collecte en attente billetage"
		);
		upsertRoleTask(
				WorkflowTaskTypeAction.EFFECTUER_BILLETAGE,
				WorkflowTaskModule.RECETTE,
				ENTITY_TYPE_COLLECTE_TERRAIN,
				collecteId,
				referenceMetier,
				"Collecte à billeter",
				"La collecte est soumise et attend le billetage du caissier.",
				RoleCode.CAISSIER,
				antenneId,
				siteId,
				WorkflowTaskPriority.NORMALE
		);
	}

	@Override
	public void onCollecteBilletageConfirme(Long collecteId, String referenceMetier, Long antenneId, Long siteId) {
		completeRoleTask(
				WorkflowTaskModule.RECETTE,
				ENTITY_TYPE_COLLECTE_TERRAIN,
				collecteId,
				RoleCode.CAISSIER,
				"Billetage confirmé"
		);
		upsertRoleTask(
				WorkflowTaskTypeAction.CONTROLER_RECETTE_TERRAIN,
				WorkflowTaskModule.RECETTE,
				ENTITY_TYPE_COLLECTE_TERRAIN,
				collecteId,
				referenceMetier,
				"Collecte à contrôler",
				"La collecte est billetée et attend le contrôle du contrôleur.",
				RoleCode.CONTROLEUR,
				antenneId,
				siteId,
				WorkflowTaskPriority.NORMALE
		);
	}

	@Override
	public void onCollecteCloturee(Long collecteId, String referenceMetier, Long antenneId, Long siteId) {
		completeAllTasksForEntity(
				WorkflowTaskModule.RECETTE,
				ENTITY_TYPE_COLLECTE_TERRAIN,
				collecteId,
				"Collecte clôturée"
		);
	}

	@Override
	public void onDemandeCreditSoumise(Long demandeId, String referenceMetier, Long antenneId, Long siteId) {
		upsertRoleTask(
				WorkflowTaskTypeAction.TRAITER_DEMANDE_CREDIT,
				WorkflowTaskModule.CREDIT,
				ENTITY_TYPE_DEMANDE_CREDIT,
				demandeId,
				referenceMetier,
				CREDIT_PRE_ANALYSE_TITLE,
				"La demande est soumise et attend la pré-analyse du gestionnaire.",
				RoleCode.GESTIONNAIRE,
				antenneId,
				siteId,
				WorkflowTaskPriority.NORMALE
		);
	}

	@Override
	public void onDemandeCreditPreAnalyseValidee(Long demandeId, String referenceMetier, Long antenneId, Long siteId) {
		completeRoleTask(
				WorkflowTaskModule.CREDIT,
				ENTITY_TYPE_DEMANDE_CREDIT,
				demandeId,
				RoleCode.GESTIONNAIRE,
				"Pré-analyse validée"
		);

		upsertRoleTask(
				WorkflowTaskTypeAction.CONTROLER_DEMANDE_CREDIT,
				WorkflowTaskModule.CREDIT,
				ENTITY_TYPE_DEMANDE_CREDIT,
				demandeId,
				referenceMetier,
				"Demande crédit à analyser",
				"La pré-analyse est validée. La demande attend l'analyse du contrôleur.",
				RoleCode.CONTROLEUR,
				antenneId,
				siteId,
				WorkflowTaskPriority.HAUTE
		);
	}

	@Override
	public void onDemandeCreditAnalyseTerrainValidee(Long demandeId, String referenceMetier, Long antenneId, Long siteId) {
		completeRoleTask(
				WorkflowTaskModule.CREDIT,
				ENTITY_TYPE_DEMANDE_CREDIT,
				demandeId,
				RoleCode.CONTROLEUR,
				"Analyse risque validée"
		);

		upsertRoleTask(
				WorkflowTaskTypeAction.CONTROLER_DEMANDE_CREDIT,
				WorkflowTaskModule.CREDIT,
				ENTITY_TYPE_DEMANDE_CREDIT,
				demandeId,
				referenceMetier,
				"Demande crédit à contrôler",
				"L'analyse est validée et la demande attend le contrôle de garantie.",
				RoleCode.CONTROLEUR,
				antenneId,
				siteId,
				WorkflowTaskPriority.NORMALE
		);
	}

	@Override
	public void onDemandeCreditValidationChef(Long demandeId, String referenceMetier, Long antenneId, Long siteId) {
		completeRoleTask(
				WorkflowTaskModule.CREDIT,
				ENTITY_TYPE_DEMANDE_CREDIT,
				demandeId,
				RoleCode.CONTROLEUR,
				"Contrôle garantie terminé"
		);

		upsertRoleTask(
				WorkflowTaskTypeAction.APPROUVER_DEMANDE_CREDIT,
				WorkflowTaskModule.CREDIT,
				ENTITY_TYPE_DEMANDE_CREDIT,
				demandeId,
				referenceMetier,
				"Demande crédit à approuver",
				"Le contrôle est terminé et la demande attend la décision du chef.",
				RoleCode.CHEF_BUREAU,
				antenneId,
				siteId,
				WorkflowTaskPriority.HAUTE
		);
	}

	@Override
	public void onDemandeCreditApprouvee(Long demandeId, Long creditId, String referenceMetier, Long antenneId, Long siteId) {
		completeRoleTask(
				WorkflowTaskModule.CREDIT,
				ENTITY_TYPE_DEMANDE_CREDIT,
				demandeId,
				RoleCode.CHEF_BUREAU,
				"Demande approuvée"
		);

		upsertRoleTask(
				WorkflowTaskTypeAction.DECAISSER_CREDIT,
				WorkflowTaskModule.CREDIT,
				ENTITY_TYPE_CREDIT,
				creditId,
				referenceMetier,
				"Crédit à décaisser",
				"Le crédit est approuvé et attend le décaissement en caisse.",
				RoleCode.CAISSIER,
				antenneId,
				siteId,
				WorkflowTaskPriority.HAUTE
		);
	}

	@Override
	public void onCreditDecaisse(Long creditId, String referenceMetier, Long antenneId, Long siteId) {
		completeRoleTask(
				WorkflowTaskModule.CREDIT,
				ENTITY_TYPE_CREDIT,
				creditId,
				RoleCode.CAISSIER,
				"Crédit décaissé"
		);
	}

	@Override
	public void onDemandeCreditRejetee(Long demandeId, String referenceMetier, Long antenneId, Long siteId) {
		completeAllTasksForEntity(
				WorkflowTaskModule.CREDIT,
				ENTITY_TYPE_DEMANDE_CREDIT,
				demandeId,
				"Demande rejetée"
		);
	}

	@Override
	public void onRetraitDemande(Long retraitId, String referenceMetier, Long antenneId, Long siteId) {
		upsertRoleTask(
				WorkflowTaskTypeAction.VALIDER_RETRAIT_EPARGNE,
				WorkflowTaskModule.EPARGNE,
				ENTITY_TYPE_RETRAIT_EPARGNE,
				retraitId,
				referenceMetier,
				"Retrait épargne à valider",
				"La demande de retrait épargne attend la validation du contrôleur.",
				RoleCode.CONTROLEUR,
				antenneId,
				siteId,
				WorkflowTaskPriority.NORMALE
		);
	}

	@Override
	public void onRetraitApprouve(Long retraitId, String referenceMetier, Long antenneId, Long siteId) {
		completeRoleTask(
				WorkflowTaskModule.EPARGNE,
				ENTITY_TYPE_RETRAIT_EPARGNE,
				retraitId,
				RoleCode.CONTROLEUR,
				"Retrait approuvé"
		);

		upsertRoleTask(
				WorkflowTaskTypeAction.PAYER_RETRAIT_EPARGNE,
				WorkflowTaskModule.EPARGNE,
				ENTITY_TYPE_RETRAIT_EPARGNE,
				retraitId,
				referenceMetier,
				"Retrait épargne à payer",
				"Le retrait est validé et attend son paiement en caisse.",
				RoleCode.CAISSIER,
				antenneId,
				siteId,
				WorkflowTaskPriority.NORMALE
		);
	}

	@Override
	public void onRetraitPaye(Long retraitId, String referenceMetier, Long antenneId, Long siteId) {
		completeAllTasksForEntity(
				WorkflowTaskModule.EPARGNE,
				ENTITY_TYPE_RETRAIT_EPARGNE,
				retraitId,
				"Retrait payé"
		);
	}

	@Override
	public void onRetraitRejete(Long retraitId, String referenceMetier, Long antenneId, Long siteId) {
		completeRoleTask(
				WorkflowTaskModule.EPARGNE,
				ENTITY_TYPE_RETRAIT_EPARGNE,
				retraitId,
				RoleCode.CONTROLEUR,
				"Retrait rejeté"
		);
	}

	private WorkflowTask getVisibleTaskOrThrow(Long id) {
		WorkflowTask task = workflowTaskRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Tâche introuvable"));

		Utilisateur currentUser = getCurrentUserOrThrow();
		RoleCode roleCode = resolveEffectiveRole(currentUser);
		Long antenneId = resolveAntenneId(currentUser);

		boolean visible = isVisibleTo(task, currentUser, roleCode, antenneId);
		if (!visible) {
			throw new BusinessException("Tâche non accessible dans ce périmètre");
		}
		return task;
	}

	private List<WorkflowTask> findVisibleTasks(
			Utilisateur currentUser,
			RoleCode roleCode,
			Long antenneId,
			Set<WorkflowTaskStatus> statuses
	) {
		Map<Long, WorkflowTask> merged = new LinkedHashMap<>();
		boolean canReadOwn = hasAnyAuthority(
				PermissionCode.TASK_READ_OWN.name(),
				PermissionCode.TASK_READ_ANTENNE.name(),
				PermissionCode.TASK_SUPERVISE.name(),
				PermissionCode.TASK_AUDIT.name()
		);
		boolean canReadAntenne = hasAnyAuthority(
				PermissionCode.TASK_READ_ANTENNE.name(),
				PermissionCode.TASK_SUPERVISE.name(),
				PermissionCode.TASK_AUDIT.name()
		);
		boolean canSupervise = hasAnyAuthority(PermissionCode.TASK_SUPERVISE.name(), PermissionCode.TASK_AUDIT.name());
		boolean canReadByBusinessRole = isWorkflowTaskBusinessRole(roleCode);

		log.warn("{} findVisibleTasks userId={} role={} statuses={} antenneId={} canReadOwn={} canReadAntenne={} canSupervise={} canReadByBusinessRole={}",
				DIAG_PREFIX,
				currentUser.getId(),
				roleCode,
				statuses,
				antenneId,
				canReadOwn,
				canReadAntenne,
				canSupervise,
				canReadByBusinessRole);

		if (canReadOwn || canReadByBusinessRole) {
			List<WorkflowTask> directTasks = workflowTaskRepository
					.findByUtilisateurDestinataireIdAndStatutInOrderByDateCreationDesc(currentUser.getId(), statuses);
			directTasks.forEach(task -> merged.put(task.getId(), task));
			findRoleTasksForCurrentScope(roleCode, antenneId, statuses)
					.forEach(task -> merged.put(task.getId(), task));
			log.warn("{} findVisibleTasks after own/role read directCount={} mergedCount={} merged={}",
					DIAG_PREFIX,
					directTasks.size(),
					merged.size(),
					merged.values().stream().map(this::formatTask).toList());
		}

		if (canSupervise) {
			if (roleCode == RoleCode.ADMIN || roleCode == RoleCode.COO || roleCode == RoleCode.GERANT_GENERAL) {
				workflowTaskRepository.findByStatutInOrderByDateCreationDesc(statuses)
						.forEach(task -> merged.put(task.getId(), task));
			} else if (antenneId != null) {
				workflowTaskRepository.findByStatutInOrderByDateCreationDesc(statuses).stream()
						.filter(task -> antenneId.equals(task.getAntenneId()))
						.forEach(task -> merged.put(task.getId(), task));
			}
			return merged.values().stream()
					.filter(task -> isVisibleToConnectedRole(task, currentUser, roleCode))
					.toList();
		}

		if (canReadAntenne && antenneId != null) {
			for (RoleCode visibleRole : resolveVisibleTaskRoles(roleCode)) {
				findRoleTasksForCurrentScope(visibleRole, antenneId, statuses)
						.forEach(task -> merged.put(task.getId(), task));
			}
		}

		return merged.values().stream()
				.filter(this::isStillActionable)
				.filter(task -> isVisibleToConnectedRole(task, currentUser, roleCode))
				.toList();
	}

	private List<WorkflowTask> findSupervisionTasks(RoleCode roleCode, Long antenneId, Set<WorkflowTaskStatus> statuses) {
		return workflowTaskRepository.findByStatutInOrderByDateCreationDesc(statuses).stream()
				.filter(this::isStillActionable)
				.filter(task -> isVisibleInSupervisionScope(task, roleCode, antenneId))
				.toList();
	}

	private boolean isVisibleInSupervisionScope(WorkflowTask task, RoleCode roleCode, Long antenneId) {
		if (task == null || roleCode == null) {
			return false;
		}
		if (roleCode == RoleCode.ADMIN || roleCode == RoleCode.COO || roleCode == RoleCode.GERANT_GENERAL) {
			return true;
		}
		if (roleCode == RoleCode.RCI) {
			return task.getModule() == WorkflowTaskModule.CAISSE;
		}
		return antenneId != null && antenneId.equals(task.getAntenneId());
	}

	private boolean canAccessWorkflowSupervision(RoleCode roleCode) {
		return hasAnyAuthority(PermissionCode.TASK_SUPERVISE.name(), PermissionCode.TASK_AUDIT.name())
				|| roleCode == RoleCode.ADMIN
				|| roleCode == RoleCode.RCI
				|| roleCode == RoleCode.COO
				|| roleCode == RoleCode.GERANT_GENERAL;
	}

	private void assertCanMutateTask(WorkflowTask task, RoleCode roleCode) {
		if (task == null || task.getRoleDestinataire() != roleCode) {
			throw new BusinessException("Seul le rôle destinataire peut traiter cette tâche");
		}
	}

	private List<WorkflowTask> findRoleTasksForCurrentScope(RoleCode roleCode, Long antenneId, Set<WorkflowTaskStatus> statuses) {
		if (roleCode == null) {
			return List.of();
		}
		if (antenneId != null) {
			return workflowTaskRepository.findByRoleDestinataireAndAntenneIdAndStatutInOrderByDateCreationDesc(roleCode, antenneId, statuses);
		}
		return workflowTaskRepository.findByRoleDestinataireAndStatutInOrderByDateCreationDesc(roleCode, statuses).stream()
				.filter(task -> task.getAntenneId() == null || antenneId == null || antenneId.equals(task.getAntenneId()))
				.toList();
	}

	private boolean isStillActionable(WorkflowTask task) {
		if (task == null || task.getStatut() == WorkflowTaskStatus.TERMINEE) {
			return true;
		}
		if (task.getTypeAction() != WorkflowTaskTypeAction.DECAISSER_CREDIT
				|| task.getModule() != WorkflowTaskModule.CREDIT
				|| !ENTITY_TYPE_CREDIT.equals(task.getEntityType())
				|| task.getEntityId() == null) {
			return true;
		}

		return creditRepository.findById(task.getEntityId())
				.map(credit -> credit.getStatut() == StatutCredit.APPROUVE)
				.orElse(false);
	}

	private boolean isVisibleTo(WorkflowTask task, Utilisateur user, RoleCode roleCode, Long antenneId) {
		if (task.getUtilisateurDestinataire() != null && task.getUtilisateurDestinataire().getId().equals(user.getId())) {
			return isVisibleToConnectedRole(task, user, roleCode);
		}

		if (hasAnyAuthority(PermissionCode.TASK_SUPERVISE.name(), PermissionCode.TASK_AUDIT.name())) {
			if (roleCode == RoleCode.ADMIN || roleCode == RoleCode.COO || roleCode == RoleCode.GERANT_GENERAL) {
				return true;
			}
			if (roleCode == RoleCode.RCI && task.getModule() != WorkflowTaskModule.CAISSE) {
				return false;
			}
			return antenneId != null && antenneId.equals(task.getAntenneId()) && isVisibleToConnectedRole(task, user, roleCode);
		}

		if (!hasAnyAuthority(PermissionCode.TASK_READ_OWN.name(), PermissionCode.TASK_READ_ANTENNE.name())) {
			return false;
		}

		return antenneId != null
				&& antenneId.equals(task.getAntenneId())
				&& isVisibleToConnectedRole(task, user, roleCode);
	}

	private boolean isVisibleToConnectedRole(WorkflowTask task, Utilisateur user, RoleCode roleCode) {
		if (task == null || task.getRoleDestinataire() == null) {
			return false;
		}
		if (roleCode == RoleCode.ADMIN || roleCode == RoleCode.COO || roleCode == RoleCode.GERANT_GENERAL) {
			return task.getRoleDestinataire() == roleCode;
		}
		return task.getRoleDestinataire() == roleCode;
	}

	private void upsertRoleTask(
			WorkflowTaskTypeAction typeAction,
			WorkflowTaskModule module,
			String entityType,
			Long entityId,
			String referenceMetier,
			String titre,
			String description,
			RoleCode roleDestinataire,
			Long antenneId,
			Long siteId,
			WorkflowTaskPriority priority
	) {
		if (antenneId == null) {
			log.warn("{} upsertRoleTask action=IGNORED reason=ANTENNE_NULL type={} module={} entityType={} entityId={} role={} reference={}",
					DIAG_PREFIX,
					typeAction,
					module,
					entityType,
					entityId,
					roleDestinataire,
					referenceMetier);
			return;
		}

		String activeKey = buildActiveKey(module, typeAction, entityType, entityId, roleDestinataire, antenneId);

		var existingByActiveKey = workflowTaskRepository.findByActiveKey(activeKey);
		if (existingByActiveKey.isPresent() && ACTIVE_STATUSES.contains(existingByActiveKey.get().getStatut())) {
			log.warn("{} upsertRoleTask action=IGNORED reason=ACTIVE_KEY_ALREADY_EXISTS activeKey={} existingTask={}",
					DIAG_PREFIX,
					activeKey,
					formatTask(existingByActiveKey.get()));
			return;
		}
		if (existingByActiveKey.isPresent()) {
			WorkflowTask inactiveTask = existingByActiveKey.get();
			log.warn("{} upsertRoleTask action=CLEAR_INACTIVE_ACTIVE_KEY activeKey={} existingTask={}",
					DIAG_PREFIX,
					activeKey,
					formatTask(inactiveTask));
			inactiveTask.setActiveKey(null);
			workflowTaskRepository.save(inactiveTask);
		}

		Utilisateur creator = SecurityUtils.getCurrentUser();

		WorkflowTask task = WorkflowTask.builder()
				.typeAction(typeAction)
				.module(module)
				.referenceMetier(referenceMetier)
				.entityType(entityType)
				.entityId(entityId)
				.titre(titre)
				.description(description)
				.roleDestinataire(roleDestinataire)
				.antenneId(antenneId)
				.siteId(siteId)
				.priorite(priority)
				.statut(WorkflowTaskStatus.A_FAIRE)
				.createdBy(creator)
				.activeKey(activeKey)
				.build();

		WorkflowTask saved = workflowTaskRepository.save(task);
		log.warn("{} upsertRoleTask action=CREATED task={} targetUrl=/credits/demandes", DIAG_PREFIX, formatTask(saved));
		auditTaskAction(saved, "Tâche workflow créée", null);
	}

	private void completeRoleTask(
			WorkflowTaskModule module,
			String entityType,
			Long entityId,
			RoleCode role,
			String commentaire
	) {
		List<WorkflowTask> tasks = workflowTaskRepository.findByModuleAndEntityTypeAndEntityIdAndRoleDestinataireAndStatutIn(
				module,
				entityType,
				entityId,
				role,
				ACTIVE_STATUSES
		);
		closeTasks(tasks, commentaire);
	}

	private void completeAllTasksForEntity(
			WorkflowTaskModule module,
			String entityType,
			Long entityId,
			String commentaire
	) {
		List<WorkflowTask> tasks = workflowTaskRepository
				.findByModuleAndEntityTypeAndEntityIdAndStatutIn(module, entityType, entityId, ACTIVE_STATUSES);
		closeTasks(tasks, commentaire);
	}

	private void completeOtherRoleTasksForEntity(
			WorkflowTaskModule module,
			String entityType,
			Long entityId,
			RoleCode expectedRole,
			String commentaire
	) {
		List<WorkflowTask> tasks = workflowTaskRepository
				.findByModuleAndEntityTypeAndEntityIdAndStatutIn(module, entityType, entityId, ACTIVE_STATUSES)
				.stream()
				.filter(task -> task.getRoleDestinataire() != expectedRole)
				.toList();
		closeTasks(tasks, commentaire);
	}

	private void closeTasks(List<WorkflowTask> tasks, String commentaire) {
		if (tasks.isEmpty()) {
			return;
		}

		Utilisateur currentUser = SecurityUtils.getCurrentUser();
		LocalDateTime now = LocalDateTime.now();
		for (WorkflowTask task : tasks) {
			task.setStatut(WorkflowTaskStatus.TERMINEE);
			task.setCompletedAt(now);
			task.setCompletedBy(currentUser);
			task.setCommentaire(cleanText(commentaire));
			task.setActiveKey(null);
		}
		List<WorkflowTask> savedTasks = workflowTaskRepository.saveAll(tasks);
		for (WorkflowTask saved : savedTasks) {
			auditTaskAction(saved, "Tâche terminée", cleanText(commentaire));
		}
	}

	private void synchronizeWorkflowTasks(RoleCode roleCode, Long antenneId) {
		synchronizeCreditWorkflowTasks(roleCode, antenneId);
		synchronizeDepenseCaisseWorkflowTasks(roleCode, antenneId);
		synchronizeRetraitWorkflowTasks(roleCode, antenneId);
		synchronizeRecetteWorkflowTasks(roleCode, antenneId);
		synchronizeCollecteTerrainWorkflowTasks(roleCode, antenneId);
		synchronizeSessionCaisseWorkflowTasks(roleCode, antenneId);
	}

	private void synchronizeCreditWorkflowTasks(RoleCode roleCode, Long antenneId) {
		if (roleCode == null) {
			log.warn("{} syncCredit SKIP role null antenneId={}", DIAG_PREFIX, antenneId);
			return;
		}
		log.warn("{} syncCredit START role={} antenneId={}", DIAG_PREFIX, roleCode, antenneId);

		if (roleCode == RoleCode.GESTIONNAIRE) {
			synchronizeDemandesCredit(StatutDemandeCredit.SOUMISE, RoleCode.GESTIONNAIRE, WorkflowTaskTypeAction.TRAITER_DEMANDE_CREDIT, antenneId);
			logActiveRoleTasks(RoleCode.GESTIONNAIRE, antenneId, "syncCredit END GESTIONNAIRE");
			return;
		}

		if (antenneId == null) {
			log.warn("{} syncCredit SKIP role={} reason=ANTENNE_NULL_NON_GESTIONNAIRE", DIAG_PREFIX, roleCode);
			return;
		}

		if (roleCode == RoleCode.CONTROLEUR) {
			synchronizeDemandesCredit(StatutDemandeCredit.EN_ANALYSE, RoleCode.CONTROLEUR, WorkflowTaskTypeAction.CONTROLER_DEMANDE_CREDIT, antenneId);
			synchronizeDemandesCredit(StatutDemandeCredit.ANALYSE_TERRAIN_VALIDEE, RoleCode.CONTROLEUR, WorkflowTaskTypeAction.CONTROLER_DEMANDE_CREDIT, antenneId);
			logActiveRoleTasks(RoleCode.CONTROLEUR, antenneId, "syncCredit END CONTROLEUR");
			return;
		}

		if (roleCode == RoleCode.CHEF_BUREAU) {
			synchronizeDemandesCredit(StatutDemandeCredit.VALIDATION_CHEF, RoleCode.CHEF_BUREAU, WorkflowTaskTypeAction.APPROUVER_DEMANDE_CREDIT, antenneId);
			logActiveRoleTasks(RoleCode.CHEF_BUREAU, antenneId, "syncCredit END CHEF_BUREAU");
			return;
		}

		if (roleCode == RoleCode.CAISSIER) {
			closeInactiveDecaissementCreditTasks(antenneId);
			synchronizeDemandesCredit(StatutDemandeCredit.APPROUVEE, RoleCode.CAISSIER, WorkflowTaskTypeAction.DECAISSER_CREDIT, antenneId);
			logActiveRoleTasks(RoleCode.CAISSIER, antenneId, "syncCredit END CAISSIER");
		}
	}

	private void closeInactiveDecaissementCreditTasks(Long antenneId) {
		if (antenneId == null) {
			return;
		}

		workflowTaskRepository.findByRoleDestinataireAndAntenneIdAndStatutInOrderByDateCreationDesc(
				RoleCode.CAISSIER,
				antenneId,
				ACTIVE_STATUSES
		).stream()
				.filter(task -> task.getTypeAction() == WorkflowTaskTypeAction.DECAISSER_CREDIT)
				.filter(task -> task.getModule() == WorkflowTaskModule.CREDIT)
				.filter(task -> ENTITY_TYPE_CREDIT.equals(task.getEntityType()))
				.filter(task -> !isStillActionable(task))
				.forEach(task -> closeTasks(List.of(task), "Crédit non éligible au décaissement"));
	}

	private void synchronizeDepenseCaisseWorkflowTasks(RoleCode roleCode, Long antenneId) {
		if (roleCode == null) {
			return;
		}

		boolean localDepenseRole = roleCode == RoleCode.CHEF_BUREAU || roleCode == RoleCode.CAISSIER;
		if (!localDepenseRole || antenneId == null) {
			return;
		}

		depenseCaisseRepository.findAllByOrderByDateDemandeDesc().stream()
				.filter(depense -> antenneId.equals(resolveAntenneId(depense)))
				.forEach(this::synchronizeDepenseCaisseTask);
	}

	private void synchronizeDepenseCaisseTask(DepenseCaisse depense) {
		if (depense == null || depense.getId() == null) {
			return;
		}

		if (depense.getStatut() == DepenseCaisseStatus.EN_ATTENTE_VALIDATION) {
			completeOtherRoleTasksForEntity(WorkflowTaskModule.CAISSE, ENTITY_TYPE_DEPENSE_CAISSE, depense.getId(), RoleCode.CHEF_BUREAU, "Dépense en attente validation");
			upsertRoleTask(
					WorkflowTaskTypeAction.DEPENSE_CAISSE_VALIDATE,
					WorkflowTaskModule.CAISSE,
					ENTITY_TYPE_DEPENSE_CAISSE,
					depense.getId(),
					resolveWorkflowReference(depense),
					"Dépense à valider",
					"Dépense caisse en attente d'autorisation",
					RoleCode.CHEF_BUREAU,
					resolveAntenneId(depense),
					resolveSiteId(depense),
					WorkflowTaskPriority.HAUTE
			);
			return;
		}

		if (isDepenseValideeNonPayee(depense)) {
			completeOtherRoleTasksForEntity(WorkflowTaskModule.CAISSE, ENTITY_TYPE_DEPENSE_CAISSE, depense.getId(), RoleCode.CAISSIER, "Dépense validée");
			upsertRoleTask(
					WorkflowTaskTypeAction.DEPENSE_CAISSE_PAY,
					WorkflowTaskModule.CAISSE,
					ENTITY_TYPE_DEPENSE_CAISSE,
					depense.getId(),
					resolveWorkflowReference(depense),
					"Dépense à payer",
					"Dépense caisse validée en attente de paiement",
					RoleCode.CAISSIER,
					resolveAntenneId(depense),
					resolveSiteId(depense),
					WorkflowTaskPriority.HAUTE
			);
			return;
		}

		completeAllTasksForEntity(
				WorkflowTaskModule.CAISSE,
				ENTITY_TYPE_DEPENSE_CAISSE,
				depense.getId(),
				"Dépense caisse statut=" + depense.getStatut()
		);
	}

	private boolean isDepenseValideeNonPayee(DepenseCaisse depense) {
		return depense != null
				&& depense.getStatut() == DepenseCaisseStatus.VALIDEE
				&& depense.getOperationCaisse() == null
				&& depense.getPayePar() == null
				&& depense.getDatePaiement() == null;
	}

	private void synchronizeRetraitWorkflowTasks(RoleCode roleCode, Long antenneId) {
		if (roleCode == null || antenneId == null) {
			return;
		}
		if (roleCode == RoleCode.CONTROLEUR) {
			demandeRetraitEpargneRepository.findByStatut(StatutDemandeRetrait.CREEE).stream()
					.filter(retrait -> antenneId.equals(resolveAntenneId(retrait)))
					.forEach(this::synchronizeRetraitTask);
			demandeRetraitEpargneRepository.findByStatut(StatutDemandeRetrait.EN_ATTENTE_VALIDATION).stream()
					.filter(retrait -> antenneId.equals(resolveAntenneId(retrait)))
					.forEach(this::synchronizeRetraitTask);
		}
		if (roleCode == RoleCode.CAISSIER) {
			demandeRetraitEpargneRepository.findByStatut(StatutDemandeRetrait.VALIDEE).stream()
					.filter(retrait -> antenneId.equals(resolveAntenneId(retrait)))
					.forEach(this::synchronizeRetraitTask);
		}
	}

	private void synchronizeRetraitTask(DemandeRetraitEpargne retrait) {
		if (retrait == null || retrait.getId() == null) {
			return;
		}
		if (retrait.getStatut() == StatutDemandeRetrait.CREEE || retrait.getStatut() == StatutDemandeRetrait.EN_ATTENTE_VALIDATION) {
			completeOtherRoleTasksForEntity(WorkflowTaskModule.EPARGNE, ENTITY_TYPE_RETRAIT_EPARGNE, retrait.getId(), RoleCode.CONTROLEUR, "Retrait en attente validation");
			onRetraitDemande(retrait.getId(), resolveWorkflowReference(retrait), resolveAntenneId(retrait), resolveSiteId(retrait));
			return;
		}
		if (retrait.getStatut() == StatutDemandeRetrait.VALIDEE) {
			completeOtherRoleTasksForEntity(WorkflowTaskModule.EPARGNE, ENTITY_TYPE_RETRAIT_EPARGNE, retrait.getId(), RoleCode.CAISSIER, "Retrait validé");
			onRetraitApprouve(retrait.getId(), resolveWorkflowReference(retrait), resolveAntenneId(retrait), resolveSiteId(retrait));
			return;
		}
		completeAllTasksForEntity(WorkflowTaskModule.EPARGNE, ENTITY_TYPE_RETRAIT_EPARGNE, retrait.getId(), "Retrait statut=" + retrait.getStatut());
	}

	private void synchronizeRecetteWorkflowTasks(RoleCode roleCode, Long antenneId) {
		if (roleCode != RoleCode.CONTROLEUR || antenneId == null) {
			return;
		}
		recetteJournaliereTerrainRepository.findByStatutOrderByDateJourDesc(StatutRecetteJournaliere.EN_ATTENTE_VALIDATION).stream()
				.filter(recette -> antenneId.equals(resolveAntenneId(recette)))
				.forEach(this::synchronizeRecetteTask);
	}

	private void synchronizeRecetteTask(RecetteJournaliereTerrain recette) {
		if (recette == null || recette.getId() == null) {
			return;
		}
		if (recette.getStatut() == StatutRecetteJournaliere.EN_ATTENTE_VALIDATION) {
			completeOtherRoleTasksForEntity(WorkflowTaskModule.RECETTE, ENTITY_TYPE_RECETTE_TERRAIN, recette.getId(), RoleCode.CONTROLEUR, "Recette soumise");
			onRecetteSoumise(recette.getId(), resolveWorkflowReference(recette), resolveAntenneId(recette), resolveSiteId(recette));
			return;
		}
		completeAllTasksForEntity(WorkflowTaskModule.RECETTE, ENTITY_TYPE_RECETTE_TERRAIN, recette.getId(), "Recette statut=" + recette.getStatut());
	}

	private void synchronizeCollecteTerrainWorkflowTasks(RoleCode roleCode, Long antenneId) {
		if ((roleCode != RoleCode.CAISSIER && roleCode != RoleCode.CONTROLEUR) || antenneId == null) {
			return;
		}
		collecteJournaliereTerrainRepository.findByStatutAndAntenneId(RecetteStatut.SOUMISE, antenneId).stream()
				.forEach(this::synchronizeCollecteTerrainTask);
		collecteJournaliereTerrainRepository.findByStatutAndAntenneId(RecetteStatut.VALIDEE, antenneId).stream()
				.forEach(collecte -> completeAllTasksForEntity(WorkflowTaskModule.RECETTE, ENTITY_TYPE_COLLECTE_TERRAIN, collecte.getId(), "Collecte validée"));
		collecteJournaliereTerrainRepository.findByStatutAndAntenneId(RecetteStatut.REJETEE, antenneId).stream()
				.forEach(collecte -> completeAllTasksForEntity(WorkflowTaskModule.RECETTE, ENTITY_TYPE_COLLECTE_TERRAIN, collecte.getId(), "Collecte rejetée"));
	}

	private void synchronizeCollecteTerrainTask(CollecteJournaliereTerrain collecte) {
		if (collecte == null || collecte.getId() == null) {
			return;
		}
		if (collecte.getStatut() == RecetteStatut.SOUMISE && Boolean.TRUE.equals(collecte.getBilletageConfirme())) {
			onCollecteBilletageConfirme(collecte.getId(), resolveWorkflowReference(collecte), collecte.getAntenneId(), resolveSiteId(collecte));
			return;
		}
		if (collecte.getStatut() == RecetteStatut.SOUMISE) {
			onCollecteSoumise(collecte.getId(), resolveWorkflowReference(collecte), collecte.getAntenneId(), resolveSiteId(collecte));
			return;
		}
		completeAllTasksForEntity(WorkflowTaskModule.RECETTE, ENTITY_TYPE_COLLECTE_TERRAIN, collecte.getId(), "Collecte statut=" + collecte.getStatut());
	}

	private void synchronizeSessionCaisseWorkflowTasks(RoleCode roleCode, Long antenneId) {
		if (roleCode == null || antenneId == null) {
			return;
		}
		if (roleCode != RoleCode.CAISSIER && roleCode != RoleCode.CONTROLEUR && roleCode != RoleCode.CHEF_BUREAU) {
			return;
		}
		sessionCaisseRepository.findAllByOrderByDateComptableDescDateOuvertureDescIdDesc().stream()
				.filter(session -> antenneId.equals(resolveAntenneId(session)))
				.forEach(this::synchronizeSessionCaisseTask);
	}

	private void synchronizeSessionCaisseTask(SessionCaisse session) {
		if (session == null || session.getId() == null) {
			return;
		}
		if (session.getStatut() == StatutSessionCaisse.OUVERTE) {
			completeOtherRoleTasksForEntity(WorkflowTaskModule.CAISSE, ENTITY_TYPE_SESSION_CAISSE, session.getId(), RoleCode.CAISSIER, "Session ouverte");
			upsertRoleTask(
					WorkflowTaskTypeAction.PRE_CLOTURER_SESSION_CAISSE,
					WorkflowTaskModule.CAISSE,
					ENTITY_TYPE_SESSION_CAISSE,
					session.getId(),
					resolveWorkflowReference(session),
					"Session caisse à pré-clôturer",
					"La session ouverte attend sa pré-clôture par le caissier.",
					RoleCode.CAISSIER,
					resolveAntenneId(session),
					resolveSiteId(session),
					WorkflowTaskPriority.NORMALE
			);
			return;
		}
		if (session.getStatut() == StatutSessionCaisse.PRE_CLOTUREE) {
			completeOtherRoleTasksForEntity(WorkflowTaskModule.CAISSE, ENTITY_TYPE_SESSION_CAISSE, session.getId(), RoleCode.CONTROLEUR, "Session pré-clôturée");
			onSessionPreCloturee(session.getId(), resolveWorkflowReference(session), resolveAntenneId(session), resolveSiteId(session));
			return;
		}
		if (session.getStatut() == StatutSessionCaisse.VALIDEE_CONTROLE) {
			completeOtherRoleTasksForEntity(WorkflowTaskModule.CAISSE, ENTITY_TYPE_SESSION_CAISSE, session.getId(), RoleCode.CHEF_BUREAU, "Session validée contrôle");
			onSessionControleValide(session.getId(), resolveWorkflowReference(session), resolveAntenneId(session), resolveSiteId(session));
			return;
		}
		completeAllTasksForEntity(WorkflowTaskModule.CAISSE, ENTITY_TYPE_SESSION_CAISSE, session.getId(), "Session statut=" + session.getStatut());
	}

	private void synchronizeDemandesCredit(
			StatutDemandeCredit statut,
			RoleCode roleDestinataire,
			WorkflowTaskTypeAction typeAction,
			Long antenneId
	) {
		List<DemandeCredit> demandes = demandeCreditRepository.findByStatut(statut);
		log.warn("{} syncDemandesCredit statut={} found={} expectedType={} expectedRole={} expectedStatus={} expectedTargetUrl=/credits/demandes userAntenneId={}",
				DIAG_PREFIX,
				statut,
				demandes.size(),
				typeAction,
				roleDestinataire,
				WorkflowTaskStatus.A_FAIRE,
				antenneId);

		for (DemandeCredit demande : demandes) {
			Long demandeAntenneId = resolveAntenneId(demande);
			boolean filteredByAntenne = antenneId != null && !antenneId.equals(demandeAntenneId);
			log.warn("{} demandeCredit scan id={} numero={} statut={} demandeAntenneId={} userAntenneId={} filteredByAntenne={} membre={} montant={} createdAt={}",
					DIAG_PREFIX,
					demande.getId(),
					demande.getNumeroDemande(),
					demande.getStatut(),
					demandeAntenneId,
					antenneId,
					filteredByAntenne,
					demande.getMembre() != null ? demande.getMembre().getId() : null,
					demande.getMontantDemande(),
					demande.getDateCreation());

			if (filteredByAntenne) {
				log.warn("{} demandeCredit action=IGNORED reason=ANTENNE_MISMATCH id={} demandeAntenneId={} userAntenneId={}",
						DIAG_PREFIX,
						demande.getId(),
						demandeAntenneId,
						antenneId);
				continue;
			}

			String entityType = roleDestinataire == RoleCode.CAISSIER ? ENTITY_TYPE_CREDIT : ENTITY_TYPE_DEMANDE_CREDIT;
			Long entityId = roleDestinataire == RoleCode.CAISSIER && demande.getCredit() != null ? demande.getCredit().getId() : demande.getId();
			List<WorkflowTask> allRoleTasks = workflowTaskRepository.findByModuleAndEntityTypeAndEntityIdAndRoleDestinataireOrderByDateCreationDesc(
					WorkflowTaskModule.CREDIT,
					entityType,
					entityId,
					roleDestinataire
			);
			log.warn("{} demandeCredit existingTasks id={} role={} entityType={} entityId={} tasks={}",
					DIAG_PREFIX,
					demande.getId(),
					roleDestinataire,
					entityType,
					entityId,
					allRoleTasks.stream().map(this::formatTask).toList());

			completeOtherRoleTasksForEntity(
					WorkflowTaskModule.CREDIT,
					entityType,
					entityId,
					roleDestinataire,
					"Crédit statut=" + statut
			);

			if (!hasNoActiveCreditTask(demande, roleDestinataire)) {
				log.warn("{} demandeCredit action=IGNORED reason=ACTIVE_TASK_ALREADY_EXISTS id={} role={} typeExpected={} targetUrl=/credits/demandes",
						DIAG_PREFIX,
						demande.getId(),
						roleDestinataire,
						typeAction);
				continue;
			}

			log.warn("{} demandeCredit action=CREATE_TASK id={} role={} type={} status={} targetUrl=/credits/demandes",
					DIAG_PREFIX,
					demande.getId(),
					roleDestinataire,
					typeAction,
					WorkflowTaskStatus.A_FAIRE);
			createCreditWorkflowTask(demande, roleDestinataire, typeAction);
		}
	}

	private boolean hasNoActiveCreditTask(DemandeCredit demande, RoleCode roleDestinataire) {
		if (demande == null || demande.getId() == null) {
			return false;
		}
		return workflowTaskRepository.findByModuleAndEntityTypeAndEntityIdAndRoleDestinataireAndStatutIn(
				WorkflowTaskModule.CREDIT,
				roleDestinataire == RoleCode.CAISSIER ? ENTITY_TYPE_CREDIT : ENTITY_TYPE_DEMANDE_CREDIT,
				roleDestinataire == RoleCode.CAISSIER && demande.getCredit() != null ? demande.getCredit().getId() : demande.getId(),
				roleDestinataire,
				ACTIVE_STATUSES
		).isEmpty();
	}

	private void createCreditWorkflowTask(DemandeCredit demande, RoleCode roleDestinataire, WorkflowTaskTypeAction typeAction) {
		Long antenneId = resolveAntenneId(demande);
		Long siteId = resolveSiteId(demande);
		String reference = resolveWorkflowReference(demande);
		log.warn("{} createCreditWorkflowTask demandeId={} role={} type={} resolvedAntenneId={} resolvedSiteId={} reference={}",
				DIAG_PREFIX,
				demande != null ? demande.getId() : null,
				roleDestinataire,
				typeAction,
				antenneId,
				siteId,
				reference);

		if (antenneId == null) {
			log.warn("{} createCreditWorkflowTask action=IGNORED reason=DEMANDE_ANTENNE_NULL demandeId={} role={} type={}",
					DIAG_PREFIX,
					demande != null ? demande.getId() : null,
					roleDestinataire,
					typeAction);
			return;
		}

		if (roleDestinataire == RoleCode.GESTIONNAIRE) {
			onDemandeCreditSoumise(demande.getId(), reference, antenneId, siteId);
			return;
		}
		if (roleDestinataire == RoleCode.CONTROLEUR) {
			if (demande.getStatut() == StatutDemandeCredit.EN_ANALYSE) {
				onDemandeCreditPreAnalyseValidee(demande.getId(), reference, antenneId, siteId);
				return;
			}
			onDemandeCreditAnalyseTerrainValidee(demande.getId(), reference, antenneId, siteId);
			return;
		}
		if (roleDestinataire == RoleCode.CHEF_BUREAU) {
			onDemandeCreditValidationChef(demande.getId(), reference, antenneId, siteId);
			return;
		}
		if (roleDestinataire == RoleCode.CAISSIER
				&& demande.getCredit() != null
				&& demande.getCredit().getId() != null
				&& demande.getCredit().getStatut() == StatutCredit.APPROUVE) {
			onDemandeCreditApprouvee(demande.getId(), demande.getCredit().getId(), reference, antenneId, siteId);
		}
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

	private Long resolveAntenneId(DepenseCaisse depense) {
		if (depense == null) {
			return null;
		}
		if (depense.getCaisse() != null && depense.getCaisse().getAgence() != null) {
			return depense.getCaisse().getAgence().getId();
		}
		if (depense.getSite() != null && depense.getSite().getAgence() != null) {
			return depense.getSite().getAgence().getId();
		}
		return null;
	}

	private Long resolveAntenneId(DemandeRetraitEpargne retrait) {
		if (retrait == null || retrait.getMembre() == null || retrait.getMembre().getSite() == null || retrait.getMembre().getSite().getAgence() == null) {
			return null;
		}
		return retrait.getMembre().getSite().getAgence().getId();
	}

	private Long resolveAntenneId(RecetteJournaliereTerrain recette) {
		if (recette == null) {
			return null;
		}
		if (recette.getAgent() != null && recette.getAgent().getSite() != null && recette.getAgent().getSite().getAgence() != null) {
			return recette.getAgent().getSite().getAgence().getId();
		}
		if (recette.getMembre() != null && recette.getMembre().getSite() != null && recette.getMembre().getSite().getAgence() != null) {
			return recette.getMembre().getSite().getAgence().getId();
		}
		return null;
	}

	private Long resolveAntenneId(SessionCaisse session) {
		if (session == null || session.getCaisse() == null) {
			return null;
		}
		if (session.getCaisse().getAgence() != null) {
			return session.getCaisse().getAgence().getId();
		}
		if (session.getCaisse().getSite() != null && session.getCaisse().getSite().getAgence() != null) {
			return session.getCaisse().getSite().getAgence().getId();
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

	private Long resolveSiteId(DepenseCaisse depense) {
		if (depense == null) {
			return null;
		}
		if (depense.getSite() != null) {
			return depense.getSite().getId();
		}
		if (depense.getCaisse() != null && depense.getCaisse().getSite() != null) {
			return depense.getCaisse().getSite().getId();
		}
		return null;
	}

	private Long resolveSiteId(DemandeRetraitEpargne retrait) {
		if (retrait == null || retrait.getMembre() == null || retrait.getMembre().getSite() == null) {
			return null;
		}
		return retrait.getMembre().getSite().getId();
	}

	private Long resolveSiteId(RecetteJournaliereTerrain recette) {
		if (recette == null) {
			return null;
		}
		if (recette.getAgent() != null && recette.getAgent().getSite() != null) {
			return recette.getAgent().getSite().getId();
		}
		if (recette.getMembre() != null && recette.getMembre().getSite() != null) {
			return recette.getMembre().getSite().getId();
		}
		return null;
	}

	private Long resolveSiteId(SessionCaisse session) {
		if (session == null || session.getCaisse() == null || session.getCaisse().getSite() == null) {
			return null;
		}
		return session.getCaisse().getSite().getId();
	}

	private Long resolveSiteId(CollecteJournaliereTerrain collecte) {
		if (collecte == null || collecte.getSite() == null) {
			return null;
		}
		return collecte.getSite().getId();
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

	private String resolveWorkflowReference(DepenseCaisse depense) {
		if (depense == null || depense.getId() == null) {
			return "DEPENSE-UNKNOWN";
		}
		return "DEPENSE-" + depense.getId();
	}

	private String resolveWorkflowReference(DemandeRetraitEpargne retrait) {
		if (retrait == null || retrait.getId() == null) {
			return "RETRAIT-UNKNOWN";
		}
		return "RETRAIT-" + retrait.getId();
	}

	private String resolveWorkflowReference(RecetteJournaliereTerrain recette) {
		if (recette == null || recette.getId() == null) {
			return "RECETTE-UNKNOWN";
		}
		return "RECETTE-" + recette.getId();
	}

	private String resolveWorkflowReference(SessionCaisse session) {
		if (session == null || session.getId() == null) {
			return "CAI-SESSION-UNKNOWN";
		}
		return "CAI-SESSION-" + session.getId();
	}

	private String resolveWorkflowReference(CollecteJournaliereTerrain collecte) {
		if (collecte == null || collecte.getId() == null) {
			return "COLLECTE-UNKNOWN";
		}
		return "COLLECTE-" + collecte.getId();
	}

	private void auditTaskAction(WorkflowTask task, String message, String commentaire) {
		String auditCommentaire = commentaire == null ? message : (message + " | " + commentaire);
		auditService.logAction(
				AuditAction.MODIFICATION_OPERATION,
				AuditModule.SESSION_CAISSE,
				"WorkflowTask",
				task.getId(),
				true,
				AuditSeverity.INFO,
				auditCommentaire,
				task.getReferenceMetier(),
				null,
				null,
				null,
				null,
				task.getEntityId(),
				task.getSiteId(),
				null
		);
	}

	private Utilisateur getCurrentUserOrThrow() {
		Utilisateur user = SecurityUtils.getCurrentUser();
		if (user != null) {
			return user;
		}

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getName() != null) {
			return utilisateurRepository.findByUsername(authentication.getName())
					.orElseThrow(() -> new BusinessException("Utilisateur authentifié introuvable"));
		}

		throw new BusinessException("Utilisateur authentifié introuvable");
	}

	private String cleanText(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

	private String buildActiveKey(
			WorkflowTaskModule module,
			WorkflowTaskTypeAction typeAction,
			String entityType,
			Long entityId,
			RoleCode roleDestinataire,
			Long antenneId
	) {
		return module.name()
				+ "|" + typeAction.name()
				+ "|" + entityType
				+ "|" + entityId
				+ "|" + roleDestinataire.name()
				+ "|" + antenneId
				+ "|ACTIVE";
	}

	private RoleCode resolveEffectiveRole(Utilisateur user) {
		if (user == null || user.getRole() == null || user.getRole().getCode() == null) {
			throw new BusinessException("Rôle utilisateur introuvable");
		}
		return user.getRole().getCode();
	}

	private Set<RoleCode> resolveVisibleTaskRoles(RoleCode roleCode) {
		return EnumSet.of(roleCode);
	}

	private boolean isWorkflowTaskBusinessRole(RoleCode roleCode) {
		return roleCode == RoleCode.ADMIN
				|| roleCode == RoleCode.CAISSIER
				|| roleCode == RoleCode.CONTROLEUR
				|| roleCode == RoleCode.CHEF_BUREAU
				|| roleCode == RoleCode.GESTIONNAIRE
				|| roleCode == RoleCode.RCI
				|| roleCode == RoleCode.COO
				|| roleCode == RoleCode.GERANT_GENERAL;
	}

	private Set<WorkflowTaskStatus> resolveStatuses(String statut) {
		if (statut == null || statut.isBlank()) {
			return EnumSet.of(WorkflowTaskStatus.A_FAIRE, WorkflowTaskStatus.EN_COURS, WorkflowTaskStatus.TERMINEE);
		}

		WorkflowTaskStatus status = WorkflowTaskStatus.valueOf(statut.trim().toUpperCase());
		return Set.of(status);
	}

	private Long resolveAntenneId(Utilisateur user) {
		if (user == null) {
			return null;
		}
		if (user.getEmploye() != null
				&& user.getEmploye().getAgence() != null
				&& user.getEmploye().getAgence().getId() != null) {
			return user.getEmploye().getAgence().getId();
		}
		if (user.getSite() != null
				&& user.getSite().getAgence() != null
				&& user.getSite().getAgence().getId() != null) {
			return user.getSite().getAgence().getId();
		}
		if (user.getEmploye() != null
				&& user.getEmploye().getSite() != null
				&& user.getEmploye().getSite().getAgence() != null
				&& user.getEmploye().getSite().getAgence().getId() != null) {
			return user.getEmploye().getSite().getAgence().getId();
		}
		return null;
	}

	private Long resolveSiteId(Utilisateur user) {
		if (user == null) {
			return null;
		}
		if (user.getSite() != null && user.getSite().getId() != null) {
			return user.getSite().getId();
		}
		if (user.getEmploye() != null && user.getEmploye().getSite() != null && user.getEmploye().getSite().getId() != null) {
			return user.getEmploye().getSite().getId();
		}
		return null;
	}

	private String resolveCurrentAuthorities() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || authentication.getAuthorities() == null) {
			return "[]";
		}
		return authentication.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.sorted()
				.toList()
				.toString();
	}

	private String formatTask(WorkflowTask task) {
		if (task == null) {
			return "null";
		}
		return "{id=" + task.getId()
				+ ",title=" + task.getTitre()
				+ ",type=" + task.getTypeAction()
				+ ",module=" + task.getModule()
				+ ",entityType=" + task.getEntityType()
				+ ",entityId=" + task.getEntityId()
				+ ",role=" + task.getRoleDestinataire()
				+ ",status=" + task.getStatut()
				+ ",antenneId=" + task.getAntenneId()
				+ ",siteId=" + task.getSiteId()
				+ ",activeKey=" + task.getActiveKey()
				+ "}";
	}

	private void logActiveRoleTasks(RoleCode roleCode, Long antenneId, String context) {
		List<WorkflowTask> tasks = findRoleTasksForCurrentScope(roleCode, antenneId, ACTIVE_STATUSES);
		log.warn("{} {} activeRoleTasks role={} antenneId={} count={} tasks={}",
				DIAG_PREFIX,
				context,
				roleCode,
				antenneId,
				tasks.size(),
				tasks.stream().map(this::formatTask).toList());
	}

	private boolean hasAuthority(String authority) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || authentication.getAuthorities() == null) {
			return false;
		}
		return authentication.getAuthorities().stream()
				.anyMatch(a -> authority.equals(a.getAuthority()));
	}

	private boolean hasAnyAuthority(String... authorities) {
		for (String authority : authorities) {
			if (hasAuthority(authority)) {
				return true;
			}
		}
		return false;
}
}
