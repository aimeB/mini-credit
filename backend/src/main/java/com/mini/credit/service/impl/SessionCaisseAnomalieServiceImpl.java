package com.mini.credit.service.impl;

import com.mini.credit.dto.caisse.DemanderAnnulationSessionRequest;
import com.mini.credit.dto.caisse.ReouvrirSessionControleeRequest;
import com.mini.credit.dto.caisse.SessionCaisseAnomalieResponse;
import com.mini.credit.dto.caisse.SessionCaisseResponse;
import com.mini.credit.dto.caisse.ValiderAnnulationSessionRequest;
import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.entity.caisse.SessionCaisseAnomalie;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.StatutDossierAnomalieSession;
import com.mini.credit.enums.StatutSessionCaisse;
import com.mini.credit.enums.TypeAnomalieSessionCaisse;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.enums.security.AuditModule;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.exception.ResourceNotFoundException;
import com.mini.credit.mapper.CashMapper;
import com.mini.credit.repository.caisse.OperationCaisseRepository;
import com.mini.credit.repository.caisse.SessionCaisseAnomalieRepository;
import com.mini.credit.repository.caisse.SessionCaisseRepository;
import com.mini.credit.repository.epargne.OperationEpargneRepository;
import com.mini.credit.repository.utilisateur.UtilisateurRepository;
import com.mini.credit.repository.workflow.WorkflowTaskRepository;
import com.mini.credit.service.SessionCaisseAnomalieService;
import com.mini.credit.service.audit.AuditService;
import com.mini.credit.enums.workflow.WorkflowTaskModule;
import com.mini.credit.enums.workflow.WorkflowTaskStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SessionCaisseAnomalieServiceImpl implements SessionCaisseAnomalieService {

    private final SessionCaisseRepository sessionCaisseRepository;
    private final SessionCaisseAnomalieRepository anomalieRepository;
    private final OperationCaisseRepository operationCaisseRepository;
    private final OperationEpargneRepository operationEpargneRepository;
    private final WorkflowTaskRepository workflowTaskRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AuditService auditService;
    private final CashMapper cashMapper;

    @Override
    public SessionCaisseAnomalieResponse demanderAnnulation(Long sessionId, DemanderAnnulationSessionRequest request) {
        SessionCaisse session = getSession(sessionId);
        ensureMotif(request != null ? request.getMotif() : null);

        if (session.getStatut() != StatutSessionCaisse.OUVERTE) {
            throw new BusinessException("SESSION_STATUT_INCOMPATIBLE");
        }

        long mouvements = operationCaisseRepository.countBySessionCaisseId(sessionId);
        if (mouvements > 0) {
            createRectificationRequiredAnomaly(session, request);
            throw new BusinessException("SESSION_AVEC_MOUVEMENTS_ANNULATION_SIMPLE_INTERDITE");
        }

        ensureNoPendingAnomaly(sessionId);

        Utilisateur actor = getCurrentUser();
        SessionCaisseAnomalie anomalie = SessionCaisseAnomalie.builder()
                .session(session)
                .typeAnomalie(resolveType(request, TypeAnomalieSessionCaisse.OUVERTURE_ERRONEE_SANS_MOUVEMENT))
                .ancienStatut(session.getStatut())
                .nouveauStatut(StatutSessionCaisse.ANNULEE)
                .motif(clean(request.getMotif()))
                .commentaire(clean(request.getCommentaire()))
                .demandePar(actor)
                .dateDemande(LocalDateTime.now())
                .statutDossier(StatutDossierAnomalieSession.DEMANDEE)
                .actionExecutee("DEMANDE_ANNULATION")
                .build();

        SessionCaisseAnomalie saved = anomalieRepository.save(anomalie);

        auditService.logInfo(
                AuditAction.SESSION_ANOMALIE_DEMANDEE,
                AuditModule.SESSION_CAISSE,
                "SessionCaisseAnomalie",
                saved.getId(),
                "Demande d'annulation de session " + sessionId,
                "SESSION-" + sessionId
        );

        return toResponse(saved);
    }

    @Override
    public SessionCaisseAnomalieResponse validerAnnulation(Long sessionId, ValiderAnnulationSessionRequest request) {
        ensureRoleValidation();
        SessionCaisse session = getSession(sessionId);
        SessionCaisseAnomalie anomalie = anomalieRepository
                .findFirstBySessionIdAndStatutDossierOrderByDateDemandeDesc(sessionId, StatutDossierAnomalieSession.DEMANDEE)
                .orElseThrow(() -> new BusinessException("ANOMALIE_DEJA_TRAITEE"));

        String decision = request != null ? clean(request.getDecision()) : null;
        if (decision == null) {
            throw new BusinessException("SESSION_STATUT_INCOMPATIBLE");
        }

        Utilisateur validateur = getCurrentUser();
        if ("REJETER".equalsIgnoreCase(decision)) {
            anomalie.setStatutDossier(StatutDossierAnomalieSession.REJETEE);
            anomalie.setValidePar(validateur);
            anomalie.setDateValidation(LocalDateTime.now());
            anomalie.setCommentaire(clean(request.getCommentaireDecision()));
            anomalie.setActionExecutee("ANNULATION_REJETEE");
            SessionCaisseAnomalie saved = anomalieRepository.save(anomalie);

            auditService.logWarning(
                    AuditAction.SESSION_ANOMALIE_REJETEE,
                    AuditModule.SESSION_CAISSE,
                    "SessionCaisseAnomalie",
                    saved.getId(),
                    "Demande d'annulation rejetée pour session " + sessionId,
                    "SESSION-" + sessionId
            );
            return toResponse(saved);
        }

        if (!"VALIDER".equalsIgnoreCase(decision)) {
            throw new BusinessException("SESSION_STATUT_INCOMPATIBLE");
        }

        long mouvements = operationCaisseRepository.countBySessionCaisseId(sessionId);
        if (mouvements > 0) {
            throw new BusinessException("SESSION_AVEC_MOUVEMENTS_ANNULATION_SIMPLE_INTERDITE");
        }

        if (session.getStatut() != StatutSessionCaisse.OUVERTE) {
            throw new BusinessException("SESSION_STATUT_INCOMPATIBLE");
        }

        anomalie.setStatutDossier(StatutDossierAnomalieSession.VALIDEE);
        anomalie.setValidePar(validateur);
        anomalie.setDateValidation(LocalDateTime.now());
        anomalie.setCommentaire(clean(request.getCommentaireDecision()));

        session.setStatut(StatutSessionCaisse.ANNULEE);
        session.setMotifAnnulation(anomalie.getMotif());
        session.setAnnuleePar(validateur);
        session.setDateAnnulation(LocalDateTime.now());
        session.setStatutCorrection("ANNULEE");
        session.setCommentaireCorrection(clean(request.getMotifDecision()));
        sessionCaisseRepository.save(session);

        anomalie.setStatutDossier(StatutDossierAnomalieSession.EXECUTEE);
        anomalie.setActionExecutee("ANNULATION_EXECUTEE");
        SessionCaisseAnomalie saved = anomalieRepository.save(anomalie);

        auditService.logInfo(
                AuditAction.SESSION_ANNULEE,
                AuditModule.SESSION_CAISSE,
                "SessionCaisse",
                sessionId,
                "Session annulée après validation hiérarchique",
                "SESSION-" + sessionId
        );

        return toResponse(saved);
    }

    @Override
    public SessionCaisseResponse annulerAdministrativement(Long sessionId, DemanderAnnulationSessionRequest request) {
        ensureRoleValidation();
        ensureMotif(request != null ? request.getMotif() : null);

        SessionCaisse session = getSession(sessionId);
        if (session.getStatut() != StatutSessionCaisse.CLOTUREE) {
            throw new BusinessException("SESSION_STATUT_INCOMPATIBLE");
        }

        long mouvements = operationCaisseRepository.countBySessionCaisseId(sessionId);
        if (mouvements > 0) {
            throw new BusinessException("SESSION_CLOTUREE_AVEC_MOUVEMENTS_RECTIFICATION_REQUISE");
        }

        Utilisateur actor = getCurrentUser();
        session.setStatut(StatutSessionCaisse.ANNULEE_ADMINISTRATIVEMENT);
        session.setMotifAnnulation(clean(request.getMotif()));
        session.setAnnuleePar(actor);
        session.setDateAnnulation(LocalDateTime.now());
        session.setStatutCorrection("ANNULEE_ADMINISTRATIVEMENT");
        session.setCommentaireCorrection(clean(request.getCommentaire()));

        SessionCaisse savedSession = sessionCaisseRepository.save(session);

        SessionCaisseAnomalie anomalie = SessionCaisseAnomalie.builder()
                .session(savedSession)
                .typeAnomalie(resolveType(request, TypeAnomalieSessionCaisse.CLOTURE_ERRONEE_SANS_MOUVEMENT))
                .ancienStatut(StatutSessionCaisse.CLOTUREE)
                .nouveauStatut(StatutSessionCaisse.ANNULEE_ADMINISTRATIVEMENT)
                .motif(clean(request.getMotif()))
                .commentaire(clean(request.getCommentaire()))
                .demandePar(actor)
                .validePar(actor)
                .dateDemande(LocalDateTime.now())
                .dateValidation(LocalDateTime.now())
                .statutDossier(StatutDossierAnomalieSession.EXECUTEE)
                .actionExecutee("ANNULATION_ADMIN_EXECUTEE")
                .build();
        anomalieRepository.save(anomalie);

        auditService.logInfo(
                AuditAction.SESSION_ANNULEE_ADMINISTRATIVEMENT,
                AuditModule.SESSION_CAISSE,
                "SessionCaisse",
                sessionId,
                "Annulation administrative d'une session clôturée sans mouvement",
                "SESSION-" + sessionId
        );

        return cashMapper.toResponse(savedSession);
    }

    @Override
    public SessionCaisseResponse annulerSessionTest(Long sessionId, DemanderAnnulationSessionRequest request) {
        ensureAdminOnly();
        ensureMotif(request != null ? request.getMotif() : null);

        SessionCaisse session = getSession(sessionId);
        if (session.getStatut() != StatutSessionCaisse.OUVERTE) {
            throw new BusinessException("SESSION_STATUT_INCOMPATIBLE");
        }

        long mouvementsCaisse = operationCaisseRepository.countBySessionCaisseId(sessionId);
        long mouvementsEpargne = operationEpargneRepository.countBySessionCaisseId(sessionId);
        if (mouvementsCaisse > 0 || mouvementsEpargne > 0) {
            createRectificationRequiredAnomaly(session, request);
            throw new BusinessException("SESSION_AVEC_MOUVEMENTS_RECTIFICATION_REQUISE");
        }

        Long caisseId = session.getCaisse() != null ? session.getCaisse().getId() : null;
        if (caisseId == null || session.getDateComptable() == null || session.getDateOuverture() == null) {
            throw new BusinessException("SESSION_INCOMPLETE_ANNULATION_TEST_REFUSEE");
        }
        if (sessionCaisseRepository.existsLaterSessionForCaisse(sessionId, caisseId, session.getDateComptable(), session.getDateOuverture())) {
            throw new BusinessException("SESSION_AVEC_SESSION_SUIVANTE_ANNULATION_TEST_REFUSEE");
        }

        ensureNoPendingAnomaly(sessionId);

        Utilisateur actor = getCurrentUser();
        session.setStatut(StatutSessionCaisse.ANNULEE);
        session.setMotifAnnulation(clean(request.getMotif()));
        session.setAnnuleePar(actor);
        session.setDateAnnulation(LocalDateTime.now());
        session.setStatutCorrection("ANNULEE_RECETTE_TEST");
        session.setCommentaireCorrection(clean(request.getCommentaire()));
        SessionCaisse savedSession = sessionCaisseRepository.save(session);

        workflowTaskRepository.findByModuleAndEntityTypeAndEntityIdAndStatutIn(
                        WorkflowTaskModule.CAISSE,
                        "SESSION_CAISSE",
                        sessionId,
                        List.of(WorkflowTaskStatus.A_FAIRE, WorkflowTaskStatus.EN_COURS)
                )
                .forEach(task -> {
                    task.setStatut(WorkflowTaskStatus.ANNULEE);
                    task.setCompletedBy(actor);
                    task.setCompletedAt(LocalDateTime.now());
                    task.setCommentaire("Session de test annulée administrativement : " + clean(request.getMotif()));
                    workflowTaskRepository.save(task);
                });

        SessionCaisseAnomalie anomalie = SessionCaisseAnomalie.builder()
                .session(savedSession)
                .typeAnomalie(resolveType(request, TypeAnomalieSessionCaisse.OUVERTURE_ERRONEE_SANS_MOUVEMENT))
                .ancienStatut(StatutSessionCaisse.OUVERTE)
                .nouveauStatut(StatutSessionCaisse.ANNULEE)
                .motif(clean(request.getMotif()))
                .commentaire(clean(request.getCommentaire()))
                .demandePar(actor)
                .validePar(actor)
                .dateDemande(LocalDateTime.now())
                .dateValidation(LocalDateTime.now())
                .statutDossier(StatutDossierAnomalieSession.EXECUTEE)
                .actionExecutee("ANNULATION_TEST_EXECUTEE")
                .build();
        anomalieRepository.save(anomalie);

        auditService.logInfo(
                AuditAction.SESSION_ANNULEE,
                AuditModule.SESSION_CAISSE,
                "SessionCaisse",
                sessionId,
                "Session de test annulée administrativement sans mouvement",
                "SESSION-" + sessionId
        );

        return cashMapper.toResponse(savedSession);
    }

    @Override
    public SessionCaisseResponse reouvrirControlee(Long sessionId, ReouvrirSessionControleeRequest request) {
        ensureRoleValidation();
        ensureMotif(request != null ? request.getMotif() : null);

        SessionCaisse session = getSession(sessionId);
        if (session.getStatut() == StatutSessionCaisse.VALIDEE_CONTROLE) {
            throw new BusinessException("SESSION_DEJA_VALIDEE_CONTROLE_REOUVERTURE_REFUSEE");
        }
        if (session.getStatut() != StatutSessionCaisse.PRE_CLOTUREE) {
            throw new BusinessException("SESSION_STATUT_INCOMPATIBLE");
        }

        Utilisateur actor = getCurrentUser();
        StatutSessionCaisse oldStatut = session.getStatut();
        session.setStatut(StatutSessionCaisse.OUVERTE);
        session.setCommentaireCorrection(clean(request.getCommentaire()));
        session.setStatutCorrection("REOUVERTURE_CONTROLEE");
        SessionCaisse savedSession = sessionCaisseRepository.save(session);

        SessionCaisseAnomalie anomalie = SessionCaisseAnomalie.builder()
                .session(savedSession)
                .typeAnomalie(TypeAnomalieSessionCaisse.PRE_CLOTURE_ERRONEE)
                .ancienStatut(oldStatut)
                .nouveauStatut(StatutSessionCaisse.OUVERTE)
                .motif(clean(request.getMotif()))
                .commentaire(clean(request.getCommentaire()))
                .demandePar(actor)
                .validePar(actor)
                .dateDemande(LocalDateTime.now())
                .dateValidation(LocalDateTime.now())
                .statutDossier(StatutDossierAnomalieSession.EXECUTEE)
                .actionExecutee("REOUVERTURE_CONTROLEE_EXECUTEE")
                .build();
        anomalieRepository.save(anomalie);

        auditService.logInfo(
                AuditAction.SESSION_REOUVERTE_CONTROLEE,
                AuditModule.SESSION_CAISSE,
                "SessionCaisse",
                sessionId,
                "Réouverture contrôlée d'une session pré-clôturée",
                "SESSION-" + sessionId
        );

        return cashMapper.toResponse(savedSession);
    }

    @Override
    public List<SessionCaisseAnomalieResponse> getAnomaliesBySession(Long sessionId) {
        return anomalieRepository.findBySessionIdOrderByDateDemandeDesc(sessionId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<SessionCaisseAnomalieResponse> getAnomaliesGlobales() {
        return anomalieRepository.findAllByOrderByDateDemandeDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private SessionCaisseAnomalieResponse toResponse(SessionCaisseAnomalie a) {
        return SessionCaisseAnomalieResponse.builder()
                .id(a.getId())
                .sessionId(a.getSession() != null ? a.getSession().getId() : null)
                .typeAnomalie(a.getTypeAnomalie())
                .ancienStatut(a.getAncienStatut())
                .nouveauStatut(a.getNouveauStatut())
                .motif(a.getMotif())
                .commentaire(a.getCommentaire())
                .statutDossier(a.getStatutDossier())
                .demandePar(a.getDemandePar() != null ? a.getDemandePar().getUsername() : null)
                .validePar(a.getValidePar() != null ? a.getValidePar().getUsername() : null)
                .dateDemande(a.getDateDemande())
                .dateValidation(a.getDateValidation())
                .actionExecutee(a.getActionExecutee())
                .build();
    }

    private void ensureRoleValidation() {
        Utilisateur current = getCurrentUser();
        if (current == null || current.getRole() == null) {
            throw new BusinessException("PERMISSION_INSUFFISANTE");
        }

        RoleCode role = current.getRole().getCode();
        if (role != RoleCode.ADMIN && role != RoleCode.CHEF_BUREAU) {
            throw new BusinessException("PERMISSION_INSUFFISANTE");
        }
    }

    private void ensureAdminOnly() {
        Utilisateur current = getCurrentUser();
        if (current == null || current.getRole() == null || current.getRole().getCode() != RoleCode.ADMIN) {
            throw new BusinessException("PERMISSION_INSUFFISANTE");
        }
    }

    private void ensureNoPendingAnomaly(Long sessionId) {
        anomalieRepository.findFirstBySessionIdAndStatutDossierOrderByDateDemandeDesc(sessionId, StatutDossierAnomalieSession.DEMANDEE)
                .ifPresent(a -> {
                    throw new BusinessException("ANOMALIE_DEJA_TRAITEE");
                });
    }

    private void createRectificationRequiredAnomaly(SessionCaisse session, DemanderAnnulationSessionRequest request) {
        Utilisateur actor = getCurrentUser();
        SessionCaisseAnomalie anomaly = SessionCaisseAnomalie.builder()
                .session(session)
                .typeAnomalie(TypeAnomalieSessionCaisse.SESSION_AVEC_MOUVEMENTS_RECTIFICATION_REQUISE)
                .ancienStatut(session.getStatut())
                .nouveauStatut(session.getStatut())
                .motif(clean(request != null ? request.getMotif() : null))
                .commentaire("Session avec mouvements : procédure de rectification requise.")
                .demandePar(actor)
                .dateDemande(LocalDateTime.now())
                .statutDossier(StatutDossierAnomalieSession.DEMANDEE)
                .actionExecutee("RECTIFICATION_REQUISE")
                .build();
        anomalieRepository.save(anomaly);

        auditService.logWarning(
                AuditAction.REFUS_TRANSITION,
                AuditModule.SESSION_CAISSE,
                "SessionCaisse",
                session.getId(),
                "Session avec mouvements : procédure de rectification requise.",
                "SESSION-" + session.getId()
        );
    }

    private TypeAnomalieSessionCaisse resolveType(DemanderAnnulationSessionRequest request, TypeAnomalieSessionCaisse fallback) {
        if (request == null || request.getTypeAnomalie() == null) {
            return fallback;
        }
        return request.getTypeAnomalie();
    }

    private void ensureMotif(String motif) {
        if (clean(motif) == null) {
            throw new BusinessException("MOTIF_OBLIGATOIRE");
        }
    }

    private SessionCaisse getSession(Long sessionId) {
        return sessionCaisseRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session caisse introuvable"));
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private Utilisateur getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getName() == null) {
                return null;
            }
            return utilisateurRepository.findByUsernameWithValidationContext(auth.getName())
                    .orElseGet(() -> utilisateurRepository.findByUsername(auth.getName()).orElse(null));
        } catch (Exception e) {
            return null;
        }
    }
}
