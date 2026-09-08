package com.mini.credit.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.mini.credit.dto.caisse.OperationCaisseRequest;
import com.mini.credit.dto.credit.ApprobationCreditRequest;
import com.mini.credit.dto.credit.CreditContratResponse;
import com.mini.credit.dto.credit.CreditEnCoursResponse;
import com.mini.credit.dto.credit.CreditDetailResponse;
import com.mini.credit.dto.credit.CreditRembourseResponse;
import com.mini.credit.dto.credit.CreditResponse;
import com.mini.credit.dto.credit.DecaissementCreditRequest;
import com.mini.credit.dto.credit.RemboursementRequest;
import com.mini.credit.dto.document.QuittanceCreateRequest;
import com.mini.credit.entity.audit.AuditLog;
import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.entity.credit.Credit;
import com.mini.credit.entity.credit.DemandeCredit;
import com.mini.credit.entity.credit.EcheanceCredit;
import com.mini.credit.entity.credit.GarantieCredit;
import com.mini.credit.entity.credit.GarantieMaterielle;
import com.mini.credit.entity.credit.RemboursementCredit;
import com.mini.credit.entity.epargne.CompteEpargne;
import com.mini.credit.entity.epargne.OperationEpargne;
import com.mini.credit.entity.employe.Employe;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.AgentTerrain;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.CategorieOperationCaisse;
import com.mini.credit.enums.DureeUnite;
import com.mini.credit.enums.ModePaiement;
import com.mini.credit.enums.PeriodiciteRemboursement;
import com.mini.credit.enums.SensOperation;
import com.mini.credit.enums.StatutCompte;
import com.mini.credit.enums.StatutCredit;
import com.mini.credit.enums.StatutDemandeCredit;
import com.mini.credit.enums.StatutEcheance;
import com.mini.credit.enums.StatutGarantieMaterielle;
import com.mini.credit.enums.StatutSessionCaisse;
import com.mini.credit.enums.SourceOperationCaisse;
import com.mini.credit.enums.TypeOperationCaisse;
import com.mini.credit.enums.TypeOperationEpargne;
import com.mini.credit.enums.TypeQuittance;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.exception.ResourceNotFoundException;
import com.mini.credit.mapper.CreditMapper;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.repository.agentTerrain.AgentTerrainRepository;
import com.mini.credit.repository.audit.AuditLogRepository;
import com.mini.credit.repository.caisse.OperationCaisseRepository;
import com.mini.credit.repository.caisse.SessionCaisseRepository;
import com.mini.credit.repository.credit.CreditRepository;
import com.mini.credit.repository.credit.DemandeCreditRepository;
import com.mini.credit.repository.credit.EcheanceCreditRepository;
import com.mini.credit.repository.credit.GarantieCreditRepository;
import com.mini.credit.repository.credit.RemboursementCreditRepository;
import com.mini.credit.repository.epargne.CompteEpargneRepository;
import com.mini.credit.repository.epargne.OperationEpargneRepository;
import com.mini.credit.repository.membre.MembreRepository;
import com.mini.credit.repository.utilisateur.UtilisateurRepository;
import com.mini.credit.service.CreditService;
import com.mini.credit.service.CreditValidationService;
import com.mini.credit.service.OperationCaisseService;
import com.mini.credit.service.QuittanceService;
import com.mini.credit.service.GarantieCreditWorkflowService;
import com.mini.credit.service.WorkflowTaskService;
import com.mini.credit.service.security.ScopeService;
import com.mini.credit.service.audit.AuditService;
import com.mini.credit.service.audit.Auditable;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.enums.security.AuditModule;
import com.mini.credit.enums.security.AuditSeverity;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CreditServiceImpl implements CreditService {

    private final CreditRepository creditRepository;
    private final DemandeCreditRepository demandeCreditRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EcheanceCreditRepository echeanceCreditRepository;
    private final RemboursementCreditRepository remboursementCreditRepository;
    private final OperationCaisseRepository operationCaisseRepository;
    private final SessionCaisseRepository sessionCaisseRepository;
    private final CompteEpargneRepository compteEpargneRepository;
    private final OperationEpargneRepository operationEpargneRepository;
    private final AgentTerrainRepository agentTerrainRepository;
    private final GarantieCreditRepository garantieCreditRepository;
    private final AuditLogRepository auditLogRepository;
    private final MembreRepository membreRepository;
    private final CreditMapper creditMapper;
    private final OperationCaisseService operationCaisseService;
    private final QuittanceService quittanceService;
    private final GarantieCreditWorkflowService garantieCreditWorkflowService;
    private final WorkflowTaskService workflowTaskService;
    private final ScopeService scopeService;
    private final AuditService auditService;
    private final CreditValidationService creditValidationService;

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)  // PHASE 9: SERIALIZABLE isolation for race condition prevention
    @Auditable(action = AuditAction.CREDIT_APPROVED, entityType = "DemandeCredit", entityIdParameter = "demandeId")
    public CreditResponse approuverDemande(Long demandeId, ApprobationCreditRequest request) {
        DemandeCredit demande = demandeCreditRepository.findById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));

        var existingCredit = creditRepository.findByDemandeCreditId(demandeId);
        if (existingCredit.isPresent()) {
            return creditMapper.toResponse(existingCredit.get());
        }

        Utilisateur decidedBy = utilisateurRepository.findById(request.getDecidedBy())
                .orElseThrow(() -> new ResourceNotFoundException("Décideur introuvable"));

        creditValidationService.verifierFraisDemandeIntegralementPayes(demande);
        validerDemandePourApprobation(demande);

        boolean hasActiveCredit = creditRepository.existsByMembreIdAndStatutIn(
                demande.getMembre().getId(),
                List.of(
                        StatutCredit.APPROUVE,
                        StatutCredit.DECAISSE,
                        StatutCredit.EN_COURS,
                        StatutCredit.EN_RETARD,
                        StatutCredit.CONTENTIEUX
                )
        );

        if (hasActiveCredit) {
            throw new BusinessException("Le membre a déjà un crédit actif");
        }

        if (demande.getAnalyseRisque() == null) {
            throw new BusinessException("Impossible d'approuver sans analyse de risque");
        }

        if (!garantieCreditWorkflowService.isGarantieValidee(demandeId)) {
            throw new BusinessException("Impossible d'approuver sans garantie validée");
        }

        if (nvl(demande.getDepotGarantieRequis()).compareTo(BigDecimal.ZERO) > 0
                && nvl(demande.getDepotGarantiePaye()).compareTo(nvl(demande.getDepotGarantieRequis())) < 0) {
            throw new BusinessException("Le dépôt de garantie n'est pas totalement payé");
        }

        int nombreEcheances = calculerNombreEcheances(
                demande.getDureeValeur(),
                demande.getDureeUnite(),
                demande.getPeriodiciteRemboursement()
        );

        BigDecimal principal = nvl(demande.getMontantDemande());

        BigDecimal interet = calculerInteretTotal(
                principal,
                nvl(demande.getTauxInteret()),
                demande.getDureeValeur(),
                demande.getDureeUnite(),
                demande.getPeriodiciteRemboursement()
        );

        BigDecimal total = principal.add(interet);

        Credit credit = Credit.builder()
                .numeroCredit("TMP")
                .demandeCredit(demande)
                .membre(demande.getMembre())
                .site(demande.getSite())
                .dateApprobation(LocalDate.now())
                .montantOctroye(principal)
                .devise(demande.getDevise())
                .tauxInteret(demande.getTauxInteret())
                .dureeValeur(demande.getDureeValeur())
                .dureeUnite(demande.getDureeUnite())
                .periodiciteRemboursement(demande.getPeriodiciteRemboursement())
                .nombreEcheances(nombreEcheances)
                .principalTotal(principal)
                .interetTotal(interet)
                .penaliteTotal(BigDecimal.ZERO)
                .totalARembourser(total)
                .encoursPrincipal(principal)
                .statut(StatutCredit.APPROUVE)
                .createdBy(decidedBy)
                .build();

        credit = creditRepository.save(credit);

        credit.setNumeroCredit(genererNumeroCredit(credit));
        credit = creditRepository.save(credit);

        if (request.isGenererEcheancier()) {
            genererEcheancier(credit);
        }

        demande.setStatut(StatutDemandeCredit.APPROUVEE);
        demande.setDateDecision(java.time.LocalDateTime.now());
        demande.setDecidedBy(decidedBy);
        demandeCreditRepository.save(demande);

        workflowTaskService.onDemandeCreditApprouvee(
            demande.getId(),
            credit.getId(),
            credit.getNumeroCredit(),
            resolveAntenneId(credit),
            resolveSiteId(credit)
        );

        return creditMapper.toResponse(credit);
    }

    private void validerDemandePourApprobation(DemandeCredit demande) {
        if (demande.getStatut() == null) {
            throw new BusinessException("Le statut de la demande est invalide");
        }

        switch (demande.getStatut()) {
            case BROUILLON -> throw new BusinessException(
                    "Impossible d'approuver une demande encore en brouillon"
            );
            case APPROUVEE -> throw new BusinessException(
                    "Cette demande est déjà approuvée"
            );
            case REJETEE -> throw new BusinessException(
                    "Impossible d'approuver une demande rejetée"
            );
            case ANNULEE -> throw new BusinessException(
                    "Impossible d'approuver une demande annulée"
            );
            case VALIDATION_CHEF, VALIDATION_CONTROLEUR -> {
                // statuts autorisés
            }
            default -> throw new BusinessException("Statut de demande non pris en charge");
        }

        // AJOUT PHASE 3A: Valider les plages à nouveau (duplex validation)
        BigDecimal montant = demande.getMontantDemande();
        BigDecimal taux = demande.getTauxInteret();
        Integer duree = demande.getDureeValeur();

        if (montant == null || montant.compareTo(BigDecimal.valueOf(1)) < 0 || 
            montant.compareTo(BigDecimal.valueOf(100_000_000)) > 0) {
            throw new BusinessException("Montant du crédit invalide pour approbation (doit être entre 1 et 100M CDF)");
        }

        if (taux == null || taux.compareTo(BigDecimal.valueOf(20)) > 0) {
            throw new BusinessException("Taux du crédit invalide pour approbation (doit être <= 20%)");
        }

        if (duree == null || duree < 1 || duree > 60) {
            throw new BusinessException("Durée du crédit invalide pour approbation (doit être entre 1 et 60)");
        }

        if (demande.getMembre() == null) {
            throw new BusinessException("Le membre de la demande est obligatoire");
        }

        if (nvl(demande.getMontantDemande()).compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Le montant demandé doit être supérieur à zéro");
        }

        if (demande.getDureeValeur() == null || demande.getDureeValeur() <= 0) {
            throw new BusinessException("La durée du crédit doit être supérieure à zéro");
        }

        if (demande.getDureeUnite() == null) {
            throw new BusinessException("L'unité de durée est obligatoire");
        }

        if (demande.getPeriodiciteRemboursement() == null) {
            throw new BusinessException("La périodicité de remboursement est obligatoire");
        }

        if (nvl(demande.getTauxInteret()).compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Le taux d'intérêt ne peut pas être négatif");
        }
    }

    @Override
    public CreditResponse getById(Long id) {
        return creditMapper.toResponse(
                creditRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Crédit introuvable"))
        );
    }

        @Override
        @Transactional(readOnly = true)
        public CreditDetailResponse getDetail(Long id) {
        Credit credit = creditRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Crédit introuvable"));
        DemandeCredit demande = credit.getDemandeCredit();
        GarantieCredit garantie = demande != null
            ? garantieCreditRepository.findByDemandeCreditId(demande.getId()).orElse(null)
            : null;
        List<EcheanceCredit> echeances = echeanceCreditRepository.findByCreditIdOrderByNumeroEcheanceAsc(id);
        List<RemboursementCredit> remboursements = remboursementCreditRepository.findByCreditIdOrderByDatePaiementAscIdAsc(id);
        List<AuditLog> auditLogs = auditLogRepository.findByEntityTypeAndEntityIdOrderByDateActionAsc("Credit", id);

        return CreditDetailResponse.builder()
            .resume(buildResumeCredit(credit, demande))
            .responsables(buildResponsables(credit, demande, garantie, remboursements))
            .garantie(buildGarantieDetail(credit, garantie))
            .echeancier(echeances.stream().map(this::toEcheanceDetail).toList())
            .suiviFinancier(buildSuiviFinancier(credit, echeances))
            .remboursements(remboursements.stream().map(this::toRemboursementDetail).toList())
            .historique(buildHistorique(credit, demande, garantie, remboursements, auditLogs))
            .build();
        }

    @Override
    @Transactional(readOnly = true)
    public CreditContratResponse getContrat(Long id) {
        Credit credit = creditRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Crédit introuvable"));
        DemandeCredit demande = credit.getDemandeCredit();
        Membre membre = credit.getMembre();
        GarantieCredit garantie = demande != null
            ? garantieCreditRepository.findByDemandeCreditId(demande.getId()).orElse(null)
            : null;
        List<EcheanceCredit> echeances = echeanceCreditRepository.findByCreditIdOrderByNumeroEcheanceAsc(id);
        List<RemboursementCredit> remboursements = remboursementCreditRepository.findByCreditIdOrderByDatePaiementAscIdAsc(id);
        CreditDetailResponse.Responsables responsables = buildResponsables(credit, demande, garantie, remboursements);
        BigDecimal montantBaseGarantie = demande != null ? nvl(demande.getMontantDemande()) : nvl(credit.getMontantOctroye());

        return CreditContratResponse.builder()
            .creditId(credit.getId())
            .numeroCredit(credit.getNumeroCredit())
            .demandeCreditId(demande != null ? demande.getId() : null)
            .numeroDemande(demande != null ? demande.getNumeroDemande() : null)
            .statut(credit.getStatut())
            .membreNomComplet(membre != null ? membre.getNomComplet() : null)
            .membreAdresse(membre != null ? membre.getAdresse() : null)
            .membreTelephone(membre != null ? membre.getTelephonePrincipal() : null)
            .montantAccorde(nvl(credit.getMontantOctroye()))
            .devise(credit.getDevise())
            .tauxInteret(credit.getTauxInteret())
            .dureeValeur(credit.getDureeValeur())
            .dureeUnite(credit.getDureeUnite() != null ? credit.getDureeUnite().name() : null)
            .datePret(credit.getDateApprobation())
            .dateDecaissement(credit.getDateDecaissement())
            .objetCredit(demande != null ? demande.getObjetCredit() : null)
            .gagePropose(demande != null ? demande.getGagePropose() : null)
            .garantieRegleMontant(montantBaseGarantie.multiply(new BigDecimal("0.20")))
            .garantieRegleLibelle("20 % du montant demandé")
            .montantGarantieRequis(garantie != null ? nvl(garantie.getMontantGarantieRequis()) : BigDecimal.ZERO)
            .montantGarantieBloque(garantie != null ? nvl(garantie.getMontantGarantieBloque()) : BigDecimal.ZERO)
            .sourceGarantie(garantie != null ? buildGarantieDetail(credit, garantie).getSourceGarantie() : null)
            .garantiesMateriellesAcceptees(buildGarantiesMateriellesAcceptees(garantie))
            .penaliteRetardLibelle("2500 FC par jour de retard")
            .totalPrincipal(nvl(credit.getPrincipalTotal()))
            .totalInteret(nvl(credit.getInteretTotal()))
            .totalAPayer(nvl(credit.getTotalARembourser()))
            .antenneNom(credit.getSite() != null ? credit.getSite().getNomSite() : null)
            .agentTerrainNom(responsables.getAgentTerrainNom())
            .gestionnaireNom(responsables.getGestionnaireNom())
            .controleurNom(responsables.getControleurNom())
            .chefBureauNom(responsables.getChefBureauNom())
            .caissierNom(responsables.getCaissierNom())
            .echeancier(echeances.stream().map(this::toEcheanceDetail).toList())
            .build();
    }

    @Override
    public List<CreditResponse> getAll() {
        return creditRepository.findAll().stream()
                .map(creditMapper::toResponse)
                .toList();
    }

    @Override
    public List<CreditResponse> getCreditsADecaisser() {
        return creditRepository.findByStatut(StatutCredit.APPROUVE).stream()
                .map(creditMapper::toResponse)
                .toList();
    }

        @Override
        @Transactional(readOnly = true)
        public List<CreditEnCoursResponse> getCreditsEnCours() {
        List<StatutCredit> statutsEnCours = List.of(
            StatutCredit.APPROUVE,
            StatutCredit.DECAISSE,
            StatutCredit.EN_COURS,
            StatutCredit.EN_RETARD,
            StatutCredit.CONTENTIEUX
        );

        return creditRepository.findByStatutInAndClotureAtIsNullOrderByDateDecaissementDescDateApprobationDescIdDesc(statutsEnCours).stream()
            .map(this::toCreditEnCoursResponse)
            .toList();
        }

        @Override
        @Transactional(readOnly = true)
        public List<CreditRembourseResponse> getCreditsRembourses() {
        List<StatutCredit> statutsSoldables = List.of(
            StatutCredit.DECAISSE,
            StatutCredit.EN_COURS,
            StatutCredit.EN_RETARD
        );

        return creditRepository.findCreditsRemboursesOuSoldes(StatutCredit.REMBOURSE, statutsSoldables).stream()
            .map(this::toCreditRembourseResponse)
            .toList();
        }

    @Override
    public List<CreditResponse> getByMembre(Long membreId) {
        return creditRepository.findByMembreId(membreId).stream()
                .map(creditMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)  // PHASE 9: SERIALIZABLE isolation for race condition prevention
    public CreditResponse decaisserCredit(Long creditId, DecaissementCreditRequest request) {
        assertCurrentUserIsCaissier();

        Credit credit = creditRepository.findById(creditId)
                .orElseThrow(() -> new ResourceNotFoundException("Crédit introuvable"));

        if (credit.getStatut() != StatutCredit.APPROUVE) {
            throw new BusinessException("Seul un crédit approuvé peut être décaissé");
        }

        SessionCaisse sessionCaisse = sessionCaisseRepository.findById(request.getSessionCaisseId())
                .orElseThrow(() -> new ResourceNotFoundException("Session caisse introuvable"));

        validerSessionCaissePourOperation(sessionCaisse);

        if (credit.getDemandeCredit() == null
                || credit.getDemandeCredit().getStatut() != StatutDemandeCredit.APPROUVEE) {
            throw new BusinessException("Le crédit ne peut être décaissé que si sa demande est approuvée");
        }

        creditValidationService.verifierFraisDemandeIntegralementPayes(credit.getDemandeCredit());

        if (credit.getDemandeCredit() == null || !garantieCreditWorkflowService.isGarantieValidee(credit.getDemandeCredit().getId())) {
            throw new BusinessException("Le décaissement exige une garantie validée");
        }

        if (request.getDateDecaissement() == null) {
            throw new BusinessException("La date de décaissement est obligatoire");
        }

        if (request.getModePaiement() == null) {
            throw new BusinessException("Le mode de paiement est obligatoire");
        }

        if (request.getCreatedBy() == null) {
            throw new BusinessException("L'utilisateur créateur est obligatoire");
        }

        if (request.getAgentId() != null) {
            agentTerrainRepository.findById(request.getAgentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Agent introuvable"));
        }

        utilisateurRepository.findById(request.getCreatedBy())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        BigDecimal montantDecaisse = nvl(credit.getMontantOctroye());
        if (montantDecaisse.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Le montant du crédit à décaisser est invalide");
        }

        BigDecimal soldeDisponibleCaisse = sessionCaisse.getSoldeTheorique() != null
                ? sessionCaisse.getSoldeTheorique()
                : nvl(sessionCaisse.getSoldeOuverture())
                    .add(nvl(sessionCaisse.getTotalEntrees()))
                    .subtract(nvl(sessionCaisse.getTotalSorties()));

        if (montantDecaisse.compareTo(soldeDisponibleCaisse) > 0) {
                auditService.logActionRequiresNew(
                    AuditAction.CREDIT_DISBURSE_REFUSED,
                    AuditModule.CREDIT,
                    "Credit",
                    credit.getId(),
                    false,
                    AuditSeverity.WARNING,
                    "Décaissement impossible : solde caisse insuffisant.",
                    credit.getNumeroCredit(),
                    null,
                    null,
                    "Montant demandé=" + montantDecaisse + ", solde disponible=" + soldeDisponibleCaisse,
                    sessionCaisse.getCaisse() != null ? sessionCaisse.getCaisse().getId() : null,
                    sessionCaisse.getId(),
                    credit.getSite() != null ? credit.getSite().getId() : null,
                    credit.getSite() != null ? credit.getSite().getNomSite() : null
            );
            throw new BusinessException("Décaissement impossible : solde caisse insuffisant.");
        }

        OperationCaisseRequest opCaisse = new OperationCaisseRequest();
        opCaisse.setSessionCaisseId(sessionCaisse.getId());
        opCaisse.setCaisseId(sessionCaisse.getCaisse().getId());
        opCaisse.setDateOperation(request.getDateDecaissement());
        opCaisse.setTypeOperation(TypeOperationCaisse.SORTIE);
        opCaisse.setCategorieOperation(CategorieOperationCaisse.DECAISSEMENT_CREDIT);
        opCaisse.setMontant(montantDecaisse);
        opCaisse.setDevise(credit.getDevise());
        opCaisse.setMembreId(credit.getMembre().getId());
        opCaisse.setCreditId(credit.getId());
        opCaisse.setAgentId(request.getAgentId());
        opCaisse.setDescription("Décaissement du crédit " + credit.getNumeroCredit());
        opCaisse.setCreatedBy(request.getCreatedBy());
        opCaisse.setModePaiement(request.getModePaiement());
        opCaisse.setObservation(request.getObservation());
        opCaisse.setSource(SourceOperationCaisse.CREDIT_DECAISSEMENT); // PATCH 6
        opCaisse.setReferenceExterne(credit.getNumeroCredit());         // PATCH 6
        opCaisse.setReferenceMetier(credit.getNumeroCredit());

        QuittanceCreateRequest quittanceRequest = buildQuittanceDecaissementRequest(
                request,
                credit,
                montantDecaisse
        );

        operationCaisseService.enregistrer(opCaisse);
        quittanceService.create(quittanceRequest);

        credit.setDateDecaissement(request.getDateDecaissement().toLocalDate());
        credit.setStatut(StatutCredit.DECAISSE);

        credit = creditRepository.save(credit);

        workflowTaskService.onCreditDecaisse(
            credit.getId(),
            credit.getNumeroCredit(),
            resolveAntenneId(credit),
            resolveSiteId(credit)
        );

    auditService.logAction(
        AuditAction.CREDIT_DISBURSED,
        AuditModule.CREDIT,
        "Credit",
        credit.getId(),
        true,
        AuditSeverity.INFO,
        "Décaissement crédit effectué avec succès. Montant=" + montantDecaisse,
        credit.getNumeroCredit(),
        null,
        null,
        null,
        sessionCaisse.getCaisse() != null ? sessionCaisse.getCaisse().getId() : null,
        sessionCaisse.getId(),
        credit.getSite() != null ? credit.getSite().getId() : null,
        credit.getSite() != null ? credit.getSite().getNomSite() : null
    );

        return creditMapper.toResponse(credit);
    }

    private void assertCurrentUserIsCaissier() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            throw new BusinessException("Accès refusé: rôle CAISSIER requis pour le décaissement");
        }

        boolean isCaissier = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_CAISSIER".equals(authority.getAuthority()));

        if (!isCaissier) {
            throw new BusinessException("Accès refusé: rôle CAISSIER requis pour le décaissement");
        }
    }

    private QuittanceCreateRequest buildQuittanceDecaissementRequest(DecaissementCreditRequest request,
                                                                     Credit credit,
                                                                     BigDecimal montantDecaisse) {
        QuittanceCreateRequest quittanceRequest = new QuittanceCreateRequest();
        quittanceRequest.setMembreId(credit.getMembre().getId());
        quittanceRequest.setTypeQuittance(TypeQuittance.DECAISSEMENT_CREDIT);
        quittanceRequest.setReferenceOperation(credit.getNumeroCredit());
        quittanceRequest.setMontant(montantDecaisse);
        quittanceRequest.setDevise(credit.getDevise());
        quittanceRequest.setDateEmission(request.getDateDecaissement());
        quittanceRequest.setCreatedBy(request.getCreatedBy());
        return quittanceRequest;
    }

    private void validerSessionCaissePourOperation(SessionCaisse sessionCaisse) {
        if (sessionCaisse.getStatut() == null) {
            throw new BusinessException("Le statut de la session de caisse est invalide");
        }

        if (sessionCaisse.getStatut() != StatutSessionCaisse.OUVERTE) {
            throw new BusinessException("La session de caisse doit être ouverte pour effectuer cette opération");
        }

        if (sessionCaisse.getDateCloture() != null) {
            throw new BusinessException("La session de caisse est déjà clôturée");
        }
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)  // PHASE 9: SERIALIZABLE isolation for race condition prevention
    @Auditable(action = AuditAction.REMBOURSEMENT_CREATED, entityType = "Credit", entityIdParameter = "creditId")
    public void enregistrerRemboursement(Long creditId, RemboursementRequest request) {
        enregistrerRemboursementInterne(creditId, request, true);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Long enregistrerRemboursementDepuisCollecte(Long creditId,
                                                       RemboursementRequest request,
                                                       Long collecteId,
                                                       Long ligneCollecteId,
                                                       Long validateurId,
                                                       Long antenneId) {
        if (!hasAnyCurrentRole("ROLE_ADMIN", "ROLE_CONTROLEUR")) {
            throw new BusinessException("Seul le workflow de validation collecte peut générer ce remboursement");
        }
        if (collecteId == null || ligneCollecteId == null) {
            throw new BusinessException("La référence collecte est obligatoire pour un remboursement automatique");
        }

        RemboursementCredit remboursement = enregistrerRemboursementInterne(creditId, request, false);

        auditService.logAction(
            AuditAction.REMBOURSEMENT_CREATED,
            AuditModule.CREDIT,
            "RemboursementCredit",
            remboursement.getId(),
            true,
            AuditSeverity.INFO,
            "Remboursement généré automatiquement depuis collecte validée"
                + " | collecteId=" + collecteId
                + " | ligneCollecteId=" + ligneCollecteId
                + " | validateurId=" + validateurId
                + " | antenneId=" + antenneId
                + " | datePaiement=" + request.getDatePaiement()
                + " | commentaire=" + (request.getObservation() == null ? "" : request.getObservation()),
            "COLLECTE-" + collecteId + "-LIGNE-" + ligneCollecteId,
            null,
            null,
            null,
            null,
            request.getSessionCaisseId(),
            null,
            null
        );

        return remboursement.getId();
    }

    private RemboursementCredit enregistrerRemboursementInterne(Long creditId,
                                                               RemboursementRequest request,
                                                               boolean enforceManualScope) {
        Credit credit = creditRepository.findById(creditId)
                .orElseThrow(() -> new ResourceNotFoundException("Crédit introuvable"));

        if (!(credit.getStatut() == StatutCredit.DECAISSE
                || credit.getStatut() == StatutCredit.EN_COURS
                || credit.getStatut() == StatutCredit.EN_RETARD)) {
            throw new BusinessException("Seul un crédit décaissé ou en cours peut être remboursé");
        }

        if (request.getDatePaiement() == null) {
            throw new BusinessException("La date de paiement est obligatoire");
        }

        if (request.getMembreId() == null) {
            throw new BusinessException("Le membre est obligatoire");
        }

        if (request.getMontantTotal() == null || request.getMontantTotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Le montant du remboursement doit être supérieur à zéro");
        }

        // AJOUT PHASE 2: Valider la scope d'accès pour la saisie manuelle (MEMBER limité à ses propres crédits)
        if (enforceManualScope && !scopeService.canRecordRemboursement(creditId)) {
            throw new BusinessException("Accès refusé: vous n'êtes pas autorisé à enregistrer ce remboursement");
        }

        // AJOUT PHASE 2: Vérifier que le membreId du DTO correspond au crédit
        if (!credit.getMembre().getId().equals(request.getMembreId())) {
            throw new BusinessException("Le membreId fourni ne correspond pas au propriétaire du crédit");
        }

        // PHASE 10: Pénalités gérées par batch job quotidien (00:01)
        // penaliteService.appliquerPenalitesCredit(creditId, request.getDatePaiement().toLocalDate());

        Membre membre = membreRepository.findById(request.getMembreId())
                .orElseThrow(() -> new ResourceNotFoundException("Membre introuvable"));

        if (!credit.getMembre().getId().equals(membre.getId())) {
            throw new BusinessException("Ce membre n'est pas le propriétaire du crédit");
        }

        AgentTerrain agent = null;
        if (request.getAgentId() != null) {
            agent = agentTerrainRepository.findById(request.getAgentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Agent introuvable"));
        }

        Utilisateur createdBy = null;
        if (request.getCreatedBy() != null) {
            createdBy = utilisateurRepository.findById(request.getCreatedBy())
                    .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        }

        if (createdBy == null && enforceManualScope) {
            createdBy = resolveCurrentUtilisateur()
                .orElseThrow(() -> new BusinessException("L'utilisateur qui enregistre le remboursement est obligatoire"));
            request.setCreatedBy(createdBy.getId());
        }

        SessionCaisse sessionCaisse = resoudreSessionCaisseRemboursementDirect(request, credit, membre, createdBy, enforceManualScope);

        List<EcheanceCredit> echeances = echeanceCreditRepository.findByCreditIdOrderByNumeroEcheanceAsc(creditId);
        if (echeances.isEmpty()) {
            throw new BusinessException("Aucune échéance trouvée pour ce crédit");
        }

        BigDecimal resteDuAvantPaiement = echeances.stream()
                .map(EcheanceCredit::getResteAPayer)
                .map(this::nvl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (request.getMontantTotal().compareTo(resteDuAvantPaiement) > 0) {
            throw new BusinessException("Le montant payé ne peut pas dépasser le reste dû du crédit");
        }

        EcheanceCredit echeanceReference = null;
        if (request.getEcheanceId() != null) {
            EcheanceCredit echeanceCible = echeanceCreditRepository.findById(request.getEcheanceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Échéance introuvable"));

            if (!echeanceCible.getCredit().getId().equals(credit.getId())) {
                throw new BusinessException("L'échéance ne correspond pas à ce crédit");
            }

            echeanceReference = echeanceCible;
        } else {
            for (EcheanceCredit echeance : echeances) {
                if (!estEcheanceSoldee(echeance)) {
                    echeanceReference = echeance;
                    break;
                }
            }
        }

        BigDecimal montantAImputer = nvl(request.getMontantTotal());
        BigDecimal principalEffectivementPaye = BigDecimal.ZERO;
        BigDecimal interetEffectivementPaye = BigDecimal.ZERO;
        BigDecimal penaliteEffectivementPayee = BigDecimal.ZERO;

        if (request.getEcheanceId() != null) {
            EcheanceCredit echeanceCible = echeanceCreditRepository.findById(request.getEcheanceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Échéance introuvable"));

            BigDecimal payePenalite = imputerSurPenalite(echeanceCible, montantAImputer, request);
            montantAImputer = montantAImputer.subtract(payePenalite);
            penaliteEffectivementPayee = penaliteEffectivementPayee.add(payePenalite);

            BigDecimal payeInteret = imputerSurInteret(echeanceCible, montantAImputer, request);
            montantAImputer = montantAImputer.subtract(payeInteret);
            interetEffectivementPaye = interetEffectivementPaye.add(payeInteret);

            BigDecimal payePrincipal = imputerSurPrincipal(echeanceCible, montantAImputer, request);
            montantAImputer = montantAImputer.subtract(payePrincipal);
            principalEffectivementPaye = principalEffectivementPaye.add(payePrincipal);

            recalculerEcheance(echeanceCible);
            echeanceCreditRepository.save(echeanceCible);
        } else {
            for (EcheanceCredit echeance : echeances) {
                if (montantAImputer.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }

                BigDecimal payePenalite = imputerSurPenalite(echeance, montantAImputer, request);
                montantAImputer = montantAImputer.subtract(payePenalite);
                penaliteEffectivementPayee = penaliteEffectivementPayee.add(payePenalite);

                BigDecimal payeInteret = imputerSurInteret(echeance, montantAImputer, request);
                montantAImputer = montantAImputer.subtract(payeInteret);
                interetEffectivementPaye = interetEffectivementPaye.add(payeInteret);

                BigDecimal payePrincipal = imputerSurPrincipal(echeance, montantAImputer, request);
                montantAImputer = montantAImputer.subtract(payePrincipal);
                principalEffectivementPaye = principalEffectivementPaye.add(payePrincipal);

                recalculerEcheance(echeance);
            }

            echeanceCreditRepository.saveAll(echeances);
        }

        BigDecimal totalEffectivementPaye = principalEffectivementPaye
                .add(interetEffectivementPaye)
                .add(penaliteEffectivementPayee);

        if (totalEffectivementPaye.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Aucun montant n'a pu être affecté aux échéances");
        }

        OperationEpargne operationEpargne = null;
        if (request.getModePaiement() == ModePaiement.COMPTE_EPARGNE) {
            operationEpargne = enregistrerDebitEpargnePourRemboursement(
                    credit,
                    membre,
                    totalEffectivementPaye,
                    request,
                    createdBy
            );
        }

        RemboursementCredit remboursement = RemboursementCredit.builder()
                .numeroRecu("TMP")
                .credit(credit)
                .echeance(echeanceReference)
                .membre(membre)
                .datePaiement(request.getDatePaiement())
                .montantPrincipal(principalEffectivementPaye)
                .montantInteret(interetEffectivementPaye)
                .montantPenalite(penaliteEffectivementPayee)
                .montantTotal(totalEffectivementPaye)
                .modePaiement(request.getModePaiement())
                .sessionCaisse(sessionCaisse)
                .operationEpargne(operationEpargne)
                .agent(agent)
                .observation(request.getObservation())
                .createdBy(createdBy)
                .build();

        remboursement = remboursementCreditRepository.save(remboursement);
        remboursement.setNumeroRecu(genererNumeroRecu(remboursement));
        remboursement = remboursementCreditRepository.save(remboursement);

        if (sessionCaisse != null) {
            OperationCaisseRequest opCaisse = new OperationCaisseRequest();
            opCaisse.setSessionCaisseId(sessionCaisse.getId());
            opCaisse.setCaisseId(sessionCaisse.getCaisse().getId());
            opCaisse.setDateOperation(request.getDatePaiement());
            opCaisse.setTypeOperation(TypeOperationCaisse.ENTREE);
            opCaisse.setCategorieOperation(CategorieOperationCaisse.REMBOURSEMENT_CREDIT);
            opCaisse.setMontant(totalEffectivementPaye);
            opCaisse.setDevise(credit.getDevise());
            opCaisse.setMembreId(membre.getId());
            opCaisse.setCreditId(credit.getId());
            opCaisse.setRemboursementId(remboursement.getId());
            opCaisse.setAgentId(request.getAgentId());
            opCaisse.setDescription("Encaissement remboursement crédit " + remboursement.getNumeroRecu());
            opCaisse.setCreatedBy(request.getCreatedBy());
            opCaisse.setModePaiement(request.getModePaiement());
            opCaisse.setObservation(request.getObservation());

            QuittanceCreateRequest quittanceRequest = new QuittanceCreateRequest();
            quittanceRequest.setMembreId(membre.getId());
            quittanceRequest.setTypeQuittance(TypeQuittance.REMBOURSEMENT);
            quittanceRequest.setReferenceOperation(remboursement.getNumeroRecu());
            quittanceRequest.setMontant(totalEffectivementPaye);
            quittanceRequest.setDevise(credit.getDevise());
            quittanceRequest.setDateEmission(request.getDatePaiement());
            quittanceRequest.setCreatedBy(request.getCreatedBy());

            opCaisse.setSource(SourceOperationCaisse.CREDIT_REMBOURSEMENT); // PATCH 6
            opCaisse.setReferenceExterne(remboursement.getNumeroRecu());    // PATCH 6
            opCaisse.setReferenceMetier(remboursement.getNumeroRecu());
            operationCaisseService.enregistrer(opCaisse);
            quittanceService.create(quittanceRequest);
        }

        if (operationEpargne != null) {
            auditService.logAction(
                AuditAction.REMBOURSEMENT_CREATED,
                AuditModule.CREDIT,
                "RemboursementCredit",
                remboursement.getId(),
                true,
                AuditSeverity.INFO,
                "Remboursement crédit par compte épargne"
                    + " | creditId=" + credit.getId()
                    + " | membreId=" + membre.getId()
                    + " | compteEpargneId=" + operationEpargne.getCompteEpargne().getId()
                    + " | montant=" + totalEffectivementPaye
                    + " | utilisateur=" + (createdBy != null ? createdBy.getId() : null)
                    + " | antenne=" + resolveAgenceIdRemboursementDirect(credit, membre)
                    + " | datePaiement=" + request.getDatePaiement()
                    + " | observation=" + (request.getObservation() == null ? "" : request.getObservation()),
                "REMBOURSEMENT-EPARGNE-" + remboursement.getNumeroRecu(),
                null,
                null,
                null,
                null,
                null,
                null,
                null
            );
        }

        BigDecimal principalRembourse = echeances.stream()
                .map(EcheanceCredit::getPrincipalPaye)
                .map(this::nvl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal penalitesCumulees = echeances.stream()
                .map(EcheanceCredit::getPenaliteCumulee)
                .map(this::nvl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal penalitesPayees = echeances.stream()
                .map(EcheanceCredit::getPenalitePayee)
                .map(this::nvl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        credit.setEncoursPrincipal(
                nvl(credit.getPrincipalTotal()).subtract(principalRembourse).max(BigDecimal.ZERO)
        );

        credit.setPenaliteTotal(
                penalitesCumulees.subtract(penalitesPayees).max(BigDecimal.ZERO)
        );

        boolean toutesPayees = echeances.stream().allMatch(this::estEcheanceSoldee);

        if (toutesPayees) {
            credit.setStatut(StatutCredit.REMBOURSE);
        } else if (credit.getPenaliteTotal().compareTo(BigDecimal.ZERO) > 0) {
            credit.setStatut(StatutCredit.EN_RETARD);
        } else {
            credit.setStatut(StatutCredit.EN_COURS);
        }

        creditRepository.save(credit);
        return remboursement;
    }

    private OperationEpargne enregistrerDebitEpargnePourRemboursement(Credit credit,
                                                                      Membre membre,
                                                                      BigDecimal montant,
                                                                      RemboursementRequest request,
                                                                      Utilisateur createdBy) {
        CompteEpargne compte = compteEpargneRepository.findFirstByMembreIdAndStatut(membre.getId(), StatutCompte.ACTIF)
                .orElseThrow(() -> new BusinessException("Aucun compte épargne actif trouvé pour ce membre"));

        if (!compte.getMembre().getId().equals(membre.getId())) {
            throw new BusinessException("Le compte épargne n'appartient pas à ce membre");
        }

        if (nvl(compte.getSoldeDisponible()).compareTo(montant) < 0) {
            throw new BusinessException("Solde disponible insuffisant sur le compte épargne");
        }

        compte.setSoldeDisponible(nvl(compte.getSoldeDisponible()).subtract(montant));

        OperationEpargne operation = OperationEpargne.builder()
                .compteEpargne(compte)
                .membre(membre)
                .credit(credit)
                .dateOperation(request.getDatePaiement())
                .typeOperation(TypeOperationEpargne.RETRAIT)
                .montant(montant)
                .sens(SensOperation.SORTIE)
                .modePaiement(ModePaiement.COMPTE_EPARGNE)
                .referenceExterne("REMBOURSEMENT-CREDIT-" + credit.getNumeroCredit())
                .observation(cleanText("Remboursement crédit par compte épargne. " + (request.getObservation() == null ? "" : request.getObservation())))
                .createdBy(createdBy)
                .build();

        OperationEpargne savedOperation = operationEpargneRepository.save(operation);
        compteEpargneRepository.save(compte);
        return savedOperation;
    }

    private String cleanText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private SessionCaisse resoudreSessionCaisseRemboursementDirect(RemboursementRequest request,
                                                                   Credit credit,
                                                                   Membre membre,
                                                                   Utilisateur createdBy,
                                                                   boolean enforceManualScope) {
        Long agenceId = resolveAgenceIdRemboursementDirect(credit, membre);

        if (request.getSessionCaisseId() != null) {
            SessionCaisse sessionCaisse = sessionCaisseRepository.findById(request.getSessionCaisseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Session caisse introuvable"));
            validerSessionCaissePourOperation(sessionCaisse);
            verifierSessionCaisseMemeAgence(sessionCaisse, agenceId);
            return sessionCaisse;
        }

        if (!enforceManualScope || request.getModePaiement() != ModePaiement.ESPECES) {
            return null;
        }

        if (createdBy == null) {
            createdBy = resolveCurrentUtilisateur()
                    .orElseThrow(() -> new BusinessException("L'utilisateur qui enregistre le remboursement est obligatoire"));
        }

        LocalDate dateComptable = request.getDatePaiement().toLocalDate();
        RoleCode roleCode = createdBy.getRole() != null ? createdBy.getRole().getCode() : null;

        if (roleCode == RoleCode.CAISSIER) {
            var sessionCaissier = sessionCaisseRepository.findFirstByUtilisateurIdAndDateComptableAndStatutOrderByDateOuvertureDesc(
                    createdBy.getId(),
                    dateComptable,
                    StatutSessionCaisse.OUVERTE
            );

            if (sessionCaissier.isPresent()) {
                SessionCaisse sessionCaisse = sessionCaissier.get();
                verifierSessionCaisseMemeAgence(sessionCaisse, agenceId);
                return sessionCaisse;
            }
        }

        List<SessionCaisse> sessionsOuvertes = sessionCaisseRepository.findByCaisse_Agence_IdAndDateComptableAndStatutOrderByDateOuvertureDesc(
                agenceId,
                dateComptable,
                StatutSessionCaisse.OUVERTE
        );

        if (sessionsOuvertes.isEmpty()) {
            throw new BusinessException("Une session de caisse ouverte est obligatoire pour un remboursement direct en espèces");
        }

        if (sessionsOuvertes.size() > 1) {
            throw new BusinessException("Plusieurs sessions de caisse ouvertes existent pour cette antenne. Veuillez sélectionner une session caisse.");
        }

        return sessionsOuvertes.get(0);
    }

    private Long resolveAgenceIdRemboursementDirect(Credit credit, Membre membre) {
        if (credit.getSite() != null && credit.getSite().getAgence() != null) {
            return credit.getSite().getAgence().getId();
        }

        if (membre.getSite() != null && membre.getSite().getAgence() != null) {
            return membre.getSite().getAgence().getId();
        }

        throw new BusinessException("Impossible de déterminer l'antenne du crédit pour rattacher la session de caisse");
    }

    private void verifierSessionCaisseMemeAgence(SessionCaisse sessionCaisse, Long agenceId) {
        Long sessionAgenceId = sessionCaisse.getCaisse() != null && sessionCaisse.getCaisse().getAgence() != null
                ? sessionCaisse.getCaisse().getAgence().getId()
                : null;

        if (sessionAgenceId == null || !sessionAgenceId.equals(agenceId)) {
            throw new BusinessException("La session caisse sélectionnée n'appartient pas à l'antenne du crédit");
        }
    }

    private java.util.Optional<Utilisateur> resolveCurrentUtilisateur() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return java.util.Optional.empty();
        }

        return utilisateurRepository.findByUsername(authentication.getName());
    }

    private boolean hasAnyCurrentRole(String... expectedRoles) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
            .anyMatch(authority -> {
                for (String expectedRole : expectedRoles) {
                    if (expectedRole.equals(authority.getAuthority())) {
                        return true;
                    }
                }
                return false;
            });
    }

    private void genererEcheancier(Credit credit) {
        if (credit.getNombreEcheances() == null || credit.getNombreEcheances() <= 0) {
            throw new BusinessException("Nombre d'échéances invalide");
        }

        BigDecimal nombre = BigDecimal.valueOf(credit.getNombreEcheances());

        BigDecimal principalParEcheance = credit.getPrincipalTotal()
                .divide(nombre, 2, RoundingMode.HALF_UP);

        BigDecimal interetParEcheance = credit.getInteretTotal()
                .divide(nombre, 2, RoundingMode.HALF_UP);

        BigDecimal principalCumule = BigDecimal.ZERO;
        BigDecimal interetCumule = BigDecimal.ZERO;

        for (int i = 1; i <= credit.getNombreEcheances(); i++) {
            BigDecimal principal = (i < credit.getNombreEcheances())
                    ? principalParEcheance
                    : credit.getPrincipalTotal().subtract(principalCumule);

            BigDecimal interet = (i < credit.getNombreEcheances())
                    ? interetParEcheance
                    : credit.getInteretTotal().subtract(interetCumule);

            BigDecimal total = principal.add(interet);

            EcheanceCredit echeance = EcheanceCredit.builder()
                    .credit(credit)
                    .numeroEcheance(i)
                    .dateEcheance(calculerDateEcheance(
                            credit.getDateApprobation(),
                            i,
                            credit.getPeriodiciteRemboursement()
                    ))
                    .principalPrevu(principal)
                    .interetPrevu(interet)
                    .principalPaye(BigDecimal.ZERO)
                    .interetPaye(BigDecimal.ZERO)
                    .penaliteCumulee(BigDecimal.ZERO)
                    .penalitePayee(BigDecimal.ZERO)
                    .totalPaye(BigDecimal.ZERO)
                    .totalPrevu(total)
                    .resteAPayer(total)
                    .statut(StatutEcheance.A_PAYER)
                    .build();

            echeanceCreditRepository.save(echeance);

            principalCumule = principalCumule.add(principal);
            interetCumule = interetCumule.add(interet);
        }
    }

    private BigDecimal calculerInteretTotal(BigDecimal principal,
                                            BigDecimal tauxInteretMensuel,
                                            Integer dureeValeur,
                                            DureeUnite dureeUnite,
                                            PeriodiciteRemboursement periodicite) {
        if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Le principal du crédit est invalide");
        }

        if (tauxInteretMensuel == null || tauxInteretMensuel.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Le taux d'intérêt mensuel est invalide");
        }

        if (dureeValeur == null || dureeValeur <= 0) {
            throw new BusinessException("La durée du crédit doit être supérieure à zéro");
        }

        if (dureeUnite == null) {
            throw new BusinessException("L'unité de durée du crédit est obligatoire");
        }

        if (periodicite == null) {
            throw new BusinessException("La périodicité de remboursement est obligatoire");
        }

        BigDecimal dureeEnMois = convertirDureeEnMois(dureeValeur, dureeUnite);

        return principal
                .multiply(tauxInteretMensuel)
                .multiply(dureeEnMois)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal convertirDureeEnMois(Integer dureeValeur, DureeUnite dureeUnite) {
        if (dureeValeur == null || dureeValeur <= 0) {
            throw new BusinessException("La durée du crédit doit être supérieure à zéro");
        }

        if (dureeUnite == null) {
            throw new BusinessException("L'unité de durée du crédit est obligatoire");
        }

        return switch (dureeUnite) {
            case JOUR -> BigDecimal.valueOf(dureeValeur)
                    .divide(BigDecimal.valueOf(30), 8, RoundingMode.HALF_UP);
            case SEMAINE -> BigDecimal.valueOf(dureeValeur)
                    .multiply(BigDecimal.valueOf(7))
                    .divide(BigDecimal.valueOf(30), 8, RoundingMode.HALF_UP);
            case MOIS -> BigDecimal.valueOf(dureeValeur);
        };
    }

    private int calculerNombreEcheances(Integer dureeValeur,
                                        DureeUnite dureeUnite,
                                        PeriodiciteRemboursement periodicite) {
        int dureeEnJours = convertirDureeEnJours(dureeValeur, dureeUnite);
        int periodiciteEnJours = convertirPeriodiciteEnJours(periodicite);

        if (periodiciteEnJours <= 0) {
            throw new BusinessException("Périodicité invalide");
        }

        int nombre = dureeEnJours / periodiciteEnJours;

        if (dureeEnJours % periodiciteEnJours != 0) {
            nombre++;
        }

        if (nombre <= 0) {
            throw new BusinessException("Impossible de calculer un nombre d'échéances valide");
        }

        return nombre;
    }

    private int convertirDureeEnJours(Integer dureeValeur, DureeUnite dureeUnite) {
        if (dureeValeur == null || dureeValeur <= 0) {
            throw new BusinessException("La durée du crédit doit être supérieure à zéro");
        }

        if (dureeUnite == null) {
            throw new BusinessException("L'unité de durée du crédit est obligatoire");
        }

        return switch (dureeUnite) {
            case JOUR -> dureeValeur;
            case SEMAINE -> dureeValeur * 7;
            case MOIS -> dureeValeur * 30;
        };
    }

    private int convertirPeriodiciteEnJours(PeriodiciteRemboursement periodicite) {
        if (periodicite == null) {
            throw new BusinessException("La périodicité de remboursement est obligatoire");
        }

        return switch (periodicite) {
            case JOURNALIER -> 1;
            case HEBDOMADAIRE -> 7;
            case MENSUEL -> 30;
        };
    }

    private LocalDate calculerDateEcheance(LocalDate dateBase,
                                           int numeroEcheance,
                                           PeriodiciteRemboursement periodicite) {
        if (dateBase == null) {
            throw new BusinessException("La date de base des échéances est invalide");
        }

        if (periodicite == null) {
            throw new BusinessException("La périodicité de remboursement est obligatoire");
        }

        return switch (periodicite) {
            case JOURNALIER -> dateBase.plusDays(numeroEcheance);
            case HEBDOMADAIRE -> dateBase.plusWeeks(numeroEcheance);
            case MENSUEL -> dateBase.plusMonths(numeroEcheance);
        };
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Long resolveAntenneId(Credit credit) {
        if (credit != null && credit.getSite() != null && credit.getSite().getAgence() != null) {
            return credit.getSite().getAgence().getId();
        }
        if (credit != null
                && credit.getDemandeCredit() != null
                && credit.getDemandeCredit().getSite() != null
                && credit.getDemandeCredit().getSite().getAgence() != null) {
            return credit.getDemandeCredit().getSite().getAgence().getId();
        }
        return null;
    }

    private Long resolveSiteId(Credit credit) {
        if (credit != null && credit.getSite() != null) {
            return credit.getSite().getId();
        }
        if (credit != null && credit.getDemandeCredit() != null && credit.getDemandeCredit().getSite() != null) {
            return credit.getDemandeCredit().getSite().getId();
        }
        return null;
    }

    private BigDecimal restantPrincipal(EcheanceCredit echeance) {
        return nvl(echeance.getPrincipalPrevu())
                .subtract(nvl(echeance.getPrincipalPaye()))
                .max(BigDecimal.ZERO);
    }

    private BigDecimal restantInteret(EcheanceCredit echeance) {
        return nvl(echeance.getInteretPrevu())
                .subtract(nvl(echeance.getInteretPaye()))
                .max(BigDecimal.ZERO);
    }

    private BigDecimal restantPenalite(EcheanceCredit echeance) {
        return nvl(echeance.getPenaliteCumulee())
                .subtract(nvl(echeance.getPenalitePayee()))
                .max(BigDecimal.ZERO);
    }

    private BigDecimal imputerSurPrincipal(EcheanceCredit echeance,
                                           BigDecimal montantDisponible,
                                           RemboursementRequest request) {
        if (montantDisponible.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal restant = restantPrincipal(echeance);
        if (restant.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal part = montantDisponible.min(restant);
        echeance.setPrincipalPaye(nvl(echeance.getPrincipalPaye()).add(part));
        echeance.setDateDernierPaiement(request.getDatePaiement().toLocalDate());
        return part;
    }

    private BigDecimal imputerSurInteret(EcheanceCredit echeance,
                                         BigDecimal montantDisponible,
                                         RemboursementRequest request) {
        if (montantDisponible.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal restant = restantInteret(echeance);
        if (restant.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal part = montantDisponible.min(restant);
        echeance.setInteretPaye(nvl(echeance.getInteretPaye()).add(part));
        echeance.setDateDernierPaiement(request.getDatePaiement().toLocalDate());
        return part;
    }

    private BigDecimal imputerSurPenalite(EcheanceCredit echeance,
                                          BigDecimal montantDisponible,
                                          RemboursementRequest request) {
        if (montantDisponible.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal restant = restantPenalite(echeance);
        if (restant.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal part = montantDisponible.min(restant);
        echeance.setPenalitePayee(nvl(echeance.getPenalitePayee()).add(part));
        echeance.setDateDernierPaiement(request.getDatePaiement().toLocalDate());
        return part;
    }

    private void recalculerEcheance(EcheanceCredit echeance) {
        BigDecimal totalPaye = nvl(echeance.getPrincipalPaye())
                .add(nvl(echeance.getInteretPaye()))
                .add(nvl(echeance.getPenalitePayee()));

        BigDecimal totalDu = nvl(echeance.getPrincipalPrevu())
                .add(nvl(echeance.getInteretPrevu()))
                .add(nvl(echeance.getPenaliteCumulee()));

        BigDecimal reste = totalDu.subtract(totalPaye).max(BigDecimal.ZERO);

        echeance.setTotalPaye(totalPaye);
        echeance.setTotalPrevu(totalDu);
        echeance.setResteAPayer(reste);

        if (reste.compareTo(BigDecimal.ZERO) == 0) {
            echeance.setStatut(StatutEcheance.PAYE);
        } else if (totalPaye.compareTo(BigDecimal.ZERO) > 0) {
            echeance.setStatut(StatutEcheance.PARTIEL);
        } else {
            echeance.setStatut(StatutEcheance.A_PAYER);
        }
    }

    private boolean estEcheanceSoldee(EcheanceCredit echeance) {
        return nvl(echeance.getResteAPayer()).compareTo(BigDecimal.ZERO) == 0;
    }

    private String genererNumeroCredit(Credit credit) {
        return "CR-" + LocalDate.now().getYear() + "-" + credit.getId();
    }

    private String genererNumeroRecu(RemboursementCredit remboursement) {
        return "REC-" + LocalDate.now().getYear() + "-" + remboursement.getId();
    }

        private CreditDetailResponse.ResumeCredit buildResumeCredit(Credit credit, DemandeCredit demande) {
        return CreditDetailResponse.ResumeCredit.builder()
            .id(credit.getId())
            .numeroCredit(credit.getNumeroCredit())
            .demandeCreditId(demande != null ? demande.getId() : null)
            .numeroDemande(demande != null ? demande.getNumeroDemande() : null)
            .membreNomComplet(credit.getMembre() != null ? credit.getMembre().getNomComplet() : null)
            .statut(credit.getStatut())
            .montantAccorde(credit.getMontantOctroye())
            .montantDecaisse(credit.getDateDecaissement() != null ? credit.getMontantOctroye() : BigDecimal.ZERO)
            .dateDemande(demande != null ? demande.getDateDemande() : null)
            .dateApprobation(credit.getDateApprobation())
            .dateDecaissement(credit.getDateDecaissement())
            .dureeValeur(credit.getDureeValeur())
            .dureeUnite(credit.getDureeUnite() != null ? credit.getDureeUnite().name() : null)
            .periodiciteRemboursement(credit.getPeriodiciteRemboursement() != null ? credit.getPeriodiciteRemboursement().name() : null)
            .objetCredit(demande != null ? demande.getObjetCredit() : null)
            .gagePropose(demande != null ? demande.getGagePropose() : null)
            .antenneId(credit.getSite() != null ? credit.getSite().getId() : null)
            .antenneNom(credit.getSite() != null ? credit.getSite().getNomSite() : null)
            .devise(credit.getDevise())
            .build();
        }

        private CreditEnCoursResponse toCreditEnCoursResponse(Credit credit) {
        DemandeCredit demande = credit.getDemandeCredit();
        GarantieCredit garantie = demande != null
            ? garantieCreditRepository.findByDemandeCreditId(demande.getId()).orElse(null)
            : null;
        List<EcheanceCredit> echeances = echeanceCreditRepository.findByCreditIdOrderByNumeroEcheanceAsc(credit.getId());
        List<RemboursementCredit> remboursements = remboursementCreditRepository.findByCreditIdOrderByDatePaiementAscIdAsc(credit.getId());
        CreditDetailResponse.Responsables responsables = buildResponsables(credit, demande, garantie, remboursements);

        BigDecimal totalPaye = remboursements.stream()
            .map(remboursement -> nvl(remboursement.getMontantTotal()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal resteAPayer = echeances.stream()
            .map(echeance -> nvl(echeance.getResteAPayer()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CreditEnCoursResponse.builder()
            .id(credit.getId())
            .numeroCredit(credit.getNumeroCredit())
            .membreNomComplet(credit.getMembre() != null ? credit.getMembre().getNomComplet() : null)
            .montantAccorde(nvl(credit.getMontantOctroye()))
            .totalPaye(totalPaye)
            .resteAPayer(resteAPayer)
            .dateDecaissement(credit.getDateDecaissement())
            .dureeValeur(credit.getDureeValeur())
            .dureeUnite(credit.getDureeUnite() != null ? credit.getDureeUnite().name() : null)
            .statut(credit.getStatut())
            .antenneNom(credit.getSite() != null ? credit.getSite().getNomSite() : null)
            .agentTerrainNom(responsables.getAgentTerrainNom())
            .gestionnaireNom(responsables.getGestionnaireNom())
            .controleurNom(responsables.getControleurNom())
            .chefBureauNom(responsables.getChefBureauNom())
            .caissierNom(responsables.getCaissierNom())
            .devise(credit.getDevise())
            .build();
        }

        private CreditRembourseResponse toCreditRembourseResponse(Credit credit) {
        List<EcheanceCredit> echeances = echeanceCreditRepository.findByCreditIdOrderByNumeroEcheanceAsc(credit.getId());
        List<RemboursementCredit> remboursements = remboursementCreditRepository.findByCreditIdOrderByDatePaiementAscIdAsc(credit.getId());

        BigDecimal totalPaye = remboursements.stream()
            .map(remboursement -> nvl(remboursement.getMontantTotal()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal resteAPayer = echeances.stream()
            .map(echeance -> nvl(echeance.getResteAPayer()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDateTime dateDernierPaiement = remboursements.stream()
            .map(RemboursementCredit::getDatePaiement)
            .filter(datePaiement -> datePaiement != null)
            .max(LocalDateTime::compareTo)
            .orElse(null);

        return CreditRembourseResponse.builder()
            .id(credit.getId())
            .numeroCredit(credit.getNumeroCredit())
            .membreNomComplet(credit.getMembre() != null ? credit.getMembre().getNomComplet() : null)
            .montantAccorde(nvl(credit.getMontantOctroye()))
            .totalInteret(nvl(credit.getInteretTotal()))
            .totalPaye(totalPaye)
            .resteAPayer(resteAPayer)
            .dateDecaissement(credit.getDateDecaissement())
            .dateDernierPaiement(dateDernierPaiement)
            .dateCloture(credit.getClotureAt())
            .statut(credit.getStatut())
            .antenneNom(credit.getSite() != null ? credit.getSite().getNomSite() : null)
            .devise(credit.getDevise())
            .build();
        }

        private CreditDetailResponse.Responsables buildResponsables(Credit credit,
                                    DemandeCredit demande,
                                    GarantieCredit garantie,
                                    List<RemboursementCredit> remboursements) {
        Utilisateur caissier = operationCaisseRepository
            .findFirstByCreditIdAndSourceOrderByDateOperationDescIdDesc(credit.getId(), SourceOperationCaisse.CREDIT_DECAISSEMENT)
            .map(operation -> operation.getCreatedBy() != null ? operation.getCreatedBy() : operation.getUtilisateur())
            .orElse(null);
        Employe gestionnaire = demande != null && demande.getAgent() != null ? demande.getAgent().getGestionnaire() : null;

        return CreditDetailResponse.Responsables.builder()
            .agentTerrainId(demande != null && demande.getAgent() != null ? demande.getAgent().getId() : null)
            .agentTerrainNom(demande != null && demande.getAgent() != null && demande.getAgent().getUtilisateur() != null
                ? demande.getAgent().getUtilisateur().getNomComplet()
                : null)
            .gestionnaireId(gestionnaire != null ? gestionnaire.getId() : demande != null && demande.getCreatedBy() != null ? demande.getCreatedBy().getId() : null)
            .gestionnaireNom(gestionnaire != null ? gestionnaire.getNomComplet() : demande != null && demande.getCreatedBy() != null ? demande.getCreatedBy().getNomComplet() : null)
            .controleurId(garantie != null && garantie.getControlePar() != null ? garantie.getControlePar().getId() : null)
            .controleurNom(garantie != null && garantie.getControlePar() != null ? garantie.getControlePar().getNomComplet() : null)
            .chefBureauId(demande != null && demande.getDecidedBy() != null ? demande.getDecidedBy().getId() : null)
            .chefBureauNom(demande != null && demande.getDecidedBy() != null ? demande.getDecidedBy().getNomComplet() : null)
            .caissierId(caissier != null ? caissier.getId() : null)
            .caissierNom(caissier != null ? caissier.getNomComplet() : null)
            .build();
        }

        private CreditDetailResponse.GarantieDetail buildGarantieDetail(Credit credit, GarantieCredit garantie) {
        if (garantie == null) {
            return CreditDetailResponse.GarantieDetail.builder()
                .montantGarantieRequis(BigDecimal.ZERO)
                .montantGarantieBloque(BigDecimal.ZERO)
                .sourceGarantie("Aucune garantie liée")
                .build();
        }

        String source = garantie.getCompteEpargne() != null
            ? "Compte épargne " + garantie.getCompteEpargne().getNumeroCompte()
            : "Garantie dossier crédit " + credit.getNumeroCredit();

        return CreditDetailResponse.GarantieDetail.builder()
            .montantGarantieRequis(nvl(garantie.getMontantGarantieRequis()))
            .montantGarantieBloque(nvl(garantie.getMontantGarantieBloque()))
            .sourceGarantie(source)
            .garantiesMateriellesAcceptees(buildGarantiesMateriellesAcceptees(garantie))
            .statutGarantie(garantie.getStatutGarantieEpargne())
            .dateBlocage(garantie.getDateBlocage())
            .dateLiberation(garantie.getDateLiberation())
            .build();
        }

        private String buildGarantiesMateriellesAcceptees(GarantieCredit garantie) {
        if (garantie == null || garantie.getGarantiesMaterielles() == null || garantie.getGarantiesMaterielles().isEmpty()) {
            return null;
        }
        String libelle = garantie.getGarantiesMaterielles().stream()
            .filter(materielle -> materielle.getStatut() == StatutGarantieMaterielle.ACCEPTEE)
            .map(this::formatGarantieMaterielle)
            .filter(value -> value != null && !value.isBlank())
            .collect(java.util.stream.Collectors.joining("; "));
        return libelle.isBlank() ? null : libelle;
        }

        private String formatGarantieMaterielle(GarantieMaterielle materielle) {
        if (materielle == null) {
            return null;
        }
        return materielle.getTypeBien() + " - " + materielle.getDescription()
            + " (" + nvl(materielle.getValeurEstimee()) + " " + materielle.getDevise() + ")";
        }

        private CreditDetailResponse.EcheanceDetail toEcheanceDetail(EcheanceCredit echeance) {
        BigDecimal penaliteRestante = nvl(echeance.getPenaliteCumulee()).subtract(nvl(echeance.getPenalitePayee())).max(BigDecimal.ZERO);

        return CreditDetailResponse.EcheanceDetail.builder()
            .id(echeance.getId())
            .numeroEcheance(echeance.getNumeroEcheance())
            .dateEcheance(echeance.getDateEcheance())
            .capitalDu(nvl(echeance.getPrincipalPrevu()))
            .interetDu(nvl(echeance.getInteretPrevu()))
            .penalite(penaliteRestante)
            .totalDu(nvl(echeance.getPrincipalPrevu()).add(nvl(echeance.getInteretPrevu())).add(penaliteRestante))
            .montantPaye(nvl(echeance.getTotalPaye()))
            .resteAPayer(nvl(echeance.getResteAPayer()))
            .statut(echeance.getStatut())
            .build();
        }

        private CreditDetailResponse.SuiviFinancier buildSuiviFinancier(Credit credit, List<EcheanceCredit> echeances) {
        BigDecimal capitalRembourse = sum(echeances.stream().map(EcheanceCredit::getPrincipalPaye).toList());
        BigDecimal interetsPayes = sum(echeances.stream().map(EcheanceCredit::getInteretPaye).toList());
        BigDecimal penalitesDues = sum(echeances.stream().map(EcheanceCredit::getPenaliteCumulee).toList());
        BigDecimal penalitesPayees = sum(echeances.stream().map(EcheanceCredit::getPenalitePayee).toList());
        BigDecimal totalPaye = sum(echeances.stream().map(EcheanceCredit::getTotalPaye).toList());
        BigDecimal totalRestant = sum(echeances.stream().map(EcheanceCredit::getResteAPayer).toList());

        return CreditDetailResponse.SuiviFinancier.builder()
            .capitalInitial(nvl(credit.getPrincipalTotal()))
            .capitalRembourse(capitalRembourse)
            .capitalRestant(nvl(credit.getPrincipalTotal()).subtract(capitalRembourse).max(BigDecimal.ZERO))
            .interetsAttendus(nvl(credit.getInteretTotal()))
            .interetsPayes(interetsPayes)
            .interetsRestants(nvl(credit.getInteretTotal()).subtract(interetsPayes).max(BigDecimal.ZERO))
            .penalitesDues(penalitesDues.subtract(penalitesPayees).max(BigDecimal.ZERO))
            .penalitesPayees(penalitesPayees)
            .totalPaye(totalPaye)
            .totalRestant(totalRestant)
            .build();
        }

        private CreditDetailResponse.RemboursementDetail toRemboursementDetail(RemboursementCredit remboursement) {
        return CreditDetailResponse.RemboursementDetail.builder()
            .id(remboursement.getId())
            .numeroRecu(remboursement.getNumeroRecu())
            .datePaiement(remboursement.getDatePaiement())
            .montantPaye(nvl(remboursement.getMontantTotal()))
            .capitalPaye(nvl(remboursement.getMontantPrincipal()))
            .interetPaye(nvl(remboursement.getMontantInteret()))
            .penalitePayee(nvl(remboursement.getMontantPenalite()))
            .utilisateurNom(remboursement.getCreatedBy() != null ? remboursement.getCreatedBy().getNomComplet() : null)
            .source(remboursement.getModePaiement() != null ? remboursement.getModePaiement().name() : null)
            .observation(remboursement.getObservation())
            .build();
        }

        private List<CreditDetailResponse.HistoriqueEvent> buildHistorique(Credit credit,
                                           DemandeCredit demande,
                                           GarantieCredit garantie,
                                           List<RemboursementCredit> remboursements,
                                           List<AuditLog> auditLogs) {
        List<CreditDetailResponse.HistoriqueEvent> events = new java.util.ArrayList<>();

        if (demande != null) {
            events.add(CreditDetailResponse.HistoriqueEvent.builder()
                .type("Demande créée")
                .utilisateur(demande.getCreatedBy() != null ? demande.getCreatedBy().getNomComplet() : null)
                .dateHeure(demande.getDateCreation())
                .antenneId(demande.getSite() != null ? demande.getSite().getId() : null)
                .antenneNom(demande.getSite() != null ? demande.getSite().getNomSite() : null)
                .commentaire(demande.getNumeroDemande())
                .build());
        }

        events.add(CreditDetailResponse.HistoriqueEvent.builder()
            .type("Crédit approuvé")
            .utilisateur(demande != null && demande.getDecidedBy() != null ? demande.getDecidedBy().getNomComplet() : null)
            .dateHeure(demande != null ? demande.getDateDecision() : null)
            .antenneId(credit.getSite() != null ? credit.getSite().getId() : null)
            .antenneNom(credit.getSite() != null ? credit.getSite().getNomSite() : null)
            .commentaire(credit.getNumeroCredit())
            .build());

        if (garantie != null && garantie.getDateBlocage() != null) {
            events.add(CreditDetailResponse.HistoriqueEvent.builder()
                .type("Garantie bloquée")
                .utilisateur(garantie.getControlePar() != null ? garantie.getControlePar().getNomComplet() : null)
                .dateHeure(garantie.getDateBlocage())
                .antenneId(credit.getSite() != null ? credit.getSite().getId() : null)
                .antenneNom(credit.getSite() != null ? credit.getSite().getNomSite() : null)
                .commentaire(nvl(garantie.getMontantGarantieBloque()) + " " + garantie.getDevise())
                .build());
        }

        if (credit.getDateDecaissement() != null) {
            events.add(CreditDetailResponse.HistoriqueEvent.builder()
                .type("Crédit décaissé")
                .utilisateur(null)
                .dateHeure(credit.getDateDecaissement().atStartOfDay())
                .antenneId(credit.getSite() != null ? credit.getSite().getId() : null)
                .antenneNom(credit.getSite() != null ? credit.getSite().getNomSite() : null)
                .commentaire(nvl(credit.getMontantOctroye()) + " " + credit.getDevise())
                .build());
        }

        remboursements.stream()
            .map(remboursement -> CreditDetailResponse.HistoriqueEvent.builder()
                .type("Remboursement enregistré")
                .utilisateur(remboursement.getCreatedBy() != null ? remboursement.getCreatedBy().getNomComplet() : null)
                .dateHeure(remboursement.getDatePaiement())
                .antenneId(credit.getSite() != null ? credit.getSite().getId() : null)
                .antenneNom(credit.getSite() != null ? credit.getSite().getNomSite() : null)
                .commentaire(remboursement.getNumeroRecu())
                .build())
            .forEach(events::add);

        auditLogs.stream()
            .map(log -> CreditDetailResponse.HistoriqueEvent.builder()
                .type(log.getAction() != null ? log.getAction().getDescription() : "Audit")
                .utilisateur(log.getUsername())
                .dateHeure(log.getDateAction())
                .antenneId(log.getSiteId())
                .antenneNom(log.getSiteLibelle())
                .commentaire(log.getCommentaire() != null ? log.getCommentaire() : log.getReason())
                .build())
            .forEach(events::add);

        return events.stream()
            .filter(event -> event.getDateHeure() != null)
            .sorted(java.util.Comparator.comparing(CreditDetailResponse.HistoriqueEvent::getDateHeure))
            .toList();
        }

        private BigDecimal sum(List<BigDecimal> values) {
        return values.stream().map(this::nvl).reduce(BigDecimal.ZERO, BigDecimal::add);
        }

    @Override
    public Page<CreditResponse> getAll(Pageable pageable) {
        // PHASE 3B: Return paginated list of all credits
        return creditRepository.findAll(pageable).map(creditMapper::toResponse);
    }
}