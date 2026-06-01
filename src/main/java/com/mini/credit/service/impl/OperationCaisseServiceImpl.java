package com.mini.credit.service.impl;

import com.mini.credit.dto.caisse.OperationCaisseRequest;
import com.mini.credit.dto.caisse.OperationCaisseResponse;
import com.mini.credit.entity.caisse.Caisse;
import com.mini.credit.entity.caisse.OperationCaisse;
import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.entity.credit.Credit;
import com.mini.credit.entity.credit.RemboursementCredit;
import com.mini.credit.entity.epargne.OperationEpargne;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.AgentTerrain;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.StatutMembre;
import com.mini.credit.enums.StatutSessionCaisse;
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
import com.mini.credit.service.audit.Auditable;
import com.mini.credit.enums.security.AuditAction;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

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

    @Override
    @Auditable(action = AuditAction.OPERATION_CAISSE_CREATED, entityType = "OperationCaisse")
    public OperationCaisseResponse enregistrer(OperationCaisseRequest request) {
        validerRequest(request);

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

        Utilisateur createdBy = request.getCreatedBy() != null
                ? utilisateurRepository.findById(request.getCreatedBy())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"))
                : null;

        verifierCoherencesMetier(membre, credit, remboursement, operationEpargne);

        OperationCaisse op = OperationCaisse.builder()
                .numeroPiece(referenceGeneratorService.genererReference("PCS"))
                .sessionCaisse(session)
                .caisse(caisse)
                .dateOperation(request.getDateOperation())
                .typeOperation(request.getTypeOperation())
                .categorieOperation(request.getCategorieOperation())
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
                .build();

        op = operationCaisseRepository.save(op);

        mettreAJourSession(session, request.getTypeOperation(), request.getMontant());
        sessionCaisseRepository.save(session);

        return cashMapper.toResponse(op);
    }

    @Override
    public List<OperationCaisseResponse> getBySession(Long sessionId) {
        return operationCaisseRepository.findBySessionCaisseIdOrderByDateOperationDesc(sessionId).stream()
                .map(cashMapper::toResponse)
                .toList();
    }

    @Override
    public List<OperationCaisseResponse> getByCaisse(Long caisseId) {
        return operationCaisseRepository.findByCaisseIdOrderByDateOperationDesc(caisseId).stream()
                .map(cashMapper::toResponse)
                .toList();
    }

    @Override
    public List<OperationCaisseResponse> getAll() {
        return operationCaisseRepository.findAll().stream()
                .sorted((a, b) -> b.getDateOperation().compareTo(a.getDateOperation()))
                .map(cashMapper::toResponse)
                .toList();
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

        // Validation de cohérence entre type et catégorie
        validerCoherenceTypeEtCategorie(request.getTypeOperation(), request.getCategorieOperation());
    }

    private void validerSession(SessionCaisse session) {
        if (session.getStatut() == null) {
            throw new BusinessException("Le statut de la session de caisse est invalide");
        }

        if (session.getStatut() != StatutSessionCaisse.OUVERTE) {
            throw new BusinessException("La session de caisse n'est pas ouverte");
        }

        if (session.getDateCloture() != null) {
            throw new BusinessException("La session de caisse est déjà clôturée");
        }

        if (session.getCaisse() == null) {
            throw new BusinessException("La caisse liée à la session est introuvable");
        }
    }

    private void validerCaisse(Caisse caisse) {
        if (Boolean.FALSE.equals(caisse.getActif())) {
            throw new BusinessException("La caisse est inactive");
        }
    }

    private void verifierCoherenceSessionEtCaisse(SessionCaisse session, Caisse caisse) {
        if (!session.getCaisse().getId().equals(caisse.getId())) {
            throw new BusinessException("La session de caisse n'appartient pas à la caisse fournie");
        }
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
            case COTISATION, EPARGNE, FRAIS_DEMANDE, FRAIS_DEMANDE_CREDIT,
                 DEPOT_GARANTIE, DEPOT_GARANTIE_CREDIT, REMBOURSEMENT_CREDIT,
                 PENALITE_RETARD, APPROVISIONNEMENT, ENTREE_DIVERSE -> true;
            default -> false;
        };
    }

    private boolean isCategorSortie(CategorieOperationCaisse categorie) {
        return switch (categorie) {
            case RETRAIT_EPARGNE, DECAISSEMENT_CREDIT, SORTIE_DIVERSE -> true;
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

    @Override
    public Page<OperationCaisseResponse> getAll(Pageable pageable) {
        // PHASE 3B: Return paginated list of all caisse operations
        return operationCaisseRepository.findAll(pageable).map(cashMapper::toResponse);
    }
}