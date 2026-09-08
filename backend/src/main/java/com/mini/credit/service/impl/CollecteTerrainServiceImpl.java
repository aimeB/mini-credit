package com.mini.credit.service.impl;

import com.mini.credit.dto.referentiel.*;
import com.mini.credit.dto.caisse.OperationCaisseRequest;
import com.mini.credit.dto.credit.RemboursementRequest;
import com.mini.credit.dto.epargne.OperationEpargneRequest;
import com.mini.credit.entity.credit.Credit;
import com.mini.credit.entity.credit.DemandeCredit;
import com.mini.credit.entity.credit.RemboursementCredit;
import com.mini.credit.entity.employe.Employe;
import com.mini.credit.entity.epargne.CompteEpargne;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.*;
import com.mini.credit.enums.CategorieOperationCaisse;
import com.mini.credit.enums.DureeUnite;
import com.mini.credit.enums.ModaliteRemboursementCollecte;
import com.mini.credit.enums.ModePaiement;
import com.mini.credit.enums.PeriodiciteRemboursement;
import com.mini.credit.enums.RecetteStatut;
import com.mini.credit.enums.SourceOperationCaisse;
import com.mini.credit.enums.StatutDemandeCredit;
import com.mini.credit.enums.StatutCompte;
import com.mini.credit.enums.StatutCredit;
import com.mini.credit.enums.TypeLigneCollecte;
import com.mini.credit.repository.UtilisateurRepository;
import com.mini.credit.repository.agentTerrain.AgentTerrainRepository;
import com.mini.credit.repository.caisse.SessionCaisseRepository;
import com.mini.credit.repository.collecteTerrain.CollecteJournaliereTerrainRepository;
import com.mini.credit.repository.collecteTerrain.CollecteMembreLigneRepository;
import com.mini.credit.repository.collecteTerrain.CollecteOperationGenereeRepository;
import com.mini.credit.repository.credit.CreditRepository;
import com.mini.credit.repository.credit.DemandeCreditRepository;
import com.mini.credit.repository.credit.RemboursementCreditRepository;
import com.mini.credit.repository.epargne.CompteEpargneRepository;
import com.mini.credit.repository.membre.MembreRepository;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.service.CollecteTerrainService;
import com.mini.credit.service.CreditService;
import com.mini.credit.service.OperationCaisseService;
import com.mini.credit.service.OperationEpargneService;
import com.mini.credit.service.ParametreMetierService;
import com.mini.credit.service.WorkflowTaskService;
import com.mini.credit.service.audit.AuditService;
import com.mini.credit.enums.security.AuditAction;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CollecteTerrainServiceImpl implements CollecteTerrainService {

    private final CollecteJournaliereTerrainRepository collecteRepository;
    private final CollecteMembreLigneRepository ligneRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AgentTerrainRepository agentTerrainRepository;
    private final MembreRepository membreRepository;
    private final CompteEpargneRepository compteEpargneRepository;
    private final CreditRepository creditRepository;
    private final DemandeCreditRepository demandeCreditRepository;
    private final SessionCaisseRepository sessionCaisseRepository;
    private final CollecteOperationGenereeRepository collecteOperationGenereeRepository;
    private final OperationEpargneService operationEpargneService;
    private final CreditService creditService;
    private final OperationCaisseService operationCaisseService;
    private final ParametreMetierService parametreMetierService;
    private final RemboursementCreditRepository remboursementCreditRepository;
    private final AuditService auditService;
    private final WorkflowTaskService workflowTaskService;

    private static final List<StatutCredit> STATUTS_CREDIT_ACTIF = List.of(
        StatutCredit.DECAISSE,
        StatutCredit.EN_COURS,
        StatutCredit.EN_RETARD
    );

    private static final List<StatutCredit> STATUTS_CREDIT_BLOQUANT_NOUVELLE_DEMANDE = List.of(
        StatutCredit.APPROUVE,
        StatutCredit.DECAISSE,
        StatutCredit.EN_COURS,
        StatutCredit.EN_RETARD,
        StatutCredit.CONTENTIEUX
    );

    @Override
    public CollecteTerrainResponse getToday() {
        AgentTerrain agent = getCurrentAgent();
        CollecteJournaliereTerrain collecte = collecteRepository
            .findByAgentTerrainIdAndDateCollecte(agent.getId(), LocalDate.now())
            .orElse(null);
        return collecte != null ? toResponse(collecte) : null;
    }

    @Override
    @Transactional(readOnly = true)
    public CollecteTerrainResponse getById(Long collecteId) {
        CollecteJournaliereTerrain collecte = collecteRepository.findById(collecteId)
            .orElseThrow(() -> new RuntimeException("Collecte introuvable"));
        assertCanViewCollecte(collecte);
        return toResponse(collecte);
    }

    @Override
    public CollecteTerrainResponse create(CreateCollecteTerrainRequest request) {
        AgentTerrain agent = getCurrentAgent();
        LocalDate today = LocalDate.now();

        CollecteJournaliereTerrain existing = collecteRepository
            .findByAgentTerrainIdAndDateCollecte(agent.getId(), today)
            .orElse(null);
        if (existing != null) {
            return toResponse(existing);
        }

        Long antenneId = agent.getSite() != null && agent.getSite().getAgence() != null
            ? agent.getSite().getAgence().getId()
            : null;
        if (antenneId == null) {
            throw new RuntimeException("Antenne introuvable pour le site de l'agent");
        }

        CollecteJournaliereTerrain collecte = CollecteJournaliereTerrain.builder()
            .agentTerrain(agent)
            .site(agent.getSite())
            .antenneId(antenneId)
            .dateCollecte(today)
            .statut(RecetteStatut.BROUILLON)
            .especesDeclareesAgent(getEspecesDeclareesFromRequest(request))
            .especesRemises(getEspecesDeclareesFromRequest(request))
            .observations(request != null ? request.getObservations() : null)
            .createdBy(getCurrentUser().getId())
            .build();

        recalculate(collecte);
        return toResponse(collecteRepository.save(collecte));
    }

    @Override
    public CollecteMembreLigneResponse addLigne(Long collecteId, CreateCollecteMembreLigneRequest request) {
        CollecteJournaliereTerrain collecte = getOwnedBrouillonCollecte(collecteId);
        Membre membre = validateMembreInPerimeter(collecte, request.getMembreId());

        CollecteMembreLigne ligne = CollecteMembreLigne.builder()
            .collecte(collecte)
            .membre(membre)
            .typeLigne(request.getTypeLigne())
            .montant(safeAmount(request.getMontant()))
            .quantite(request.getQuantite() == null ? 0 : request.getQuantite())
            .reference(request.getReference())
            .commentaire(request.getCommentaire())
            .montantSouhaite(safeAmount(request.getMontantSouhaite()))
            .objetCredit(request.getObjetCredit())
            .gagePropose(request.getGagePropose())
            .dureeValeur(request.getDureeValeur())
            .dureeUnite(request.getDureeUnite())
            .modaliteRemboursement(request.getModaliteRemboursement())
            .build();

        hydrateOptionalLinks(ligne, request.getCompteEpargneId(), request.getCreditId(), request.getDemandeCreditId());
        validateLigneBusiness(ligne);

        CollecteMembreLigne saved = ligneRepository.save(ligne);
        recalculateAndSave(collecte);
        return toLineResponse(saved);
    }

    @Override
    public CollecteMembreLigneResponse updateLigne(Long collecteId, Long ligneId, UpdateCollecteMembreLigneRequest request) {
        CollecteJournaliereTerrain collecte = getOwnedBrouillonCollecte(collecteId);
        CollecteMembreLigne ligne = ligneRepository.findById(ligneId)
            .orElseThrow(() -> new RuntimeException("Ligne introuvable"));
        if (!Objects.equals(ligne.getCollecte().getId(), collecte.getId())) {
            throw new RuntimeException("Ligne hors collecte");
        }

        Membre membre = validateMembreInPerimeter(collecte, request.getMembreId());
        ligne.setMembre(membre);
        ligne.setTypeLigne(request.getTypeLigne());
        ligne.setMontant(safeAmount(request.getMontant()));
        ligne.setQuantite(request.getQuantite() == null ? 0 : request.getQuantite());
        ligne.setReference(request.getReference());
        ligne.setCommentaire(request.getCommentaire());
        ligne.setMontantSouhaite(safeAmount(request.getMontantSouhaite()));
        ligne.setObjetCredit(request.getObjetCredit());
        ligne.setGagePropose(request.getGagePropose());
        ligne.setDureeValeur(request.getDureeValeur());
        ligne.setDureeUnite(request.getDureeUnite());
        ligne.setModaliteRemboursement(request.getModaliteRemboursement());

        hydrateOptionalLinks(ligne, request.getCompteEpargneId(), request.getCreditId(), request.getDemandeCreditId());
        validateLigneBusiness(ligne);

        CollecteMembreLigne saved = ligneRepository.save(ligne);
        recalculateAndSave(collecte);
        return toLineResponse(saved);
    }

    @Override
    public void deleteLigne(Long collecteId, Long ligneId) {
        CollecteJournaliereTerrain collecte = getOwnedBrouillonCollecte(collecteId);
        CollecteMembreLigne ligne = ligneRepository.findById(ligneId)
            .orElseThrow(() -> new RuntimeException("Ligne introuvable"));
        if (!Objects.equals(ligne.getCollecte().getId(), collecte.getId())) {
            throw new RuntimeException("Ligne hors collecte");
        }
        ligneRepository.delete(ligne);
        recalculateAndSave(collecte);
    }

    @Override
    public CollecteTerrainResponse soumettre(Long collecteId, CreateCollecteTerrainRequest request) {
        CollecteJournaliereTerrain collecte = getOwnedBrouillonCollecte(collecteId);
        if (request != null) {
            BigDecimal especesDeclarees = getEspecesDeclareesFromRequest(request);
            collecte.setEspecesDeclareesAgent(especesDeclarees);
            collecte.setEspecesRemises(especesDeclarees);
            collecte.setObservations(request.getObservations());
        }
        recalculate(collecte);

        List<CollecteMembreLigne> lignes = ligneRepository.findByCollecteId(collecte.getId());
        if (lignes.isEmpty()) {
            throw new RuntimeException("Impossible de soumettre une collecte sans lignes");
        }
        if (collecte.getEcartTresorerie().compareTo(BigDecimal.ZERO) != 0
            && (collecte.getObservations() == null || collecte.getObservations().trim().isEmpty())) {
            throw new RuntimeException("Observation obligatoire si écart trésorerie différent de 0");
        }

        collecte.setStatut(RecetteStatut.SOUMISE);
        collecte.setSubmittedAt(LocalDateTime.now());
        collecte.setBilletageConfirme(false);
        collecte.setEspecesConfirmeesCaissier(BigDecimal.ZERO);
        collecte.setDateConfirmationBilletage(null);
        collecte.setConfirmeParCaissierId(null);
        collecte.setObservationBilletage(null);
        CollecteJournaliereTerrain saved = collecteRepository.save(collecte);
        workflowTaskService.onCollecteSoumise(saved.getId(), resolveWorkflowReference(saved), saved.getAntenneId(), resolveSiteId(saved));
        return toResponse(saved);
    }

    @Override
    public CollecteTerrainResponse confirmerBilletage(Long collecteId, ConfirmerBilletageRequest request) {
        Utilisateur current = getCurrentUser();

        if (!hasRole(current, "CAISSIER")) {
            throw new RuntimeException("Seul le CAISSIER peut confirmer le billetage");
        }

        CollecteJournaliereTerrain collecte = collecteRepository.findById(collecteId)
            .orElseThrow(() -> new RuntimeException("Collecte introuvable"));

        if (collecte.getStatut() != RecetteStatut.SOUMISE) {
            throw new RuntimeException("Seule une collecte SOUMISE peut être confirmée au billetage");
        }

        Long antenneCaissier = current.getEmploye() != null && current.getEmploye().getAgence() != null
            ? current.getEmploye().getAgence().getId()
            : null;
        if (antenneCaissier == null || !Objects.equals(antenneCaissier, collecte.getAntenneId())) {
            throw new RuntimeException("CAISSIER hors périmètre antenne");
        }

        collecte.setEspecesConfirmeesCaissier(safeAmount(request.getEspecesConfirmeesCaissier()));
        collecte.setEspecesRemises(collecte.getEspecesConfirmeesCaissier());
        collecte.setDateConfirmationBilletage(LocalDateTime.now());
        collecte.setConfirmeParCaissierId(current.getId());
        collecte.setObservationBilletage(request.getObservationBilletage());
        collecte.setBilletageConfirme(true);

        recalculate(collecte);

        auditService.logSuccess(
            AuditAction.RECETTE_JOURNALIERE_UPDATED,
            "CollecteJournaliereTerrain",
            collecte.getId(),
            "Billetage confirmé par user=" + current.getId()
                + " antenne=" + collecte.getAntenneId()
                + " date=" + collecte.getDateConfirmationBilletage()
                + " commentaire=" + (request.getObservationBilletage() == null ? "" : request.getObservationBilletage())
        );

        CollecteJournaliereTerrain saved = collecteRepository.save(collecte);
        workflowTaskService.onCollecteBilletageConfirme(saved.getId(), resolveWorkflowReference(saved), saved.getAntenneId(), resolveSiteId(saved));
        return toResponse(saved);
    }

    @Override
    public CollecteTerrainResponse valider(Long collecteId, ValidateCollecteTerrainRequest request) {
        Utilisateur current = getCurrentUser();
        assertCanValidate();

        CollecteJournaliereTerrain collecte = collecteRepository.findById(collecteId)
            .orElseThrow(() -> new RuntimeException("Collecte introuvable"));

        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
            .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        boolean isControleur = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
            .anyMatch(a -> "ROLE_CONTROLEUR".equals(a.getAuthority()));
        if (!isAdmin && isControleur) {
            Long antenneUser = current.getEmploye() != null && current.getEmploye().getAgence() != null
                ? current.getEmploye().getAgence().getId()
                : null;
            if (antenneUser == null || !Objects.equals(antenneUser, collecte.getAntenneId())) {
                throw new RuntimeException("CONTROLEUR hors périmètre antenne");
            }
        }

        String decision = request != null ? request.getDecision() : null;
        if ("VALIDEE".equalsIgnoreCase(decision)) {
            if (collecte.getStatut() == RecetteStatut.VALIDEE) {
                auditService.logSuccess(
                    AuditAction.RECETTE_JOURNALIERE_VALIDATED,
                    "CollecteJournaliereTerrain",
                    collecte.getId(),
                    "Validation déjà effectuée, retour idempotent"
                );
                return toResponse(collecte);
            }
            if (collecte.getStatut() != RecetteStatut.SOUMISE) {
                throw new RuntimeException("Seule une collecte SOUMISE peut être validée");
            }
            if (!Boolean.TRUE.equals(collecte.getBilletageConfirme())) {
                throw new BusinessException("La collecte ne peut pas être validée ou rejetée avant le billetage.");
            }
            BigDecimal ecartOfficiel = safeAmount(collecte.getEspecesConfirmeesCaissier()).subtract(safeAmount(collecte.getTotalGeneralCalcule()));
            collecte.setEcartTresorerie(ecartOfficiel);
            if (ecartOfficiel.compareTo(BigDecimal.ZERO) != 0
                && (collecte.getObservationBilletage() == null || collecte.getObservationBilletage().trim().isEmpty())) {
                throw new RuntimeException("Observation billetage obligatoire si écart officiel différent de 0");
            }
            collecte.setStatut(RecetteStatut.VALIDEE);
            collecte.setValidatedBy(current);
            collecte.setValidatedAt(LocalDateTime.now());
            generateOfficialOperationsOnce(collecte, current);
            auditService.logSuccess(
                AuditAction.RECETTE_JOURNALIERE_VALIDATED,
                "CollecteJournaliereTerrain",
                collecte.getId(),
                "Collecte validée et opérations générées"
            );
        } else if ("REJETEE".equalsIgnoreCase(decision)) {
            if (collecte.getStatut() != RecetteStatut.SOUMISE) {
                throw new RuntimeException("Seule une collecte SOUMISE peut être rejetée");
            }
            if (!Boolean.TRUE.equals(collecte.getBilletageConfirme())) {
                throw new BusinessException("La collecte ne peut pas être validée ou rejetée avant le billetage.");
            }
            if (request.getMotifRejet() == null || request.getMotifRejet().isBlank()) {
                throw new RuntimeException("Motif de rejet obligatoire");
            }
            collecte.setStatut(RecetteStatut.REJETEE);
            collecte.setObservations(request.getMotifRejet());
            collecte.setValidatedBy(current);
            collecte.setValidatedAt(LocalDateTime.now());
            auditService.logSuccess(
                AuditAction.RECETTE_JOURNALIERE_REJECTED,
                "CollecteJournaliereTerrain",
                collecte.getId(),
                "Collecte rejetée"
            );
        } else {
            throw new RuntimeException("Décision invalide");
        }

        CollecteJournaliereTerrain saved = collecteRepository.save(collecte);
        workflowTaskService.onCollecteCloturee(saved.getId(), resolveWorkflowReference(saved), saved.getAntenneId(), resolveSiteId(saved));
        return toResponse(saved);
    }

    @Override
    public CollecteTerrainResponse rejeter(Long collecteId, ValidateCollecteTerrainRequest request) {
        ValidateCollecteTerrainRequest reject = ValidateCollecteTerrainRequest.builder()
            .decision("REJETEE")
            .motifRejet(request != null ? request.getMotifRejet() : null)
            .build();
        return valider(collecteId, reject);
    }

    private BigDecimal getEspecesDeclareesFromRequest(CreateCollecteTerrainRequest request) {
        if (request == null) {
            return BigDecimal.ZERO;
        }
        if (request.getEspecesDeclareesAgent() != null) {
            return safeAmount(request.getEspecesDeclareesAgent());
        }
        return safeAmount(request.getEspecesRemises());
    }

    @Override
    @Transactional(readOnly = true)
    public CollecteRecapResponse recap(Long collecteId) {
        CollecteJournaliereTerrain collecte = collecteRepository.findById(collecteId)
            .orElseThrow(() -> new RuntimeException("Collecte introuvable"));
        assertCanViewCollecte(collecte);
        return toRecap(collecte);
    }

    private void generateOfficialOperationsOnce(CollecteJournaliereTerrain collecte, Utilisateur validatedBy) {
        if (collecte.getOperationsGeneratedAt() != null) {
            auditService.logSuccess(
                AuditAction.RECETTE_JOURNALIERE_VALIDATED,
                "CollecteJournaliereTerrain",
                collecte.getId(),
                "Tentative de double génération bloquée"
            );
            return;
        }

        List<CollecteMembreLigne> lignes = ligneRepository.findByCollecteId(collecte.getId());
        if (lignes.isEmpty()) {
            throw new RuntimeException("Aucune ligne à générer");
        }

        Long sessionId = getOpenedSessionIdOrThrow();
        int generatedCount = 0;

        for (CollecteMembreLigne ligne : lignes) {
            if (ligne.getTypeLigne() == TypeLigneCollecte.EPARGNE) {
                generatedCount += generateEpargneOperation(collecte, ligne, validatedBy);
            } else if (ligne.getTypeLigne() == TypeLigneCollecte.REMBOURSEMENT_CREDIT) {
                generatedCount += generateRemboursementOperation(collecte, ligne, validatedBy);
            } else if (ligne.getTypeLigne() == TypeLigneCollecte.CARNET) {
                generatedCount += generateCarnetCaisseOperation(collecte, ligne, sessionId, validatedBy);
            } else if (ligne.getTypeLigne() == TypeLigneCollecte.FRAIS_ANALYSE) {
                generatedCount += generateLineTraceWithoutCaisse(collecte, ligne, validatedBy, "FRAIS_ANALYSE_TRACE");
            } else if (ligne.getTypeLigne() == TypeLigneCollecte.DEMANDE_CREDIT) {
                generatedCount += generateDemandeCreditTrace(collecte, ligne, validatedBy);
            }
        }

        generatedCount += generateRetourTerrainCaisse(collecte, lignes, sessionId, validatedBy);
        collecte.setOperationsGeneratedAt(LocalDateTime.now());
        collecte.setDateModification(LocalDateTime.now());
    }

    private void hydrateOptionalLinks(CollecteMembreLigne ligne, Long compteId, Long creditId, Long demandeId) {
        ligne.setCompteEpargne(null);
        ligne.setCredit(null);
        ligne.setDemandeCredit(null);

        if (compteId != null) {
            CompteEpargne compte = compteEpargneRepository.findById(compteId)
                .orElseThrow(() -> new RuntimeException("Compte épargne introuvable"));
            ligne.setCompteEpargne(compte);
        }
        if (creditId != null) {
            Credit credit = creditRepository.findById(creditId)
                .orElseThrow(() -> new RuntimeException("Crédit introuvable"));
            ligne.setCredit(credit);
        }
        if (demandeId != null) {
            DemandeCredit demande = demandeCreditRepository.findById(demandeId)
                .orElseThrow(() -> new RuntimeException("Demande crédit introuvable"));
            ligne.setDemandeCredit(demande);
        }
    }

    private void validateLigneBusiness(CollecteMembreLigne ligne) {
        if (ligne.getMembre() == null) {
            throw new RuntimeException("Chaque ligne doit être rattachée à un membre");
        }

        if (ligne.getTypeLigne() == TypeLigneCollecte.FRAIS_ANALYSE) {
            throw new RuntimeException("Le type FRAIS_ANALYSE n'est pas autorisé dans Ma collecte du jour.");
        }

        if (ligne.getTypeLigne() == TypeLigneCollecte.CARNET) {
            List<CollecteMembreLigne> existingCarnetLines = ligneRepository.findByCollecteIdAndMembreIdAndTypeLigne(
                ligne.getCollecte().getId(),
                ligne.getMembre().getId(),
                TypeLigneCollecte.CARNET
            );
            boolean duplicateCarnet = existingCarnetLines.stream()
                .anyMatch(value -> ligne.getId() == null || !Objects.equals(value.getId(), ligne.getId()));
            if (duplicateCarnet) {
                throw new RuntimeException("Un carnet a déjà été enregistré pour ce membre aujourd'hui.");
            }

            ligne.setQuantite(1);
            ligne.setMontant(getPrixCarnet());
        }

        if (isFinancialType(ligne.getTypeLigne()) && safeAmount(ligne.getMontant()).compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Montant positif obligatoire pour ce type de ligne");
        }
        if (!isFinancialType(ligne.getTypeLigne()) && safeAmount(ligne.getMontant()).compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Montant invalide");
        }

        if (ligne.getTypeLigne() == TypeLigneCollecte.REMBOURSEMENT_CREDIT) {
            if (ligne.getCredit() == null) {
                List<Credit> actifs = findCreditsActifs(ligne.getMembre().getId());
                if (actifs.isEmpty()) {
                    throw new RuntimeException("Ce membre n'a aucun crédit en cours.");
                }
                if (actifs.size() > 1) {
                    throw new RuntimeException("Plusieurs crédits actifs trouvés pour ce membre. Contrôle requis.");
                }
                ligne.setCredit(actifs.get(0));
            }
            StatutCredit statut = ligne.getCredit().getStatut();
            boolean actif = STATUTS_CREDIT_ACTIF.contains(statut);
            if (!actif) {
                throw new RuntimeException("Crédit actif obligatoire pour REMBOURSEMENT_CREDIT");
            }
        }

        if (ligne.getTypeLigne() == TypeLigneCollecte.EPARGNE) {
            if (ligne.getCompteEpargne() == null) {
                CompteEpargne compte = compteEpargneRepository.findFirstByMembreIdAndStatut(
                        ligne.getMembre().getId(),
                        StatutCompte.ACTIF
                ).orElseThrow(() -> new RuntimeException("Ce membre n'a pas de compte épargne actif."));
                ligne.setCompteEpargne(compte);
            }
            if (ligne.getCompteEpargne().getStatut() != StatutCompte.ACTIF) {
                throw new RuntimeException("Compte épargne actif obligatoire pour EPARGNE");
            }
        }

        if (ligne.getTypeLigne() == TypeLigneCollecte.DEMANDE_CREDIT) {
            if (creditRepository.existsByMembreIdAndStatutIn(
                    ligne.getMembre().getId(),
                    STATUTS_CREDIT_BLOQUANT_NOUVELLE_DEMANDE
            )) {
                throw new RuntimeException("Ce membre a déjà un crédit actif ou approuvé. Nouvelle demande interdite.");
            }
            if (safeAmount(ligne.getMontantSouhaite()).compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Le montant demandé du crédit est obligatoire.");
            }
            if (safeAmount(ligne.getMontant()).compareTo(BigDecimal.ZERO) < 0) {
                throw new RuntimeException("Les frais de demande prévus ne peuvent pas être négatifs.");
            }
            if (ligne.getObjetCredit() == null || ligne.getObjetCredit().isBlank()) {
                throw new RuntimeException("L'objet de crédit est obligatoire.");
            }
            if (ligne.getGagePropose() == null || ligne.getGagePropose().isBlank()) {
                throw new RuntimeException("Le gage proposé est obligatoire.");
            }
            if (ligne.getDureeValeur() == null || ligne.getDureeValeur() < 1 || ligne.getDureeValeur() > 60) {
                throw new RuntimeException("La durée du crédit doit être comprise entre 1 et 60.");
            }
            if (ligne.getDureeUnite() == null) {
                throw new RuntimeException("L'unité de durée du crédit est obligatoire.");
            }
            if (ligne.getModaliteRemboursement() == null) {
                throw new RuntimeException("La modalité de remboursement est obligatoire.");
            }
            ligne.setQuantite(0);
        }
    }

    private boolean isFinancialType(TypeLigneCollecte type) {
        return type == TypeLigneCollecte.EPARGNE
            || type == TypeLigneCollecte.REMBOURSEMENT_CREDIT
            || type == TypeLigneCollecte.CARNET;
    }

    private Membre validateMembreInPerimeter(CollecteJournaliereTerrain collecte, Long membreId) {
        Membre membre = membreRepository.findById(membreId)
            .orElseThrow(() -> new RuntimeException("Membre introuvable"));
        Long memberSiteId = membre.getSite() != null ? membre.getSite().getId() : null;
        if (!Objects.equals(memberSiteId, collecte.getSite().getId())) {
            throw new RuntimeException("Membre hors site/périmètre de la collecte");
        }
        return membre;
    }

    private CollecteJournaliereTerrain getOwnedBrouillonCollecte(Long collecteId) {
        AgentTerrain agent = getCurrentAgent();
        CollecteJournaliereTerrain collecte = collecteRepository.findById(collecteId)
            .orElseThrow(() -> new RuntimeException("Collecte introuvable"));
        if (!Objects.equals(collecte.getAgentTerrain().getId(), agent.getId())) {
            throw new RuntimeException("Accès refusé à cette collecte");
        }
        if (collecte.getStatut() != RecetteStatut.BROUILLON) {
            throw new RuntimeException("Interdiction de modifier une collecte SOUMISE/VALIDEE/REJETEE");
        }
        return collecte;
    }

    private void recalculateAndSave(CollecteJournaliereTerrain collecte) {
        recalculate(collecte);
        collecteRepository.save(collecte);
    }

    private void recalculate(CollecteJournaliereTerrain collecte) {
        List<CollecteMembreLigne> lignes = ligneRepository.findByCollecteId(collecte.getId() == null ? -1L : collecte.getId());
        if (collecte.getId() == null) {
            lignes = collecte.getLignes();
        }

        BigDecimal totalEpargne = BigDecimal.ZERO;
        BigDecimal totalRemboursements = BigDecimal.ZERO;
        BigDecimal totalFrais = BigDecimal.ZERO;
        int totalCarnets = 0;

        if (lignes != null) {
            for (CollecteMembreLigne l : lignes) {
                BigDecimal montant = safeAmount(l.getMontant());
                int qty = l.getQuantite() == null ? 0 : l.getQuantite();
                switch (l.getTypeLigne()) {
                    case EPARGNE -> totalEpargne = totalEpargne.add(montant);
                    case REMBOURSEMENT_CREDIT -> totalRemboursements = totalRemboursements.add(montant);
                    case CARNET -> {
                        totalFrais = totalFrais.add(montant);
                        totalCarnets += qty > 0 ? qty : 1;
                    }
                    case FRAIS_ANALYSE -> totalFrais = totalFrais.add(montant);
                    case DEMANDE_CREDIT -> {
                        // Ligne non financière en total principal.
                    }
                }
            }
        }

        BigDecimal totalGeneral = totalEpargne.add(totalRemboursements).add(totalFrais);
        BigDecimal montantReference = Boolean.TRUE.equals(collecte.getBilletageConfirme())
            ? safeAmount(collecte.getEspecesConfirmeesCaissier())
            : safeAmount(collecte.getEspecesDeclareesAgent());
        BigDecimal ecart = montantReference.subtract(totalGeneral);

        collecte.setTotalEpargneCalcule(totalEpargne);
        collecte.setTotalRemboursementsCalcule(totalRemboursements);
        collecte.setTotalFraisCalcule(totalFrais);
        collecte.setTotalCarnetsCalcule(totalCarnets);
        collecte.setTotalGeneralCalcule(totalGeneral);
        collecte.setEcartTresorerie(ecart);
    }

    private CollecteTerrainResponse toResponse(CollecteJournaliereTerrain c) {
        List<CollecteOperationGeneree> generated = collecteOperationGenereeRepository.findByCollecteId(c.getId());
        List<CollecteMembreLigneResponse> lignes = ligneRepository.findByCollecteId(c.getId()).stream()
            .map(this::toLineResponse)
            .toList();
        return CollecteTerrainResponse.builder()
            .id(c.getId())
            .agentTerrainId(c.getAgentTerrain() != null ? c.getAgentTerrain().getId() : null)
            .agentTerrainNom(c.getAgentTerrain() != null && c.getAgentTerrain().getUtilisateur() != null ? c.getAgentTerrain().getUtilisateur().getNomComplet() : null)
            .siteId(c.getSite() != null ? c.getSite().getId() : null)
            .siteNom(c.getSite() != null ? c.getSite().getNomSite() : null)
            .antenneId(c.getAntenneId())
            .dateCollecte(c.getDateCollecte())
            .statut(c.getStatut())
            .especesRemises(c.getEspecesRemises())
            .especesDeclareesAgent(c.getEspecesDeclareesAgent())
            .especesConfirmeesCaissier(c.getEspecesConfirmeesCaissier())
            .dateConfirmationBilletage(c.getDateConfirmationBilletage())
            .confirmeParCaissierId(c.getConfirmeParCaissierId())
            .confirmeParCaissierNom(resolveConfirmeParCaissierNom(c.getConfirmeParCaissierId()))
            .billetagePar(c.getConfirmeParCaissierId())
            .billetageParNom(resolveConfirmeParCaissierNom(c.getConfirmeParCaissierId()))
            .dateBilletage(c.getDateConfirmationBilletage())
            .observationBilletage(c.getObservationBilletage())
            .billetageConfirme(c.getBilletageConfirme())
            .totalEpargneCalcule(c.getTotalEpargneCalcule())
            .totalRemboursementsCalcule(c.getTotalRemboursementsCalcule())
            .totalFraisCalcule(c.getTotalFraisCalcule())
            .totalCarnetsCalcule(c.getTotalCarnetsCalcule())
            .totalGeneralCalcule(c.getTotalGeneralCalcule())
            .ecartTresorerie(c.getEcartTresorerie())
            .observations(c.getObservations())
            .createdBy(c.getCreatedBy())
            .createdAt(c.getDateCreation())
            .submittedAt(c.getSubmittedAt())
            .validatedBy(c.getValidatedBy() != null ? c.getValidatedBy().getId() : null)
            .validatedByNom(c.getValidatedBy() != null ? c.getValidatedBy().getNomComplet() : null)
            .validatedAt(c.getValidatedAt())
            .operationsGeneratedAt(c.getOperationsGeneratedAt())
                .operationsGeneratedCount(generated.size())
                .generationSummary(generated.isEmpty() ? null : ("Opérations générées: " + generated.size()))
            .lignes(lignes)
            .build();
    }

    private CollecteMembreLigneResponse toLineResponse(CollecteMembreLigne l) {
        BigDecimal total = safeAmount(l.getMontant()).multiply(BigDecimal.valueOf(l.getQuantite() == null || l.getQuantite() == 0 ? 1 : l.getQuantite()));
        return CollecteMembreLigneResponse.builder()
            .id(l.getId())
            .membreId(l.getMembre() != null ? l.getMembre().getId() : null)
            .membreCode(l.getMembre() != null ? l.getMembre().getCodeMembre() : null)
            .membreNom(l.getMembre() != null ? l.getMembre().getNomComplet() : null)
            .compteEpargneId(l.getCompteEpargne() != null ? l.getCompteEpargne().getId() : null)
            .creditId(l.getCredit() != null ? l.getCredit().getId() : null)
            .demandeCreditId(l.getDemandeCredit() != null ? l.getDemandeCredit().getId() : null)
            .typeLigne(l.getTypeLigne())
            .montant(l.getMontant())
            .quantite(l.getQuantite())
            .reference(l.getReference())
            .commentaire(l.getCommentaire())
            .totalLigne(total)
                .montantSouhaite(l.getMontantSouhaite())
                .objetCredit(l.getObjetCredit())
                .gagePropose(l.getGagePropose())
                .dureeValeur(l.getDureeValeur())
                .dureeUnite(l.getDureeUnite())
                .modaliteRemboursement(l.getModaliteRemboursement())
            .build();
    }

    private CollecteRecapResponse toRecap(CollecteJournaliereTerrain c) {
        int membresVisites = (int) ligneRepository.findByCollecteId(c.getId()).stream()
            .map(l -> l.getMembre() != null ? l.getMembre().getId() : null)
            .filter(Objects::nonNull)
            .distinct()
            .count();

        return CollecteRecapResponse.builder()
            .collecteId(c.getId())
            .membresVisites(membresVisites)
            .nouveauxMembres(0)
            .carnetsVendusDistribues(c.getTotalCarnetsCalcule())
            .totalEpargne(c.getTotalEpargneCalcule())
            .totalRemboursements(c.getTotalRemboursementsCalcule())
            .totalFrais(c.getTotalFraisCalcule())
            .totalGeneralAttendu(c.getTotalGeneralCalcule())
            .especesRemises(Boolean.TRUE.equals(c.getBilletageConfirme()) ? c.getEspecesConfirmeesCaissier() : c.getEspecesDeclareesAgent())
            .ecartTresorerie(c.getEcartTresorerie())
            .build();
    }

    private String resolveConfirmeParCaissierNom(Long caissierId) {
        if (caissierId == null) {
            return null;
        }
        return utilisateurRepository.findById(caissierId)
            .map(Utilisateur::getNomComplet)
            .orElse(null);
    }

    private Long resolveSiteId(CollecteJournaliereTerrain collecte) {
        return collecte != null && collecte.getSite() != null ? collecte.getSite().getId() : null;
    }

    private String resolveWorkflowReference(CollecteJournaliereTerrain collecte) {
        return collecte != null && collecte.getId() != null ? "COLLECTE-" + collecte.getId() : "COLLECTE-UNKNOWN";
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CollecteTerrainResponse> list(
        RecetteStatut statut,
        LocalDate dateDebut,
        LocalDate dateFin,
        Long agentId,
        Long siteId,
        Long antenneId,
        int page,
        int size
    ) {
        if (size <= 0) size = 10;
        if (size > 100) size = 100;
        if (page < 0) page = 0;

        Utilisateur current = getCurrentUser();
        List<CollecteJournaliereTerrain> all = collecteRepository.findAll();

        List<CollecteJournaliereTerrain> scoped = all.stream()
            .filter(c -> canViewByRole(current, c))
            .filter(c -> statut == null || c.getStatut() == statut)
            .filter(c -> dateDebut == null || (c.getDateCollecte() != null && !c.getDateCollecte().isBefore(dateDebut)))
            .filter(c -> dateFin == null || (c.getDateCollecte() != null && !c.getDateCollecte().isAfter(dateFin)))
            .filter(c -> agentId == null || (c.getAgentTerrain() != null && Objects.equals(c.getAgentTerrain().getId(), agentId)))
            .filter(c -> siteId == null || (c.getSite() != null && Objects.equals(c.getSite().getId(), siteId)))
            .filter(c -> antenneId == null || Objects.equals(c.getAntenneId(), antenneId))
            .sorted(Comparator.comparing(CollecteJournaliereTerrain::getDateCollecte, Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();

        int from = Math.min(page * size, scoped.size());
        int to = Math.min(from + size, scoped.size());
        List<CollecteTerrainResponse> content = scoped.subList(from, to).stream().map(this::toResponse).toList();

        return new PageImpl<>(content, PageRequest.of(page, size), scoped.size());
    }

    private int generateEpargneOperation(CollecteJournaliereTerrain collecte, CollecteMembreLigne ligne, Utilisateur validatedBy) {
        if (collecteOperationGenereeRepository.existsByCollecteIdAndLigneCollecteIdAndTypeOperation(collecte.getId(), ligne.getId(), "EPARGNE_DEPOT")) {
            auditService.logSuccess(AuditAction.INVALID_OPERATION, "CollecteOperationGeneree", collecte.getId(), "Ligne déjà générée EPARGNE");
            return 0;
        }
        OperationEpargneRequest request = new OperationEpargneRequest();
        request.setCompteEpargneId(requireNonNullId(ligne.getCompteEpargne(), "compteEpargneId manquant"));
        request.setMembreId(requireNonNullId(ligne.getMembre(), "membreId manquant"));
        request.setDateOperation(LocalDateTime.now());
        request.setTypeOperation(com.mini.credit.enums.TypeOperationEpargne.EPARGNE);
        request.setMontant(safeAmount(ligne.getMontant()));
        request.setModePaiement(ModePaiement.ESPECES);
        request.setSessionCaisseId(null);
        request.setAgentId(requireNonNullId(collecte.getAgentTerrain(), "agentTerrainId manquant"));
        request.setCreatedBy(validatedBy.getId());
        request.setReferenceExterne("COLLECTE-" + collecte.getId() + "-LIGNE-" + ligne.getId());
        request.setObservation("Collecte validée #" + collecte.getId());

        var response = operationEpargneService.enregistrer(request);
        collectOperation(collecte, ligne, "EPARGNE_DEPOT", response.getId(), validatedBy.getId());
        return 1;
    }

    private int generateRemboursementOperation(CollecteJournaliereTerrain collecte, CollecteMembreLigne ligne, Utilisateur validatedBy) {
        if (collecteOperationGenereeRepository.existsByCollecteIdAndLigneCollecteIdAndTypeOperation(collecte.getId(), ligne.getId(), "REMBOURSEMENT_CREDIT")) {
            auditService.logSuccess(AuditAction.INVALID_OPERATION, "CollecteOperationGeneree", collecte.getId(), "Ligne déjà générée REMBOURSEMENT");
            return 0;
        }

        Long creditId = requireNonNullId(ligne.getCredit(), "creditId manquant");
        RemboursementRequest request = new RemboursementRequest();
        request.setMembreId(requireNonNullId(ligne.getMembre(), "membreId manquant"));
        request.setDatePaiement(LocalDateTime.now());
        request.setMontantTotal(safeAmount(ligne.getMontant()));
        request.setModePaiement(ModePaiement.ESPECES);
        request.setSessionCaisseId(null);
        request.setAgentId(requireNonNullId(collecte.getAgentTerrain(), "agentTerrainId manquant"));
        request.setCreatedBy(validatedBy.getId());
        request.setObservation("Remboursement généré automatiquement depuis collecte validée #" + collecte.getId()
            + " ligne #" + ligne.getId());

        Long remboursementId = creditService.enregistrerRemboursementDepuisCollecte(
            creditId,
            request,
            collecte.getId(),
            ligne.getId(),
            validatedBy.getId(),
            collecte.getAntenneId()
        );

        collectOperation(collecte, ligne, "REMBOURSEMENT_CREDIT", remboursementId, validatedBy.getId());
        return 1;
    }

    private int generateLineTraceWithoutCaisse(
        CollecteJournaliereTerrain collecte,
        CollecteMembreLigne ligne,
        Utilisateur validatedBy,
        String typeOperation
    ) {
        if (collecteOperationGenereeRepository.existsByCollecteIdAndLigneCollecteIdAndTypeOperation(collecte.getId(), ligne.getId(), typeOperation)) {
            auditService.logSuccess(AuditAction.INVALID_OPERATION, "CollecteOperationGeneree", collecte.getId(), "Trace déjà générée " + typeOperation);
            return 0;
        }
        collectOperation(collecte, ligne, typeOperation, ligne.getId(), validatedBy.getId());
        return 1;
    }

    private int generateDemandeCreditTrace(CollecteJournaliereTerrain collecte, CollecteMembreLigne ligne, Utilisateur validatedBy) {
        if (collecteOperationGenereeRepository.existsByCollecteIdAndLigneCollecteIdAndTypeOperation(collecte.getId(), ligne.getId(), "DEMANDE_CREDIT")) {
            auditService.logSuccess(AuditAction.INVALID_OPERATION, "CollecteOperationGeneree", collecte.getId(), "Ligne déjà tracée DEMANDE_CREDIT");
            return 0;
        }

        if (ligne.getDemandeCredit() != null && ligne.getDemandeCredit().getId() != null) {
            collectOperation(collecte, ligne, "DEMANDE_CREDIT", ligne.getDemandeCredit().getId(), validatedBy.getId());
            return 1;
        }

        DemandeCredit demande = buildDemandeCreditFromCollecteLine(collecte, ligne);
        DemandeCredit saved = demandeCreditRepository.save(demande);
        ligne.setDemandeCredit(saved);
        ligneRepository.save(ligne);

        collectOperation(collecte, ligne, "DEMANDE_CREDIT", saved.getId(), validatedBy.getId());
        return 1;
    }

    private DemandeCredit buildDemandeCreditFromCollecteLine(CollecteJournaliereTerrain collecte, CollecteMembreLigne ligne) {
        BigDecimal tauxInteret = parametreMetierService.getDecimal("TAUX_INTERET_CREDIT_MAX");
        if (tauxInteret == null || tauxInteret.compareTo(BigDecimal.ZERO) <= 0) {
            tauxInteret = new BigDecimal("5");
        }

        BigDecimal fraisDemande = safeAmount(ligne.getMontant());
        if (fraisDemande.compareTo(BigDecimal.ZERO) <= 0) {
            fraisDemande = parametreMetierService.getDecimal("FRAIS_ANALYSE_DEMANDE");
        }
        if (fraisDemande == null || fraisDemande.compareTo(BigDecimal.ZERO) < 0) {
            fraisDemande = BigDecimal.ZERO;
        }

        BigDecimal montantDemande = safeAmount(ligne.getMontantSouhaite());
        BigDecimal depotGarantieRequis = montantDemande.multiply(new BigDecimal("0.20"));

        return DemandeCredit.builder()
            .numeroDemande("DEMANDE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
            .membre(ligne.getMembre())
            .site(collecte.getSite())
            .agent(collecte.getAgentTerrain())
            .dateDemande(LocalDate.now())
            .montantDemande(montantDemande)
            .fraisDemandePayes(BigDecimal.ZERO)
            .devise("CDF")
            .dureeValeur(resolveDureeValeur(ligne))
            .dureeUnite(resolveDureeUnite(ligne))
            .periodiciteRemboursement(mapPeriodicite(ligne.getModaliteRemboursement()))
            .tauxInteret(tauxInteret)
            .objetCredit(ligne.getObjetCredit())
            .gagePropose(cleanText(ligne.getGagePropose()))
            .activiteFinancee(cleanText(ligne.getCommentaire()))
            .revenusEstimes(BigDecimal.ZERO)
            .chargesEstimees(BigDecimal.ZERO)
            .fraisDemande(fraisDemande)
            .depotGarantieRequis(depotGarantieRequis)
            .depotGarantiePaye(BigDecimal.ZERO)
            .statut(StatutDemandeCredit.SOUMISE)
            .build();
    }

    private Integer resolveDureeValeur(CollecteMembreLigne ligne) {
        return ligne.getDureeValeur() != null && ligne.getDureeValeur() > 0 ? ligne.getDureeValeur() : 1;
    }

    private DureeUnite resolveDureeUnite(CollecteMembreLigne ligne) {
        return ligne.getDureeUnite() != null ? ligne.getDureeUnite() : DureeUnite.MOIS;
    }

    private PeriodiciteRemboursement mapPeriodicite(ModaliteRemboursementCollecte modalite) {
        if (modalite == null) {
            return PeriodiciteRemboursement.MENSUEL;
        }

        return switch (modalite) {
            case JOURNALIERE -> PeriodiciteRemboursement.JOURNALIER;
            case HEBDOMADAIRE -> PeriodiciteRemboursement.HEBDOMADAIRE;
            case MENSUELLE -> PeriodiciteRemboursement.MENSUEL;
        };
    }

    private List<Credit> findCreditsActifs(Long membreId) {
        return creditRepository.findByMembreId(membreId)
            .stream()
            .filter(value -> value.getStatut() != null && STATUTS_CREDIT_ACTIF.contains(value.getStatut()))
            .toList();
    }

    private BigDecimal getPrixCarnet() {
        BigDecimal configured = parametreMetierService.getDecimal("FRAIS_CARNET_EPARGNE");
        if (configured == null || configured.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Le paramètre métier FRAIS_CARNET_EPARGNE est obligatoire");
        }
        return configured;
    }

    private int generateCarnetCaisseOperation(
        CollecteJournaliereTerrain collecte,
        CollecteMembreLigne ligne,
        Long sessionId,
        Utilisateur validatedBy
    ) {
        if (collecteOperationGenereeRepository.existsByCollecteIdAndLigneCollecteIdAndTypeOperation(collecte.getId(), ligne.getId(), "CARNET_CAISSE")) {
            auditService.logSuccess(AuditAction.INVALID_OPERATION, "CollecteOperationGeneree", collecte.getId(), "Carnet caisse déjà généré");
            return 0;
        }

        var session = sessionCaisseRepository.findById(sessionId).orElseThrow(() -> new RuntimeException("Session caisse introuvable"));
        OperationCaisseRequest request = new OperationCaisseRequest();
        request.setSessionCaisseId(sessionId);
        request.setCaisseId(session.getCaisse().getId());
        request.setDateOperation(LocalDateTime.now());
        request.setTypeOperation(com.mini.credit.enums.TypeOperationCaisse.ENTREE);
        request.setCategorieOperation(CategorieOperationCaisse.ENTREE_DIVERSE);
        request.setMontant(safeAmount(ligne.getMontant()));
        request.setDevise("CDF");
        request.setMembreId(requireNonNullId(ligne.getMembre(), "membreId manquant"));
        request.setAgentId(requireNonNullId(collecte.getAgentTerrain(), "agentTerrainId manquant"));
        request.setDescription("Frais carnet épargne - adhésion membre #" + requireNonNullId(ligne.getMembre(), "membreId manquant"));
        request.setObservation(ligne.getCommentaire());
        request.setCreatedBy(validatedBy.getId());
        request.setModePaiement(ModePaiement.ESPECES);
        request.setSource(SourceOperationCaisse.RECETTE_JOURNALIERE);
        request.setRecetteId(collecte.getId());
        request.setReferenceMetier("CARNET-" + collecte.getId() + "-" + ligne.getId());

        var response = operationCaisseService.enregistrerDepuisCollecteValidee(request);
        collectOperation(collecte, ligne, "CARNET_CAISSE", response.getId(), validatedBy.getId());
        return 1;
    }

    private int generateRetourTerrainCaisse(CollecteJournaliereTerrain collecte, List<CollecteMembreLigne> lignes, Long sessionId, Utilisateur validatedBy) {
        if (collecteOperationGenereeRepository.existsByCollecteIdAndTypeOperation(collecte.getId(), "RETOUR_TERRAIN_CAISSE")) {
            auditService.logSuccess(AuditAction.INVALID_OPERATION, "CollecteOperationGeneree", collecte.getId(), "Retour terrain déjà généré");
            return 0;
        }

        BigDecimal retourAmount = lignes.stream()
            .filter(ligne -> ligne.getTypeLigne() == TypeLigneCollecte.EPARGNE || ligne.getTypeLigne() == TypeLigneCollecte.REMBOURSEMENT_CREDIT)
            .map(ligne -> safeAmount(ligne.getMontant()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (retourAmount.compareTo(BigDecimal.ZERO) <= 0) {
            auditService.logSuccess(AuditAction.INVALID_OPERATION, "CollecteOperationGeneree", collecte.getId(), "Retour terrain non généré: montant nul");
            return 0;
        }

        var session = sessionCaisseRepository.findById(sessionId).orElseThrow(() -> new RuntimeException("Session caisse introuvable"));
        OperationCaisseRequest request = new OperationCaisseRequest();
        request.setSessionCaisseId(sessionId);
        request.setCaisseId(session.getCaisse().getId());
        request.setDateOperation(LocalDateTime.now());
        request.setTypeOperation(com.mini.credit.enums.TypeOperationCaisse.ENTREE);
        request.setCategorieOperation(CategorieOperationCaisse.ENTREE_DIVERSE);
        request.setMontant(retourAmount);
        request.setDevise("CDF");
        request.setAgentId(requireNonNullId(collecte.getAgentTerrain(), "agentTerrainId manquant"));
        request.setDescription("Retour terrain collecte #" + collecte.getId() + " site=" + (collecte.getSite() != null ? collecte.getSite().getId() : null) + " antenne=" + collecte.getAntenneId());
        request.setCreatedBy(validatedBy.getId());
        request.setModePaiement(ModePaiement.ESPECES);
        request.setSource(SourceOperationCaisse.RECETTE_JOURNALIERE);
        request.setRecetteId(collecte.getId());
        request.setReferenceMetier("RECETTE-" + collecte.getId());

        var response = operationCaisseService.enregistrerDepuisCollecteValidee(request);
        collectOperation(collecte, null, "RETOUR_TERRAIN_CAISSE", response.getId(), validatedBy.getId());
        return 1;
    }

    private void collectOperation(CollecteJournaliereTerrain collecte, CollecteMembreLigne ligne, String type, Long operationId, Long createdBy) {
        CollecteOperationGeneree entry = CollecteOperationGeneree.builder()
            .collecte(collecte)
            .ligneCollecte(ligne)
            .typeOperation(type)
            .operationId(operationId)
            .createdBy(createdBy)
            .build();
        collecteOperationGenereeRepository.save(entry);
        auditService.logSuccess(
            AuditAction.OPERATION_CAISSE_CREATED,
            "CollecteOperationGeneree",
            entry.getId(),
            "Operation générée type=" + type + " opId=" + operationId + " collecte=" + collecte.getId()
        );
    }

    private Long getOpenedSessionIdOrThrow() {
        return sessionCaisseRepository
            .findFirstByStatutOrderByDateOuvertureDesc(com.mini.credit.enums.StatutSessionCaisse.OUVERTE)
            .map(s -> s.getId())
            .orElseThrow(() -> new RuntimeException("Aucune session de caisse ouverte pour la génération officielle"));
    }

    private Long requireNonNullId(Object entity, String message) {
        if (entity == null) {
            throw new RuntimeException(message);
        }
        if (entity instanceof Membre m && m.getId() != null) return m.getId();
        if (entity instanceof AgentTerrain a && a.getId() != null) return a.getId();
        if (entity instanceof CompteEpargne c && c.getId() != null) return c.getId();
        if (entity instanceof Credit c && c.getId() != null) return c.getId();
        throw new RuntimeException(message);
    }

    private boolean canViewByRole(Utilisateur user, CollecteJournaliereTerrain collecte) {
        if (hasRole(user, "ADMIN")) {
            return true;
        }

        if (hasRole(user, "AGENT_TERRAIN")) {
            return agentTerrainRepository.findByUtilisateurId(user.getId())
                .map(a -> Objects.equals(a.getId(), collecte.getAgentTerrain().getId()))
                .orElse(false);
        }

        Long currentAntenne = user.getEmploye() != null && user.getEmploye().getAgence() != null ? user.getEmploye().getAgence().getId() : null;

        if (hasRole(user, "CAISSIER")) {
            return collecte.getStatut() == RecetteStatut.SOUMISE && currentAntenne != null && Objects.equals(collecte.getAntenneId(), currentAntenne);
        }

        if (hasRole(user, "CONTROLEUR")) {
            boolean allowedStatus = collecte.getStatut() == RecetteStatut.SOUMISE
                || collecte.getStatut() == RecetteStatut.VALIDEE
                || collecte.getStatut() == RecetteStatut.REJETEE;
            return allowedStatus && currentAntenne != null && Objects.equals(collecte.getAntenneId(), currentAntenne);
        }

        if (hasRole(user, "CHEF_BUREAU") || hasRole(user, "RCI")) {
            return currentAntenne != null && Objects.equals(collecte.getAntenneId(), currentAntenne);
        }

        if (hasRole(user, "GESTIONNAIRE")) {
            Employe employe = user.getEmploye();
            if (employe == null || employe.getId() == null) return false;
            Set<Long> supervisedAgentIds = agentTerrainRepository.findByGestionnaireIdAndActifTrue(employe.getId())
                .stream()
                .map(AgentTerrain::getId)
                .collect(Collectors.toSet());
            return supervisedAgentIds.contains(collecte.getAgentTerrain().getId());
        }

        return false;
    }

    private boolean hasRole(Utilisateur user, String roleCode) {
        return user != null
            && user.getRole() != null
            && user.getRole().getCode() != null
            && roleCode.equalsIgnoreCase(user.getRole().getCode().name());
    }

    private Utilisateur getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            throw new RuntimeException("Utilisateur non authentifié");
        }
        return utilisateurRepository.findByUsername(authentication.getName())
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
    }

    private AgentTerrain getCurrentAgent() {
        Utilisateur user = getCurrentUser();
        AgentTerrain agent = agentTerrainRepository.findByUtilisateurId(user.getId())
            .orElseThrow(() -> new RuntimeException("Profil AGENT_TERRAIN introuvable"));

        if (agent.getSite() == null || agent.getSite().getId() == null) {
            throw new RuntimeException("Aucun site affecté à votre compte");
        }
        return agent;
    }

    private void assertCanValidate() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            throw new RuntimeException("Utilisateur non authentifié");
        }
        boolean isAllowed = authentication.getAuthorities().stream()
            .map(a -> a.getAuthority())
            .anyMatch(a -> "ROLE_ADMIN".equals(a) || "ROLE_CONTROLEUR".equals(a));
        if (!isAllowed) {
            throw new RuntimeException("Seuls ADMIN/CONTROLEUR peuvent valider");
        }
    }

    private void assertCanViewCollecte(CollecteJournaliereTerrain collecte) {
        Utilisateur user = getCurrentUser();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        boolean isAgent = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_AGENT_TERRAIN".equals(a.getAuthority()));
        if (isAgent) {
            AgentTerrain agent = agentTerrainRepository.findByUtilisateurId(user.getId())
                .orElseThrow(() -> new RuntimeException("Profil AGENT_TERRAIN introuvable"));
            if (!Objects.equals(agent.getId(), collecte.getAgentTerrain().getId())) {
                throw new RuntimeException("Accès refusé");
            }
        }
    }

    private BigDecimal safeAmount(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String cleanText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
