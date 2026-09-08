package com.mini.credit.service.impl;

import com.mini.credit.dto.referentiel.AgentTerrainResponse;
import com.mini.credit.dto.referentiel.CreateAgentTerrainRequest;
import com.mini.credit.dto.referentiel.UpdateAgentTerrainRequest;
import com.mini.credit.dto.referentiel.SiteResponse;
import com.mini.credit.entity.employe.Employe;
import com.mini.credit.entity.referentiel.AgentTerrain;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.PosteEmploye;
import com.mini.credit.mapper.AgentTerrainMapper;
import com.mini.credit.repository.EmployeRepository;
import com.mini.credit.repository.referentiel.SiteRepository;
import com.mini.credit.repository.UtilisateurRepository;
import com.mini.credit.repository.agentTerrain.AgentTerrainRepository;
import com.mini.credit.service.AgentTerrainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service implementation for AgentTerrain (Field Agent) management
 * Handles CRUD operations and Phase 4 site assignment methods
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AgentTerrainServiceImpl implements AgentTerrainService {

    private final AgentTerrainRepository agentTerrainRepository;
    private final SiteRepository siteRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EmployeRepository employeRepository;
    private final AgentTerrainMapper agentTerrainMapper;

    @Override
    public AgentTerrainResponse create(CreateAgentTerrainRequest request) {
        // 1. Validate utilisateur exists and is not already an agent terrain
        Utilisateur utilisateur = utilisateurRepository.findById(request.getUtilisateurId())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        agentTerrainRepository.findByUtilisateurId(request.getUtilisateurId())
                .ifPresent(agent -> {
                    throw new RuntimeException("Cet utilisateur est déjà un agent terrain");
                });

        // 1b. Validate utilisateur has an employe with fonction AGENT_TERRAIN
        if (utilisateur.getEmploye() == null) {
            throw new RuntimeException(
                "L'utilisateur doit être lié à un employé avec la fonction Agent Terrain");
        }
        if (utilisateur.getEmploye().getFonction() != PosteEmploye.AGENT_TERRAIN) {
            throw new RuntimeException(
                "L'employé lié doit avoir la fonction AGENT_TERRAIN (actuel : "
                + utilisateur.getEmploye().getFonction() + ")");
        }

        // 2. Validate matricule is unique
        if (request.getMatricule() != null && 
            agentTerrainRepository.findAll().stream()
                .anyMatch(a -> a.getMatricule().equalsIgnoreCase(request.getMatricule()))) {
            throw new RuntimeException("Matricule est déjà utilisé");
        }

        // 3. Validate main site exists
        Site mainSite = siteRepository.findById(request.getSiteId())
                .orElseThrow(() -> new RuntimeException("Site principal non trouvé"));

        // 4. Validate main site is active
        if (!mainSite.getActif()) {
            throw new RuntimeException("Le site principal doit être actif");
        }

        // 5. Validate coherence agence : l’employé de l’agent terrain doit appartenir
        //    à la même agence que le site principal
        Employe agentEmploye = utilisateur.getEmploye();
        if (agentEmploye.getAgence() == null ||
                !agentEmploye.getAgence().getId().equals(mainSite.getAgence().getId())) {
            throw new RuntimeException(
                "L’agent terrain (employé " + agentEmploye.getMatricule() + ") n’appartient pas "
                + "\u00e0 la m\u00eame agence que le site principal. "
                + "Agence employ\u00e9 : " + (agentEmploye.getAgence() != null ? agentEmploye.getAgence().getNomAgence() : "non définie")
                + " / Agence site : " + mainSite.getAgence().getNomAgence());
        }

        // Create entity via mapper
        AgentTerrain agent = agentTerrainMapper.toEntity(request);
        agent.setUtilisateur(utilisateur);
        agent.setSite(mainSite);
        agent.setActif(true);

        // Assign additional sites if provided (Phase 4)
        if (request.getSiteIds() != null && !request.getSiteIds().isEmpty()) {
            Set<Site> sites = request.getSiteIds().stream()
                    .map(siteId -> siteRepository.findById(siteId)
                            .orElseThrow(() -> new RuntimeException("Site non trouvé: " + siteId)))
                    .filter(Site::getActif)  // Only active sites
                    .filter(site -> site.getAgence().getId().equals(mainSite.getAgence().getId()))
                    .collect(Collectors.toSet());
            agent.setSitesAffectes(sites);
        }

        // Valider et assigner le gestionnaire (obligatoire à la création)
        Employe gestionnaire = employeRepository.findById(request.getGestionnaireId())
                .orElseThrow(() -> new RuntimeException("Gestionnaire non trouvé"));
        if (!gestionnaire.getActif()) {
            throw new RuntimeException("Le gestionnaire doit être actif");
        }
        if (gestionnaire.getFonction() != PosteEmploye.GESTIONNAIRE) {
            throw new RuntimeException(
                "L'employé sélectionné n'est pas un Gestionnaire (fonction=" + gestionnaire.getFonction() + ")");
        }
        // Validate coherence agence : le gestionnaire doit appartenir à la même agence
        if (gestionnaire.getAgence() == null ||
                !gestionnaire.getAgence().getId().equals(mainSite.getAgence().getId())) {
            throw new RuntimeException(
                "Le gestionnaire (" + gestionnaire.getMatricule() + ") n'appartient pas "
                + "\u00e0 la m\u00eame agence que le site principal. "
                + "Agence gestionnaire : " + (gestionnaire.getAgence() != null ? gestionnaire.getAgence().getNomAgence() : "non d\u00e9finie")
                + " / Agence site : " + mainSite.getAgence().getNomAgence());
        }
        agent.setGestionnaire(gestionnaire);

        AgentTerrain saved = agentTerrainRepository.save(agent);
        return agentTerrainMapper.toDTO(saved);
    }

    @Override
    public AgentTerrainResponse update(Long id, UpdateAgentTerrainRequest request) {
        AgentTerrain agent = agentTerrainRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agent terrain non trouvé"));

        // Update main site if provided
        if (request.getSiteId() != null && !request.getSiteId().equals(agent.getSite().getId())) {
            Site newSite = siteRepository.findById(request.getSiteId())
                    .orElseThrow(() -> new RuntimeException("Site principal non trouvé"));
            
            if (!newSite.getActif()) {
                throw new RuntimeException("Le site principal doit être actif");
            }

            // Coherence : l’employé de l’agent doit appartenir à la même agence que le nouveau site
            Employe agentEmploye = agent.getUtilisateur() != null ? agent.getUtilisateur().getEmploye() : null;
            if (agentEmploye != null && (agentEmploye.getAgence() == null ||
                    !agentEmploye.getAgence().getId().equals(newSite.getAgence().getId()))) {
                throw new RuntimeException(
                    "L’agent terrain n’appartient pas \u00e0 la m\u00eame agence que le nouveau site. "
                    + "Agence employ\u00e9 : " + (agentEmploye.getAgence() != null ? agentEmploye.getAgence().getNomAgence() : "non définie")
                    + " / Agence nouveau site : " + newSite.getAgence().getNomAgence());
            }

            agent.setSite(newSite);
        }

        // Update additional sites if provided (Phase 4)
        if (request.getSiteIds() != null && !request.getSiteIds().isEmpty()) {
            Set<Site> sites = request.getSiteIds().stream()
                    .map(siteId -> siteRepository.findById(siteId)
                            .orElseThrow(() -> new RuntimeException("Site non trouvé: " + siteId)))
                    .filter(Site::getActif)
                    .collect(Collectors.toSet());
            agent.setSitesAffectes(sites);
        }

        // Mettre à jour le gestionnaire si fourni
        if (request.getGestionnaireId() != null) {
            Employe gestionnaire = employeRepository.findById(request.getGestionnaireId())
                    .orElseThrow(() -> new RuntimeException("Gestionnaire non trouvé"));
            if (!gestionnaire.getActif()) {
                throw new RuntimeException("Le gestionnaire doit être actif");
            }
            if (gestionnaire.getFonction() != PosteEmploye.GESTIONNAIRE) {
                throw new RuntimeException(
                    "L'employé sélectionné n'est pas un Gestionnaire (fonction=" + gestionnaire.getFonction() + ")");
            }
            // Coherence agence : le gestionnaire doit appartenir à la même agence que le site de l’agent
            Site currentSite = agent.getSite();
            if (currentSite != null && gestionnaire.getAgence() != null &&
                    !gestionnaire.getAgence().getId().equals(currentSite.getAgence().getId())) {
                throw new RuntimeException(
                    "Le gestionnaire (" + gestionnaire.getMatricule() + ") n'appartient pas "
                    + "\u00e0 la m\u00eame agence que le site de l'agent. "
                    + "Agence gestionnaire : " + gestionnaire.getAgence().getNomAgence()
                    + " / Agence site : " + currentSite.getAgence().getNomAgence());
            }
            agent.setGestionnaire(gestionnaire);
        }

        // Update other fields via mapper
        agentTerrainMapper.updateEntityFromDTO(request, agent);

        AgentTerrain updated = agentTerrainRepository.save(agent);
        return agentTerrainMapper.toDTO(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public AgentTerrainResponse getById(Long id) {
        AgentTerrain agent = agentTerrainRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agent terrain non trouvé"));
        return agentTerrainMapper.toDTO(agent);
    }

    @Override
    public List<AgentTerrainResponse> getAll() {
        synchroniserAgentsTerrainDepuisUtilisateurs(null);
        return agentTerrainRepository.findAll().stream()
                .map(agentTerrainMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Long id) {
        AgentTerrain agent = agentTerrainRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agent terrain non trouvé"));
        // Soft delete
        agent.setActif(false);
        agentTerrainRepository.save(agent);
    }

    // ===== PHASE 4: Site Assignment Methods =====

    @Override
    public void assignerSite(Long agentId, Long siteId) {
        // Validate agent exists and is active
        AgentTerrain agent = agentTerrainRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent terrain non trouvé"));

        if (!agent.getActif()) {
            throw new RuntimeException("L'agent doit être actif");
        }

        // Validate site exists and is active
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new RuntimeException("Site non trouvé"));

        if (!site.getActif()) {
            throw new RuntimeException("Le site doit être actif");
        }

        // Validate site belongs to same agence as agent's main site
        if (!site.getAgence().getId().equals(agent.getSite().getAgence().getId())) {
            throw new RuntimeException("Le site doit appartenir à la même agence que l'agent");
        }

        // Check for duplicate assignment
        if (agent.getSitesAffectes().stream().anyMatch(s -> s.getId().equals(siteId))) {
            throw new RuntimeException("L'agent est déjà assigné à ce site");
        }

        // Add site to agent's assigned sites
        agent.getSitesAffectes().add(site);
        agentTerrainRepository.save(agent);
    }

    @Override
    public void retirerSite(Long agentId, Long siteId) {
        // Validate agent exists
        AgentTerrain agent = agentTerrainRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent terrain non trouvé"));

        // Validate site exists
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new RuntimeException("Site non trouvé"));

        // Remove site from agent's assigned sites
        agent.getSitesAffectes().removeIf(s -> s.getId().equals(siteId));
        agentTerrainRepository.save(agent);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> getSitesByAgent(Long agentId) {
        // Validate agent exists and is active
        AgentTerrain agent = agentTerrainRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent terrain non trouvé"));

        if (!agent.getActif()) {
            throw new RuntimeException("L'agent doit être actif");
        }

        // Return active sites only
        return agent.getSitesAffectes().stream()
                .filter(Site::getActif)
                .map(Site::getId)
                .collect(Collectors.toSet());
    }

    @Override
    public List<AgentTerrainResponse> getAgentsBySite(Long siteId) {
        // Validate site exists and is active
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new RuntimeException("Site non trouvé"));

        if (!site.getActif()) {
            throw new RuntimeException("Le site doit être actif");
        }

        synchroniserAgentsTerrainDepuisUtilisateurs(siteId);

        // Requête JPQL couvrant les deux cas :
        //   1. agent.site.id = siteId   (site principal)
        //   2. siteId dans agent.sitesAffectes
        return agentTerrainRepository.findActifsBySiteId(siteId).stream()
                .map(agentTerrainMapper::toDTO)
                .collect(Collectors.toList());
    }

    private void synchroniserAgentsTerrainDepuisUtilisateurs(Long siteId) {
        if (siteId == null) {
            return;
        }

        List<Utilisateur> utilisateurs = utilisateurRepository.findSelectableAgentsTerrainByEmployeSiteId(siteId);
        if (utilisateurs == null || utilisateurs.isEmpty()) {
            return;
        }

        utilisateurs.forEach(utilisateur -> {
            Employe employe = utilisateur.getEmploye();
            if (employe == null || employe.getSite() == null) {
                return;
            }

            AgentTerrain agent = agentTerrainRepository.findByUtilisateurId(utilisateur.getId())
                    .orElseGet(AgentTerrain::new);
            agent.setUtilisateur(utilisateur);
            agent.setSite(employe.getSite());
            agent.setMatricule(resolveMatriculeAgentTerrain(utilisateur, employe));
            agent.setActif(true);
            agentTerrainRepository.save(agent);
        });
    }

    private String resolveMatriculeAgentTerrain(Utilisateur utilisateur, Employe employe) {
        String matricule = employe.getMatricule();
        if (matricule == null || matricule.isBlank()) {
            matricule = "AT-" + utilisateur.getUsername();
        }
        matricule = matricule.trim().toUpperCase();
        return matricule.length() <= 50 ? matricule : matricule.substring(0, 50);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgentTerrainResponse> getAgentsByGestionnaire(Long gestionnaireId) {
        // Vérifier que l'employé existe et est bien un Gestionnaire actif
        Employe gestionnaire = employeRepository.findById(gestionnaireId)
                .orElseThrow(() -> new RuntimeException("Gestionnaire non trouvé"));
        if (gestionnaire.getFonction() != PosteEmploye.GESTIONNAIRE) {
            throw new RuntimeException(
                "L'employé n'est pas un Gestionnaire (fonction=" + gestionnaire.getFonction() + ")");
        }
        return agentTerrainRepository.findByGestionnaireIdAndActifTrue(gestionnaireId)
                .stream()
                .map(agentTerrainMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SiteResponse> getSitesByGestionnaire(Long gestionnaireId) {
        // Vérifier que l'employé existe et est bien un Gestionnaire
        Employe gestionnaire = employeRepository.findById(gestionnaireId)
                .orElseThrow(() -> new RuntimeException("Gestionnaire non trouvé"));
        if (gestionnaire.getFonction() != PosteEmploye.GESTIONNAIRE) {
            throw new RuntimeException(
                "L'employé n'est pas un Gestionnaire (fonction=" + gestionnaire.getFonction() + ")");
        }
        return agentTerrainRepository.findSitesByGestionnaireId(gestionnaireId)
                .stream()
                .map(SiteResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long countMembresByGestionnaire(Long gestionnaireId) {
        return agentTerrainRepository.countMembresByGestionnaireId(gestionnaireId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgentTerrainResponse> getAnomaliesByAgence(Long agenceId) {
        return agentTerrainRepository.findAnomaliesByAgenceId(agenceId).stream()
                .map(agentTerrainMapper::toDTO)
                .collect(Collectors.toList());
    }
}
