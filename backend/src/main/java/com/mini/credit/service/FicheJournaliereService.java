package com.mini.credit.service;

import com.mini.credit.dto.caisse.CreateFicheJournaliereRequest;
import com.mini.credit.dto.caisse.FicheJournaliereResponse;
import com.mini.credit.dto.caisse.UpdateFicheJournaliereRequest;

import java.time.LocalDate;
import java.util.List;

/**
 * PHASE 6B.1: Service interface pour FicheJournaliereAgentTerrain
 *
 * CRUD operations only (no consolidation/validation in 6B.1).
 * Consolidation (6B.2) and Validation (6B.3) are separate services.
 */
public interface FicheJournaliereService {

    /**
     * Create new fiche journalière for an agent on a given date
     * Initializes status to BROUILLON
     */
    FicheJournaliereResponse creerFiche(CreateFicheJournaliereRequest request);

    /**
     * Get fiche by ID
     */
    FicheJournaliereResponse getById(Long id);

    /**
     * Get fiche by agent and date (unique constraint)
     */
    FicheJournaliereResponse getByAgentAndDate(Long agentTerrainId, LocalDate dateFiche);

    /**
     * Get all fiches for an agent (ordered by date DESC)
     */
    List<FicheJournaliereResponse> getByAgent(Long agentTerrainId);

    /**
     * Get all fiches for a specific date (across all agents)
     */
    List<FicheJournaliereResponse> getByDate(LocalDate dateFiche);

    /**
     * Get all fiches for a date range
     */
    List<FicheJournaliereResponse> getByDateRange(LocalDate dateDebut, LocalDate dateFin);

    /**
     * Get all fiches for a site
     */
    List<FicheJournaliereResponse> getBySite(Long siteId);

    /**
     * Get all fiches by status
     */
    List<FicheJournaliereResponse> getByStatut(String statut);

    /**
     * Get all draft (BROUILLON) fiches (editable)
     */
    List<FicheJournaliereResponse> getDraftFiches();

    /**
     * Update fiche (only BROUILLON can be updated)
     * Currently allows updating observations_agent
     */
    FicheJournaliereResponse update(Long id, UpdateFicheJournaliereRequest request);

    /**
     * Delete fiche (only BROUILLON can be deleted)
     */
    void delete(Long id);

    /**
     * Check if fiche exists for agent and date
     */
    boolean exists(Long agentTerrainId, LocalDate dateFiche);

    /**
     * Get count of fiches by status
     */
    long countByStatut(String statut);
}
