package com.mini.credit.service.impl;

import com.mini.credit.dto.credit.AnalyseRisqueRequest;
import com.mini.credit.dto.credit.AnalyseRisqueCreditResponse;
import com.mini.credit.dto.credit.CritereAnalyseRisqueDto;
import com.mini.credit.dto.credit.DemandeCreditCreateRequest;
import com.mini.credit.dto.credit.DemandeCreditResponse;
import com.mini.credit.dto.credit.FraisCreditAEncaisserResponse;
import com.mini.credit.dto.credit.PreAnalyseRequest;
import com.mini.credit.dto.garantie.BloquerGarantieEpargneRequest;
import com.mini.credit.entity.credit.AnalyseRisque;
import com.mini.credit.entity.credit.DemandeCredit;
import com.mini.credit.entity.credit.GarantieCredit;
import com.mini.credit.entity.epargne.CompteEpargne;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.AgentTerrain;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.DureeUnite;
import com.mini.credit.enums.NiveauRisque;
import com.mini.credit.enums.RecommandationRisque;
import com.mini.credit.enums.StatutCompte;
import com.mini.credit.enums.StatutGarantieCredit;
import com.mini.credit.enums.StatutDemandeCredit;
import com.mini.credit.enums.StatutMembre;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.exception.ResourceNotFoundException;
import com.mini.credit.mapper.CreditMapper;
import com.mini.credit.repository.agentTerrain.AgentTerrainRepository;
import com.mini.credit.repository.credit.AnalyseRisqueRepository;
import com.mini.credit.repository.credit.DemandeCreditRepository;
import com.mini.credit.repository.credit.GarantieCreditRepository;
import com.mini.credit.repository.epargne.CompteEpargneRepository;
import com.mini.credit.repository.membre.MembreRepository;
import com.mini.credit.repository.referentiel.SiteRepository;
import com.mini.credit.repository.utilisateur.UtilisateurRepository;
import com.mini.credit.service.DemandeCreditService;
import com.mini.credit.service.CreditValidationService;
import com.mini.credit.service.GarantieCreditWorkflowService;
import com.mini.credit.service.ParametreMetierService;
import com.mini.credit.service.WorkflowTaskService;
import com.mini.credit.service.audit.AuditService;
import com.mini.credit.service.audit.Auditable;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.service.security.ScopeService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class DemandeCreditServiceImpl implements DemandeCreditService {

    private static final BigDecimal TAUX_DEPOT_GARANTIE = new BigDecimal("0.20");
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final DemandeCreditRepository demandeCreditRepository;
    private final MembreRepository membreRepository;
    private final SiteRepository siteRepository;
    private final AgentTerrainRepository agentTerrainRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AnalyseRisqueRepository analyseRisqueRepository;
    private final GarantieCreditRepository garantieCreditRepository;
    private final CompteEpargneRepository compteEpargneRepository;
    private final CreditMapper creditMapper;
    private final ScopeService scopeService;
    private final CreditValidationService creditValidationService;
    private final GarantieCreditWorkflowService garantieCreditWorkflowService;
    private final WorkflowTaskService workflowTaskService;
    private final ParametreMetierService parametreMetierService;
    private final AuditService auditService;

    @Value("${credit.risk.threshold.mediumMin:40}")
    private BigDecimal scoreMediumMin;

    @Value("${credit.risk.threshold.lowMin:70}")
    private BigDecimal scoreLowMin;

    @Value("${credit.risk.weights.activiteVerifiee:10}")
    private BigDecimal poidsActiviteVerifiee;

    @Value("${credit.risk.weights.capaciteRemboursement:30}")
    private BigDecimal poidsCapaciteRemboursement;

    @Value("${credit.risk.weights.ratioCharges:10}")
    private BigDecimal poidsRatioCharges;

    @Value("${credit.risk.weights.montantRevenu:20}")
    private BigDecimal poidsMontantRevenu;

    @Value("${credit.risk.weights.garantie:20}")
    private BigDecimal poidsGarantie;

    @Value("${credit.risk.weights.visite:10}")
    private BigDecimal poidsVisite;

    @Override
    @Auditable(action = AuditAction.DEMANDE_CREDIT_CREATED, entityType = "DemandeCredit")
    public DemandeCreditResponse create(DemandeCreditCreateRequest request) {
        Membre membre = membreRepository.findById(request.getMembreId())
                .orElseThrow(() -> new ResourceNotFoundException("Membre introuvable"));

        if (membre.getStatut() != StatutMembre.ACTIF) {
            throw new BusinessException(
                    "Impossible de créer une demande de crédit pour un membre non actif (statut : " + membre.getStatut() + ")"
            );
        }

        Site site = siteRepository.findById(request.getSiteId())
                .orElseThrow(() -> new ResourceNotFoundException("Site introuvable"));

        AgentTerrain agent = null;
        if (request.getAgentId() != null) {
            agent = agentTerrainRepository.findById(request.getAgentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Agent introuvable"));
        }

        BigDecimal montantDemande = normalizeMoney(request.getMontantDemande());
        BigDecimal tauxInteret = normalizeMoney(request.getTauxInteret());
        BigDecimal revenusEstimes = normalizeMoney(request.getRevenusEstimes());
        BigDecimal chargesEstimees = normalizeMoney(request.getChargesEstimees());
        BigDecimal fraisDemande = resolveFraisDemande(request.getFraisDemande());

        // AJOUT PHASE 2: Valider la scope d'accès (GESTIONNAIRE limité à son site, MEMBER pour soi-même)
        if (!scopeService.canCreateDemandeCredit(request.getMembreId())) {
            throw new BusinessException("Accès refusé: vous n'êtes pas autorisé à créer une demande de crédit pour ce membre");
        }

        // AJOUT PHASE 3A: Validation des plages de durée
        Integer dureeValeur = request.getDureeValeur();
        if (dureeValeur == null || dureeValeur < 1 || dureeValeur > 60) {
            throw new BusinessException("La durée doit être entre 1 et 60 (peu importe l'unité: jour, semaine, mois)");
        }

        validateCreationRequest(
                montantDemande,
                tauxInteret,
                revenusEstimes,
                chargesEstimees,
                fraisDemande
        );

        BigDecimal depotGarantieRequis = calculerDepotGarantieRequis(montantDemande);

        DemandeCredit demande = DemandeCredit.builder()
                .numeroDemande(genererNumeroDemande())
                .membre(membre)
                .site(site)
                .agent(agent)
                .dateDemande(LocalDate.now())
                .montantDemande(montantDemande)
                .fraisDemandePayes(ZERO)
                .devise(normalizeDevise(request.getDevise()))
                .dureeValeur(request.getDureeValeur())
                .dureeUnite(request.getDureeUnite())
                .periodiciteRemboursement(request.getPeriodiciteRemboursement())
                .tauxInteret(tauxInteret)
                .objetCredit(cleanRequiredText(request.getObjetCredit(), "L'objet du crédit est obligatoire"))
                .gagePropose(cleanNullableText(request.getGagePropose()))
                .activiteFinancee(cleanNullableText(request.getActiviteFinancee()))
                .revenusEstimes(revenusEstimes)
                .chargesEstimees(chargesEstimees)
                .fraisDemande(fraisDemande)
                .depotGarantieRequis(depotGarantieRequis)
                .depotGarantiePaye(ZERO)
                .statut(StatutDemandeCredit.SOUMISE)
                .build();

        DemandeCredit saved = demandeCreditRepository.save(demande);
        workflowTaskService.onDemandeCreditSoumise(
            saved.getId(),
            saved.getNumeroDemande(),
            resolveAntenneId(saved),
            resolveSiteId(saved)
        );
        return creditMapper.toResponse(saved);
    }

    @Override
    public DemandeCreditResponse getById(Long id) {
        DemandeCredit demande = demandeCreditRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));

        return enrichGarantieState(creditMapper.toResponse(demande));
    }

    @Override
    public Page<DemandeCreditResponse> getAll(Pageable pageable) {
        Page<DemandeCredit> pageDemande = demandeCreditRepository.findAll(pageable);
        Map<Long, GarantieCredit> garanties = loadGarantiesMap(pageDemande.getContent());

        return pageDemande.map(demande -> enrichGarantieState(creditMapper.toResponse(demande), garanties.get(demande.getId())));
    }

    @Override
    public List<DemandeCreditResponse> getAll() {
        List<DemandeCredit> demandes = demandeCreditRepository.findAll();
        Map<Long, GarantieCredit> garanties = loadGarantiesMap(demandes);

        return demandes.stream()
            .map(demande -> enrichGarantieState(creditMapper.toResponse(demande), garanties.get(demande.getId())))
            .toList();
    }

    @Override
    public List<DemandeCreditResponse> getByMembre(Long membreId) {
        List<DemandeCredit> demandes = demandeCreditRepository.findByMembreId(membreId);
        Map<Long, GarantieCredit> garanties = loadGarantiesMap(demandes);

        return demandes.stream()
            .map(demande -> enrichGarantieState(creditMapper.toResponse(demande), garanties.get(demande.getId())))
            .toList();
    }

            @Override
            public List<FraisCreditAEncaisserResponse> getFraisCreditAEncaisser() {
            return demandeCreditRepository.findFraisCreditAEncaisser().stream()
                .map(this::toFraisCreditAEncaisserResponse)
                .toList();
            }

    @Override
    public Page<DemandeCreditResponse> getByMembre(Long membreId, Pageable pageable) {
        // PHASE 3B: Return paginated list of credit requests for a specific member
        // Filtering in-memory since repository doesn't have findByMembreId(Long, Pageable)
        List<DemandeCredit> demandes = demandeCreditRepository.findByMembreId(membreId);
        Map<Long, GarantieCredit> garanties = loadGarantiesMap(demandes);
        List<DemandeCreditResponse> filtered = demandes.stream()
            .map(demande -> enrichGarantieState(creditMapper.toResponse(demande), garanties.get(demande.getId())))
            .toList();
        
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filtered.size());
        List<DemandeCreditResponse> page = filtered.subList(start, end);
        
        return new org.springframework.data.domain.PageImpl<>(page, pageable, filtered.size());
    }

    @Override
    @Auditable(action = AuditAction.ANALYSE_RISQUE_CREATED, entityType = "DemandeCredit", entityIdParameter = "demandeId")
    public DemandeCreditResponse ajouterAnalyse(Long demandeId, AnalyseRisqueRequest request) {
        DemandeCredit demande = demandeCreditRepository.findById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));

        if (demande.getStatut() != StatutDemandeCredit.EN_ANALYSE) {
            throw new BusinessException("Analyse de risque autorisée uniquement au statut EN_ANALYSE");
        }

        if (!garantieCreditWorkflowService.isGarantieBloquee(demandeId)) {
            throw new BusinessException("Garantie insuffisante. Le membre doit compléter son épargne.");
        }

        validateAnalyseRequest(request);

        Utilisateur utilisateurCourant = getCurrentAuthenticatedUser();
        if (utilisateurCourant == null) {
            throw new BusinessException("Utilisateur connecté introuvable pour enregistrer l'analyse");
        }

        RoleCode roleCode = utilisateurCourant != null && utilisateurCourant.getRole() != null
                ? utilisateurCourant.getRole().getCode()
                : null;

        if (roleCode != null && roleCode != RoleCode.ADMIN && roleCode != RoleCode.CONTROLEUR) {
            throw new BusinessException("Seul un CONTROLEUR (ou ADMIN) peut enregistrer une analyse de risque");
        }

        if (roleCode == RoleCode.CONTROLEUR && utilisateurCourant.getEmploye() == null) {
            throw new BusinessException("Le CONTROLEUR connecté doit être rattaché à un employé pour enregistrer l'analyse");
        }

        Utilisateur analyste = utilisateurCourant;

        AnalyseRisque analyse = demande.getAnalyseRisque();
        if (analyse == null) {
            analyse = new AnalyseRisque();
            analyse.setDemandeCredit(demande);
            analyse.setAnalyseId(genererAnalyseIdUnique(demandeId));
        } else if (analyse.getAnalyseId() == null || analyse.getAnalyseId().isBlank()) {
            analyse.setAnalyseId(genererAnalyseIdUnique(demandeId));
        }

        String deviseDemande = normalizeDevise(demande.getDevise());
        BigDecimal scoreCalcule = calculerScoreRisque(request, demande);
        boolean scoreCorrigeManuellement = Boolean.TRUE.equals(request.getScoreRisqueCorrigeManuellement());
        BigDecimal scoreFinal = scoreCorrigeManuellement
                ? normalizeScore(request.getScoreRisque())
                : scoreCalcule;

        NiveauRisque niveauFinal = scoreCorrigeManuellement && request.getRisqueNiveau() != null
                ? request.getRisqueNiveau()
                : determinerNiveauRisque(scoreFinal);

        if (scoreFinal != null) {
            NiveauRisque niveauDerive = determinerNiveauRisque(scoreFinal);
            if (niveauDerive != niveauFinal) {
                throw new BusinessException("Incohérence score/niveau: le niveau ne correspond pas au score calculé");
            }
        }

        String commentaire = cleanNullableText(request.getCommentaire());
        validateCommentaireObligatoire(request.getRecommandation(), niveauFinal, commentaire, request.getScoreRisqueCorrigeManuellement());

        analyse.setAnalyste(analyste);
        analyse.setDateVisite(request.getDateVisite());
        analyse.setLieuVisite(cleanRequiredText(request.getLieuVisite(), "Le lieu visité est obligatoire"));
        analyse.setActiviteVerifiee(request.getActiviteVerifiee());
        analyse.setDescriptionActivite(cleanNullableText(request.getDescriptionActivite()));
        analyse.setAncienneteActivite(request.getAncienneteActivite());
        analyse.setChiffreAffairesEstime(normalizeMoney(request.getChiffreAffairesEstime()));
        analyse.setChiffreAffairesDevise(resolveDevise(request.getChiffreAffairesDevise(), deviseDemande));
        analyse.setRevenuNetEstime(normalizeMoney(request.getRevenuNetEstime()));
        analyse.setRevenuNetDevise(resolveDevise(request.getRevenuNetDevise(), deviseDemande));
        analyse.setChargesMensuelles(normalizeMoney(request.getChargesMensuelles()));
        analyse.setChargesMensuellesDevise(resolveDevise(request.getChargesMensuellesDevise(), deviseDemande));
        analyse.setCapaciteRemboursement(normalizeMoney(request.getCapaciteRemboursement()));
        analyse.setCapaciteRemboursementDevise(resolveDevise(request.getCapaciteRemboursementDevise(), deviseDemande));
        analyse.setMontantDemandeDevise(resolveDevise(request.getMontantDemandeDevise(), deviseDemande));
        analyse.setFraisDemandeDevise(resolveDevise(request.getFraisDemandeDevise(), deviseDemande));
        analyse.setDepotRequisDevise(resolveDevise(request.getDepotRequisDevise(), deviseDemande));
        analyse.setDepotPayeDevise(resolveDevise(request.getDepotPayeDevise(), deviseDemande));
        analyse.setRisqueNiveau(niveauFinal);
        analyse.setScoreRisque(scoreFinal);
        analyse.setScoreRisqueCorrigeManuellement(scoreCorrigeManuellement);
        analyse.setRecommandation(request.getRecommandation());
        analyse.setCommentaire(commentaire);

        analyseRisqueRepository.save(analyse);

        demande.setAnalyseRisque(analyse);
        demande.setStatut(StatutDemandeCredit.EN_ANALYSE);

        String siteInfo = demande.getSite() != null ? demande.getSite().getNomSite() : "SITE_INCONNU";
        auditService.logSuccess(
                AuditAction.ANALYSE_RISQUE_CREATED,
                "DemandeCredit",
                demandeId,
                "Analyse enregistrée | demandeId=" + demandeId
                        + " | analyseId=" + analyse.getAnalyseId()
                        + " | score=" + (scoreFinal == null ? "NON_CALCULE" : scoreFinal)
                        + " | niveau=" + niveauFinal
                        + " | recommandation=" + request.getRecommandation()
                        + " | role=" + (roleCode == null ? "UNKNOWN" : roleCode)
                        + " | site=" + siteInfo
                        + " | user=" + analyste.getUsername()
                        + " | dateHeure=" + LocalDateTime.now()
                        + (commentaire == null ? "" : " | commentaire=" + commentaire)
        );

        DemandeCredit saved = demandeCreditRepository.save(demande);
        return enrichGarantieState(creditMapper.toResponse(saved));
    }

    @Override
    public DemandeCreditResponse preAnalyser(Long demandeId, String commentaire) {
        PreAnalyseRequest request = new PreAnalyseRequest();
        request.setAction(PreAnalyseRequest.Action.TRANSMETTRE_ANALYSE);
        request.setCommentaire(commentaire);
        request.setDossierComplet(Boolean.TRUE);
        return preAnalyserDecision(demandeId, request);
    }

    @Override
    public DemandeCreditResponse preAnalyserDecision(Long demandeId, PreAnalyseRequest request) {
        DemandeCredit demande = demandeCreditRepository.findById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));

        if (demande.getStatut() != StatutDemandeCredit.SOUMISE) {
            throw new BusinessException("Pré-analyse autorisée uniquement pour une demande SOUMISE");
        }

        if (request == null || request.getAction() == null) {
            throw new BusinessException("La décision de pré-analyse est obligatoire");
        }

        String commentairePreAnalyse = cleanRequiredText(request.getCommentaire(), "Le commentaire de pré-analyse est obligatoire");
        Utilisateur decideur = getCurrentAuthenticatedUser();
        LocalDateTime maintenant = LocalDateTime.now();

        if (request.getAction() == PreAnalyseRequest.Action.RETOUR_COMPLEMENT) {
            demande.setCommentaireDecision("PRE-ANALYSE RETOUR COMPLEMENT: " + commentairePreAnalyse);
            demande.setDateDecision(maintenant);
            demande.setDecidedBy(decideur);
            return creditMapper.toResponse(demandeCreditRepository.save(demande));
        }

        if (!Boolean.TRUE.equals(request.getDossierComplet())) {
            throw new BusinessException("Dossier incomplet: utilisez l'action RETOUR_COMPLEMENT");
        }

        BloquerGarantieEpargneRequest blocageRequest = BloquerGarantieEpargneRequest.builder()
                .commentaire("Pré-analyse validée: " + commentairePreAnalyse)
                .build();
        garantieCreditWorkflowService.bloquerEpargne(demandeId, blocageRequest);

        demande.setStatut(StatutDemandeCredit.EN_ANALYSE);
        demande.setCommentaireDecision("PRE-ANALYSE VALIDEE: " + commentairePreAnalyse);
        demande.setDateDecision(maintenant);
        demande.setDecidedBy(decideur);

        DemandeCredit saved = demandeCreditRepository.save(demande);
        workflowTaskService.onDemandeCreditPreAnalyseValidee(
            saved.getId(),
            saved.getNumeroDemande(),
            resolveAntenneId(saved),
            resolveSiteId(saved)
        );
        return enrichGarantieState(creditMapper.toResponse(saved));
    }

    @Override
    public DemandeCreditResponse enregistrerObservationRisque(Long demandeId, String commentaire) {
        DemandeCredit demande = demandeCreditRepository.findById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));

        if (demande.getStatut() != StatutDemandeCredit.EN_ANALYSE) {
            throw new BusinessException("L'observation risque exige une demande EN_ANALYSE");
        }

        if (demande.getAnalyseRisque() == null) {
            throw new BusinessException("Impossible d'enregistrer une observation sans analyse de risque");
        }

        String observation = cleanRequiredText(commentaire, "Le commentaire d'observation est obligatoire");
        demande.setCommentaireDecision(observation);
        demande.setDateDecision(java.time.LocalDateTime.now());
        demande.setDecidedBy(getCurrentAuthenticatedUser());

        DemandeCredit saved = demandeCreditRepository.save(demande);
        return enrichGarantieState(creditMapper.toResponse(saved));
    }

    @Override
    public DemandeCreditResponse validerAnalyseRisque(Long demandeId, String commentaire) {
        DemandeCredit demande = demandeCreditRepository.findById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));

        if (demande.getStatut() != StatutDemandeCredit.EN_ANALYSE) {
            throw new BusinessException("La validation risque exige une demande EN_ANALYSE");
        }

        creditValidationService.verifierFraisDemandeIntegralementPayes(demande);

        if (!garantieCreditWorkflowService.isGarantieBloquee(demandeId)) {
            throw new BusinessException("Garantie non bloquée — analyse impossible");
        }

        AnalyseRisque analyse = demande.getAnalyseRisque();
        if (analyse == null) {
            throw new BusinessException("Impossible de valider le risque sans analyse de risque");
        }

        if (!isAnalyseRisqueComplete(analyse)) {
            throw new BusinessException("Validation analyse risque impossible: analyse incomplète");
        }

        String validationCommentaire = cleanRequiredText(commentaire, "Le commentaire de validation est obligatoire");
        demande.setStatut(StatutDemandeCredit.ANALYSE_TERRAIN_VALIDEE);
        demande.setCommentaireDecision(validationCommentaire);
        demande.setDateDecision(java.time.LocalDateTime.now());
        demande.setDecidedBy(getCurrentAuthenticatedUser());

        DemandeCredit saved = demandeCreditRepository.save(demande);
        workflowTaskService.onDemandeCreditAnalyseTerrainValidee(
            saved.getId(),
            saved.getNumeroDemande(),
            resolveAntenneId(saved),
            resolveSiteId(saved)
        );
        return enrichGarantieState(creditMapper.toResponse(saved));
    }

    @Override
    public DemandeCreditResponse controlerRisque(Long demandeId, String commentaire) {
        return validerAnalyseRisque(demandeId, commentaire);
    }

    @Override
    public DemandeCreditResponse controlerGarantie(Long demandeId, String commentaire) {
        DemandeCredit demande = demandeCreditRepository.findById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));

        if (demande.getStatut() != StatutDemandeCredit.ANALYSE_TERRAIN_VALIDEE) {
            throw new BusinessException("Le contrôle garantie exige une demande ANALYSE_TERRAIN_VALIDEE");
        }

        creditValidationService.verifierFraisDemandeIntegralementPayes(demande);

        if (!garantieCreditWorkflowService.isGarantieValidee(demandeId)) {
            throw new BusinessException("La garantie doit être validée avant le passage en validation chef");
        }

        if (normalizeMoney(demande.getDepotGarantiePaye()).compareTo(normalizeMoney(demande.getDepotGarantieRequis())) < 0) {
            throw new BusinessException("Garantie insuffisante: 20% requis avant passage en validation chef");
        }

        demande.setStatut(StatutDemandeCredit.VALIDATION_CHEF);
        demande.setCommentaireDecision(cleanNullableText(commentaire));
        demande.setDateDecision(java.time.LocalDateTime.now());
        demande.setDecidedBy(getCurrentAuthenticatedUser());

        DemandeCredit saved = demandeCreditRepository.save(demande);
        workflowTaskService.onDemandeCreditValidationChef(
            saved.getId(),
            saved.getNumeroDemande(),
            resolveAntenneId(saved),
            resolveSiteId(saved)
        );
        return enrichGarantieState(creditMapper.toResponse(saved));
    }

    @Override
    public DemandeCreditResponse rejeter(Long demandeId, String commentaire) {
        DemandeCredit demande = demandeCreditRepository.findById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));

        if (demande.getStatut() == StatutDemandeCredit.APPROUVEE
                || demande.getStatut() == StatutDemandeCredit.ANNULEE
                || demande.getStatut() == StatutDemandeCredit.REJETEE) {
            throw new BusinessException("Cette demande ne peut plus être rejetée à ce stade");
        }

        String motif = cleanRequiredText(commentaire, "Le motif de rejet est obligatoire");
        demande.setStatut(StatutDemandeCredit.REJETEE);
        demande.setCommentaireDecision(motif);
        demande.setDateDecision(java.time.LocalDateTime.now());
        demande.setDecidedBy(getCurrentAuthenticatedUser());

        DemandeCredit saved = demandeCreditRepository.save(demande);
        workflowTaskService.onDemandeCreditRejetee(
            saved.getId(),
            saved.getNumeroDemande(),
            resolveAntenneId(saved),
            resolveSiteId(saved)
        );
        return enrichGarantieState(creditMapper.toResponse(saved));
    }

    private void validateCreationRequest(BigDecimal montantDemande,
                                         BigDecimal tauxInteret,
                                         BigDecimal revenusEstimes,
                                         BigDecimal chargesEstimees,
                                         BigDecimal fraisDemande) {

        if (montantDemande == null || montantDemande.compareTo(ZERO) <= 0) {
            throw new BusinessException("Le montant demandé doit être supérieur à 0");
        }

        // AJOUT PHASE 3A: Validation des plages de montant
        if (montantDemande.compareTo(BigDecimal.valueOf(1)) < 0) {
            throw new BusinessException("Le montant demandé doit être >= 1 CDF");
        }
        if (montantDemande.compareTo(BigDecimal.valueOf(100_000_000)) > 0) {
            throw new BusinessException("Le montant demandé ne peut pas dépasser 100 millions CDF");
        }

        if (tauxInteret == null || tauxInteret.compareTo(ZERO) < 0) {
            throw new BusinessException("Le taux d'intérêt ne peut pas être négatif");
        }

        // AJOUT PHASE 3A: Validation des plages de taux
        if (tauxInteret.compareTo(BigDecimal.valueOf(20)) > 0) {
            throw new BusinessException("Le taux d'intérêt ne peut pas dépasser 20%");
        }

        if (revenusEstimes == null || revenusEstimes.compareTo(ZERO) < 0) {
            throw new BusinessException("Les revenus estimés ne peuvent pas être négatifs");
        }

        if (chargesEstimees == null || chargesEstimees.compareTo(ZERO) < 0) {
            throw new BusinessException("Les charges estimées ne peuvent pas être négatives");
        }

        if (fraisDemande == null || fraisDemande.compareTo(ZERO) < 0) {
            throw new BusinessException("Les frais de demande ne peuvent pas être négatifs");
        }

        // Validation plafond temporaire: frais ne peuvent pas dépasser 10% du montant
        BigDecimal plafondFrais = montantDemande
                .multiply(new BigDecimal("0.10"))
                .setScale(2, RoundingMode.HALF_UP);

        if (fraisDemande.compareTo(plafondFrais) > 0) {
            throw new BusinessException(
                "Les frais de demande ne peuvent pas dépasser 10% du montant demandé (" +
                "montant: " + montantDemande + ", plafond frais: " + plafondFrais + ")"
            );
        }
    }

    private BigDecimal calculerDepotGarantieRequis(BigDecimal montantDemande) {
        return montantDemande
                .multiply(TAUX_DEPOT_GARANTIE)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return value == null
                ? ZERO
                : value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveFraisDemande(BigDecimal fraisDemandeSaisis) {
        if (fraisDemandeSaisis != null) {
            return normalizeMoney(fraisDemandeSaisis);
        }

        BigDecimal fraisParDefaut = parametreMetierService.getDecimal("FRAIS_ANALYSE_DEMANDE");
        if (fraisParDefaut == null || fraisParDefaut.compareTo(ZERO) < 0) {
            return ZERO;
        }
        return normalizeMoney(fraisParDefaut);
    }

    private BigDecimal normalizeScore(BigDecimal value) {
        if (value == null) {
            return null;
        }

        BigDecimal normalized = value.setScale(2, RoundingMode.HALF_UP);
        if (normalized.compareTo(BigDecimal.ZERO) < 0 || normalized.compareTo(new BigDecimal("100")) > 0) {
            throw new BusinessException("Le score risque doit être entre 0 et 100");
        }
        return normalized;
    }

    private String normalizeDevise(String devise) {
        if (devise == null || devise.isBlank()) {
            return "CDF";
        }
        return devise.trim().toUpperCase();
    }

    private String resolveDevise(String devise, String fallback) {
        if (devise == null || devise.isBlank()) {
            return normalizeDevise(fallback);
        }
        return normalizeDevise(devise);
    }

    private String cleanRequiredText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    private String cleanNullableText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void validateAnalyseRequest(AnalyseRisqueRequest request) {
        if (request == null) {
            throw new BusinessException("La requête d'analyse est obligatoire");
        }
        if (request.getAnalyseId() != null && !request.getAnalyseId().isBlank()) {
            throw new BusinessException("analyseId est généré automatiquement et ne doit pas être fourni");
        }
        if (request.getDateVisite() == null) {
            throw new BusinessException("La date de visite est obligatoire");
        }
        if (request.getActiviteVerifiee() != null && request.getActiviteVerifiee()
                && (request.getDescriptionActivite() == null || request.getDescriptionActivite().isBlank())) {
            throw new BusinessException("Description activité obligatoire si l'activité est vérifiée");
        }
        if (request.getCapaciteRemboursement() == null) {
            throw new BusinessException("La capacité de remboursement est obligatoire");
        }
        validateMontantNonNegatif(request.getChiffreAffairesEstime(), "chiffre d'affaires estimé");
        validateMontantNonNegatif(request.getRevenuNetEstime(), "revenu net estimé");
        validateMontantNonNegatif(request.getChargesMensuelles(), "charges mensuelles");
        validateMontantNonNegatif(request.getCapaciteRemboursement(), "capacité de remboursement");
        if (request.getRecommandation() == null) {
            throw new BusinessException("La recommandation est obligatoire");
        }
    }

    private void validateMontantNonNegatif(BigDecimal montant, String fieldLabel) {
        if (montant != null && montant.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Le champ " + fieldLabel + " ne peut pas être négatif");
        }
    }

    private BigDecimal calculerScoreRisque(AnalyseRisqueRequest request, DemandeCredit demande) {
        return buildAnalyseRisqueSnapshot(
                normalizeMoney(demande.getMontantDemande()),
                demande.getDureeValeur(),
                demande.getDureeUnite(),
                normalizeMoney(demande.getTauxInteret()),
                normalizeMoney(request.getRevenuNetEstime()),
                normalizeMoney(request.getChargesMensuelles()),
                normalizeMoney(request.getCapaciteRemboursement()),
                normalizeMoney(demande.getDepotGarantieRequis()),
                normalizeMoney(demande.getDepotGarantiePaye()),
                request.getActiviteVerifiee(),
                request.getDateVisite(),
                request.getLieuVisite()
        ).scoreTotal();
    }

    private AnalyseRisqueSnapshot buildAnalyseRisqueSnapshot(BigDecimal montantDemande,
                                                             Integer dureeValeur,
                                                             DureeUnite dureeUnite,
                                                             BigDecimal tauxInteret,
                                                             BigDecimal revenu,
                                                             BigDecimal charges,
                                                             BigDecimal capacite,
                                                             BigDecimal garantieRequise,
                                                             BigDecimal garantieDisponible,
                                                             Boolean activiteVerifiee,
                                                             LocalDate dateVisite,
                                                             String lieuVisite) {
        BigDecimal poidsActivite = defaultIfNull(poidsActiviteVerifiee, new BigDecimal("10"));
        BigDecimal poidsCapacite = defaultIfNull(poidsCapaciteRemboursement, new BigDecimal("30"));
        BigDecimal poidsRatio = defaultIfNull(poidsRatioCharges, new BigDecimal("10"));
        BigDecimal poidsMontant = defaultIfNull(poidsMontantRevenu, new BigDecimal("20"));
        BigDecimal poidsGar = defaultIfNull(poidsGarantie, new BigDecimal("20"));
        BigDecimal poidsVisit = defaultIfNull(poidsVisite, new BigDecimal("10"));

        List<CritereAnalyseRisqueDto> criteres = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        BigDecimal pointsActivite = BigDecimal.ZERO;
        if (Boolean.TRUE.equals(activiteVerifiee)) {
            pointsActivite = poidsActivite;
        }
        total = total.add(pointsActivite);
        criteres.add(critere("ACTIVITE_VERIFIEE", "Activité vérifiée", pointsActivite, poidsActivite,
                pointsActivite.compareTo(BigDecimal.ZERO) > 0 ? "Activité vérifiée sur terrain." : "Activité non vérifiée sur terrain."));

        BigDecimal mensualiteEstimee = calculerMensualiteEstimee(montantDemande, tauxInteret, dureeValeur, dureeUnite);
        BigDecimal ratioMensualiteCapacite = null;
        BigDecimal pointsCapacite = BigDecimal.ZERO;
        if (capacite.compareTo(BigDecimal.ZERO) > 0 && mensualiteEstimee.compareTo(BigDecimal.ZERO) > 0) {
            ratioMensualiteCapacite = mensualiteEstimee.divide(capacite, 4, RoundingMode.HALF_UP);
            if (ratioMensualiteCapacite.compareTo(new BigDecimal("0.30")) <= 0) {
                pointsCapacite = poidsCapacite;
            } else if (ratioMensualiteCapacite.compareTo(new BigDecimal("0.50")) <= 0) {
                pointsCapacite = poidsCapacite.multiply(new BigDecimal("0.60"));
            }
        }
        total = total.add(pointsCapacite);
        criteres.add(critere("MENSUALITE_CAPACITE", "Mensualité estimée / capacité", pointsCapacite, poidsCapacite,
                ratioMensualiteCapacite == null
                        ? "Capacité ou mensualité non disponible pour calculer le ratio."
                        : "La mensualité estimée représente " + formatPourcentage(ratioMensualiteCapacite) + " de la capacité de remboursement."));

        BigDecimal ratioMontantRevenu = null;
        BigDecimal pointsMontant = BigDecimal.ZERO;
        if (revenu.compareTo(BigDecimal.ZERO) > 0) {
            ratioMontantRevenu = montantDemande.divide(revenu, 4, RoundingMode.HALF_UP);
            if (ratioMontantRevenu.compareTo(BigDecimal.ONE) <= 0) {
                pointsMontant = poidsMontant;
            } else if (ratioMontantRevenu.compareTo(new BigDecimal("3.00")) <= 0) {
                pointsMontant = poidsMontant.multiply(new BigDecimal("0.60"));
            }
        }
        total = total.add(pointsMontant);
        criteres.add(critere("MONTANT_REVENU", "Montant demandé / revenu mensuel", pointsMontant, poidsMontant,
                ratioMontantRevenu == null
                        ? "Revenu mensuel non disponible pour comparer le montant demandé."
                        : "Le montant demandé représente " + ratioMontantRevenu.setScale(2, RoundingMode.HALF_UP) + " mois de revenus déclarés."));

        BigDecimal pointsRatio = BigDecimal.ZERO;
        BigDecimal ratioCharges = null;
        if (revenu.compareTo(BigDecimal.ZERO) > 0) {
            ratioCharges = charges.divide(revenu, 4, RoundingMode.HALF_UP);
            if (ratioCharges.compareTo(new BigDecimal("0.50")) <= 0) {
                pointsRatio = poidsRatio;
            } else if (ratioCharges.compareTo(new BigDecimal("0.70")) <= 0) {
                pointsRatio = poidsRatio.multiply(new BigDecimal("0.60"));
            } else {
                pointsRatio = poidsRatio.multiply(new BigDecimal("0.20"));
            }
        }
        total = total.add(pointsRatio);
        criteres.add(critere("CHARGES_REVENU", "Charges / revenu mensuel", pointsRatio, poidsRatio,
                ratioCharges == null
                        ? "Revenu mensuel non disponible pour calculer le poids des charges."
                        : "Les charges représentent " + formatPourcentage(ratioCharges) + " du revenu mensuel."));

        BigDecimal pointsGarantie = BigDecimal.ZERO;
        if (garantieRequise.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal couvertureGarantie = garantieDisponible.divide(garantieRequise, 4, RoundingMode.HALF_UP);
            if (couvertureGarantie.compareTo(BigDecimal.ONE) >= 0) {
                pointsGarantie = poidsGar;
            } else if (couvertureGarantie.compareTo(new BigDecimal("0.80")) >= 0) {
                pointsGarantie = poidsGar.multiply(new BigDecimal("0.60"));
            }
        }
        total = total.add(pointsGarantie);
        criteres.add(critere("GARANTIE_EPARGNE_20", "Garantie épargne 20%", pointsGarantie, poidsGar,
                garantieRequise.compareTo(BigDecimal.ZERO) <= 0
                        ? "Garantie épargne requise non calculée."
                        : "Garantie requise: " + garantieRequise + ", garantie disponible: " + garantieDisponible + "."));

        BigDecimal pointsVisite = dateVisite != null && lieuVisite != null && !lieuVisite.isBlank()
                ? poidsVisit
                : BigDecimal.ZERO;
        total = total.add(pointsVisite);
        criteres.add(critere("VISITE_TERRAIN", "Visite terrain", pointsVisite, poidsVisit,
                pointsVisite.compareTo(BigDecimal.ZERO) > 0 ? "Date et lieu de visite renseignés." : "Date ou lieu de visite manquant."));

        BigDecimal score = normalizeScore(total.max(BigDecimal.ZERO).min(new BigDecimal("100")));
        return new AnalyseRisqueSnapshot(score, mensualiteEstimee, ratioMensualiteCapacite, ratioMontantRevenu, criteres);
    }

    private CritereAnalyseRisqueDto critere(String code, String libelle, BigDecimal points, BigDecimal maximum, String commentaire) {
        BigDecimal normalizedPoints = points.setScale(2, RoundingMode.HALF_UP);
        String niveau = normalizedPoints.compareTo(maximum) >= 0
                ? "BON"
                : normalizedPoints.compareTo(BigDecimal.ZERO) > 0 ? "MOYEN" : "ELEVE";
        return CritereAnalyseRisqueDto.builder()
                .codeCritere(code)
                .libelle(libelle)
                .points(normalizedPoints)
                .niveau(niveau)
                .commentaire(commentaire)
                .build();
    }

    private BigDecimal calculerMensualiteEstimee(BigDecimal montantDemande,
                                                 BigDecimal tauxInteret,
                                                 Integer dureeValeur,
                                                 DureeUnite dureeUnite) {
        BigDecimal dureeMois = convertirDureeEnMois(dureeValeur, dureeUnite);
        if (dureeMois.compareTo(BigDecimal.ZERO) <= 0) {
            return ZERO;
        }

        BigDecimal taux = tauxInteret == null ? BigDecimal.ZERO : tauxInteret;
        BigDecimal totalRemboursable = montantDemande.multiply(
                BigDecimal.ONE.add(taux.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP))
        );
        return totalRemboursable.divide(dureeMois, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal convertirDureeEnMois(Integer dureeValeur, DureeUnite dureeUnite) {
        if (dureeValeur == null || dureeValeur <= 0) {
            return BigDecimal.ZERO;
        }
        if (dureeUnite == DureeUnite.JOUR) {
            return BigDecimal.valueOf(dureeValeur).divide(new BigDecimal("30"), 4, RoundingMode.HALF_UP);
        }
        if (dureeUnite == DureeUnite.SEMAINE) {
            return BigDecimal.valueOf(dureeValeur).divide(new BigDecimal("4.3333"), 4, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(dureeValeur);
    }

    private String formatPourcentage(BigDecimal ratio) {
        return ratio.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP) + "%";
    }

    private NiveauRisque determinerNiveauRisque(BigDecimal score) {
        BigDecimal lowMin = defaultIfNull(scoreLowMin, new BigDecimal("70"));
        BigDecimal mediumMin = defaultIfNull(scoreMediumMin, new BigDecimal("40"));

        if (score == null) {
            throw new BusinessException("Impossible de déterminer le niveau de risque sans score");
        }
        if (score.compareTo(lowMin) >= 0) {
            return NiveauRisque.FAIBLE;
        }
        if (score.compareTo(mediumMin) >= 0) {
            return NiveauRisque.MOYEN;
        }
        return NiveauRisque.ELEVE;
    }

    private BigDecimal defaultIfNull(BigDecimal value, BigDecimal fallback) {
        return value == null ? fallback : value;
    }

    private void validateCommentaireObligatoire(RecommandationRisque recommandation,
                                                NiveauRisque risqueNiveau,
                                                String commentaire,
                                                Boolean scoreCorrigeManuellement) {
        boolean commentaireObligatoire = recommandation == RecommandationRisque.FAVORABLE_AVEC_RESERVE
                || recommandation == RecommandationRisque.DEFAVORABLE
                || risqueNiveau == NiveauRisque.ELEVE
                || Boolean.TRUE.equals(scoreCorrigeManuellement);

        if (commentaireObligatoire && (commentaire == null || commentaire.isBlank())) {
            throw new BusinessException("Commentaire obligatoire pour la recommandation/niveau/score manuel sélectionné");
        }
    }

    private boolean isAnalyseRisqueComplete(AnalyseRisque analyse) {
        return analyse.getDateVisite() != null
                && analyse.getLieuVisite() != null
                && !analyse.getLieuVisite().isBlank()
                && analyse.getActiviteVerifiee() != null
                && analyse.getCapaciteRemboursement() != null
                && analyse.getRecommandation() != null
                && analyse.getRisqueNiveau() != null
                && analyse.getScoreRisque() != null;
    }

    private String genererAnalyseIdUnique(Long demandeId) {
        String prefix = "ANR-" + demandeId + "-" + java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now()) + "-";
        String candidate;
        int sequence = 1;
        do {
            candidate = prefix + String.format("%04d", sequence++);
        } while (analyseRisqueRepository.existsByAnalyseId(candidate));

        return candidate;
    }

    private Utilisateur getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Utilisateur user) {
            return user;
        }

        String username = authentication.getName();
        if (username == null || username.isBlank() || "anonymousUser".equalsIgnoreCase(username)) {
            return null;
        }

        return utilisateurRepository.findByUsernameWithValidationContext(username).orElse(null);
    }

    @Override
    public boolean isCurrentUserRequest(Long demandeId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof com.mini.credit.entity.referentiel.Utilisateur)) {
            return false;
        }

        com.mini.credit.entity.referentiel.Utilisateur utilisateur = (com.mini.credit.entity.referentiel.Utilisateur) authentication.getPrincipal();

        if (utilisateur.getMembre() == null || utilisateur.getMembre().getId() == null) {
            return false;
        }

        // Vérifier si la demande appartient au membre de l'utilisateur
        DemandeCredit demande = demandeCreditRepository.findById(demandeId)
                .orElse(null);
        return demande != null && demande.getMembre() != null &&
               demande.getMembre().getId().equals(utilisateur.getMembre().getId());
    }

    @Override
    public boolean isCurrentUserMembre(Long membreId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof com.mini.credit.entity.referentiel.Utilisateur)) {
            return false;
        }

        com.mini.credit.entity.referentiel.Utilisateur utilisateur = (com.mini.credit.entity.referentiel.Utilisateur) authentication.getPrincipal();

        // Pour les MEMBER, vérifier si l'ID demandé correspond à leur membreId
        if (utilisateur.getMembre() != null) {
            return utilisateur.getMembre().getId().equals(membreId);
        }

        return false;
    }

    @Override
    public List<DemandeCreditResponse> getCurrentMemberRequests() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof com.mini.credit.entity.referentiel.Utilisateur)) {
            throw new ResourceNotFoundException("Utilisateur non authentifié");
        }

        com.mini.credit.entity.referentiel.Utilisateur utilisateur = (com.mini.credit.entity.referentiel.Utilisateur) authentication.getPrincipal();

        if (utilisateur.getMembre() == null || utilisateur.getMembre().getId() == null) {
            throw new ResourceNotFoundException("Accès non autorisé");
        }

        return getByMembre(utilisateur.getMembre().getId());
    }

    private String genererNumeroDemande() {
        return "DCR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private Long resolveAntenneId(DemandeCredit demande) {
        if (demande == null || demande.getSite() == null || demande.getSite().getAgence() == null) {
            return null;
        }
        return demande.getSite().getAgence().getId();
    }

    private Long resolveSiteId(DemandeCredit demande) {
        if (demande == null || demande.getSite() == null) {
            return null;
        }
        return demande.getSite().getId();
    }

    private Map<Long, GarantieCredit> loadGarantiesMap(List<DemandeCredit> demandes) {
        List<Long> ids = demandes.stream().map(DemandeCredit::getId).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }

        return garantieCreditRepository.findByDemandeCreditIdIn(ids).stream()
                .filter(g -> g.getDemandeCredit() != null && ids.contains(g.getDemandeCredit().getId()))
                .collect(Collectors.toMap(g -> g.getDemandeCredit().getId(), g -> g, (a, b) -> a));
    }

    private DemandeCreditResponse enrichGarantieState(DemandeCreditResponse response) {
        if (response == null || response.getId() == null) {
            return response;
        }

        GarantieCredit garantie = garantieCreditRepository.findByDemandeCreditId(response.getId()).orElse(null);
        return enrichGarantieState(response, garantie);
    }

    private DemandeCreditResponse enrichGarantieState(DemandeCreditResponse response, GarantieCredit garantie) {
        if (response == null) {
            return null;
        }

        BigDecimal requis = response.getDepotGarantieRequis() == null ? ZERO : normalizeMoney(response.getDepotGarantieRequis());
        BigDecimal bloque = resolveMontantGarantieBloque(response, garantie, requis);
        BigDecimal depotGarantiePaye = resolveDepotGarantiePaye(response.getDepotGarantiePaye(), bloque, requis);

        boolean garantieBloquee = garantie != null
                && (garantie.getStatutGarantieEpargne() == StatutGarantieCredit.BLOQUEE
                || garantie.getStatutGarantieEpargne() == StatutGarantieCredit.VALIDEE)
                && bloque.compareTo(requis) >= 0;

        String statutGarantie;
        if (garantieBloquee) {
            statutGarantie = "BLOQUEE";
        } else if ((garantie != null && garantie.getStatutGarantieEpargne() == StatutGarantieCredit.INSUFFISANTE)
            || (requis.compareTo(ZERO) > 0 && bloque.compareTo(ZERO) > 0 && bloque.compareTo(requis) < 0)) {
            statutGarantie = "INSUFFISANTE";
        } else {
            statutGarantie = "NON_BLOQUEE";
        }

        return DemandeCreditResponse.builder()
                .id(response.getId())
                .numeroDemande(response.getNumeroDemande())
                .membreId(response.getMembreId())
                .membreNomComplet(response.getMembreNomComplet())
                .siteId(response.getSiteId())
                .siteNom(response.getSiteNom())
                .agentId(response.getAgentId())
                .dateDemande(response.getDateDemande())
                .montantDemande(response.getMontantDemande())
                .fraisDemandePayes(response.getFraisDemandePayes())
                .devise(response.getDevise())
                .dureeValeur(response.getDureeValeur())
                .dureeUnite(response.getDureeUnite())
                .periodiciteRemboursement(response.getPeriodiciteRemboursement())
                .tauxInteret(response.getTauxInteret())
                .objetCredit(response.getObjetCredit())
                .gagePropose(response.getGagePropose())
                .activiteFinancee(response.getActiviteFinancee())
                .revenusEstimes(response.getRevenusEstimes())
                .chargesEstimees(response.getChargesEstimees())
                .fraisDemande(response.getFraisDemande())
                .depotGarantieRequis(response.getDepotGarantieRequis())
                .depotGarantiePaye(depotGarantiePaye)
                .montantGarantieBloque(bloque)
                .garantieBloquee(garantieBloquee)
                .statutGarantie(statutGarantie)
                .statut(response.getStatut())
                .commentaireDecision(response.getCommentaireDecision())
                .dateDecision(response.getDateDecision())
                .createdAt(response.getCreatedAt())
                .updatedAt(response.getUpdatedAt())
                .analyseRisque(buildAnalyseRisqueResponse(response, depotGarantiePaye))
                .build();
    }

    private AnalyseRisqueCreditResponse buildAnalyseRisqueResponse(DemandeCreditResponse response, BigDecimal depotGarantiePaye) {
        if (response == null || response.getId() == null) {
            return null;
        }

        AnalyseRisque analyse = analyseRisqueRepository.findByDemandeCreditId(response.getId());
        if (analyse == null) {
            return null;
        }

        BigDecimal montantDemande = normalizeMoney(response.getMontantDemande());
        BigDecimal revenus = normalizeMoney(analyse.getRevenuNetEstime());
        BigDecimal charges = normalizeMoney(analyse.getChargesMensuelles());
        BigDecimal capacite = normalizeMoney(analyse.getCapaciteRemboursement());
        BigDecimal garantieRequise = normalizeMoney(response.getDepotGarantieRequis());
        BigDecimal garantieDisponible = normalizeMoney(depotGarantiePaye);
        AnalyseRisqueSnapshot snapshot = buildAnalyseRisqueSnapshot(
                montantDemande,
                response.getDureeValeur(),
                response.getDureeUnite(),
                normalizeMoney(response.getTauxInteret()),
                revenus,
                charges,
                capacite,
                garantieRequise,
                garantieDisponible,
                analyse.getActiviteVerifiee(),
                analyse.getDateVisite(),
                analyse.getLieuVisite()
        );

        return AnalyseRisqueCreditResponse.builder()
                .scoreTotal(analyse.getScoreRisque())
                .niveauRisque(analyse.getRisqueNiveau())
                .montantDemande(montantDemande)
                .revenusMensuels(revenus)
                .chargesMensuelles(charges)
                .capaciteRemboursement(capacite)
                .mensualiteEstimee(snapshot.mensualiteEstimee())
                .ratioMensualiteCapacite(snapshot.ratioMensualiteCapacite())
                .ratioMontantRevenu(snapshot.ratioMontantRevenu())
                .garantieEpargneRequise(garantieRequise)
                .garantieEpargneDisponible(garantieDisponible)
                .garantieMaterielleRequise(montantDemande.multiply(new BigDecimal("2.00")).setScale(2, RoundingMode.HALF_UP))
                .garantieMaterielleDeclaree(null)
                .criteres(snapshot.criteres())
                .build();
    }

    private record AnalyseRisqueSnapshot(BigDecimal scoreTotal,
                                         BigDecimal mensualiteEstimee,
                                         BigDecimal ratioMensualiteCapacite,
                                         BigDecimal ratioMontantRevenu,
                                         List<CritereAnalyseRisqueDto> criteres) {
    }

    private BigDecimal resolveMontantGarantieBloque(DemandeCreditResponse response, GarantieCredit garantie, BigDecimal requis) {
        BigDecimal bloqueGarantie = garantie != null && garantie.getMontantGarantieBloque() != null
                ? normalizeMoney(garantie.getMontantGarantieBloque())
                : ZERO;
        if (bloqueGarantie.compareTo(ZERO) > 0 || response.getMembreId() == null) {
            return bloqueGarantie.min(requis);
        }

        return compteEpargneRepository.findFirstByMembreIdAndStatut(response.getMembreId(), StatutCompte.ACTIF)
                .map(CompteEpargne::getSoldeBloque)
                .map(this::normalizeMoney)
                .map(soldeBloque -> soldeBloque.min(requis))
                .orElse(ZERO);
    }

    private BigDecimal resolveDepotGarantiePaye(BigDecimal depotGarantiePaye, BigDecimal bloque, BigDecimal requis) {
        BigDecimal paye = depotGarantiePaye == null ? ZERO : normalizeMoney(depotGarantiePaye);
        return paye.max(bloque).min(requis);
    }

    private FraisCreditAEncaisserResponse toFraisCreditAEncaisserResponse(DemandeCredit demande) {
        BigDecimal fraisDemande = normalizeMoney(demande.getFraisDemande());
        BigDecimal fraisPayes = normalizeMoney(demande.getFraisDemandePayes());

        return FraisCreditAEncaisserResponse.builder()
                .demandeCreditId(demande.getId())
                .numeroDemande(demande.getNumeroDemande())
                .membreNomComplet(demande.getMembre() != null ? demande.getMembre().getNomComplet() : null)
                .siteNom(demande.getSite() != null ? demande.getSite().getNomSite() : null)
                .antenneNom(demande.getSite() != null && demande.getSite().getAgence() != null ? demande.getSite().getAgence().getNomAgence() : null)
                .dateDemande(demande.getDateDemande())
                .montantDemande(demande.getMontantDemande())
                .fraisDemande(fraisDemande)
                .fraisDemandePayes(fraisPayes)
                .resteFraisAPayer(fraisDemande.subtract(fraisPayes).max(ZERO).setScale(2, RoundingMode.HALF_UP))
                .statutDemande(demande.getStatut())
                .objetCredit(demande.getObjetCredit())
                .sessionCaisseRequise(true)
                .build();
    }

    /**
     * PHASE 4: Valide les critères strictes de crédit
     */
    @Override
    public com.mini.credit.dto.credit.CreditValidationResult validerCredit(Long demandeId) {
        DemandeCredit demandeCredit = demandeCreditRepository.findById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande crédit non trouvée: " + demandeId));

        return creditValidationService.validerCreditComplet(demandeCredit);
    }
}