package com.mini.credit.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini.credit.dto.caisse.SessionCaisseCloseRequest;
import com.mini.credit.dto.caisse.SessionCaisseOpeningContextResponse;
import com.mini.credit.dto.caisse.SessionCaisseOpenRequest;
import com.mini.credit.dto.caisse.SessionCaisseResponse;
import com.mini.credit.entity.caisse.Caisse;
import com.mini.credit.entity.caisse.EcartCaisse;
import com.mini.credit.entity.caisse.OperationCaisse;
import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.entity.caisse.SessionCaisseAnomalie;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.DepenseCaisseStatus;
import com.mini.credit.enums.security.PermissionCode;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.enums.security.AuditModule;
import com.mini.credit.enums.StatutSessionCaisse;
import com.mini.credit.enums.StatutEcartCaisse;
import com.mini.credit.enums.TypeEcartCaisse;
import com.mini.credit.enums.SourceOperationCaisse;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.exception.ResourceNotFoundException;
import com.mini.credit.mapper.CashMapper;
import com.mini.credit.repository.caisse.CaisseRepository;
import com.mini.credit.repository.caisse.DepenseCaisseRepository;
import com.mini.credit.repository.caisse.EcartCaisseRepository;
import com.mini.credit.repository.caisse.OperationCaisseRepository;
import com.mini.credit.repository.caisse.SessionCaisseAnomalieRepository;
import com.mini.credit.repository.caisse.SessionCaisseRepository;
import com.mini.credit.repository.utilisateur.UtilisateurRepository;
import com.mini.credit.service.EcartThresholdConfigService;
import com.mini.credit.service.SessionCaisseService;
import com.mini.credit.service.WorkflowTaskService;
import com.mini.credit.service.audit.AuditService;
import com.mini.credit.service.audit.Auditable;
import com.mini.credit.enums.security.AuditAction;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SessionCaisseServiceImpl implements SessionCaisseService {

    private static final String DEBUG_CAISSE_CODE = "CAI202606250001";

    private static final List<StatutSessionCaisse> STATUTS_SESSION_ACTIVE = List.of(
            StatutSessionCaisse.OUVERTE,
            StatutSessionCaisse.PRE_CLOTUREE,
            StatutSessionCaisse.VALIDEE_CONTROLE
    );

    private static final List<StatutSessionCaisse> STATUTS_BLOQUANTS_OUVERTURE = STATUTS_SESSION_ACTIVE;

    private final SessionCaisseRepository sessionCaisseRepository;
    private final CaisseRepository caisseRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final OperationCaisseRepository operationCaisseRepository;
    private final SessionCaisseAnomalieRepository sessionCaisseAnomalieRepository;
    private final EcartCaisseRepository ecartCaisseRepository;
    private final DepenseCaisseRepository depenseCaisseRepository;
    private final CashMapper cashMapper;
    private final EcartThresholdConfigService ecartThresholdConfigService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final WorkflowTaskService workflowTaskService;

        private record OpeningBalanceDecision(
            boolean premiereSession,
            BigDecimal soldeOuvertureAutomatique,
            BigDecimal soldeOuvertureEffectif,
            BigDecimal soldeOuvertureDemande,
            boolean forcageManuel,
            boolean forcageAutorise
        ) {}

    @Override
    @Auditable(action = AuditAction.SESSION_CAISSE_OPENED, entityType = "SessionCaisse")
    public SessionCaisseResponse ouvrir(SessionCaisseOpenRequest request) {
        validerOuvertureRequest(request);

        LocalDate today = LocalDate.now();
        LocalDate dateComptable = request.getDateComptable() != null
                ? request.getDateComptable()
                : request.getDateOuverture().toLocalDate();

        if (!dateComptable.equals(today)) {
            throw new BusinessException("La session de caisse doit être ouverte pour la date comptable du jour");
        }

        if (!request.getDateOuverture().toLocalDate().equals(dateComptable)) {
            throw new BusinessException("La date d'ouverture doit correspondre à la date comptable");
        }

        Caisse caisse = caisseRepository.findById(request.getCaisseId())
                .orElseThrow(() -> new ResourceNotFoundException("Caisse introuvable"));

        if (Boolean.FALSE.equals(caisse.getActif())) {
            throw new BusinessException("La caisse est inactive");
        }

        // Utiliser l'utilisateur connecté (extrait du contexte de sécurité)
        Utilisateur utilisateur = getCurrentUser();
        if (utilisateur == null) {
            throw new BusinessException("Utilisateur authentifié introuvable");
        }

        if (utilisateur.getRole() != null && utilisateur.getRole().getCode() == RoleCode.CAISSIER) {
            if (!canAccessCaisseForCaissier(caisse, utilisateur)) {
                throw new BusinessException("Cette caisse n'est pas accessible à ce caissier");
            }
        }

        if (sessionCaisseRepository.existsByCaisseIdAndStatutIn(request.getCaisseId(), STATUTS_SESSION_ACTIVE)) {
            auditService.logWarning(
                    AuditAction.REFUS_TRANSITION,
                    AuditModule.SESSION_CAISSE,
                    "SessionCaisse",
                    null,
                    "Ouverture refusée: session active déjà existante pour cette caisse",
                    "CAISSE-" + request.getCaisseId()
            );
            throw new BusinessException("Une session active existe déjà pour cette caisse");
        }

        if (sessionCaisseRepository.existsByUtilisateurIdAndStatutIn(utilisateur.getId(), STATUTS_SESSION_ACTIVE)) {
            auditService.logWarning(
                    AuditAction.REFUS_TRANSITION,
                    AuditModule.SESSION_CAISSE,
                    "SessionCaisse",
                    null,
                    "Ouverture refusée: l'utilisateur a déjà une session active",
                    "USER-" + utilisateur.getId()
            );
            throw new BusinessException("Cet utilisateur a déjà une session de caisse active");
        }

        List<SessionCaisse> sessionsAnciennesOuvertes = sessionCaisseRepository
                .findByCaisseIdAndDateComptableBeforeAndStatutInOrderByDateComptableAscDateOuvertureAsc(
                        request.getCaisseId(),
                        dateComptable,
                        STATUTS_SESSION_ACTIVE
                );
        if (!sessionsAnciennesOuvertes.isEmpty()) {
            SessionCaisse sessionBloquante = sessionsAnciennesOuvertes.get(0);
            auditService.logWarning(
                AuditAction.REFUS_TRANSITION,
                AuditModule.SESSION_CAISSE,
                "SessionCaisse",
                sessionBloquante.getId(),
                "Ouverture refusée: session ancienne non clôturée",
                "SESSION-" + sessionBloquante.getId()
            );
            throw new BusinessException(
                    "Une session ancienne ouverte existe déjà pour cette caisse (date comptable "
                            + sessionBloquante.getDateComptable()
                            + "). Clôturez ou régularisez-la avant d'ouvrir la session du jour."
            );
        }

        if (sessionCaisseRepository.existsByCaisseIdAndDateComptableAndStatutIn(
            request.getCaisseId(),
            dateComptable,
            STATUTS_BLOQUANTS_OUVERTURE
        )) {
            throw new BusinessException("Une session existe déjà pour cette caisse à la date comptable du jour");
        }

        List<SessionCaisse> sessionsCloturees = sessionCaisseRepository
            .findClosedSessionsForOpening(request.getCaisseId(), dateComptable, request.getDateOuverture());
        OpeningBalanceDecision openingBalanceDecision = resolveOpeningBalanceDecision(
                request,
                utilisateur,
                sessionsCloturees
        );

        String observationOuverture = cleanNullableText(request.getObservation());
        if (openingBalanceDecision.premiereSession() && observationOuverture == null) {
            observationOuverture = "Fonds de roulement initial";
        } else if (!openingBalanceDecision.premiereSession() && observationOuverture == null) {
            observationOuverture = "Report automatique du solde de clôture de la session précédente";
        }

        SessionCaisse session = SessionCaisse.builder()
                .caisse(caisse)
                .utilisateur(utilisateur)
                .dateComptable(dateComptable)
                .dateOuverture(request.getDateOuverture())
                .soldeOuverture(openingBalanceDecision.soldeOuvertureEffectif())
                .totalEntrees(BigDecimal.ZERO)
                .totalSorties(BigDecimal.ZERO)
                .soldeTheorique(openingBalanceDecision.soldeOuvertureEffectif())
                .statut(StatutSessionCaisse.OUVERTE)
                .observation(observationOuverture)
                .build();

            SessionCaisse savedSession = sessionCaisseRepository.save(session);
            Map<String, Object> ouvertureAuditDetails = buildOpeningAuditDetails(
                    caisse,
                    utilisateur,
                    openingBalanceDecision,
                    observationOuverture,
                    sessionsCloturees
            );
            auditerTransition(
                AuditAction.OUVERTURE_SESSION,
                savedSession,
                null,
                StatutSessionCaisse.OUVERTE,
                observationOuverture,
                buildMotifAvecSupplance(
                        utilisateur,
                        openingBalanceDecision.forcageManuel()
                                ? "Ouverture de session caisse avec override manuel du solde d'ouverture"
                                : "Ouverture de session caisse"
                ),
                ouvertureAuditDetails
            );

            return enrichSessionResponse(savedSession, cashMapper.toResponse(savedSession));
    }

    @Override
    public SessionCaisseOpeningContextResponse getOpeningContext(Long caisseId, LocalDate dateComptable) {
        if (caisseId == null) {
            throw new BusinessException("La caisse est obligatoire");
        }

        LocalDate dateReference = dateComptable != null ? dateComptable : LocalDate.now();

        Caisse caisse = caisseRepository.findById(caisseId)
                .orElseThrow(() -> new ResourceNotFoundException("Caisse introuvable"));

        if (!canAccessCaisse(caisse)) {
            throw new BusinessException("Cette caisse n'est pas accessible");
        }

        SessionCaisse sessionExistante = sessionCaisseRepository
            .findFirstByCaisseIdAndStatutInOrderByDateOuvertureDesc(caisseId, STATUTS_SESSION_ACTIVE)
            .orElse(null);

        logDebugSessionsIfTargetCaisse(caisse);

        List<SessionCaisse> sessionsCloturees = sessionCaisseRepository.findClosedSessionsForOpening(
            caisseId,
            dateReference,
            dateReference.plusDays(1).atStartOfDay()
        );
        boolean premiereSession = sessionsCloturees.isEmpty();
        BigDecimal soldeOuvertureAutomatique = resolveAutomaticOpeningBalance(sessionsCloturees);
        boolean forcageAutorise = hasAuthority(PermissionCode.SESSION_CAISSE_OPEN_OVERRIDE.name());

        return SessionCaisseOpeningContextResponse.builder()
                .caisseId(caisseId)
                .dateComptable(dateReference)
                .devise(caisse.getDevise())
                .premiereSession(premiereSession)
                .soldeOuvertureAutomatique(soldeOuvertureAutomatique)
                .forcageAutorise(forcageAutorise)
                .soldeVerrouille(!premiereSession && !forcageAutorise)
            .sessionExistante(sessionExistante != null)
            .sessionExistanteId(sessionExistante != null ? sessionExistante.getId() : null)
            .sessionExistanteStatut(sessionExistante != null && sessionExistante.getStatut() != null ? sessionExistante.getStatut().name() : null)
            .sessionExistanteDateOuverture(sessionExistante != null ? sessionExistante.getDateOuverture() : null)
            .sessionExistanteDateCloture(sessionExistante != null ? sessionExistante.getDateCloture() : null)
            .sessionExistanteUtilisateurId(sessionExistante != null && sessionExistante.getUtilisateur() != null ? sessionExistante.getUtilisateur().getId() : null)
            .sessionExistanteUtilisateurNom(sessionExistante != null && sessionExistante.getUtilisateur() != null ? sessionExistante.getUtilisateur().getNomComplet() : null)
                .build();
    }

    private void logDebugSessionsIfTargetCaisse(Caisse caisse) {
        if (caisse == null || caisse.getId() == null || caisse.getCodeCaisse() == null) {
            return;
        }

        if (!DEBUG_CAISSE_CODE.equalsIgnoreCase(caisse.getCodeCaisse())) {
            return;
        }

        List<SessionCaisse> sessions = sessionCaisseRepository.findTop50ByCaisseIdOrderByDateOuvertureDesc(caisse.getId());
        log.info("[DEBUG_SESSION_ACTIVE] caisseCode={} caisseId={} sessionsCount={}", caisse.getCodeCaisse(), caisse.getId(), sessions.size());

        for (SessionCaisse session : sessions) {
            boolean isActiveCalculated = isActiveSessionStatus(session != null ? session.getStatut() : null);
            log.info(
                    "[DEBUG_SESSION_ACTIVE] sessionId={} caisseId={} statut={} dateOuverture={} dateCloture={} soldeOuverture={} soldeTheorique={} isActiveCalculated={}",
                    session != null ? session.getId() : null,
                    session != null && session.getCaisse() != null ? session.getCaisse().getId() : null,
                    session != null ? session.getStatut() : null,
                    session != null ? session.getDateOuverture() : null,
                    session != null ? session.getDateCloture() : null,
                    session != null ? session.getSoldeOuverture() : null,
                    session != null ? session.getSoldeTheorique() : null,
                    isActiveCalculated
            );
        }
    }

    private boolean isActiveSessionStatus(StatutSessionCaisse statut) {
        return statut != null && STATUTS_SESSION_ACTIVE.contains(statut);
    }

    @Override
    @Auditable(action = AuditAction.SESSION_CAISSE_CLOSED, entityType = "SessionCaisse", entityIdParameter = "sessionId")
    public SessionCaisseResponse preCloturer(Long sessionId, SessionCaisseCloseRequest request) {
        // STEP 1: Validate input
        validerClotureRequest(request);

        // Load session
        SessionCaisse session = sessionCaisseRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session caisse introuvable"));

        if (!canAccessSession(session)) {
            throw new BusinessException("Cette session de caisse n'est pas accessible");
        }

        // Check session is open
        if (session.getStatut() != StatutSessionCaisse.OUVERTE) {
            auditerRefusTransition(
                    session,
                    session.getStatut(),
                    StatutSessionCaisse.PRE_CLOTUREE,
                    cleanNullableText(request.getObservation()),
                    "Pré-clôture refusée: état source invalide"
            );
            throw new BusinessException("Cette session n'est pas ouverte");
        }

        if (request.getDateCloture().isBefore(session.getDateOuverture())) {
            auditerRefusTransition(
                    session,
                    session.getStatut(),
                    StatutSessionCaisse.PRE_CLOTUREE,
                    cleanNullableText(request.getObservation()),
                    "Pré-clôture refusée: date de clôture antérieure"
            );
            throw new BusinessException("La date de clôture ne peut pas être antérieure à la date d'ouverture");
        }

        if (hasPendingDepenseValidation(session)) {
            throw new BusinessException(
                    "La session contient des dépenses non autorisées. Elles doivent être validées ou rejetées avant la soumission au contrôle."
            );
        }

        // STEP 2: Recalc soldeTheorique for coherence check
        // soldeTheorique = soldeOuverture + totalEntrees - totalSorties
        BigDecimal soldeTheorique = session.getSoldeOuverture()
                .add(session.getTotalEntrees())
                .subtract(session.getTotalSorties());
        session.setSoldeTheorique(soldeTheorique);

        // STEP 3: Calc ecart = soldePhysique - soldeTheorique
        BigDecimal ecart = request.getSoldePhysique().subtract(soldeTheorique);
        session.setEcartCaisse(ecart);
        session.setSoldePhysique(request.getSoldePhysique());

        // Règle métier 3N: En cas d'écart, une justification est obligatoire.
        // Le Contrôleur doit expliquer la variance avant de pouvoir clôturer.
        if (ecart.compareTo(BigDecimal.ZERO) != 0) {
            String justification = cleanNullableText(request.getObservation());
            if (justification == null) {
                throw new BusinessException(
                    "Une justification (observation) est obligatoire en cas d'écart de caisse. " +
                    "Écart détecté: " + ecart + " CDF.");
            }
            // Validation technique provisoire — NON défini dans documents 3N
            // À réviser avec 3N si besoin d'assouplir ou durcir la règle
            if (justification.length() < 10) {
                throw new BusinessException(
                    "La justification doit contenir au moins 10 caractères " +
                    "[validation technique provisoire — non défini 3N].");
            }
        }

        // STEP 4: Validate before closing (Q8: 4 validations)
        validateBeforeClosing(session);

        // STEP 5: Save pre-closure data (no final closure yet)
        session.setObservation(cleanNullableText(request.getObservation()));

        // Get current user (CONTROLEUR) for audit trail
        Utilisateur currentUser = getCurrentUser();
        session.setFermePar(currentUser);

        session.setStatut(StatutSessionCaisse.PRE_CLOTUREE);

        // Save session
        SessionCaisse savedSession = sessionCaisseRepository.save(session);

        // Auto-create EcartCaisse if ecart != 0 (requires justification via workflow)
        if (ecart.compareTo(BigDecimal.ZERO) != 0) {
            try {
                creerEcartAutomatique(savedSession);
            } catch (Exception e) {
                // Log but don't fail closure if écart creation fails
                // The session is already closed and can be investigated later
            }
        }

        auditerTransition(
            AuditAction.PRE_CLOTURE,
            savedSession,
            StatutSessionCaisse.OUVERTE,
            StatutSessionCaisse.PRE_CLOTUREE,
            cleanNullableText(request.getObservation()),
            buildMotifAvecSupplance(getCurrentUser(), "Pré-clôture de session effectuée")
        );

        workflowTaskService.onSessionPreCloturee(
            savedSession.getId(),
            resolveWorkflowReference(savedSession),
            resolveSessionAntenneId(savedSession),
            resolveSessionSiteId(savedSession)
        );

        return enrichSessionResponse(savedSession, cashMapper.toResponse(savedSession));
    }

    @Override
    public SessionCaisseResponse cloturer(Long sessionId, SessionCaisseCloseRequest request) {
        // Backward compatibility: old endpoint /cloturer performs pre-closure only.
        return preCloturer(sessionId, request);
    }

    @Override
    public List<SessionCaisseResponse> getAll() {
        Utilisateur currentUser = getCurrentUser();
        if (currentUser != null && currentUser.getRole() != null && currentUser.getRole().getCode() == RoleCode.CAISSIER) {
            return sessionCaisseRepository.findAllByOrderByDateComptableDescDateOuvertureDescIdDesc().stream()
                .filter(this::canAccessSession)
                .map(session -> enrichSessionResponse(session, cashMapper.toResponse(session)))
                .toList();
        }

        return sessionCaisseRepository.findAllByOrderByDateComptableDescDateOuvertureDescIdDesc().stream()
            .map(session -> enrichSessionResponse(session, cashMapper.toResponse(session)))
            .toList();
    }

    @Override
    public SessionCaisseResponse getById(Long id) {
        SessionCaisse session = sessionCaisseRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Session caisse introuvable"));

        if (!canAccessSession(session)) {
            throw new BusinessException("Cette session de caisse n'est pas accessible");
        }

        return enrichSessionResponse(session, cashMapper.toResponse(session).toBuilder()
            .mouvements(operationCaisseRepository.findBySessionCaisseIdOrderByDateOperationDesc(id)
                .stream()
                .map(cashMapper::toResponse)
                .toList())
            .build());
    }

    @Override
    public SessionCaisseResponse getSessionActive() {
        LocalDate today = LocalDate.now();
        Utilisateur currentUser = getCurrentUser();

        if (currentUser != null && currentUser.getRole() != null && currentUser.getRole().getCode() == RoleCode.CAISSIER) {
            SessionCaisse sessionCaissier = sessionCaisseRepository
                .findFirstByUtilisateurIdAndDateComptableAndStatutOrderByDateOuvertureDesc(currentUser.getId(), today, StatutSessionCaisse.OUVERTE)
                .orElseThrow(() -> new ResourceNotFoundException("Aucune session caisse ouverte pour ce Caissier aujourd’hui. Veuillez ouvrir une session caisse avant d’encaisser les frais."));

            return enrichSessionResponse(sessionCaissier, cashMapper.toResponse(sessionCaissier));
        }

        SessionCaisse session = sessionCaisseRepository
            .findFirstByDateComptableAndStatutInOrderByDateOuvertureDesc(today, STATUTS_SESSION_ACTIVE)
            .orElseThrow(() -> {
                SessionCaisse ancienneSession = sessionCaisseRepository
                    .findFirstByDateComptableBeforeAndStatutInOrderByDateComptableDescDateOuvertureDesc(today, STATUTS_SESSION_ACTIVE)
                    .orElse(null);
                if (ancienneSession != null) {
                return new BusinessException(
                    "Une session ancienne ouverte du " + ancienneSession.getDateComptable()
                        + " bloque le travail du jour. Clôturez ou régularisez-la avant de continuer."
                );
                }
                return new ResourceNotFoundException("Aucune session de caisse ouverte pour aujourd'hui");
            });

        if (!canAccessSession(session)) {
            throw new ResourceNotFoundException("Aucune session de caisse accessible pour aujourd'hui");
        }

        return enrichSessionResponse(session, cashMapper.toResponse(session));
    }

    @Override
    public SessionCaisseResponse getSessionOuverteByCaisse(Long caisseId) {
        LocalDate today = LocalDate.now();
        Caisse caisse = caisseRepository.findById(caisseId)
            .orElseThrow(() -> new ResourceNotFoundException("Caisse introuvable"));

        if (!canAccessCaisse(caisse)) {
            throw new ResourceNotFoundException("Aucune session ouverte pour cette caisse aujourd'hui");
        }

        SessionCaisse session = sessionCaisseRepository
            .findFirstByCaisseIdAndDateComptableAndStatutInOrderByDateOuvertureDesc(caisseId, today, STATUTS_SESSION_ACTIVE)
            .orElseThrow(() -> {
                SessionCaisse ancienneSession = sessionCaisseRepository
                    .findFirstByCaisseIdAndDateComptableBeforeAndStatutInOrderByDateComptableDescDateOuvertureDesc(caisseId, today, STATUTS_SESSION_ACTIVE)
                    .orElse(null);
                if (ancienneSession != null) {
                return new BusinessException(
                    "Une session ancienne ouverte du " + ancienneSession.getDateComptable()
                        + " bloque la caisse. Clôturez ou régularisez-la avant de continuer."
                );
                }
                return new ResourceNotFoundException("Aucune session ouverte pour cette caisse aujourd'hui");
            });

        return enrichSessionResponse(session, cashMapper.toResponse(session));
    }

        @Override
        public BigDecimal calculerSoldeOuvertureAutomatique(Long caisseId, LocalDate dateComptable) {
        List<SessionCaisse> sessionsCloturees = sessionCaisseRepository.findClosedSessionsForOpening(
            caisseId,
            dateComptable,
            dateComptable != null ? dateComptable.plusDays(1).atStartOfDay() : null
        );
        return resolveAutomaticOpeningBalance(sessionsCloturees);
        }

    private OpeningBalanceDecision resolveOpeningBalanceDecision(
            SessionCaisseOpenRequest request,
            Utilisateur utilisateur,
            List<SessionCaisse> sessionsCloturees
    ) {
        boolean premiereSession = sessionsCloturees.isEmpty();
        BigDecimal soldeOuvertureAutomatique = resolveAutomaticOpeningBalance(sessionsCloturees);
        BigDecimal soldeOuvertureDemande = request.getSoldeOuverture();
        boolean forcageAutorise = hasAuthority(PermissionCode.SESSION_CAISSE_OPEN_OVERRIDE.name());
        boolean forcageManuel = false;
        BigDecimal soldeOuvertureEffectif;

        if (premiereSession) {
            if (!isRoleAllowedForFirstSession(utilisateur)) {
                throw new BusinessException(
                        "Première session: seuls ADMIN, CAISSIER ou CHEF_BUREAU en suppléance peuvent saisir le fonds de roulement initial."
                );
            }
            soldeOuvertureEffectif = soldeOuvertureDemande != null ? soldeOuvertureDemande : BigDecimal.ZERO;
        } else {
            soldeOuvertureEffectif = soldeOuvertureAutomatique;
        }

        return new OpeningBalanceDecision(
                premiereSession,
                soldeOuvertureAutomatique,
                soldeOuvertureEffectif,
                soldeOuvertureDemande,
                forcageManuel,
                forcageAutorise
        );
    }

    private BigDecimal resolveAutomaticOpeningBalance(List<SessionCaisse> sessionsCloturees) {
        return sessionsCloturees.stream()
                .findFirst()
                .map(session -> session.getSoldePhysique() != null ? session.getSoldePhysique() : session.getSoldeTheorique())
                .orElse(BigDecimal.ZERO);
    }

    private boolean isRoleAllowedForFirstSession(Utilisateur utilisateur) {
        if (utilisateur == null || utilisateur.getRole() == null || utilisateur.getRole().getCode() == null) {
            return false;
        }

        RoleCode roleCode = utilisateur.getRole().getCode();
        return roleCode == RoleCode.ADMIN
            || roleCode == RoleCode.CAISSIER
            || roleCode == RoleCode.CHEF_BUREAU;
    }

    private Map<String, Object> buildOpeningAuditDetails(
            Caisse caisse,
            Utilisateur utilisateur,
            OpeningBalanceDecision decision,
            String observation,
            List<SessionCaisse> sessionsCloturees
    ) {
        Map<String, Object> details = new LinkedHashMap<>();
        SessionCaisse derniereSessionCloturee = (sessionsCloturees != null && !sessionsCloturees.isEmpty())
            ? sessionsCloturees.get(0)
            : null;
        BigDecimal dernierSoldeCloture = derniereSessionCloturee != null
            ? (derniereSessionCloturee.getSoldePhysique() != null
                ? derniereSessionCloturee.getSoldePhysique()
                : derniereSessionCloturee.getSoldeTheorique())
            : null;

        details.put("caisseId", caisse != null ? caisse.getId() : null);
        details.put("deviseCaisse", caisse != null ? caisse.getDevise() : null);
        details.put("premiereSession", decision.premiereSession());
        details.put("derniereSessionClotureeId", derniereSessionCloturee != null ? derniereSessionCloturee.getId() : null);
        details.put("dernierSoldeCloture", dernierSoldeCloture);
        details.put("soldeOuvertureCalcule", decision.soldeOuvertureAutomatique());
        details.put("soldeOuvertureSaisi", decision.soldeOuvertureDemande());
        details.put("soldeOuvertureEffectif", decision.soldeOuvertureEffectif());
        details.put("reportSoldeDepuisSessionPrecedente", !decision.premiereSession());
        details.put("soldeOuvertureFrontendIgnore", !decision.premiereSession()
            && decision.soldeOuvertureDemande() != null
            && decision.soldeOuvertureDemande().compareTo(decision.soldeOuvertureEffectif()) != 0);
        details.put("forcageManuel", decision.forcageManuel());
        details.put("forcageAutorise", decision.forcageAutorise());
        details.put("utilisateurId", utilisateur != null ? utilisateur.getId() : null);
        details.put("roleUtilisateur", utilisateur != null && utilisateur.getRole() != null
                ? utilisateur.getRole().getCode()
                : null);
        details.put("observation", observation);
        return details;
    }

    @Override
    public SessionCaisseResponse validerControle(Long sessionId, String observation) {
        SessionCaisse session = sessionCaisseRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session caisse introuvable"));

        if (session.getStatut() != StatutSessionCaisse.PRE_CLOTUREE) {
            auditerRefusTransition(
                    session,
                    session.getStatut(),
                    StatutSessionCaisse.VALIDEE_CONTROLE,
                    cleanNullableText(observation),
                    "Validation contrôle refusée: état source invalide"
            );
            throw new BusinessException("Seules les sessions pré-clôturées peuvent être validées en contrôle");
        }

        Utilisateur controleur = getCurrentUser();
        if (controleur == null) {
            throw new BusinessException("Utilisateur contrôleur introuvable");
        }

        if (controleur.getRole() == null
            || (controleur.getRole().getCode() != RoleCode.CONTROLEUR
            && controleur.getRole().getCode() != RoleCode.ADMIN)) {
            auditerRefusTransition(
                    session,
                    session.getStatut(),
                    StatutSessionCaisse.VALIDEE_CONTROLE,
                    cleanNullableText(observation),
                    "Validation contrôle refusée: rôle non autorisé"
            );
                    throw new BusinessException("Seuls ADMIN et CONTROLEUR peuvent valider le contrôle final");
        }

        if (session.getUtilisateur() != null && session.getUtilisateur().getId().equals(controleur.getId())) {
            auditerRefusTransition(
                    session,
                    session.getStatut(),
                    StatutSessionCaisse.VALIDEE_CONTROLE,
                    cleanNullableText(observation),
                    "Validation contrôle refusée: auto-contrôle interdit"
            );
            throw new BusinessException("Un caissier ne peut pas valider le contrôle de sa propre session");
        }

        String noteControle = "Contrôle validé le "
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                + " par " + (controleur.getNomComplet() != null ? controleur.getNomComplet() : controleur.getUsername());

        String observationControle = cleanNullableText(observation);
        if (observationControle != null) {
            noteControle += " - " + observationControle;
        }

        String observationExistante = cleanNullableText(session.getObservation());
        session.setObservation(observationExistante == null
                ? noteControle
                : observationExistante + "\n" + noteControle);

        session.setControleValidePar(controleur);
        session.setDateControle(LocalDateTime.now());
        session.setStatut(StatutSessionCaisse.VALIDEE_CONTROLE);

        SessionCaisse savedSession = sessionCaisseRepository.save(session);
        auditerTransition(
            AuditAction.VALIDATION_CONTROLE,
            savedSession,
            StatutSessionCaisse.PRE_CLOTUREE,
            StatutSessionCaisse.VALIDEE_CONTROLE,
            cleanNullableText(observation),
            "Validation contrôle effectuée"
        );

        workflowTaskService.onSessionControleValide(
            savedSession.getId(),
            resolveWorkflowReference(savedSession),
            resolveSessionAntenneId(savedSession),
            resolveSessionSiteId(savedSession)
        );

        return enrichSessionResponse(savedSession, cashMapper.toResponse(savedSession));
    }

    @Override
    public SessionCaisseResponse cloturerFinale(Long sessionId, String observation) {
        SessionCaisse session = sessionCaisseRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session caisse introuvable"));

        Utilisateur validateurFinal = getCurrentUser();
        if (validateurFinal == null || validateurFinal.getRole() == null
            || (validateurFinal.getRole().getCode() != RoleCode.CHEF_BUREAU
            && validateurFinal.getRole().getCode() != RoleCode.ADMIN)) {
            auditerRefusTransition(
                session,
                session.getStatut(),
                StatutSessionCaisse.CLOTUREE,
                cleanNullableText(observation),
                "Clôture finale refusée: rôle non autorisé"
            );
            throw new BusinessException("Seuls ADMIN et CHEF_BUREAU peuvent clôturer définitivement une session");
        }

        if (session.getStatut() != StatutSessionCaisse.VALIDEE_CONTROLE) {
            auditerRefusTransition(
                    session,
                    session.getStatut(),
                    StatutSessionCaisse.CLOTUREE,
                    cleanNullableText(observation),
                    "Clôture finale refusée: validation contrôle absente"
            );
            throw new BusinessException("La clôture finale nécessite une session validée en contrôle");
        }

        String observationFinale = cleanNullableText(observation);
        if (observationFinale != null) {
            String observationExistante = cleanNullableText(session.getObservation());
            session.setObservation(observationExistante == null
                    ? observationFinale
                    : observationExistante + "\n" + observationFinale);
        }

        session.setDateCloture(LocalDateTime.now());
        session.setStatut(StatutSessionCaisse.CLOTUREE);

        SessionCaisse savedSession = sessionCaisseRepository.save(session);
        BigDecimal soldeFinalValide = savedSession.getSoldePhysique() != null
                ? savedSession.getSoldePhysique()
                : savedSession.getSoldeTheorique();
        Map<String, Object> clotureAuditDetails = new LinkedHashMap<>();
        clotureAuditDetails.put("soldeTheoriqueFinal", savedSession.getSoldeTheorique());
        clotureAuditDetails.put("soldePhysiqueValide", savedSession.getSoldePhysique());
        clotureAuditDetails.put("ecartFinal", savedSession.getEcartCaisse());
        clotureAuditDetails.put("soldeFinalValide", soldeFinalValide);

        auditerTransition(
            AuditAction.CLOTURE_FINALE,
            savedSession,
            StatutSessionCaisse.VALIDEE_CONTROLE,
            StatutSessionCaisse.CLOTUREE,
            observationFinale,
            "Clôture finale effectuée",
            clotureAuditDetails
        );

        workflowTaskService.onSessionCloturee(
                savedSession.getId(),
                resolveWorkflowReference(savedSession),
                resolveSessionAntenneId(savedSession),
                resolveSessionSiteId(savedSession)
        );

        return enrichSessionResponse(savedSession, cashMapper.toResponse(savedSession));
    }

    private Long resolveSessionAntenneId(SessionCaisse session) {
        if (session == null || session.getCaisse() == null || session.getCaisse().getAgence() == null) {
            return null;
        }
        return session.getCaisse().getAgence().getId();
    }

    private Long resolveSessionSiteId(SessionCaisse session) {
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

    private void validerOuvertureRequest(SessionCaisseOpenRequest request) {
        if (request == null) {
            throw new BusinessException("La requête d'ouverture de session est obligatoire");
        }

        if (request.getCaisseId() == null) {
            throw new BusinessException("La caisse est obligatoire");
        }

        if (request.getDateOuverture() == null) {
            throw new BusinessException("La date d'ouverture est obligatoire");
        }

        if (request.getSoldeOuverture() != null && request.getSoldeOuverture().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Le solde d'ouverture ne peut pas être négatif");
        }
    }

    private void validerClotureRequest(SessionCaisseCloseRequest request) {
        if (request == null) {
            throw new BusinessException("La requête de clôture de session est obligatoire");
        }

        if (request.getDateCloture() == null) {
            throw new BusinessException("La date de clôture est obligatoire");
        }

        if (request.getSoldePhysique() == null) {
            throw new BusinessException("Le solde physique est obligatoire");
        }

        if (request.getSoldePhysique().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Le solde physique ne peut pas être négatif");
        }
    }

    private boolean canAccessSession(SessionCaisse session) {
        if (session == null || session.getCaisse() == null) {
            return false;
        }

        return canAccessCaisse(session.getCaisse());
    }

    private boolean canAccessCaisse(Caisse caisse) {
        Utilisateur currentUser = getCurrentUser();
        if (currentUser == null || currentUser.getRole() == null) {
            return false;
        }

        RoleCode role = currentUser.getRole().getCode();
        if (role == RoleCode.ADMIN
            || role == RoleCode.CONTROLEUR
            || role == RoleCode.CHEF_BUREAU
            || role == RoleCode.RCI
            || role == RoleCode.GERANT_GENERAL) {
            return true;
        }

        if (role != RoleCode.CAISSIER) {
            return false;
        }

        return canAccessCaisseForCaissier(caisse, currentUser);
    }

    private boolean canAccessCaisseForCaissier(Caisse caisse, Utilisateur user) {
        if (caisse == null || user == null) {
            return false;
        }

        Long caisseAgenceId = caisse.getAgence() != null ? caisse.getAgence().getId() : null;
        Long userAgenceId = resolveCurrentUserAgenceId(user);
        boolean agenceCompatible = caisseAgenceId != null && userAgenceId != null && caisseAgenceId.equals(userAgenceId);
        boolean affectationExplicite = user.getId() != null
                && caisse.getCaissierResponsable() != null
                && caisse.getCaissierResponsable().getId() != null
                && user.getId().equals(caisse.getCaissierResponsable().getId());
        return agenceCompatible || affectationExplicite;
    }

    private Long resolveCurrentUserAgenceId(Utilisateur currentUser) {
        if (currentUser == null) {
            return null;
        }

        if (currentUser.getEmploye() != null
                && currentUser.getEmploye().getAgence() != null
                && currentUser.getEmploye().getAgence().getId() != null) {
            return currentUser.getEmploye().getAgence().getId();
        }

        if (currentUser.getSite() != null
                && currentUser.getSite().getAgence() != null
                && currentUser.getSite().getAgence().getId() != null) {
            return currentUser.getSite().getAgence().getId();
        }

        if (currentUser.getEmploye() != null
                && currentUser.getEmploye().getSite() != null
                && currentUser.getEmploye().getSite().getAgence() != null
                && currentUser.getEmploye().getSite().getAgence().getId() != null) {
            return currentUser.getEmploye().getSite().getAgence().getId();
        }

        return null;
    }

    private String cleanNullableText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private SessionCaisseResponse enrichSessionResponse(SessionCaisse session, SessionCaisseResponse response) {
        if (session == null || response == null) {
            return response;
        }

        long mouvements = operationCaisseRepository.countBySessionCaisseId(session.getId());
        boolean hasNoMouvement = mouvements == 0;
        boolean canValidate = hasAnyRole(RoleCode.ADMIN, RoleCode.CHEF_BUREAU);
        Long derniereAnomalieId = sessionCaisseAnomalieRepository
                .findFirstBySessionIdOrderByDateDemandeDesc(session.getId())
                .map(SessionCaisseAnomalie::getId)
                .orElse(null);

        return response.toBuilder()
                .peutDemanderAnnulation(hasAuthority(PermissionCode.SESSION_CAISSE_ANOMALIE_REQUEST.name())
                        && session.getStatut() == StatutSessionCaisse.OUVERTE
                        && hasNoMouvement)
                .peutValiderAnnulation(hasAuthority(PermissionCode.SESSION_CAISSE_ANOMALIE_VALIDATE.name())
                        && canValidate
                        && session.getStatut() == StatutSessionCaisse.OUVERTE
                        && hasNoMouvement)
                .peutAnnulerAdministrativement(hasAuthority(PermissionCode.SESSION_CAISSE_ADMIN_CANCEL.name())
                        && canValidate
                        && session.getStatut() == StatutSessionCaisse.CLOTUREE
                        && hasNoMouvement)
                .peutReouvrirControlee(hasAuthority(PermissionCode.SESSION_CAISSE_REOPEN_CONTROLLED.name())
                        && canValidate
                        && session.getStatut() == StatutSessionCaisse.PRE_CLOTUREE
                        && session.getDateControle() == null)
                .statutCorrection(session.getStatutCorrection())
                .derniereAnomalieId(derniereAnomalieId)
                .motifAnnulation(session.getMotifAnnulation())
                .annuleePar(session.getAnnuleePar() != null ? session.getAnnuleePar().getNomComplet() : null)
                .dateAnnulation(session.getDateAnnulation())
                .build();
    }

    private boolean hasAnyRole(RoleCode... roles) {
        Utilisateur currentUser = getCurrentUser();
        if (currentUser == null || currentUser.getRole() == null || currentUser.getRole().getCode() == null) {
            return false;
        }

        return Arrays.stream(roles).anyMatch(role -> role == currentUser.getRole().getCode());
    }

    private void auditerRefusTransition(
            SessionCaisse session,
            StatutSessionCaisse ancienStatut,
            StatutSessionCaisse nouveauStatut,
            String observation,
            String motif
    ) {
        auditerTransition(
                AuditAction.REFUS_TRANSITION,
                session,
                ancienStatut,
                nouveauStatut,
                observation,
                motif,
                false,
            motif,
            null
        );
    }

    private void auditerTransition(
            AuditAction action,
            SessionCaisse session,
            StatutSessionCaisse ancienStatut,
            StatutSessionCaisse nouveauStatut,
            String observation,
            String motif
    ) {
        auditerTransition(action, session, ancienStatut, nouveauStatut, observation, motif, true, null, null);
    }

    private void auditerTransition(
            AuditAction action,
            SessionCaisse session,
            StatutSessionCaisse ancienStatut,
            StatutSessionCaisse nouveauStatut,
            String observation,
            String motif,
            Map<String, Object> additionalDetails
    ) {
        auditerTransition(action, session, ancienStatut, nouveauStatut, observation, motif, true, null, additionalDetails);
    }

    private void auditerTransition(
            AuditAction action,
            SessionCaisse session,
            StatutSessionCaisse ancienStatut,
            StatutSessionCaisse nouveauStatut,
            String observation,
            String motif,
            boolean success,
            String errorMessage,
            Map<String, Object> additionalDetails
    ) {
        if (session == null) {
            return;
        }

        String oldValuesJson = toJson(snapshotAuditTransition(session, ancienStatut, observation, motif, additionalDetails));
        String newValuesJson = toJson(snapshotAuditTransition(session, nouveauStatut, observation, motif, additionalDetails));

        auditService.logWithValues(
                action,
                "SessionCaisse",
                session.getId(),
                success,
                motif,
                oldValuesJson,
                newValuesJson,
                errorMessage
        );
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.warn("Impossible de sérialiser le payload d'audit session", e);
            return null;
        }
    }

    private Map<String, Object> snapshotAuditTransition(
            SessionCaisse session,
            StatutSessionCaisse statut,
            String observation,
            String motif,
            Map<String, Object> additionalDetails
    ) {
        Utilisateur currentUser = getCurrentUser();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sessionId", session.getId());
        payload.put("caisseId", session.getCaisse() != null ? session.getCaisse().getId() : null);
        payload.put("caisseCode", session.getCaisse() != null ? session.getCaisse().getCodeCaisse() : null);
        payload.put("agenceId", session.getCaisse() != null && session.getCaisse().getAgence() != null
            ? session.getCaisse().getAgence().getId()
            : null);
        payload.put("antenneId", session.getCaisse() != null && session.getCaisse().getAgence() != null
            ? session.getCaisse().getAgence().getId()
            : null);
        payload.put("siteId", session.getCaisse() != null && session.getCaisse().getSite() != null
                ? session.getCaisse().getSite().getId()
                : null);
        payload.put("utilisateurId", currentUser != null ? currentUser.getId() : null);
        payload.put("nomUtilisateur", currentUser != null ? currentUser.getUsername() : null);
        payload.put("roleUtilisateur", currentUser != null && currentUser.getRole() != null
                ? currentUser.getRole().getCode()
                : null);
        payload.put("modeIntervention", isSupplanceCaissier(currentUser) ? "SUPPLEANCE_CAISSIER" : "STANDARD");
        payload.put("estSupplanceCaissier", isSupplanceCaissier(currentUser));
        payload.put("statut", statut);
        payload.put("observation", observation);
        payload.put("motif", motif);
        payload.put("horodatage", LocalDateTime.now());
        if (additionalDetails != null && !additionalDetails.isEmpty()) {
            payload.putAll(additionalDetails);
        }
        return payload;
    }

    // =========================================================================
    // PATCH 3: Méthodes privées — Clôture contrôlée (Q4, Q8)
    // =========================================================================

    /**
     * Q8: 4 validations pré-clôture obligatoires.
     *
     * Règles métier 3N:
     *   1. Toute opération RECETTE_JOURNALIERE doit avoir un recette_id (traçabilité Q3).
     *   2. Le solde théorique ne peut pas être négatif (cohérence comptable).
     *   3. Aucun écart antérieur non résolu sur cette session (Q6 workflow).
     *
     * Bloque la clôture si une condition échoue.
     *
     * @param session SessionCaisse dont le soldeTheorique et écart sont déjà calculés
     * @throws BusinessException si une validation échoue
     */
    private void validateBeforeClosing(SessionCaisse session) {

        // Validation 1 (Q3): Pas d'opérations RECETTE_JOURNALIERE orphelines
        // Une opération source=RECETTE_JOURNALIERE doit toujours avoir recette_id non-null
        long orphelineCount = operationCaisseRepository
                .countBySessionCaisseIdAndSourceAndRecetteIdIsNull(
                        session.getId(), SourceOperationCaisse.RECETTE_JOURNALIERE);
        if (orphelineCount > 0) {
            throw new BusinessException(
                    "Clôture bloquée: " + orphelineCount +
                    " opération(s) RECETTE_JOURNALIERE sans recette_id associé(e). " +
                    "Vérifier les liens avant clôture.");
        }

        // Validation 2: Solde théorique >= 0 (cohérence comptable obligatoire)
        if (session.getSoldeTheorique() != null &&
                session.getSoldeTheorique().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(
                    "Clôture bloquée: solde théorique négatif (" +
                    session.getSoldeTheorique() + " CDF). " +
                    "Vérifier les opérations de sorties de la session.");
        }

        // Validation 3 (Q6): Aucun écart DETECTE ou EN_INVESTIGATION sur cette session
        // Les écarts doivent être RESOLU, ACCEPTE ou REJETE avant clôture
        List<EcartCaisse> ecartsBloquants = ecartCaisseRepository
                .findBySessionCaisseIdOrderByDateCreationAsc(session.getId())
                .stream()
                .filter(e -> e.getStatut() == StatutEcartCaisse.DETECTE
                          || e.getStatut() == StatutEcartCaisse.EN_INVESTIGATION)
                .toList();

        if (!ecartsBloquants.isEmpty()) {
            throw new BusinessException(
                    "Clôture bloquée: " + ecartsBloquants.size() +
                    " écart(s) non résolu(s) sur cette session (statut DETECTE ou EN_INVESTIGATION). " +
                    "Résoudre ou accepter les écarts avant clôture.");
        }

        log.debug("Session #{} — validations pré-clôture OK.", session.getId());
    }

    private boolean hasPendingDepenseValidation(SessionCaisse session) {
        if (session == null || session.getId() == null || session.getCaisse() == null || session.getCaisse().getId() == null) {
            return false;
        }

        LocalDate dateComptable = session.getDateComptable() != null
                ? session.getDateComptable()
                : LocalDate.now();
        LocalDateTime startOfDay = dateComptable.atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        return depenseCaisseRepository.existsPendingForSessionClosure(
                session.getId(),
                session.getCaisse().getId(),
                startOfDay,
                endOfDay,
                DepenseCaisseStatus.EN_ATTENTE_VALIDATION
        );
    }

    /**
     * Créer automatiquement un EcartCaisse après clôture si écart ≠ 0.
     *
     * Règle 3N: Toute variance doit être tracée et soumise au workflow
     * DETECTE → EN_INVESTIGATION → RESOLU/ACCEPTE.
     *
     * Le seuil de déclenchement enquête automatique est paramétrable
     * via EcartThresholdConfigService (aucun seuil hardcodé ici).
     *
     * @param session SessionCaisse fermée avec écartCaisse calculé
     */
    private void creerEcartAutomatique(SessionCaisse session) {
        try {
            BigDecimal montantEcart = session.getEcartCaisse();
            if (montantEcart == null || montantEcart.compareTo(BigDecimal.ZERO) == 0) {
                return; // Pas d'écart — rien à créer
            }

            // Déterminer type: DEFICIT (argent manquant) ou EXCEDENT (argent en trop)
            TypeEcartCaisse typeEcart = montantEcart.compareTo(BigDecimal.ZERO) < 0
                    ? TypeEcartCaisse.DEFICIT
                    : TypeEcartCaisse.EXCEDENT;

            BigDecimal montantAbs = montantEcart.abs();

            // Vérifier seuil via service paramétrable (pas de valeur fixe ici)
            boolean seuilDepassé = ecartThresholdConfigService.necessiteAutoenquete(montantAbs);

            String description = String.format(
                    "Écart détecté automatiquement lors de la clôture. " +
                    "Solde physique: %s CDF | Solde théorique: %s CDF | Écart: %s CDF.",
                    session.getSoldePhysique(), session.getSoldeTheorique(), montantEcart);

            EcartCaisse ecart = EcartCaisse.builder()
                    .sessionCaisse(session)
                    .dateJour(session.getDateCloture() != null
                            ? session.getDateCloture().toLocalDate()
                            : java.time.LocalDate.now())
                    .typeEcart(typeEcart)
                    .montantEcart(montantAbs)
                    .description(description)
                    .statut(StatutEcartCaisse.DETECTE)
                    .seuilDepassé(seuilDepassé)
                    .build();

            ecartCaisseRepository.save(ecart);
            log.info("EcartCaisse créé automatiquement pour session #{}: type={}, montant={} CDF, enquête={}",
                    session.getId(), typeEcart, montantAbs, seuilDepassé);

        } catch (Exception e) {
            // L'écart peut être créé manuellement si la création auto échoue.
            // Ne pas bloquer: la session est déjà fermée de façon cohérente.
            log.error("Erreur création EcartCaisse auto pour session #{}: {}",
                    session.getId(), e.getMessage());
        }
    }

    /**
     * Obtenir l'utilisateur connecté (CONTROLEUR) qui effectue la clôture.
     *
     * Règle 3N: Le Caissier ne peut pas valider son propre contrôle.
     * Le RBAC est appliqué au niveau Controller (@PreAuthorize).
     * Cette méthode récupère juste l'utilisateur pour l'audit trail (fermePar).
     *
     * Pattern identique à EcartCaisseServiceImpl.getCurrentUtilisateur().
     * Retourne null si le principal Spring Security n'est pas un Utilisateur.
     *
     * @return Utilisateur authentifié, ou null si non résolu
     */
    private Utilisateur getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Utilisateur utilisateur) {
            if (utilisateur.getId() != null) {
                return utilisateurRepository.findByIdWithValidationContext(utilisateur.getId()).orElse(utilisateur);
            }
            if (utilisateur.getUsername() != null && !utilisateur.getUsername().isBlank()) {
                return utilisateurRepository.findByUsernameWithValidationContext(utilisateur.getUsername()).orElse(utilisateur);
            }
            return utilisateur;
        }
        log.warn("Clôture session: utilisateur non résolu depuis SecurityContext — fermePar sera null.");
        return null;
    }

    private String buildMotifAvecSupplance(Utilisateur utilisateur, String motifBase) {
        if (isSupplanceCaissier(utilisateur)) {
            return motifBase + " | SUPPLEANCE_CAISSIER";
        }
        return motifBase;
    }

    private boolean isSupplanceCaissier(Utilisateur utilisateur) {
        return utilisateur != null
                && utilisateur.getRole() != null
                && utilisateur.getRole().getCode() == RoleCode.CHEF_BUREAU;
    }

    private boolean hasAuthority(String authority) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null
                && auth.getAuthorities() != null
                && auth.getAuthorities().stream().anyMatch(grantedAuthority -> authority.equals(grantedAuthority.getAuthority()));
    }
}