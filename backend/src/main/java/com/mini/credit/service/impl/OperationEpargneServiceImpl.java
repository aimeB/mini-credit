package com.mini.credit.service.impl;

import com.mini.credit.dto.caisse.OperationCaisseRequest;
import com.mini.credit.dto.caisse.OperationCaisseResponse;
import com.mini.credit.dto.document.QuittanceCreateRequest;
import com.mini.credit.dto.document.TicketRecuGenerationRequest;
import com.mini.credit.dto.epargne.OperationEpargneRequest;
import com.mini.credit.dto.epargne.OperationEpargneResponse;
import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.entity.epargne.CompteEpargne;
import com.mini.credit.entity.epargne.OperationEpargne;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.CollecteJournaliereTerrain;
import com.mini.credit.entity.referentiel.CollecteMembreLigne;
import com.mini.credit.entity.referentiel.AgentTerrain;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.agence.Agence;
import com.mini.credit.enums.CategorieOperationCaisse;
import com.mini.credit.enums.RecetteStatut;
import com.mini.credit.enums.SensOperation;
import com.mini.credit.enums.StatutMembre;
import com.mini.credit.enums.TypeCompteEpargne;
import com.mini.credit.enums.TypeOperationCaisse;
import com.mini.credit.enums.TypeOperationEpargne;
import com.mini.credit.enums.StatutSessionCaisse;
import com.mini.credit.enums.SourceOperationCaisse;
import com.mini.credit.enums.TypeLigneCollecte;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.enums.TypeQuittance;
import com.mini.credit.enums.TypeTicketRecu;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.exception.ResourceNotFoundException;
import com.mini.credit.mapper.SavingMapper;
import com.mini.credit.repository.agentTerrain.AgentTerrainRepository;
import com.mini.credit.repository.caisse.SessionCaisseRepository;
import com.mini.credit.repository.collecteTerrain.CollecteJournaliereTerrainRepository;
import com.mini.credit.repository.collecteTerrain.CollecteMembreLigneRepository;
import com.mini.credit.repository.epargne.CompteEpargneRepository;
import com.mini.credit.repository.epargne.OperationEpargneRepository;
import com.mini.credit.repository.membre.MembreRepository;
import com.mini.credit.repository.utilisateur.UtilisateurRepository;
import com.mini.credit.service.OperationCaisseService;
import com.mini.credit.service.OperationEpargneService;
import com.mini.credit.service.QuittanceService;
import com.mini.credit.service.TicketRecuService;
import com.mini.credit.service.security.ScopeService;
import com.mini.credit.service.security.SecurityUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class OperationEpargneServiceImpl implements OperationEpargneService {

    private static final String DEVISE_PAR_DEFAUT = "CDF";
    private static final Pattern COLLECTE_REFERENCE_PATTERN = Pattern.compile("^COLLECTE-(\\d+)-LIGNE-(\\d+)$");

    private final OperationEpargneRepository operationEpargneRepository;
    private final CompteEpargneRepository compteEpargneRepository;
    private final MembreRepository membreRepository;
    private final AgentTerrainRepository agentTerrainRepository;
    private final SessionCaisseRepository sessionCaisseRepository;
    private final CollecteJournaliereTerrainRepository collecteJournaliereTerrainRepository;
    private final CollecteMembreLigneRepository collecteMembreLigneRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final SavingMapper savingMapper;
    private final OperationCaisseService operationCaisseService;
    private final QuittanceService quittanceService;
    private final TicketRecuService ticketRecuService;
    private final ScopeService scopeService;

    @Override
    public OperationEpargneResponse enregistrer(OperationEpargneRequest request) {
        try {

            validerRequestOperation(request);
            boolean isAutomaticCollecteGeneration = isAutomaticCollecteGenerationRequest(request);

            // Distinction stricte: saisie manuelle vs génération automatique issue d'une collecte validée.
            if (isAutomaticCollecteGeneration) {
                validateAutomaticCollecteGenerationScope(request);
            } else if (!scopeService.canRecordEpargneOperation(request.getMembreId())) {
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

            Utilisateur createdBy = resolveOperationAuthor(request.getCreatedBy());
            Long createdById = createdBy != null ? createdBy.getId() : null;

            SensOperation sens = resolveSens(request);

            if (!isAutomaticCollecteGeneration && caisseObligatoire(request.getTypeOperation()) && session == null) {
                session = resolveOpenCashSessionForMembre(membre);
                request.setSessionCaisseId(session.getId());
            }

            if (session != null) {
                validerSessionCaisse(session, membre);
            }

            validerSoldes(compte, request.getTypeOperation(), sens, request.getMontant());

            BigDecimal ancienSolde = compte.getSoldeDisponible();

            appliquerMouvementSolde(compte, request.getTypeOperation(), sens, request.getMontant());
            BigDecimal nouveauSolde = compte.getSoldeDisponible();

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

            OperationCaisseResponse operationCaisse = null;
            if (!isAutomaticCollecteGeneration && genererMouvementCaisse(request.getTypeOperation(), session)) {
                operationCaisse = operationCaisseService.enregistrer(
                        buildOperationCaisseRequest(
                                request,
                                session.getCaisse().getId(),
                                op.getId(),
                                membre.getId(),
                                sens,
                                createdById
                        )
                );
            }

            if (!isAutomaticCollecteGeneration && doitGenererTicketDepot(request.getTypeOperation(), operationCaisse)) {
                ticketRecuService.genererDepuisOperation(TicketRecuGenerationRequest.builder()
                        .typeTicket(TypeTicketRecu.DEPOT_EPARGNE)
                        .operationEpargneId(op.getId())
                        .operationCaisseId(operationCaisse.getId())
                        .sessionCaisseId(session.getId())
                        .caisseId(session.getCaisse().getId())
                        .membreId(membre.getId())
                        .compteEpargneId(compte.getId())
                        .utilisateurCreateurId(createdById)
                        .devise(DEVISE_PAR_DEFAUT)
                        .montantPrincipal(request.getMontant())
                        .ancienSolde(ancienSolde)
                        .nouveauSolde(nouveauSolde)
                        .commentaire("Ticket dépôt épargne direct")
                        .build());
            }

            if (isAutomaticCollecteGeneration) {
                Matcher collecteMatcher = COLLECTE_REFERENCE_PATTERN.matcher(request.getReferenceExterne().trim());
                if (collecteMatcher.matches()) {
                    ticketRecuService.genererDepuisOperation(TicketRecuGenerationRequest.builder()
                            .typeTicket(TypeTicketRecu.COLLECTE_TERRAIN)
                            .operationEpargneId(op.getId())
                            .collecteJournaliereId(Long.valueOf(collecteMatcher.group(1)))
                            .collecteMembreLigneId(Long.valueOf(collecteMatcher.group(2)))
                            .membreId(membre.getId())
                            .compteEpargneId(compte.getId())
                            .utilisateurCreateurId(createdById)
                            .devise(DEVISE_PAR_DEFAUT)
                            .montantPrincipal(request.getMontant())
                            .ancienSolde(ancienSolde)
                            .nouveauSolde(nouveauSolde)
                            .commentaire("Ticket collecte terrain par ligne membre")
                            .build());
                }
            }

            if (!isAutomaticCollecteGeneration && doitGenererQuittance(request.getTypeOperation(), session)) {
                QuittanceCreateRequest q = new QuittanceCreateRequest();
                q.setMembreId(membre.getId());
                q.setTypeQuittance(resolveTypeQuittance(request.getTypeOperation(), session));
                q.setReferenceOperation(op.getId().toString());
                q.setMontant(request.getMontant());
                q.setDevise(DEVISE_PAR_DEFAUT);
                q.setDateEmission(request.getDateOperation());
                q.setCreatedBy(createdById);

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

    private boolean isAutomaticCollecteGenerationRequest(OperationEpargneRequest request) {
        if (request == null || request.getReferenceExterne() == null) {
            return false;
        }
        return COLLECTE_REFERENCE_PATTERN.matcher(request.getReferenceExterne().trim()).matches();
    }

    private void validateAutomaticCollecteGenerationScope(OperationEpargneRequest request) {
        Utilisateur currentUser = getCurrentUser();
        if (currentUser.getRole() == null || currentUser.getRole().getCode() == null) {
            throw new BusinessException("Accès refusé: rôle utilisateur introuvable");
        }

        RoleCode roleCode = currentUser.getRole().getCode();
        boolean isAdmin = roleCode == RoleCode.ADMIN;
        boolean isControleur = roleCode == RoleCode.CONTROLEUR;
        if (!isAdmin && !isControleur) {
            throw new BusinessException("Accès refusé: génération automatique réservée au contrôle validé");
        }

        if (!Objects.equals(request.getCreatedBy(), currentUser.getId())) {
            throw new BusinessException("Accès refusé: incohérence utilisateur validateur");
        }

        Matcher matcher = COLLECTE_REFERENCE_PATTERN.matcher(request.getReferenceExterne().trim());
        if (!matcher.matches()) {
            throw new BusinessException("Référence externe collecte invalide");
        }

        Long collecteId = Long.valueOf(matcher.group(1));
        Long ligneId = Long.valueOf(matcher.group(2));

        CollecteJournaliereTerrain collecte = collecteJournaliereTerrainRepository.findById(collecteId)
                .orElseThrow(() -> new BusinessException("Collecte source introuvable"));
        if (collecte.getStatut() != RecetteStatut.VALIDEE) {
            throw new BusinessException("Collecte source non validée");
        }

        if (!Objects.equals(request.getAgentId(), collecte.getAgentTerrain() != null ? collecte.getAgentTerrain().getId() : null)) {
            throw new BusinessException("Contexte agent incohérent avec la collecte source");
        }

        if (!isAdmin) {
            Long userAntenne = currentUser.getEmploye() != null && currentUser.getEmploye().getAgence() != null
                    ? currentUser.getEmploye().getAgence().getId()
                    : null;
            if (userAntenne == null || !Objects.equals(userAntenne, collecte.getAntenneId())) {
                throw new BusinessException("Accès refusé: contrôleur hors périmètre antenne de la collecte");
            }
        }

        CollecteMembreLigne ligne = collecteMembreLigneRepository.findById(ligneId)
                .orElseThrow(() -> new BusinessException("Ligne de collecte source introuvable"));
        if (ligne.getCollecte() == null || !Objects.equals(ligne.getCollecte().getId(), collecteId)) {
            throw new BusinessException("Ligne source non rattachée à la collecte");
        }
        if (ligne.getTypeLigne() != TypeLigneCollecte.EPARGNE) {
            throw new BusinessException("Ligne source incompatible pour génération d'épargne");
        }
        if (!Objects.equals(request.getMembreId(), ligne.getMembre() != null ? ligne.getMembre().getId() : null)) {
            throw new BusinessException("Contexte membre incohérent avec la ligne source");
        }

        Membre membre = membreRepository.findById(request.getMembreId())
                .orElseThrow(() -> new BusinessException("Membre source introuvable"));
        Site membreSite = membre.getSite();
        Site collecteSite = collecte.getSite();
        if (membreSite == null || collecteSite == null || !Objects.equals(membreSite.getId(), collecteSite.getId())) {
            throw new BusinessException("Accès refusé: membre hors site de la recette validée");
        }

        Agence membreAgence = membreSite.getAgence();
        if (membreAgence == null || !Objects.equals(membreAgence.getId(), collecte.getAntenneId())) {
            throw new BusinessException("Accès refusé: membre hors antenne de la recette validée");
        }

        if (safeAmount(request.getMontant()).compareTo(safeAmount(ligne.getMontant())) != 0) {
            throw new BusinessException("Montant incohérent avec la ligne source de collecte");
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
                        || operation == TypeOperationEpargne.BLOCAGE_GARANTIE
                        || operation == TypeOperationEpargne.DEBLOCAGE_GARANTIE
                        || operation == TypeOperationEpargne.AJUSTEMENT)) {
                    throw new BusinessException("Cette opération n'est pas autorisée sur un compte d'épargne volontaire");
                }
            }
            case MIXTE -> {
                // OK, toutes les opérations épargne sont autorisées
            }
        }
    }

    private void validerSessionCaisse(SessionCaisse session, Membre membre) {
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

        Long sessionAntenneId = session.getCaisse().getAgence() != null ? session.getCaisse().getAgence().getId() : null;
        Long membreAntenneId = resolveAntenneId(membre);
        if (sessionAntenneId == null || membreAntenneId == null || !Objects.equals(sessionAntenneId, membreAntenneId)) {
            throw new BusinessException("La session de caisse ouverte ne correspond pas à l'antenne du membre");
        }
    }

    private SessionCaisse resolveOpenCashSessionForMembre(Membre membre) {
        Long antenneId = resolveAntenneId(membre);
        if (antenneId == null) {
            throw new BusinessException("Antenne du membre introuvable pour résoudre la session de caisse");
        }

        return sessionCaisseRepository
                .findFirstByCaisse_Agence_IdAndDateComptableAndStatutOrderByDateOuvertureDesc(
                        antenneId,
                        LocalDate.now(),
                        StatutSessionCaisse.OUVERTE
                )
                .orElseThrow(() -> new BusinessException("Une session de caisse ouverte est obligatoire pour l'antenne du membre"));
    }

    private Long resolveAntenneId(Membre membre) {
        if (membre == null || membre.getSite() == null || membre.getSite().getAgence() == null) {
            return null;
        }
        return membre.getSite().getAgence().getId();
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
            case COTISATION, EPARGNE, DEBLOCAGE_GARANTIE, INTERET -> SensOperation.ENTREE;
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
            case COTISATION, EPARGNE, INTERET -> {
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
            case BLOCAGE_GARANTIE, DEBLOCAGE_GARANTIE, AJUSTEMENT, INTERET -> false;
        };
    }

    private boolean genererMouvementCaisse(TypeOperationEpargne type, SessionCaisse session) {
        return switch (type) {
            case COTISATION, EPARGNE, RETRAIT -> true;
            case AJUSTEMENT -> session != null;
            case BLOCAGE_GARANTIE, DEBLOCAGE_GARANTIE, INTERET -> false;
        };
    }

    private boolean doitGenererQuittance(TypeOperationEpargne type, SessionCaisse session) {
        return switch (type) {
            case COTISATION, EPARGNE, RETRAIT -> true;
            case AJUSTEMENT -> session != null;
            case BLOCAGE_GARANTIE, DEBLOCAGE_GARANTIE, INTERET -> false;
        };
    }

    private boolean doitGenererTicketDepot(TypeOperationEpargne type, OperationCaisseResponse operationCaisse) {
        return operationCaisse != null && (type == TypeOperationEpargne.EPARGNE || type == TypeOperationEpargne.COTISATION);
    }

    private OperationCaisseRequest buildOperationCaisseRequest(
            OperationEpargneRequest request,
            Long caisseId,
            Long operationEpargneId,
            Long membreId,
                SensOperation sens,
                Long createdById
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
        oc.setCreatedBy(createdById);
        oc.setModePaiement(request.getModePaiement());
        oc.setObservation(request.getObservation());
        oc.setSource(resolveSourceOperationCaisse(request.getTypeOperation())); // PATCH 6
        return oc;
    }

    private TypeOperationCaisse resolveTypeOperationCaisse(TypeOperationEpargne type, SensOperation sens) {
        return switch (type) {
            case COTISATION, EPARGNE -> TypeOperationCaisse.ENTREE;
            case RETRAIT -> TypeOperationCaisse.SORTIE;
            case BLOCAGE_GARANTIE, DEBLOCAGE_GARANTIE, INTERET ->
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
            case BLOCAGE_GARANTIE, DEBLOCAGE_GARANTIE, INTERET ->
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

    /** PATCH 6 — Source selon le type d'operation épargne qui génère un mouvement caisse. */
    private SourceOperationCaisse resolveSourceOperationCaisse(TypeOperationEpargne type) {
        return switch (type) {
            case RETRAIT -> SourceOperationCaisse.RETRAIT_EPARGNE;
            case AJUSTEMENT -> SourceOperationCaisse.AJUSTEMENT;
            default -> SourceOperationCaisse.MANUEL; // COTISATION, EPARGNE: depot manuel via service
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
            case BLOCAGE_GARANTIE, DEBLOCAGE_GARANTIE, INTERET ->
                    throw new BusinessException("Aucune quittance ne doit être générée pour ce type d'opération");
        };
    }

    private String cleanNullableText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private Utilisateur resolveOperationAuthor(Long requestCreatedById) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Utilisateur utilisateur) {
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

    private BigDecimal safeAmount(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
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

    @Override
    public Page<OperationEpargneResponse> getAll(Pageable pageable) {
        Utilisateur currentUser = SecurityUtils.getCurrentUser();
        if (currentUser != null
                && currentUser.getRole() != null
                && currentUser.getRole().getCode() == RoleCode.CONTROLEUR) {
            Long controleurAntenneId = extractUserAntenneId(currentUser);
            if (controleurAntenneId == null) {
                return Page.empty(pageable);
            }

            List<OperationEpargneResponse> scoped = operationEpargneRepository.findAllByOrderByDateOperationDesc().stream()
                    .filter(op -> op.getMembre() != null
                            && op.getMembre().getSite() != null
                            && op.getMembre().getSite().getAgence() != null
                            && controleurAntenneId.equals(op.getMembre().getSite().getAgence().getId()))
                    .map(savingMapper::toResponse)
                    .toList();

            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), scoped.size());
            List<OperationEpargneResponse> content = start >= scoped.size() ? List.of() : scoped.subList(start, end);
            return new PageImpl<>(content, pageable, scoped.size());
        }

        // PHASE 3B: Return paginated list of all savings operations
        return operationEpargneRepository.findAll(pageable).map(savingMapper::toResponse);
    }

    private Long extractUserAntenneId(Utilisateur user) {
        if (user == null) {
            return null;
        }

        if (user.getEmploye() != null && user.getEmploye().getAgence() != null) {
            return user.getEmploye().getAgence().getId();
        }

        if (user.getSite() != null && user.getSite().getAgence() != null) {
            return user.getSite().getAgence().getId();
        }

        return null;
    }

    private Utilisateur getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new BusinessException("Utilisateur non authentifié");
        }

        if (authentication.getPrincipal() instanceof Utilisateur principalUser && principalUser.getId() != null) {
            return utilisateurRepository.findByIdWithValidationContext(principalUser.getId())
                    .orElseThrow(() -> new BusinessException("Utilisateur introuvable"));
        }

        return utilisateurRepository.findByUsernameWithValidationContext(authentication.getName())
                .orElseThrow(() -> new BusinessException("Utilisateur introuvable"));
    }
}