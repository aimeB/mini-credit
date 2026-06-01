package com.mini.credit.service.impl;

import com.mini.credit.dto.caisse.OperationCaisseRequest;
import com.mini.credit.dto.document.QuittanceCreateRequest;
import com.mini.credit.dto.epargne.OperationEpargneRequest;
import com.mini.credit.dto.epargne.OperationEpargneResponse;
import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.entity.epargne.CompteEpargne;
import com.mini.credit.entity.epargne.OperationEpargne;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.AgentTerrain;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.CategorieOperationCaisse;
import com.mini.credit.enums.SensOperation;
import com.mini.credit.enums.StatutMembre;
import com.mini.credit.enums.TypeCompteEpargne;
import com.mini.credit.enums.TypeOperationCaisse;
import com.mini.credit.enums.TypeOperationEpargne;
import com.mini.credit.enums.StatutSessionCaisse;
import com.mini.credit.enums.TypeQuittance;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.exception.ResourceNotFoundException;
import com.mini.credit.exception.ResourceNotFoundException;
import com.mini.credit.mapper.SavingMapper;
import com.mini.credit.repository.agentTerrain.AgentTerrainRepository;
import com.mini.credit.repository.caisse.SessionCaisseRepository;
import com.mini.credit.repository.epargne.CompteEpargneRepository;
import com.mini.credit.repository.epargne.OperationEpargneRepository;
import com.mini.credit.repository.membre.MembreRepository;
import com.mini.credit.repository.utilisateur.UtilisateurRepository;
import com.mini.credit.service.OperationCaisseService;
import com.mini.credit.service.OperationEpargneService;
import com.mini.credit.service.QuittanceService;
import com.mini.credit.service.security.ScopeService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OperationEpargneServiceImpl implements OperationEpargneService {

    private static final String DEVISE_PAR_DEFAUT = "CDF";

    private final OperationEpargneRepository operationEpargneRepository;
    private final CompteEpargneRepository compteEpargneRepository;
    private final MembreRepository membreRepository;
    private final AgentTerrainRepository agentTerrainRepository;
    private final SessionCaisseRepository sessionCaisseRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final SavingMapper savingMapper;
    private final OperationCaisseService operationCaisseService;
    private final QuittanceService quittanceService;
    private final ScopeService scopeService;

    @Override
    public OperationEpargneResponse enregistrer(OperationEpargneRequest request) {
        try {

            validerRequestOperation(request);

            // AJOUT PHASE 2: Valider la scope d'accès (AGENT_BUREAU limité à son site)
            if (!scopeService.canRecordEpargneOperation(request.getMembreId())) {
                throw new BusinessException("Accès refusé: vous n'êtes pas autorisé à enregistrer une opération pour ce membre");
            }

            CompteEpargne compte = compteEpargneRepository.findById(request.getCompteEpargneId())
                    .orElseThrow(() -> new ResourceNotFoundException("Compte épargne introuvable"));

            Membre membre = membreRepository.findById(request.getMembreId())
                    .orElseThrow(() -> new ResourceNotFoundException("Membre introuvable"));

            if (!compte.getMembre().getId().equals(membre.getId())) {
                throw new BusinessException("Le compte épargne n'appartient pas à ce membre");
            }

            if (membre.getStatut() != StatutMembre.ACTIF) {
                throw new BusinessException(
                        "Impossible d'enregistrer une opération d'épargne pour un membre non actif (statut : " + membre.getStatut() + ")"
                );
            }

            validerComptePourOperation(compte, request.getTypeOperation());
            validerCompatibiliteTypeCompteEtOperation(compte, request.getTypeOperation());

            AgentTerrain agent = null;
            if (request.getAgentId() != null) {
                agent = agentTerrainRepository.findById(request.getAgentId())
                        .orElseThrow(() -> new ResourceNotFoundException("Agent introuvable"));
            }

            SessionCaisse session = null;
            if (request.getSessionCaisseId() != null) {
                session = sessionCaisseRepository.findById(request.getSessionCaisseId())
                        .orElseThrow(() -> new ResourceNotFoundException("Session caisse introuvable"));
            }

            Utilisateur createdBy = null;
            if (request.getCreatedBy() != null) {
                createdBy = utilisateurRepository.findById(request.getCreatedBy())
                        .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
            }

            SensOperation sens = resolveSens(request);

            if (caisseObligatoire(request.getTypeOperation()) && session == null) {
                throw new BusinessException("Une session de caisse est obligatoire pour ce type d'opération");
            }

            if (session != null) {
                validerSessionCaisse(session);
            }

            validerSoldes(compte, request.getTypeOperation(), sens, request.getMontant());

            appliquerMouvementSolde(compte, request.getTypeOperation(), sens, request.getMontant());

            OperationEpargne op = OperationEpargne.builder()
                    .compteEpargne(compte)
                    .membre(membre)
                    .dateOperation(request.getDateOperation())
                    .typeOperation(request.getTypeOperation())
                    .montant(request.getMontant())
                    .sens(sens)
                    .modePaiement(request.getModePaiement())
                    .referenceExterne(cleanNullableText(request.getReferenceExterne()))
                    .agent(agent)
                    .sessionCaisse(session)
                    .observation(cleanNullableText(request.getObservation()))
                    .createdBy(createdBy)
                    .build();

            op = operationEpargneRepository.save(op);
            compteEpargneRepository.save(compte);

            if (genererMouvementCaisse(request.getTypeOperation(), session)) {
                operationCaisseService.enregistrer(
                        buildOperationCaisseRequest(
                                request,
                                session.getCaisse().getId(),
                                op.getId(),
                                membre.getId(),
                                sens
                        )
                );
            }

            if (doitGenererQuittance(request.getTypeOperation(), session)) {
                QuittanceCreateRequest q = new QuittanceCreateRequest();
                q.setMembreId(membre.getId());
                q.setTypeQuittance(resolveTypeQuittance(request.getTypeOperation(), session));
                q.setReferenceOperation(op.getId().toString());
                q.setMontant(request.getMontant());
                q.setDevise(DEVISE_PAR_DEFAUT);
                q.setDateEmission(request.getDateOperation());
                q.setCreatedBy(request.getCreatedBy());

                quittanceService.create(q);
            }

            return savingMapper.toResponse(op);

        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BusinessException(
                    "Le compte épargne a été modifié par une autre opération. Veuillez réessayer."
            );
        }
    }

    @Override
    public List<OperationEpargneResponse> getByCompte(Long compteId) {
        return operationEpargneRepository.findByCompteEpargneIdOrderByDateOperationDesc(compteId).stream()
                .map(savingMapper::toResponse)
                .toList();
    }

    @Override
    public List<OperationEpargneResponse> getByMembre(Long membreId) {
        return operationEpargneRepository.findByMembreIdOrderByDateOperationDesc(membreId).stream()
                .map(savingMapper::toResponse)
                .toList();
    }

    @Override
    public List<OperationEpargneResponse> getAll() {
        return operationEpargneRepository.findAllByOrderByDateOperationDesc().stream()
                .map(savingMapper::toResponse)
                .toList();
    }

    private void validerRequestOperation(OperationEpargneRequest request) {
        if (request == null) {
            throw new BusinessException("La requête d'opération épargne est obligatoire");
        }

        if (request.getCompteEpargneId() == null) {
            throw new BusinessException("Le compte épargne est obligatoire");
        }

        if (request.getMembreId() == null) {
            throw new BusinessException("Le membre est obligatoire");
        }

        if (request.getDateOperation() == null) {
            throw new BusinessException("La date de l'opération est obligatoire");
        }

        if (request.getTypeOperation() == null) {
            throw new BusinessException("Le type d'opération est obligatoire");
        }

        if (request.getMontant() == null || request.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Le montant doit être supérieur à zéro");
        }

        if (caisseObligatoire(request.getTypeOperation()) && request.getModePaiement() == null) {
            throw new BusinessException("Le mode de paiement est obligatoire pour cette opération");
        }

        if (request.getTypeOperation() == TypeOperationEpargne.AJUSTEMENT && request.getSens() == null) {
            throw new BusinessException("Le sens est obligatoire pour un ajustement");
        }
    }

    private void validerComptePourOperation(CompteEpargne compte, TypeOperationEpargne typeOperation) {
        if (compte.getStatut() == null) {
            throw new BusinessException("Le statut du compte épargne est invalide");
        }

        switch (compte.getStatut()) {
            case FERME -> throw new BusinessException("Aucune opération n'est autorisée sur un compte fermé");
            case INACTIF -> throw new BusinessException("Aucune opération n'est autorisée sur un compte inactif");
            case BLOQUE -> {
                if (typeOperation != TypeOperationEpargne.DEBLOCAGE_GARANTIE) {
                    throw new BusinessException("Le compte est bloqué");
                }
            }
            case ACTIF -> {
                // OK
            }
        }
    }

    private void validerCompatibiliteTypeCompteEtOperation(CompteEpargne compte, TypeOperationEpargne operation) {
        TypeCompteEpargne typeCompte = compte.getTypeCompte();

        if (typeCompte == null) {
            throw new BusinessException("Le type de compte épargne est invalide");
        }

        switch (typeCompte) {
            case COTISATION -> {
                if (!(operation == TypeOperationEpargne.COTISATION
                        || operation == TypeOperationEpargne.BLOCAGE_GARANTIE
                        || operation == TypeOperationEpargne.DEBLOCAGE_GARANTIE
                        || operation == TypeOperationEpargne.AJUSTEMENT)) {
                    throw new BusinessException("Cette opération n'est pas autorisée sur un compte de cotisation");
                }
            }
            case EPARGNE_VOLONTAIRE -> {
                if (!(operation == TypeOperationEpargne.EPARGNE
                        || operation == TypeOperationEpargne.RETRAIT
                        || operation == TypeOperationEpargne.AJUSTEMENT)) {
                    throw new BusinessException("Cette opération n'est pas autorisée sur un compte d'épargne volontaire");
                }
            }
            case MIXTE -> {
                // OK, toutes les opérations épargne sont autorisées
            }
        }
    }

    private void validerSessionCaisse(SessionCaisse session) {
        if (session == null) {
            throw new BusinessException("Session caisse introuvable");
        }

        if (session.getStatut() != StatutSessionCaisse.OUVERTE) {
            throw new BusinessException("La session de caisse doit être ouverte");
        }

        if (session.getDateCloture() != null) {
            throw new BusinessException("La session de caisse est déjà clôturée");
        }

        if (session.getCaisse() == null) {
            throw new BusinessException("La caisse liée à la session est introuvable");
        }

        if (Boolean.FALSE.equals(session.getCaisse().getActif())) {
            throw new BusinessException("La caisse liée à la session est inactive");
        }
    }

    private void validerSoldes(
            CompteEpargne compte,
            TypeOperationEpargne typeOperation,
            SensOperation sens,
            BigDecimal montant
    ) {
        if (typeOperation == TypeOperationEpargne.DEBLOCAGE_GARANTIE) {
            if (compte.getSoldeBloque().compareTo(montant) < 0) {
                throw new BusinessException("Solde bloqué insuffisant");
            }
            return;
        }

        if (sens == SensOperation.SORTIE && compte.getSoldeDisponible().compareTo(montant) < 0) {
            throw new BusinessException("Solde disponible insuffisant");
        }
    }

    private SensOperation resolveSens(OperationEpargneRequest request) {
        return switch (request.getTypeOperation()) {
            case COTISATION, EPARGNE, DEBLOCAGE_GARANTIE -> SensOperation.ENTREE;
            case RETRAIT, BLOCAGE_GARANTIE -> SensOperation.SORTIE;
            case AJUSTEMENT -> {
                if (request.getSens() == null) {
                    throw new BusinessException("Le sens est obligatoire pour un ajustement");
                }
                if (request.getMontant() == null || request.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessException("Montant invalide pour ajustement");
                }
                yield request.getSens();
            }
        };
    }

    private void appliquerMouvementSolde(
            CompteEpargne compte,
            TypeOperationEpargne type,
            SensOperation sens,
            BigDecimal montant
    ) {
        switch (type) {
            case COTISATION, EPARGNE -> {
                compte.setSoldeDisponible(compte.getSoldeDisponible().add(montant));
            }
            case RETRAIT -> {
                compte.setSoldeDisponible(compte.getSoldeDisponible().subtract(montant));
            }
            case BLOCAGE_GARANTIE -> {
                compte.setSoldeDisponible(compte.getSoldeDisponible().subtract(montant));
                compte.setSoldeBloque(compte.getSoldeBloque().add(montant));
            }
            case DEBLOCAGE_GARANTIE -> {
                compte.setSoldeBloque(compte.getSoldeBloque().subtract(montant));
                compte.setSoldeDisponible(compte.getSoldeDisponible().add(montant));
            }
            case AJUSTEMENT -> {
                if (sens == SensOperation.ENTREE) {
                    compte.setSoldeDisponible(compte.getSoldeDisponible().add(montant));
                } else {
                    compte.setSoldeDisponible(compte.getSoldeDisponible().subtract(montant));
                }
            }
        }
    }

    private boolean caisseObligatoire(TypeOperationEpargne type) {
        return switch (type) {
            case COTISATION, EPARGNE, RETRAIT -> true;
            case BLOCAGE_GARANTIE, DEBLOCAGE_GARANTIE, AJUSTEMENT -> false;
        };
    }

    private boolean genererMouvementCaisse(TypeOperationEpargne type, SessionCaisse session) {
        return switch (type) {
            case COTISATION, EPARGNE, RETRAIT -> true;
            case AJUSTEMENT -> session != null;
            case BLOCAGE_GARANTIE, DEBLOCAGE_GARANTIE -> false;
        };
    }

    private boolean doitGenererQuittance(TypeOperationEpargne type, SessionCaisse session) {
        return switch (type) {
            case COTISATION, EPARGNE, RETRAIT -> true;
            case AJUSTEMENT -> session != null;
            case BLOCAGE_GARANTIE, DEBLOCAGE_GARANTIE -> false;
        };
    }

    private OperationCaisseRequest buildOperationCaisseRequest(
            OperationEpargneRequest request,
            Long caisseId,
            Long operationEpargneId,
            Long membreId,
            SensOperation sens
    ) {
        OperationCaisseRequest oc = new OperationCaisseRequest();
        oc.setSessionCaisseId(request.getSessionCaisseId());
        oc.setCaisseId(caisseId);
        oc.setDateOperation(request.getDateOperation());
        oc.setTypeOperation(resolveTypeOperationCaisse(request.getTypeOperation(), sens));
        oc.setCategorieOperation(resolveCategorie(request.getTypeOperation(), sens));
        oc.setMontant(request.getMontant());
        oc.setDevise(DEVISE_PAR_DEFAUT);
        oc.setMembreId(membreId);
        oc.setOperationEpargneId(operationEpargneId);
        oc.setAgentId(request.getAgentId());
        oc.setDescription("Mouvement épargne automatique");
        oc.setCreatedBy(request.getCreatedBy());
        oc.setModePaiement(request.getModePaiement());
        oc.setObservation(request.getObservation());
        return oc;
    }

    private TypeOperationCaisse resolveTypeOperationCaisse(TypeOperationEpargne type, SensOperation sens) {
        return switch (type) {
            case COTISATION, EPARGNE -> TypeOperationCaisse.ENTREE;
            case RETRAIT -> TypeOperationCaisse.SORTIE;
            case BLOCAGE_GARANTIE, DEBLOCAGE_GARANTIE ->
                    throw new BusinessException("Aucune opération de caisse ne doit être générée pour ce type d'opération");
            case AJUSTEMENT -> sens == SensOperation.ENTREE
                    ? TypeOperationCaisse.ENTREE
                    : TypeOperationCaisse.SORTIE;
        };
    }

    private CategorieOperationCaisse resolveCategorie(TypeOperationEpargne type, SensOperation sens) {
        return switch (type) {
            case COTISATION -> CategorieOperationCaisse.COTISATION;
            case EPARGNE -> CategorieOperationCaisse.EPARGNE;
            case RETRAIT -> CategorieOperationCaisse.RETRAIT_EPARGNE;
            case BLOCAGE_GARANTIE, DEBLOCAGE_GARANTIE ->
                    throw new BusinessException("Aucune catégorie caisse ne doit être générée pour ce type d'opération");
            case AJUSTEMENT -> {
                if (sens == SensOperation.ENTREE) {
                    yield CategorieOperationCaisse.ENTREE_DIVERSE;
                } else {
                    yield CategorieOperationCaisse.SORTIE_DIVERSE;
                }
            }
        };
    }

    private TypeQuittance resolveTypeQuittance(TypeOperationEpargne type, SessionCaisse session) {
        return switch (type) {
            case COTISATION -> TypeQuittance.COTISATION;
            case EPARGNE -> TypeQuittance.EPARGNE;
            case RETRAIT -> TypeQuittance.RETRAIT;
            case AJUSTEMENT -> {
                if (session == null) {
                    throw new BusinessException("Aucune quittance ne doit être générée pour un ajustement sans mouvement de caisse");
                }
                yield TypeQuittance.AJUSTEMENT_EPARGNE;
            }
            case BLOCAGE_GARANTIE, DEBLOCAGE_GARANTIE ->
                    throw new BusinessException("Aucune quittance ne doit être générée pour ce type d'opération");
        };
    }

    private String cleanNullableText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    @Override
    public boolean isCurrentUserAccount(Long compteId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof com.mini.credit.entity.referentiel.Utilisateur)) {
            return false;
        }

        com.mini.credit.entity.referentiel.Utilisateur utilisateur = (com.mini.credit.entity.referentiel.Utilisateur) authentication.getPrincipal();

        if (utilisateur.getMembre() == null) {
            return false;
        }

        // Vérifier si le compte appartient au membre de l'utilisateur
        CompteEpargne compte = compteEpargneRepository.findById(compteId).orElse(null);
        return compte != null && compte.getMembre() != null &&
               compte.getMembre().getId().equals(utilisateur.getMembre().getId());
    }

    @Override
    public boolean isCurrentUserMembre(Long membreId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof com.mini.credit.entity.referentiel.Utilisateur)) {
            return false;
        }

        com.mini.credit.entity.referentiel.Utilisateur utilisateur = (com.mini.credit.entity.referentiel.Utilisateur) authentication.getPrincipal();

        // Pour les utilisateurs avec un membre, vérifier si l'ID demandé correspond
        if (utilisateur.getMembre() != null) {
            return utilisateur.getMembre().getId().equals(membreId);
        }

        return false;
    }
}