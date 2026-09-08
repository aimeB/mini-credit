package com.mini.credit.service;

import com.mini.credit.dto.referentiel.AgentTerrainResponse;
import com.mini.credit.dto.referentiel.CreateAgentTerrainRequest;
import com.mini.credit.dto.referentiel.UpdateAgentTerrainRequest;
import com.mini.credit.dto.referentiel.SiteResponse;

import java.util.List;
import java.util.Set;

/**
 * Service interface for AgentTerrain (Field Agent) management
 * Handles CRUD operations and Phase 4 site assignment methods
 */
public interface AgentTerrainService {
    
    /**
     * Create a new field agent
     */
    AgentTerrainResponse create(CreateAgentTerrainRequest request);
    
    /**
     * Update an existing field agent
     */
    AgentTerrainResponse update(Long id, UpdateAgentTerrainRequest request);
    
    /**
     * Get field agent by ID
     */
    AgentTerrainResponse getById(Long id);
    
    /**
     * Get all field agents
     */
    List<AgentTerrainResponse> getAll();
    
    /**
     * Delete (soft delete) field agent
     */
    void delete(Long id);
    
    // ===== PHASE 4: Site Assignment Methods =====
    
    /**
     * Assign a site to an agent
     * Validates agent and site exist, prevents duplicate assignments
     * 
     * @param agentId ID of the agent terrain
     * @param siteId ID of the site to assign
     */
    void assignerSite(Long agentId, Long siteId);
    
    /**
     * Remove a site assignment from an agent
     * 
     * @param agentId ID of the agent terrain
     * @param siteId ID of the site to remove
     */
    void retirerSite(Long agentId, Long siteId);
    
    /**
     * Get all sites assigned to an agent
     * Filters for active agents and sites only
     * 
     * @param agentId ID of the agent terrain
     * @return Set of site IDs assigned to the agent
     */
    Set<Long> getSitesByAgent(Long agentId);
    
    /**
     * Get all field agents assigned to a site
     * Filters for active agents and sites only
     * 
     * @param siteId ID of the site
     * @return List of agents assigned to the site
     */
    List<AgentTerrainResponse> getAgentsBySite(Long siteId);

    // ===== Gestionnaire — Portefeuille Methods =====

    /**
     * Retourne tous les agents terrain actifs supervisés par un gestionnaire.
     *
     * @param gestionnaireId ID de l'employé Gestionnaire
     * @return Liste des agents du gestionnaire
     */
    List<AgentTerrainResponse> getAgentsByGestionnaire(Long gestionnaireId);

    /**
     * Retourne les agents terrain dont l'employé ou le gestionnaire n'appartient pas
     * à la même agence que leur site principal.
     * Utilisé pour détecter et corriger les incohérences d'affectation.
     *
     * @param agenceId ID de l'agence à contrôler
     * @return Liste des agents en anomalie
     */
    List<AgentTerrainResponse> getAnomaliesByAgence(Long agenceId);

    /**
     * Retourne les sites distincts couverts par les agents d'un gestionnaire.
     *
     * @param gestionnaireId ID de l'employé Gestionnaire
     * @return Liste de SiteResponse distincts
     */
    List<SiteResponse> getSitesByGestionnaire(Long gestionnaireId);

    /**
     * Compte les membres distincts affiliés aux sites couverts par un gestionnaire.
     *
     * @param gestionnaireId ID de l'employé Gestionnaire
     * @return Nombre total de membres
     */
    long countMembresByGestionnaire(Long gestionnaireId);
}
