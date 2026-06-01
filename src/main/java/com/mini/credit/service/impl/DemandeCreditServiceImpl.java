package com.mini.credit.service.impl;

import com.mini.credit.dto.credit.AnalyseRisqueRequest;
import com.mini.credit.dto.credit.DemandeCreditCreateRequest;
import com.mini.credit.dto.credit.DemandeCreditResponse;
import com.mini.credit.entity.credit.AnalyseRisque;
import com.mini.credit.entity.credit.DemandeCredit;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.AgentTerrain;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.StatutDemandeCredit;
import com.mini.credit.enums.StatutMembre;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.exception.ResourceNotFoundException;
import com.mini.credit.mapper.CreditMapper;
import com.mini.credit.repository.agentTerrain.AgentTerrainRepository;
import com.mini.credit.repository.credit.AnalyseRisqueRepository;
import com.mini.credit.repository.credit.DemandeCreditRepository;
import com.mini.credit.repository.membre.MembreRepository;
import com.mini.credit.repository.site.SiteRepository;
import com.mini.credit.repository.utilisateur.UtilisateurRepository;
import com.mini.credit.service.DemandeCreditService;
import com.mini.credit.service.audit.Auditable;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.service.security.ScopeService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
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
    private final CreditMapper creditMapper;
    private final ScopeService scopeService;

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
        BigDecimal fraisDemande = normalizeMoney(request.getFraisDemande());

        // AJOUT PHASE 2: Valider la scope d'accès (AGENT_BUREAU limité à son site, MEMBER pour soi-même)
        if (!scopeService.canCreateDemandeCredit(request.getMembreId())) {
            throw new BusinessException("Accès refusé: vous n'êtes pas autorisé à créer une demande de crédit pour ce membre");
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
                .activiteFinancee(cleanNullableText(request.getActiviteFinancee()))
                .revenusEstimes(revenusEstimes)
                .chargesEstimees(chargesEstimees)
                .fraisDemande(fraisDemande)
                .depotGarantieRequis(depotGarantieRequis)
                .depotGarantiePaye(ZERO)
                .statut(StatutDemandeCredit.SOUMISE)
                .build();

        DemandeCredit saved = demandeCreditRepository.save(demande);
        return creditMapper.toResponse(saved);
    }

    @Override
    public DemandeCreditResponse getById(Long id) {
        DemandeCredit demande = demandeCreditRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));

        return creditMapper.toResponse(demande);
    }

    @Override
    public Page<DemandeCreditResponse> getAll(Pageable pageable) {
        return demandeCreditRepository.findAll(pageable)
                .map(creditMapper::toResponse);
    }

    @Override
    public List<DemandeCreditResponse> getAll() {
        return demandeCreditRepository.findAll()
                .stream()
                .map(creditMapper::toResponse)
                .toList();
    }

    @Override
    public List<DemandeCreditResponse> getByMembre(Long membreId) {
        return demandeCreditRepository.findByMembreId(membreId)
                .stream()
                .map(creditMapper::toResponse)
                .toList();
    }

    @Override
    @Auditable(action = AuditAction.ANALYSE_RISQUE_CREATED, entityType = "DemandeCredit", entityIdParameter = "demandeId")
    public DemandeCreditResponse ajouterAnalyse(Long demandeId, AnalyseRisqueRequest request) {
        DemandeCredit demande = demandeCreditRepository.findById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));

        validatePaiementInitialAvantAnalyse(demande);

        Utilisateur analyste = utilisateurRepository.findById(request.getAnalysteId())
                .orElseThrow(() -> new ResourceNotFoundException("Analyste introuvable"));

        AnalyseRisque analyse = demande.getAnalyseRisque();
        if (analyse == null) {
            analyse = new AnalyseRisque();
            analyse.setDemandeCredit(demande);
        }

        analyse.setAnalyste(analyste);
        analyse.setDateVisite(request.getDateVisite());
        analyse.setLieuVisite(request.getLieuVisite());
        analyse.setActiviteVerifiee(request.getActiviteVerifiee());
        analyse.setDescriptionActivite(request.getDescriptionActivite());
        analyse.setAncienneteActivite(request.getAncienneteActivite());
        analyse.setChiffreAffairesEstime(request.getChiffreAffairesEstime());
        analyse.setRevenuNetEstime(request.getRevenuNetEstime());
        analyse.setChargesMensuelles(request.getChargesMensuelles());
        analyse.setCapaciteRemboursement(request.getCapaciteRemboursement());
        analyse.setRisqueNiveau(request.getRisqueNiveau());
        analyse.setScoreRisque(request.getScoreRisque());
        analyse.setRecommandation(request.getRecommandation());
        analyse.setCommentaire(request.getCommentaire());

        analyseRisqueRepository.save(analyse);

        demande.setAnalyseRisque(analyse);
        demande.setStatut(StatutDemandeCredit.EN_ANALYSE);

        DemandeCredit saved = demandeCreditRepository.save(demande);
        return creditMapper.toResponse(saved);
    }

    private void validatePaiementInitialAvantAnalyse(DemandeCredit demande) {
        BigDecimal fraisDemande = normalizeMoney(demande.getFraisDemande());
        BigDecimal fraisPayes = normalizeMoney(demande.getFraisDemandePayes());

        BigDecimal depotRequis = normalizeMoney(demande.getDepotGarantieRequis());
        BigDecimal depotPaye = normalizeMoney(demande.getDepotGarantiePaye());

        BigDecimal fraisRestants = fraisDemande.subtract(fraisPayes).max(ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal depotRestant = depotRequis.subtract(depotPaye).max(ZERO).setScale(2, RoundingMode.HALF_UP);

        if (fraisRestants.compareTo(ZERO) > 0 || depotRestant.compareTo(ZERO) > 0) {
            throw new BusinessException(
                    "Le paiement initial doit être soldé avant l'analyse : " +
                            "frais restants = " + fraisRestants + ", dépôt restant = " + depotRestant
            );
        }
    }

    private void validateCreationRequest(BigDecimal montantDemande,
                                         BigDecimal tauxInteret,
                                         BigDecimal revenusEstimes,
                                         BigDecimal chargesEstimees,
                                         BigDecimal fraisDemande) {

        if (montantDemande == null || montantDemande.compareTo(ZERO) <= 0) {
            throw new BusinessException("Le montant demandé doit être supérieur à 0");
        }

        if (tauxInteret == null || tauxInteret.compareTo(ZERO) < 0) {
            throw new BusinessException("Le taux d'intérêt ne peut pas être négatif");
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

    private String normalizeDevise(String devise) {
        if (devise == null || devise.isBlank()) {
            return "CDF";
        }
        return devise.trim().toUpperCase();
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
}