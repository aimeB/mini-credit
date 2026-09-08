package com.mini.credit.service.impl;

import com.mini.credit.dto.referentiel.CreateRecetteTerrainRequest;
import com.mini.credit.dto.referentiel.RecetteTerrainResponse;
import com.mini.credit.dto.referentiel.UpdateRecetteTerrainRequest;
import com.mini.credit.dto.referentiel.ValidateRecetteTerrainRequest;
import com.mini.credit.entity.referentiel.AgentTerrain;
import com.mini.credit.entity.referentiel.RecetteTerrainJournaliere;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.RecetteStatut;
import com.mini.credit.mapper.RecetteTerrainMapper;
import com.mini.credit.repository.UtilisateurRepository;
import com.mini.credit.repository.recetteTerrain.RecetteTerrainRepository;
import com.mini.credit.repository.referentiel.SiteRepository;
import com.mini.credit.repository.agentTerrain.AgentTerrainRepository;
import com.mini.credit.service.RecetteTerrainService;
import com.mini.credit.service.WorkflowTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implémentation du service RecetteTerrainService
 * Gère la création, modification et validation des recettes terrain
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RecetteTerrainServiceImpl implements RecetteTerrainService {

    private final RecetteTerrainRepository recetteRepository;
    private final AgentTerrainRepository agentTerrainRepository;
    private final SiteRepository siteRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final RecetteTerrainMapper recetteMapper;
    private final WorkflowTaskService workflowTaskService;

    // ========== CRUD ==========

    @Override
    public RecetteTerrainResponse create(CreateRecetteTerrainRequest request) {
        AgentAndSiteResolution resolution = resolveAgentAndSiteForCreate(request);
        Long agentTerrainId = resolution.agentTerrainId();
        Long siteId = resolution.siteId();

        // Validations
        validerAgent(agentTerrainId);
        validerSite(siteId);
        validerMemeAgence(agentTerrainId, siteId);
        validerAgentAffecteAuSite(agentTerrainId, siteId);
        validerPasDoublon(agentTerrainId, request.getDateRecette());
        validerMontants(request);

        // Mapper et créer
        RecetteTerrainJournaliere recette = recetteMapper.toEntity(request);
        recette.setAgentTerrain(agentTerrainRepository.findById(agentTerrainId).get());
        recette.setSite(siteRepository.findById(siteId).get());
        recette.setDateCreation(LocalDateTime.now());

        // Calcul des écarts automatique via @PrePersist
        RecetteTerrainJournaliere saved = recetteRepository.save(recette);
        return recetteMapper.toDTO(saved);
    }

    @Override
    public RecetteTerrainResponse update(Long id, UpdateRecetteTerrainRequest request) {
        RecetteTerrainJournaliere recette = recetteRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Recette not found: " + id));
        assertCurrentAgentOwnsRecetteIfNeeded(recette);

        // Vérifier que c'est BROUILLON
        if (recette.getStatut() != RecetteStatut.BROUILLON) {
            throw new RuntimeException("Cannot modify recette with status: " + recette.getStatut());
        }

        // Appliquer mises à jour
        recetteMapper.updateEntityFromDTO(request, recette);
        recette.setDateModification(LocalDateTime.now());

        // Validation des montants
        validerMontants(request);

        RecetteTerrainJournaliere saved = recetteRepository.save(recette);
        return recetteMapper.toDTO(saved);
    }

    @Override
    public RecetteTerrainResponse getById(Long id) {
        RecetteTerrainJournaliere recette = recetteRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Recette not found: " + id));
        assertCurrentAgentOwnsRecetteIfNeeded(recette);
        return recetteMapper.toDTO(recette);
    }

    @Override
    public List<RecetteTerrainResponse> getAll() {
        return recetteRepository.findAll().stream()
            .map(recetteMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public void delete(Long id) {
        RecetteTerrainJournaliere recette = recetteRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Recette not found: " + id));
        assertCurrentAgentOwnsRecetteIfNeeded(recette);

        // Vérifier que c'est BROUILLON
        if (recette.getStatut() != RecetteStatut.BROUILLON) {
            throw new RuntimeException("Cannot delete recette with status: " + recette.getStatut());
        }

        recette.setActif(false);
        recetteRepository.save(recette);
    }

    // ========== RECHERCHES ==========

    @Override
    public List<RecetteTerrainResponse> getByAgentTerrain(Long agentTerrainId) {
        return recetteRepository.findByAgentTerrainIdAndActifTrue(agentTerrainId).stream()
            .map(recetteMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public List<RecetteTerrainResponse> getMine() {
        Utilisateur currentUser = getCurrentUser();
        AgentTerrain myAgent = agentTerrainRepository.findByUtilisateurId(currentUser.getId())
            .orElseThrow(() -> new RuntimeException("Aucun profil AgentTerrain lié à l'utilisateur connecté"));
        return getByAgentTerrain(myAgent.getId());
    }

    @Override
    public List<RecetteTerrainResponse> getBySite(Long siteId) {
        return recetteRepository.findBySiteIdAndActifTrue(siteId).stream()
            .map(recetteMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public List<RecetteTerrainResponse> getByDateRecette(LocalDate date) {
        return recetteRepository.findByDateRecetteAndActifTrue(date).stream()
            .map(recetteMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public List<RecetteTerrainResponse> getByStatut(RecetteStatut statut) {
        return recetteRepository.findByStatutAndActifTrue(statut).stream()
            .map(recetteMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public List<RecetteTerrainResponse> getBySiteAndStatut(Long siteId, RecetteStatut statut) {
        return recetteRepository.findBySiteIdAndStatutAndActifTrue(siteId, statut).stream()
            .map(recetteMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public List<RecetteTerrainResponse> getByDateRange(LocalDate debut, LocalDate fin) {
        return recetteRepository.findByDateRecetteBetweenAndActifTrue(debut, fin).stream()
            .map(recetteMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public List<RecetteTerrainResponse> getEnAttenteValidation(Long siteId) {
        return recetteRepository.findEnAttenteValidationBySite(siteId).stream()
            .map(recetteMapper::toDTO)
            .collect(Collectors.toList());
    }

    // ========== WORKFLOW ==========

    @Override
    public RecetteTerrainResponse soumettre(Long id) {
        RecetteTerrainJournaliere recette = recetteRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Recette not found: " + id));
        assertCurrentAgentOwnsRecetteIfNeeded(recette);

        // Vérifier statut BROUILLON
        if (recette.getStatut() != RecetteStatut.BROUILLON) {
            throw new RuntimeException("Only BROUILLON recettes can be submitted. Current status: " + recette.getStatut());
        }

        // Vérifier complétude
        if (!recette.isComplete()) {
            throw new RuntimeException("Recette incomplete. Missing required fields.");
        }

        // Changer statut
        recette.setStatut(RecetteStatut.SOUMISE);
        recette.setDateModification(LocalDateTime.now());

        RecetteTerrainJournaliere saved = recetteRepository.save(recette);

        String referenceMetier = buildRecetteReferenceMetier(saved);
        Long antenneId = resolveAntenneId(saved);
        Long siteId = resolveSiteId(saved);
        workflowTaskService.onRecetteSoumise(saved.getId(), referenceMetier, antenneId, siteId);
        if (hasEcart(saved)) {
            workflowTaskService.onRecetteEcartConstate(saved.getId(), referenceMetier, antenneId, siteId);
        }

        return recetteMapper.toDTO(saved);
    }

    @Override
    public RecetteTerrainResponse valider(Long id, ValidateRecetteTerrainRequest request) {
        assertCanValidate();
        RecetteTerrainJournaliere recette = recetteRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Recette not found: " + id));

        // Vérifier statut SOUMISE
        if (recette.getStatut() != RecetteStatut.SOUMISE) {
            throw new RuntimeException("Only SOUMISE recettes can be validated. Current status: " + recette.getStatut());
        }

        // Traiter décision
        if ("VALIDEE".equals(request.getDecision())) {
            recette.setStatut(RecetteStatut.VALIDEE);
            recette.setDateValidation(LocalDateTime.now());
        } else if ("REJETEE".equals(request.getDecision())) {
            if (request.getMotifRejet() == null || request.getMotifRejet().isEmpty()) {
                throw new RuntimeException("Motif de rejet requis pour rejeter une recette");
            }
            recette.setStatut(RecetteStatut.REJETEE);
            recette.setMotifRejet(request.getMotifRejet());
            recette.setDateValidation(LocalDateTime.now());
        } else {
            throw new RuntimeException("Invalid decision: " + request.getDecision());
        }

        // Enregistrer qui valide
        if (request.getValidePar() != null) {
            Utilisateur valideur = utilisateurRepository.findById(request.getValidePar())
                .orElseThrow(() -> new RuntimeException("Utilisateur not found: " + request.getValidePar()));
            recette.setValidePar(valideur);
        }

        recette.setDateModification(LocalDateTime.now());

        RecetteTerrainJournaliere saved = recetteRepository.save(recette);

        String referenceMetier = buildRecetteReferenceMetier(saved);
        Long antenneId = resolveAntenneId(saved);
        Long siteId = resolveSiteId(saved);
        if (saved.getStatut() == RecetteStatut.VALIDEE) {
            workflowTaskService.onRecetteValidee(saved.getId(), referenceMetier, antenneId, siteId);
        } else if (saved.getStatut() == RecetteStatut.REJETEE) {
            workflowTaskService.onRecetteRejetee(saved.getId(), referenceMetier, antenneId, siteId);
        }

        return recetteMapper.toDTO(saved);
    }

    @Override
    public RecetteTerrainResponse rejeter(Long id, String motifRejet, Long validePar) {
        ValidateRecetteTerrainRequest request = ValidateRecetteTerrainRequest.builder()
            .decision("REJETEE")
            .motifRejet(motifRejet)
            .validePar(validePar)
            .build();
        return valider(id, request);
    }

    // ========== MÉTIERS ==========

    @Override
    public RecetteTerrainResponse calculerEcarts(Long id) {
        RecetteTerrainJournaliere recette = recetteRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Recette not found: " + id));

        // Le calcul se fait via @PrePersist/@PreUpdate
        // Ici on force juste le recalcul
        recette.calculerEcarts();

        RecetteTerrainJournaliere saved = recetteRepository.save(recette);
        return recetteMapper.toDTO(saved);
    }

    @Override
    public BigDecimal getTotalCollecteByAgentAndDate(Long agentTerrainId, LocalDate date) {
        RecetteTerrainJournaliere recette = recetteRepository
            .findByAgentTerrainIdAndDateRecetteAndActifTrue(agentTerrainId, date)
            .orElse(null);

        if (recette == null) {
            return BigDecimal.ZERO;
        }

        return recette.getTotalCollecte();
    }

    // ========== VALIDATIONS PRIVÉES ==========

    public void validerAgent(Long agentTerrainId) {
        AgentTerrain agent = agentTerrainRepository.findById(agentTerrainId)
            .orElseThrow(() -> new RuntimeException("Agent terrain not found: " + agentTerrainId));

        if (!agent.getActif()) {
            throw new RuntimeException("Agent terrain is not active: " + agentTerrainId);
        }
    }

    public void validerSite(Long siteId) {
        Site site = siteRepository.findById(siteId)
            .orElseThrow(() -> new RuntimeException("Site not found: " + siteId));

        if (!site.getActif()) {
            throw new RuntimeException("Site is not active: " + siteId);
        }
    }

    public void validerMemeAgence(Long agentTerrainId, Long siteId) {
        AgentTerrain agent = agentTerrainRepository.findById(agentTerrainId).get();
        Site site = siteRepository.findById(siteId).get();

        if (!agent.getSite().getAgence().getId().equals(site.getAgence().getId())) {
            throw new RuntimeException("Agent and Site must belong to same agence");
        }
    }

    public void validerAgentAffecteAuSite(Long agentTerrainId, Long siteId) {
        AgentTerrain agent = agentTerrainRepository.findById(agentTerrainId).get();

        // Vérifier agent affecté au site (via sitesAffectes ou site principal)
        boolean affecte = agent.getSite().getId().equals(siteId) ||
            (agent.getSitesAffectes() != null && agent.getSitesAffectes().stream()
                .anyMatch(s -> s.getId().equals(siteId)));

        if (!affecte) {
            throw new RuntimeException("Agent is not assigned to this site: " + siteId);
        }
    }

    private void validerPasDoublon(Long agentTerrainId, LocalDate dateRecette) {
        boolean exists = recetteRepository.existsByAgentTerrainIdAndDateRecetteAndActifTrue(
            agentTerrainId, dateRecette);

        if (exists) {
            throw new RuntimeException("Recette already exists for this agent and date");
        }
    }

    private void validerMontants(CreateRecetteTerrainRequest request) {
        if (safeAmount(request.getEpargneCollectee()).compareTo(BigDecimal.ZERO) < 0
            || safeAmount(request.getRemboursementsCreditCollectes()).compareTo(BigDecimal.ZERO) < 0
            || safeAmount(request.getFraisCollectes()).compareTo(BigDecimal.ZERO) < 0
            || safeAmount(request.getEspecesRemises()).compareTo(BigDecimal.ZERO) < 0
            || safeAmount(request.getEspecesEmises()).compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("All amounts must be >= 0");
        }

        BigDecimal totalCollecte = safeAmount(request.getEpargneCollectee())
            .add(safeAmount(request.getRemboursementsCreditCollectes()))
            .add(safeAmount(request.getFraisCollectes()));
        BigDecimal ecartTresorerie = safeAmount(request.getEspecesRemises()).subtract(totalCollecte);
        if (ecartTresorerie.compareTo(BigDecimal.ZERO) != 0
            && (request.getObservations() == null || request.getObservations().trim().isEmpty())) {
            throw new RuntimeException("Observation obligatoire si écart trésorerie différent de 0");
        }
    }

    private void validerMontants(UpdateRecetteTerrainRequest request) {
        if (safeAmount(request.getEpargneCollectee()).compareTo(BigDecimal.ZERO) < 0
            || safeAmount(request.getRemboursementsCreditCollectes()).compareTo(BigDecimal.ZERO) < 0
            || safeAmount(request.getFraisCollectes()).compareTo(BigDecimal.ZERO) < 0
            || safeAmount(request.getEspecesRemises()).compareTo(BigDecimal.ZERO) < 0
            || safeAmount(request.getEspecesEmises()).compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("All amounts must be >= 0");
        }

        BigDecimal totalCollecte = safeAmount(request.getEpargneCollectee())
            .add(safeAmount(request.getRemboursementsCreditCollectes()))
            .add(safeAmount(request.getFraisCollectes()));
        BigDecimal ecartTresorerie = safeAmount(request.getEspecesRemises()).subtract(totalCollecte);
        if (ecartTresorerie.compareTo(BigDecimal.ZERO) != 0
            && (request.getObservations() == null || request.getObservations().trim().isEmpty())) {
            throw new RuntimeException("Observation obligatoire si écart trésorerie différent de 0");
        }
    }

    private AgentAndSiteResolution resolveAgentAndSiteForCreate(CreateRecetteTerrainRequest request) {
        Utilisateur currentUser = getCurrentUser();
        if (isCurrentUserAgentTerrain(currentUser)) {
            AgentTerrain myAgent = agentTerrainRepository.findByUtilisateurId(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Aucun profil AgentTerrain lié à l'utilisateur connecté"));

            if (request.getAgentTerrainId() != null && !request.getAgentTerrainId().equals(myAgent.getId())) {
                throw new RuntimeException("Un AGENT_TERRAIN ne peut pas créer une recette pour un autre agent");
            }

            Set<Long> allowedSiteIds = resolveAllowedSiteIds(myAgent);
            if (allowedSiteIds.isEmpty()) {
                throw new RuntimeException("Aucun site n'est affecté à votre compte. Veuillez contacter le gestionnaire ou l'administrateur.");
            }

            if (allowedSiteIds.size() == 1) {
                Long uniqueSiteId = allowedSiteIds.iterator().next();
                if (request.getSiteId() != null && !request.getSiteId().equals(uniqueSiteId)) {
                    throw new RuntimeException("Un AGENT_TERRAIN ne peut pas créer une recette pour un site non affecté");
                }
                return new AgentAndSiteResolution(myAgent.getId(), uniqueSiteId);
            }

            if (request.getSiteId() == null) {
                throw new RuntimeException("Plusieurs sites sont affectés: siteId est obligatoire");
            }
            if (!allowedSiteIds.contains(request.getSiteId())) {
                throw new RuntimeException("Un AGENT_TERRAIN ne peut pas créer une recette pour un site non affecté");
            }
            return new AgentAndSiteResolution(myAgent.getId(), request.getSiteId());
        }

        if (request.getAgentTerrainId() == null) {
            throw new RuntimeException("agentTerrainId est obligatoire pour ce rôle");
        }
        if (request.getSiteId() == null) {
            throw new RuntimeException("siteId est obligatoire pour ce rôle");
        }
        return new AgentAndSiteResolution(request.getAgentTerrainId(), request.getSiteId());
    }

    private Set<Long> resolveAllowedSiteIds(AgentTerrain agent) {
        Set<Long> allowedSiteIds = new HashSet<>();
        if (agent.getSite() != null && agent.getSite().getId() != null) {
            allowedSiteIds.add(agent.getSite().getId());
        }
        if (agent.getSitesAffectes() != null) {
            agent.getSitesAffectes().stream()
                .filter(s -> s != null && s.getId() != null)
                .map(Site::getId)
                .forEach(allowedSiteIds::add);
        }
        return allowedSiteIds;
    }

    private BigDecimal safeAmount(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private record AgentAndSiteResolution(Long agentTerrainId, Long siteId) {}

    private void assertCurrentAgentOwnsRecetteIfNeeded(RecetteTerrainJournaliere recette) {
        Utilisateur currentUser = getCurrentUser();
        if (!isCurrentUserAgentTerrain(currentUser)) {
            return;
        }
        Long ownerUserId = recette.getAgentTerrain() != null
            && recette.getAgentTerrain().getUtilisateur() != null
            ? recette.getAgentTerrain().getUtilisateur().getId()
            : null;
        if (ownerUserId == null || !ownerUserId.equals(currentUser.getId())) {
            throw new RuntimeException("Accès refusé: un AGENT_TERRAIN ne peut accéder qu'à ses propres recettes");
        }
    }

    private void assertCanValidate() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            throw new RuntimeException("Utilisateur non authentifié");
        }
        boolean isAdminOrControleur = authentication.getAuthorities().stream()
            .map(a -> a.getAuthority())
            .anyMatch(a -> "ROLE_ADMIN".equals(a) || "ROLE_CONTROLEUR".equals(a));
        if (!isAdminOrControleur) {
            throw new RuntimeException("Seul un CONTROLEUR peut valider/rejeter une recette");
        }
    }

    private Utilisateur getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            throw new RuntimeException("Utilisateur non authentifié");
        }
        String username = authentication.getName();
        return utilisateurRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("Utilisateur connecté introuvable: " + username));
    }

    private boolean isCurrentUserAgentTerrain(Utilisateur user) {
        return user != null
            && user.getRole() != null
            && user.getRole().getCode() != null
            && "AGENT_TERRAIN".equals(user.getRole().getCode().name());
    }

    private String buildRecetteReferenceMetier(RecetteTerrainJournaliere recette) {
        return "RECETTE-" + recette.getId();
    }

    private Long resolveAntenneId(RecetteTerrainJournaliere recette) {
        if (recette == null || recette.getSite() == null || recette.getSite().getAgence() == null) {
            return null;
        }
        return recette.getSite().getAgence().getId();
    }

    private Long resolveSiteId(RecetteTerrainJournaliere recette) {
        if (recette == null || recette.getSite() == null) {
            return null;
        }
        return recette.getSite().getId();
    }

    private boolean hasEcart(RecetteTerrainJournaliere recette) {
        return (recette.getExcedent() != null && recette.getExcedent().compareTo(BigDecimal.ZERO) > 0)
                || (recette.getManquant() != null && recette.getManquant().compareTo(BigDecimal.ZERO) > 0);
    }

    // ========== PAGINATION ==========

    @Override
    public Page<RecetteTerrainResponse> getAllPaginated(Pageable pageable) {
        Page<RecetteTerrainJournaliere> page = recetteRepository.findByActifTrue(pageable);
        return page.map(recetteMapper::toDTO);
    }

    @Override
    public Page<RecetteTerrainResponse> searchAndFilter(
        Pageable pageable,
        RecetteStatut statut,
        Long siteId,
        Long agentTerrainId,
        LocalDate dateDebut,
        LocalDate dateFin) {
        Page<RecetteTerrainJournaliere> page = recetteRepository.searchRecettes(
            statut, siteId, agentTerrainId, dateDebut, dateFin, pageable);
        return page.map(recetteMapper::toDTO);
    }
}
