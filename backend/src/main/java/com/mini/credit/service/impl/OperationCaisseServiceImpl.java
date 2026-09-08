package com.mini.credit.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini.credit.dto.caisse.OperationCaisseRequest;
import com.mini.credit.dto.caisse.OperationCaisseResponse;
import com.mini.credit.dto.caisse.JournalCaisseFilterRequest;
import com.mini.credit.dto.caisse.JournalCaisseResponse;
import com.mini.credit.dto.caisse.RequalificationNatureFinancementRequest;
import com.mini.credit.entity.caisse.Caisse;
import com.mini.credit.entity.caisse.OperationCaisse;
import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.entity.credit.Credit;
import com.mini.credit.entity.credit.RemboursementCredit;
import com.mini.credit.entity.epargne.OperationEpargne;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.AgentTerrain;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.SourceOperationCaisse;
import com.mini.credit.enums.StatutMembre;
import com.mini.credit.enums.StatutSessionCaisse;
import com.mini.credit.enums.NatureFinancementApprovisionnement;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.enums.TypeEvenementAuditOperation;
import com.mini.credit.enums.TypeOperationCaisse;
import com.mini.credit.enums.CategorieOperationCaisse;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.exception.ResourceNotFoundException;
import com.mini.credit.mapper.CashMapper;
import com.mini.credit.repository.agentTerrain.AgentTerrainRepository;
import com.mini.credit.repository.caisse.CaisseRepository;
import com.mini.credit.repository.caisse.OperationCaisseRepository;
import com.mini.credit.repository.caisse.SessionCaisseRepository;
import com.mini.credit.repository.credit.CreditRepository;
import com.mini.credit.repository.credit.RemboursementCreditRepository;
import com.mini.credit.repository.epargne.OperationEpargneRepository;
import com.mini.credit.repository.membre.MembreRepository;
import com.mini.credit.repository.utilisateur.UtilisateurRepository;
import com.mini.credit.service.OperationCaisseService;
import com.mini.credit.service.ReferenceGeneratorService;
import com.mini.credit.service.SessionCaisseValidationService;
import com.mini.credit.service.audit.AuditService;
import com.mini.credit.service.audit.Auditable;
import com.mini.credit.enums.security.AuditAction;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class OperationCaisseServiceImpl implements OperationCaisseService {

    private static final String DEVISE_PAR_DEFAUT = "CDF";
    private final OperationCaisseRepository operationCaisseRepository;
    private final SessionCaisseRepository sessionCaisseRepository;
    private final CaisseRepository caisseRepository;
    private final MembreRepository membreRepository;
    private final CreditRepository creditRepository;
    private final RemboursementCreditRepository remboursementCreditRepository;
    private final OperationEpargneRepository operationEpargneRepository;
    private final AgentTerrainRepository agentTerrainRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ReferenceGeneratorService referenceGeneratorService;
    private final CashMapper cashMapper;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final SessionCaisseValidationService sessionCaisseValidationService;

    @Override
    @Auditable(action = AuditAction.OPERATION_CAISSE_CREATED, entityType = "OperationCaisse")
    public OperationCaisseResponse enregistrer(OperationCaisseRequest request) {
        validerRequest(request);
        validerOperationLibreAutorisee(request);

        return enregistrerOperationValidee(request);
    }

    @Override
    @Auditable(action = AuditAction.OPERATION_CAISSE_CREATED, entityType = "OperationCaisse")
    public OperationCaisseResponse enregistrerDepuisCollecteValidee(OperationCaisseRequest request) {
        validerRequest(request);
        validerOperationCollecteValidee(request);

        return enregistrerOperationValidee(request);
    }

    private OperationCaisseResponse enregistrerOperationValidee(OperationCaisseRequest request) {

        SessionCaisse session = sessionCaisseRepository.findById(request.getSessionCaisseId())
                .orElseThrow(() -> new ResourceNotFoundException("Session caisse introuvable"));

        validerSession(session);

        Caisse caisse = caisseRepository.findById(request.getCaisseId())
                .orElseThrow(() -> new ResourceNotFoundException("Caisse introuvable"));

        validerCaisse(caisse);
        verifierCoherenceSessionEtCaisse(session, caisse);

        Membre membre = request.getMembreId() != null
                ? membreRepository.findById(request.getMembreId())
                .orElseThrow(() -> new ResourceNotFoundException("Membre introuvable"))
                : null;

        if (membre != null && membre.getStatut() != StatutMembre.ACTIF) {
            throw new BusinessException(
                    "Impossible d'enregistrer une opération de caisse pour un membre non actif (statut : " + membre.getStatut() + ")"
            );
        }

        Credit credit = request.getCreditId() != null
                ? creditRepository.findById(request.getCreditId())
                .orElseThrow(() -> new ResourceNotFoundException("Crédit introuvable"))
                : null;

        RemboursementCredit remboursement = request.getRemboursementId() != null
                ? remboursementCreditRepository.findById(request.getRemboursementId())
                .orElseThrow(() -> new ResourceNotFoundException("Remboursement introuvable"))
                : null;

        OperationEpargne operationEpargne = request.getOperationEpargneId() != null
                ? operationEpargneRepository.findById(request.getOperationEpargneId())
                .orElseThrow(() -> new ResourceNotFoundException("Opération épargne introuvable"))
                : null;

        AgentTerrain agent = request.getAgentId() != null
                ? agentTerrainRepository.findById(request.getAgentId())
                .orElseThrow(() -> new ResourceNotFoundException("Agent introuvable"))
                : null;

        Utilisateur createdBy = resolveOperationAuthor(request.getCreatedBy());

        verifierCoherencesMetier(membre, credit, remboursement, operationEpargne);

        OperationCaisse op = OperationCaisse.builder()
                .numeroPiece(referenceGeneratorService.genererReference("PCS"))
                .sessionCaisse(session)
                .caisse(caisse)
                .dateOperation(request.getDateOperation())
                .typeOperation(request.getTypeOperation())
                .categorieOperation(request.getCategorieOperation())
                .natureFinancement(request.getNatureFinancement())
                .montant(request.getMontant())
                .devise(normalizeDevise(request.getDevise()))
                .membre(membre)
                .credit(credit)
                .remboursement(remboursement)
                .operationEpargne(operationEpargne)
                .agent(agent)
                .description(cleanNullableText(request.getDescription()))
                .createdBy(createdBy)
                .modePaiement(request.getModePaiement())
                .source(request.getSource())
                .referenceExterne(cleanNullableText(request.getReferenceExterne()))
                .referenceMetier(resolveReferenceMetier(request, credit, remboursement, operationEpargne))
                .observation(cleanNullableText(request.getObservation()))
                .commentaire(resolveCommentaire(request))
                .recetteId(request.getRecetteId())
                .depenseCaisseId(request.getDepenseCaisseId())
                .retraitEpargneId(request.getRetraitEpargneId())
                .utilisateur(resolveUtilisateurJournal(request, createdBy))
                .roleUtilisateur(resolveRoleUtilisateur(createdBy))
                .site(resolveSiteJournal(caisse, session))
                .build();

            op.setSoldeApresOperation(calculerSoldeApresOperation(session, request.getTypeOperation(), request.getMontant()));

        op = operationCaisseRepository.save(op);

        mettreAJourSession(session, request.getTypeOperation(), request.getMontant());
        sessionCaisseRepository.save(session);

        auditerOperationP2(request, op, session);

        return cashMapper.toResponse(op);
    }

    @Override
    public OperationCaisseResponse requalifierNatureFinancement(Long operationId, RequalificationNatureFinancementRequest request) {
        if (operationId == null) {
            throw new BusinessException("L'identifiant de l'opération est obligatoire");
        }
        if (request == null || request.getNatureFinancement() == null) {
            throw new BusinessException("La nature du financement est obligatoire");
        }

        String commentaireCorrection = cleanNullableText(request.getCommentaireCorrection());
        String referenceCorrection = cleanNullableText(request.getReferenceCorrection());
        if (commentaireCorrection == null) {
            throw new BusinessException("Le commentaire de correction est obligatoire");
        }
        if ((request.getNatureFinancement() == NatureFinancementApprovisionnement.PRET_RECU
                || request.getNatureFinancement() == NatureFinancementApprovisionnement.AUTRE_FINANCEMENT)
                && commentaireCorrection == null
                && referenceCorrection == null) {
            throw new BusinessException("Un commentaire ou une référence est obligatoire pour cette nature de financement");
        }

        OperationCaisse operation = operationCaisseRepository.findById(operationId)
                .orElseThrow(() -> new ResourceNotFoundException("Opération caisse introuvable"));
        verifierAccesCaisse(operation.getCaisse());

        if (operation.getCategorieOperation() != CategorieOperationCaisse.APPROVISIONNEMENT) {
            throw new BusinessException("La nature de financement ne peut être requalifiée que pour un approvisionnement caisse");
        }

        NatureFinancementApprovisionnement ancienneNature = operation.getNatureFinancement();
        NatureFinancementApprovisionnement nouvelleNature = request.getNatureFinancement();
        Utilisateur utilisateur = getCurrentUser();

        Map<String, Object> oldValues = snapshotNatureFinancement(operation, ancienneNature, null, utilisateur);
        operation.setNatureFinancement(nouvelleNature);
        operation.setCommentaire(appendCorrectionComment(operation.getCommentaire(), ancienneNature, nouvelleNature, commentaireCorrection, referenceCorrection, utilisateur));
        OperationCaisse saved = operationCaisseRepository.save(operation);

        auditService.logWithValues(
                AuditAction.MODIFICATION_OPERATION,
                "OperationCaisse",
                saved.getId(),
                true,
                "Requalification nature financement approvisionnement",
                toJson(oldValues),
                toJson(snapshotNatureFinancement(saved, nouvelleNature, commentaireCorrection, utilisateur)),
                null
        );

        return cashMapper.toResponse(saved);
    }

    @Override
    public List<OperationCaisseResponse> getBySession(Long sessionId) {
        SessionCaisse session = sessionCaisseRepository.findById(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Session caisse introuvable"));
        verifierAccesSession(session);

        return operationCaisseRepository.findBySessionCaisseIdOrderByDateOperationDesc(sessionId).stream()
                .map(cashMapper::toResponse)
                .toList();
    }

    @Override
    public List<OperationCaisseResponse> getByCaisse(Long caisseId) {
        Caisse caisse = caisseRepository.findById(caisseId)
            .orElseThrow(() -> new ResourceNotFoundException("Caisse introuvable"));
        verifierAccesCaisse(caisse);

        return operationCaisseRepository.findByCaisseIdOrderByDateOperationDesc(caisseId).stream()
                .map(cashMapper::toResponse)
                .toList();
    }

    @Override
    public List<OperationCaisseResponse> getAll() {
        Long agenceId = resolveCurrentUserAgenceId(getCurrentUser());
        if (agenceId != null && getCurrentUser() != null && getCurrentUser().getRole() != null
            && getCurrentUser().getRole().getCode() == RoleCode.CAISSIER) {
            return operationCaisseRepository.findAllByAgenceId(agenceId, Pageable.unpaged())
                .getContent()
                .stream()
                .sorted((a, b) -> b.getDateOperation().compareTo(a.getDateOperation()))
                .map(cashMapper::toResponse)
                .toList();
        }

        return operationCaisseRepository.findAll().stream()
            .sorted((a, b) -> b.getDateOperation().compareTo(a.getDateOperation()))
            .map(cashMapper::toResponse)
            .toList();
    }

    @Override
    public Page<OperationCaisseResponse> getAll(Pageable pageable) {
        Long agenceId = resolveCurrentUserAgenceId(getCurrentUser());
        return operationCaisseRepository.findAllByAgenceId(agenceId, pageable).map(cashMapper::toResponse);
    }

    @Override
    public Page<JournalCaisseResponse> getJournal(JournalCaisseFilterRequest filter, Pageable pageable) {
        JournalCaisseFilterRequest safeFilter = filter != null ? filter : new JournalCaisseFilterRequest();
        Long agenceId = resolveCurrentUserAgenceId(getCurrentUser());

        if (agenceId != null && getCurrentUser() != null && getCurrentUser().getRole() != null
                && getCurrentUser().getRole().getCode() == RoleCode.CAISSIER) {
            if (safeFilter.getCaisseId() != null) {
                Caisse caisse = caisseRepository.findById(safeFilter.getCaisseId())
                        .orElseThrow(() -> new ResourceNotFoundException("Caisse introuvable"));
                verifierAccesCaisse(caisse);
            }
            if (safeFilter.getSessionCaisseId() != null) {
                SessionCaisse session = sessionCaisseRepository.findById(safeFilter.getSessionCaisseId())
                        .orElseThrow(() -> new ResourceNotFoundException("Session caisse introuvable"));
                verifierAccesSession(session);
            }
        }

        return operationCaisseRepository.findJournal(
                safeFilter.getSessionCaisseId(),
                safeFilter.getCaisseId(),
                agenceId,
                safeFilter.getSiteId(),
                safeFilter.getUtilisateurId(),
                safeFilter.getTypeOperation(),
                safeFilter.getCategorie(),
                safeFilter.getSource(),
                safeFilter.getDateDebut(),
                safeFilter.getDateFin(),
                safeFilter.getReferenceMetier(),
                safeFilter.getRecetteId(),
                safeFilter.getDepenseCaisseId(),
                safeFilter.getCreditId(),
                safeFilter.getRetraitEpargneId(),
                safeFilter.getOperationEpargneId(),
                pageable
        ).map(cashMapper::toJournalResponse);
    }

    private void validerRequest(OperationCaisseRequest request) {
        if (request == null) {
            throw new BusinessException("La requête d'opération de caisse est obligatoire");
        }

        if (request.getSessionCaisseId() == null) {
            throw new BusinessException("La session de caisse est obligatoire");
        }

        if (request.getCaisseId() == null) {
            throw new BusinessException("La caisse est obligatoire");
        }

        if (request.getDateOperation() == null) {
            throw new BusinessException("La date de l'opération est obligatoire");
        }

        if (request.getTypeOperation() == null) {
            throw new BusinessException("Le type d'opération est obligatoire");
        }

        if (request.getCategorieOperation() == null) {
            throw new BusinessException("La catégorie d'opération est obligatoire");
        }

        if (request.getMontant() == null || request.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Le montant doit être supérieur à zéro");
        }

        // PATCH 4 — Validation source + tracabilite recetteId
        if (request.getSource() == null) {
            throw new BusinessException("La source de l'operation est obligatoire");
        }
        if (request.getSource() == SourceOperationCaisse.RECETTE_JOURNALIERE
                && request.getRecetteId() == null) {
            throw new BusinessException(
                    "recetteId est obligatoire pour les operations de source RECETTE_JOURNALIERE");
        }

        if (request.getSource() == SourceOperationCaisse.AJUSTEMENT
            && cleanNullableText(request.getObservation()) == null) {
            throw new BusinessException("Une observation est obligatoire pour une opération de type AJUSTEMENT");
        }

        validerNatureFinancementApprovisionnement(request);

        TypeEvenementAuditOperation typeEvenementAudit = request.getTypeEvenementAudit();
        if (typeEvenementAudit != null
                && request.getSource() != SourceOperationCaisse.AJUSTEMENT
                && typeEvenementAudit != TypeEvenementAuditOperation.CREATION_OPERATION) {
            throw new BusinessException(
                    "Les événements d'audit MODIFICATION_OPERATION/ANNULATION_OPERATION sont réservés aux opérations AJUSTEMENT"
            );
        }

        // Validation de cohérence entre type et catégorie
        validerCoherenceTypeEtCategorie(request.getTypeOperation(), request.getCategorieOperation());
    }

    private void validerNatureFinancementApprovisionnement(OperationCaisseRequest request) {
        if (request.getCategorieOperation() != CategorieOperationCaisse.APPROVISIONNEMENT) {
            return;
        }

        if (request.getNatureFinancement() == null) {
            throw new BusinessException("La nature du financement est obligatoire pour un approvisionnement caisse");
        }

        if (request.getNatureFinancement() == NatureFinancementApprovisionnement.AUTRE_FINANCEMENT
                && cleanNullableText(request.getObservation()) == null
                && cleanNullableText(request.getCommentaire()) == null) {
            throw new BusinessException("Un commentaire est obligatoire pour un autre financement");
        }

        if (request.getNatureFinancement() == NatureFinancementApprovisionnement.PRET_RECU
                && cleanNullableText(request.getObservation()) == null
                && cleanNullableText(request.getCommentaire()) == null
                && cleanNullableText(request.getReferenceExterne()) == null
                && cleanNullableText(request.getReferenceMetier()) == null) {
            throw new BusinessException("Un commentaire ou une référence est obligatoire pour un prêt reçu par l'institution");
        }
    }

    private void validerOperationLibreAutorisee(OperationCaisseRequest request) {
        if (!isSourceOperationLibre(request.getSource()) && !isCategorieOperationLibre(request.getCategorieOperation())) {
            return;
        }

        if (isPaiementFraisDemandeCredit(request)) {
            return;
        }

        String motif = cleanNullableText(request.getDescription()) != null
                ? cleanNullableText(request.getDescription())
                : cleanNullableText(request.getCommentaire()) != null
                    ? cleanNullableText(request.getCommentaire())
                    : cleanNullableText(request.getObservation());
        if (motif == null) {
            throw new BusinessException("Un motif/commentaire est obligatoire pour une entrée/sortie libre de caisse");
        }

        Utilisateur currentUser = getCurrentUser();
        RoleCode role = currentUser != null && currentUser.getRole() != null ? currentUser.getRole().getCode() : null;
        if (role == null) {
            throw new BusinessException("Utilisateur authentifié introuvable pour une entrée/sortie libre de caisse");
        }

        if (role == RoleCode.CAISSIER) {
            throw new BusinessException("Entrée/sortie libre interdite au CAISSIER: utilisez un workflow autorisé (retrait validé, dépense autorisée, décaissement crédit, billetage ou recette terrain)");
        }

        if (role != RoleCode.ADMIN && role != RoleCode.CHEF_BUREAU) {
            throw new BusinessException("Entrée/sortie libre réservée à ADMIN ou CHEF_BUREAU");
        }
    }

    private boolean isSourceOperationLibre(SourceOperationCaisse source) {
        return source == SourceOperationCaisse.MANUEL
                || source == SourceOperationCaisse.AJUSTEMENT
                || source == SourceOperationCaisse.APPROVISIONNEMENT
                || source == SourceOperationCaisse.AUTRE;
    }

    private boolean isCategorieOperationLibre(CategorieOperationCaisse categorie) {
        return categorie == CategorieOperationCaisse.ENTREE_DIVERSE
                || categorie == CategorieOperationCaisse.SORTIE_DIVERSE
                || categorie == CategorieOperationCaisse.APPROVISIONNEMENT;
    }

    private boolean isPaiementFraisDemandeCredit(OperationCaisseRequest request) {
        return request.getTypeOperation() == TypeOperationCaisse.ENTREE
                && request.getCategorieOperation() == CategorieOperationCaisse.FRAIS_DEMANDE_CREDIT
                && cleanNullableText(request.getReferenceMetier()) != null
                && cleanNullableText(request.getReferenceMetier()).startsWith("DEMANDE_CREDIT:")
                && request.getMembreId() != null;
    }

    private void validerOperationCollecteValidee(OperationCaisseRequest request) {
        if (request.getSource() != SourceOperationCaisse.RECETTE_JOURNALIERE) {
            throw new BusinessException("Source opération caisse invalide pour une collecte validée");
        }
        if (request.getTypeOperation() != TypeOperationCaisse.ENTREE) {
            throw new BusinessException("Une collecte validée ne peut générer qu'une entrée caisse");
        }
        if (request.getRecetteId() == null) {
            throw new BusinessException("recetteId obligatoire pour une opération générée depuis collecte validée");
        }
        String referenceMetier = cleanNullableText(request.getReferenceMetier());
        if (referenceMetier == null || !(referenceMetier.startsWith("RECETTE-") || referenceMetier.startsWith("CARNET-"))) {
            throw new BusinessException("Référence métier collecte obligatoire pour une opération générée automatiquement");
        }
    }

    private void validerSession(SessionCaisse session) {
        try {
            sessionCaisseValidationService.validateSessionForOperation(session);
        } catch (BusinessException exception) {
            auditService.logWithValues(
                    AuditAction.REFUS_TRANSITION,
                    "SessionCaisse",
                    session.getId(),
                    false,
                    exception.getMessage(),
                    toJson(snapshotSession(session, session.getStatut(), exception.getMessage())),
                    toJson(snapshotSession(session, StatutSessionCaisse.OUVERTE, exception.getMessage())),
                    exception.getMessage()
            );

            if (session.getStatut() == StatutSessionCaisse.CLOTUREE) {
                auditService.logFailure(
                        AuditAction.SUPPRESSION_REFUSEE,
                        "SessionCaisse",
                        session.getId(),
                        "Ajout d'opération refusé sur session clôturée",
                        exception.getMessage()
                );
            }

            throw exception;
        }
    }

    private void validerCaisse(Caisse caisse) {
        if (Boolean.FALSE.equals(caisse.getActif())) {
            throw new BusinessException("La caisse est inactive");
        }

        verifierAccesCaisse(caisse);
    }

    private void verifierCoherenceSessionEtCaisse(SessionCaisse session, Caisse caisse) {
        if (!session.getCaisse().getId().equals(caisse.getId())) {
            throw new BusinessException("La session de caisse n'appartient pas à la caisse fournie");
        }
    }

    private void verifierAccesSession(SessionCaisse session) {
        if (session == null || session.getCaisse() == null) {
            throw new ResourceNotFoundException("Session caisse introuvable");
        }

        verifierAccesCaisse(session.getCaisse());
    }

    private void verifierAccesCaisse(Caisse caisse) {
        Utilisateur currentUser = getCurrentUser();
        if (currentUser == null || currentUser.getRole() == null) {
            throw new BusinessException("Utilisateur authentifié introuvable");
        }

        RoleCode role = currentUser.getRole().getCode();
        if (role == RoleCode.ADMIN || role == RoleCode.CONTROLEUR || role == RoleCode.CHEF_BUREAU || role == RoleCode.RCI) {
            return;
        }

        if (role != RoleCode.CAISSIER) {
            throw new BusinessException("Accès caisse refusé pour ce rôle");
        }

        Long userAgenceId = resolveCurrentUserAgenceId(currentUser);
        Long caisseAgenceId = caisse.getAgence() != null ? caisse.getAgence().getId() : null;
        boolean agenceCompatible = userAgenceId != null && caisseAgenceId != null && Objects.equals(userAgenceId, caisseAgenceId);
        boolean affectationExplicite = currentUser.getId() != null
                && caisse.getCaissierResponsable() != null
                && caisse.getCaissierResponsable().getId() != null
                && Objects.equals(currentUser.getId(), caisse.getCaissierResponsable().getId());

        if (!agenceCompatible && !affectationExplicite) {
            throw new BusinessException("Cette caisse n'est pas accessible à ce caissier");
        }
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

    private void validerCoherenceTypeEtCategorie(
            TypeOperationCaisse type,
            CategorieOperationCaisse categorie
    ) {
        boolean estEntree = (type == TypeOperationCaisse.ENTREE);

        // Catégories d'ENTRÉE
        if (estEntree) {
            if (!isCategoriEntree(categorie)) {
                throw new BusinessException(
                    "La catégorie '" + categorie + "' doit être enregistrée avec un type SORTIE, pas ENTREE"
                );
            }
        } else {
            // Type = SORTIE
            if (!isCategorSortie(categorie)) {
                throw new BusinessException(
                    "La catégorie '" + categorie + "' doit être enregistrée avec un type ENTREE, pas SORTIE"
                );
            }
        }
    }

    private boolean isCategoriEntree(CategorieOperationCaisse categorie) {
        return switch (categorie) {
            case COTISATION, EPARGNE, FRAIS_DEMANDE, FRAIS_DEMANDE_CREDIT, FRAIS_RETRAIT_EPARGNE,
                 DEPOT_GARANTIE, DEPOT_GARANTIE_CREDIT, REMBOURSEMENT_CREDIT,
                 PENALITE_RETARD, APPROVISIONNEMENT, ENTREE_DIVERSE -> true;
            default -> false;
        };
    }

    private boolean isCategorSortie(CategorieOperationCaisse categorie) {
        return switch (categorie) {
            case RETRAIT_EPARGNE, DECAISSEMENT_CREDIT, SORTIE_DIVERSE, DEPENSE, REMBOURSEMENT_APPORT_PROPRIETAIRE -> true;
            default -> false;
        };
    }

    private void verifierCoherencesMetier(
            Membre membre,
            Credit credit,
            RemboursementCredit remboursement,
            OperationEpargne operationEpargne
    ) {
        // Protection double comptage épargne/caisse
        if (operationEpargne != null) {
            long countExistantes = operationCaisseRepository.countByOperationEpargneId(operationEpargne.getId());
            if (countExistantes > 0) {
                throw new BusinessException(
                    "Une opération de caisse existe déjà pour cette opération épargne. Double comptage détecté."
                );
            }
        }

        if (remboursement != null && credit != null) {
            if (remboursement.getCredit() == null || !remboursement.getCredit().getId().equals(credit.getId())) {
                throw new BusinessException("Le remboursement ne correspond pas au crédit fourni");
            }
        }

        if (credit != null && membre != null) {
            if (credit.getMembre() == null || !credit.getMembre().getId().equals(membre.getId())) {
                throw new BusinessException("Le crédit ne correspond pas au membre fourni");
            }
        }

        if (remboursement != null && membre != null) {
            if (remboursement.getMembre() == null || !remboursement.getMembre().getId().equals(membre.getId())) {
                throw new BusinessException("Le remboursement ne correspond pas au membre fourni");
            }
        }

        if (operationEpargne != null && membre != null) {
            if (operationEpargne.getMembre() == null || !operationEpargne.getMembre().getId().equals(membre.getId())) {
                throw new BusinessException("L'opération épargne ne correspond pas au membre fourni");
            }
        }
    }

    private void mettreAJourSession(SessionCaisse session, TypeOperationCaisse type, BigDecimal montant) {
        if (type == TypeOperationCaisse.ENTREE) {
            session.setTotalEntrees(session.getTotalEntrees().add(montant));
        } else {
            session.setTotalSorties(session.getTotalSorties().add(montant));
        }

        session.setSoldeTheorique(
                session.getSoldeOuverture()
                        .add(session.getTotalEntrees())
                        .subtract(session.getTotalSorties())
        );
    }

    private BigDecimal calculerSoldeApresOperation(SessionCaisse session, TypeOperationCaisse type, BigDecimal montant) {
        BigDecimal soldeCourant = session.getSoldeOuverture()
                .add(session.getTotalEntrees())
                .subtract(session.getTotalSorties());

        if (type == TypeOperationCaisse.ENTREE) {
            return soldeCourant.add(montant);
        }
        return soldeCourant.subtract(montant);
    }

    private String resolveReferenceMetier(OperationCaisseRequest request,
                                          Credit credit,
                                          RemboursementCredit remboursement,
                                          OperationEpargne operationEpargne) {
        String referenceMetier = cleanNullableText(request.getReferenceMetier());
        if (referenceMetier != null) {
            return referenceMetier;
        }

        if (request.getSource() == SourceOperationCaisse.RECETTE_JOURNALIERE && request.getRecetteId() != null) {
            return "RECETTE-" + request.getRecetteId();
        }
        if (request.getSource() == SourceOperationCaisse.DEPENSE_CAISSE && request.getDepenseCaisseId() != null) {
            return "DEPENSE-" + request.getDepenseCaisseId();
        }
        if (request.getSource() == SourceOperationCaisse.RETRAIT_EPARGNE) {
            if (request.getRetraitEpargneId() != null) {
                return "RETRAIT-" + request.getRetraitEpargneId();
            }
            if (operationEpargne != null) {
                return "OP_EPARGNE-" + operationEpargne.getId();
            }
        }
        if (request.getSource() == SourceOperationCaisse.CREDIT_DECAISSEMENT && credit != null) {
            return credit.getNumeroCredit();
        }
        if (request.getSource() == SourceOperationCaisse.CREDIT_REMBOURSEMENT) {
            if (remboursement != null) {
                return remboursement.getNumeroRecu();
            }
            if (credit != null) {
                return credit.getNumeroCredit();
            }
        }

        return cleanNullableText(request.getReferenceExterne());
    }

    private String resolveCommentaire(OperationCaisseRequest request) {
        String commentaire = cleanNullableText(request.getCommentaire());
        if (commentaire != null) {
            return commentaire;
        }
        String observation = cleanNullableText(request.getObservation());
        if (observation != null) {
            return observation;
        }
        return cleanNullableText(request.getDescription());
    }

    private Utilisateur resolveUtilisateurJournal(OperationCaisseRequest request, Utilisateur createdBy) {
        if (request.getUtilisateurId() != null) {
            return utilisateurRepository.findById(request.getUtilisateurId()).orElse(createdBy);
        }
        return createdBy;
    }

    private com.mini.credit.enums.security.RoleCode resolveRoleUtilisateur(Utilisateur utilisateur) {
        if (utilisateur != null && utilisateur.getRole() != null) {
            return utilisateur.getRole().getCode();
        }
        return null;
    }

    private Site resolveSiteJournal(Caisse caisse, SessionCaisse session) {
        if (session != null && session.getCaisse() != null && session.getCaisse().getSite() != null) {
            return session.getCaisse().getSite();
        }
        if (caisse != null) {
            return caisse.getSite();
        }
        return null;
    }

    private String normalizeDevise(String devise) {
        if (devise == null || devise.isBlank()) {
            return DEVISE_PAR_DEFAUT;
        }
        return devise.trim().toUpperCase();
    }

    private String cleanNullableText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void auditerOperationP2(OperationCaisseRequest request, OperationCaisse operation, SessionCaisse session) {
        TypeEvenementAuditOperation typeEvenement = resolveTypeEvenementAudit(request);
        if (typeEvenement == TypeEvenementAuditOperation.CREATION_OPERATION) {
            return;
        }

        String observation = cleanNullableText(request.getObservation());
        String description = cleanNullableText(request.getDescription());
        String motif = description != null ? description : "Ajustement opération caisse";
        AuditAction action = typeEvenement == TypeEvenementAuditOperation.ANNULATION_OPERATION
                ? AuditAction.ANNULATION_OPERATION
                : AuditAction.MODIFICATION_OPERATION;

        auditService.logWithValues(
                action,
                "OperationCaisse",
                operation.getId(),
                true,
                motif,
                toJson(snapshotSession(session, session.getStatut(), observation)),
                toJson(snapshotOperation(operation, observation, motif)),
                null
        );
    }

    private TypeEvenementAuditOperation resolveTypeEvenementAudit(OperationCaisseRequest request) {
        if (request.getTypeEvenementAudit() != null) {
            return request.getTypeEvenementAudit();
        }

        if (request.getSource() == SourceOperationCaisse.AJUSTEMENT) {
            return TypeEvenementAuditOperation.MODIFICATION_OPERATION;
        }

        return TypeEvenementAuditOperation.CREATION_OPERATION;
    }

    private Map<String, Object> snapshotSession(SessionCaisse session, StatutSessionCaisse statut, String motif) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sessionId", session.getId());
        payload.put("siteId", session.getCaisse() != null && session.getCaisse().getSite() != null
                ? session.getCaisse().getSite().getId()
                : null);
        payload.put("statut", statut);
        payload.put("motif", motif);
        payload.put("horodatage", LocalDateTime.now());
        return payload;
    }

    private Map<String, Object> snapshotOperation(OperationCaisse operation, String observation, String motif) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sessionId", operation.getSessionCaisse() != null ? operation.getSessionCaisse().getId() : null);
        payload.put("siteId", operation.getCaisse() != null && operation.getCaisse().getSite() != null
                ? operation.getCaisse().getSite().getId()
                : null);
        payload.put("operationId", operation.getId());
        payload.put("observation", observation);
        payload.put("motif", motif);
        payload.put("horodatage", LocalDateTime.now());
        return payload;
    }

    private Map<String, Object> snapshotNatureFinancement(
            OperationCaisse operation,
            NatureFinancementApprovisionnement nature,
            String commentaireCorrection,
            Utilisateur utilisateur
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("operationId", operation.getId());
        payload.put("numeroPiece", operation.getNumeroPiece());
        payload.put("categorie", operation.getCategorieOperation());
        payload.put("natureFinancement", nature);
        payload.put("montant", operation.getMontant());
        payload.put("dateOperation", operation.getDateOperation());
        payload.put("sessionId", operation.getSessionCaisse() != null ? operation.getSessionCaisse().getId() : null);
        payload.put("caisseId", operation.getCaisse() != null ? operation.getCaisse().getId() : null);
        payload.put("antenneId", operation.getCaisse() != null && operation.getCaisse().getAgence() != null ? operation.getCaisse().getAgence().getId() : null);
        payload.put("utilisateurId", utilisateur != null ? utilisateur.getId() : null);
        payload.put("utilisateur", utilisateur != null ? utilisateur.getNomComplet() : null);
        payload.put("commentaireCorrection", commentaireCorrection);
        payload.put("horodatageCorrection", LocalDateTime.now());
        return payload;
    }

    private String appendCorrectionComment(
            String currentComment,
            NatureFinancementApprovisionnement ancienneNature,
            NatureFinancementApprovisionnement nouvelleNature,
            String commentaireCorrection,
            String referenceCorrection,
            Utilisateur utilisateur
    ) {
        StringBuilder correction = new StringBuilder();
        correction.append("[REQUALIFICATION_FINANCEMENT ")
                .append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .append("] ")
                .append(ancienneNature != null ? ancienneNature : "NON_QUALIFIE")
                .append(" -> ")
                .append(nouvelleNature)
                .append("; par=")
                .append(utilisateur != null ? utilisateur.getNomComplet() : "Utilisateur inconnu")
                .append("; commentaire=")
                .append(commentaireCorrection);
        if (referenceCorrection != null) {
            correction.append("; reference=").append(referenceCorrection);
        }

        String cleanedCurrent = cleanNullableText(currentComment);
        return cleanedCurrent == null ? correction.toString() : cleanedCurrent + "\n" + correction;
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private Utilisateur resolveOperationAuthor(Long requestCreatedById) {
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

        if (requestCreatedById != null) {
            return utilisateurRepository.findById(requestCreatedById)
                    .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        }

        return null;
    }

    private Utilisateur getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }

        if (authentication.getPrincipal() instanceof Utilisateur utilisateur) {
            if (utilisateur.getId() != null) {
                return utilisateurRepository.findByIdWithValidationContext(utilisateur.getId()).orElse(utilisateur);
            }
            if (utilisateur.getUsername() != null && !utilisateur.getUsername().isBlank()) {
                return utilisateurRepository.findByUsernameWithValidationContext(utilisateur.getUsername()).orElse(utilisateur);
            }
            return utilisateur;
        }

        if (authentication.getName() != null && !authentication.getName().isBlank()) {
            return utilisateurRepository.findByUsernameWithValidationContext(authentication.getName()).orElse(null);
        }

        return null;
    }

}