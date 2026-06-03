package com.mini.credit.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.mini.credit.dto.caisse.OperationCaisseRequest;
import com.mini.credit.dto.credit.ApprobationCreditRequest;
import com.mini.credit.dto.credit.CreditResponse;
import com.mini.credit.dto.credit.DecaissementCreditRequest;
import com.mini.credit.dto.credit.RemboursementRequest;
import com.mini.credit.dto.document.QuittanceCreateRequest;
import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.entity.credit.Credit;
import com.mini.credit.entity.credit.DemandeCredit;
import com.mini.credit.entity.credit.EcheanceCredit;
import com.mini.credit.entity.credit.RemboursementCredit;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.AgentTerrain;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.CategorieOperationCaisse;
import com.mini.credit.enums.DureeUnite;
import com.mini.credit.enums.PeriodiciteRemboursement;
import com.mini.credit.enums.StatutCredit;
import com.mini.credit.enums.StatutDemandeCredit;
import com.mini.credit.enums.StatutEcheance;
import com.mini.credit.enums.StatutSessionCaisse;
import com.mini.credit.enums.TypeOperationCaisse;
import com.mini.credit.enums.TypeQuittance;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.exception.ResourceNotFoundException;
import com.mini.credit.mapper.CreditMapper;
import com.mini.credit.repository.agentTerrain.AgentTerrainRepository;
import com.mini.credit.repository.caisse.SessionCaisseRepository;
import com.mini.credit.repository.credit.CreditRepository;
import com.mini.credit.repository.credit.DemandeCreditRepository;
import com.mini.credit.repository.credit.EcheanceCreditRepository;
import com.mini.credit.repository.credit.RemboursementCreditRepository;
import com.mini.credit.repository.membre.MembreRepository;
import com.mini.credit.repository.utilisateur.UtilisateurRepository;
import com.mini.credit.service.CreditService;
import com.mini.credit.service.OperationCaisseService;
import com.mini.credit.service.QuittanceService;
import com.mini.credit.service.security.ScopeService;
import com.mini.credit.service.audit.Auditable;
import com.mini.credit.enums.security.AuditAction;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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
    private final SessionCaisseRepository sessionCaisseRepository;
    private final AgentTerrainRepository agentTerrainRepository;
    private final MembreRepository membreRepository;
    private final CreditMapper creditMapper;
    private final OperationCaisseService operationCaisseService;
    private final QuittanceService quittanceService;
    private final ScopeService scopeService;

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)  // PHASE 9: SERIALIZABLE isolation for race condition prevention
    @Auditable(action = AuditAction.CREDIT_APPROVED, entityType = "DemandeCredit", entityIdParameter = "demandeId")
    public CreditResponse approuverDemande(Long demandeId, ApprobationCreditRequest request) {
        DemandeCredit demande = demandeCreditRepository.findById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));

        Utilisateur decidedBy = utilisateurRepository.findById(request.getDecidedBy())
                .orElseThrow(() -> new ResourceNotFoundException("Décideur introuvable"));

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
            case SOUMISE, EN_ANALYSE -> {
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

        if (demande.getCredit() != null) {
            throw new BusinessException("Cette demande a déjà généré un crédit");
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
    public List<CreditResponse> getAll() {
        return creditRepository.findAll().stream()
                .map(creditMapper::toResponse)
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
    @Auditable(action = AuditAction.CREDIT_DISBURSED, entityType = "Credit", entityIdParameter = "creditId")
    public CreditResponse decaisserCredit(Long creditId, DecaissementCreditRequest request) {
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

        return creditMapper.toResponse(credit);
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

        // AJOUT PHASE 2: Valider la scope d'accès (MEMBER limité à ses propres crédits)
        if (!scopeService.canRecordRemboursement(creditId)) {
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

        SessionCaisse sessionCaisse = null;
        if (request.getSessionCaisseId() != null) {
            sessionCaisse = sessionCaisseRepository.findById(request.getSessionCaisseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Session caisse introuvable"));

            validerSessionCaissePourOperation(sessionCaisse);
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

        List<EcheanceCredit> echeances = echeanceCreditRepository.findByCreditIdOrderByNumeroEcheanceAsc(creditId);
        if (echeances.isEmpty()) {
            throw new BusinessException("Aucune échéance trouvée pour ce crédit");
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

            operationCaisseService.enregistrer(opCaisse);
            quittanceService.create(quittanceRequest);
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

    @Override
    public Page<CreditResponse> getAll(Pageable pageable) {
        // PHASE 3B: Return paginated list of all credits
        return creditRepository.findAll(pageable).map(creditMapper::toResponse);
    }
}