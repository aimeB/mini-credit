package com.mini.credit.service.impl;

import com.mini.credit.dto.caisse.CreateFicheJournaliereRequest;
import com.mini.credit.dto.caisse.FicheJournaliereResponse;
import com.mini.credit.dto.caisse.UpdateFicheJournaliereRequest;
import com.mini.credit.entity.caisse.FicheJournaliereAgentTerrain;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.StatutFicheJournaliere;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.exception.ResourceNotFoundException;
import com.mini.credit.mapper.FicheJournaliereMapper;
import com.mini.credit.repository.caisse.FicheJournaliereRepository;
import com.mini.credit.repository.utilisateur.UtilisateurRepository;
import com.mini.credit.service.FicheJournaliereService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PHASE 6B.1: Implementation of FicheJournaliereService
 * 
 * Handles CRUD operations for FicheJournaliereAgentTerrain.
 * Consolidation (6B.2) and Validation (6B.3) logic will be in separate services.
 */
@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class FicheJournaliereServiceImpl implements FicheJournaliereService {

    private final FicheJournaliereRepository ficheRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final FicheJournaliereMapper ficheMapper;

    /**
     * Create new fiche journalière
     */
    @Override
    public FicheJournaliereResponse creerFiche(CreateFicheJournaliereRequest request) {
        log.info("Creating fiche journalière: agent={}, date={}", 
                request.getAgentTerrainId(), request.getDateFiche());

        // Validate request
        if (request.getAgentTerrainId() == null) {
            throw new BusinessException("Agent terrain ID is required");
        }
        if (request.getDateFiche() == null) {
            throw new BusinessException("Date fiche is required");
        }

        // Check unique constraint: only one fiche per agent per day
        if (ficheRepository.existsByAgentTerrainIdAndDateFiche(
                request.getAgentTerrainId(), request.getDateFiche())) {
            throw new BusinessException("Fiche journalière already exists for this agent on " + request.getDateFiche());
        }

        // Get agent
        Utilisateur agent = utilisateurRepository.findById(request.getAgentTerrainId())
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found: " + request.getAgentTerrainId()));

        // Create entity from request
        FicheJournaliereAgentTerrain fiche = ficheMapper.toEntityFromCreateRequest(request, agent);
        
        // Save
        fiche = ficheRepository.save(fiche);
        log.info("Fiche journalière created: id={}, agent={}, date={}, statut={}", 
                fiche.getId(), fiche.getAgentTerrain().getId(), fiche.getDateFiche(), fiche.getStatut());

        return ficheMapper.toResponse(fiche);
    }

    /**
     * Get fiche by ID
     */
    @Override
    public FicheJournaliereResponse getById(Long id) {
        log.debug("Getting fiche: id={}", id);
        
        FicheJournaliereAgentTerrain fiche = ficheRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fiche not found: " + id));

        return ficheMapper.toResponse(fiche);
    }

    /**
     * Get fiche by agent and date (unique constraint)
     */
    @Override
    public FicheJournaliereResponse getByAgentAndDate(Long agentTerrainId, LocalDate dateFiche) {
        log.debug("Getting fiche: agent={}, date={}", agentTerrainId, dateFiche);
        
        FicheJournaliereAgentTerrain fiche = ficheRepository.findByAgentTerrainIdAndDateFiche(agentTerrainId, dateFiche)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Fiche not found for agent " + agentTerrainId + " on " + dateFiche));

        return ficheMapper.toResponse(fiche);
    }

    /**
     * Get all fiches for an agent
     */
    @Override
    public List<FicheJournaliereResponse> getByAgent(Long agentTerrainId) {
        log.debug("Getting fiches for agent: {}", agentTerrainId);
        
        return ficheRepository.findByAgentTerrainIdOrderByDateFicheDesc(agentTerrainId)
                .stream()
                .map(ficheMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all fiches for a specific date
     */
    @Override
    public List<FicheJournaliereResponse> getByDate(LocalDate dateFiche) {
        log.debug("Getting fiches for date: {}", dateFiche);
        
        return ficheRepository.findByDateFicheOrderByAgentTerrainId(dateFiche)
                .stream()
                .map(ficheMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all fiches for a date range
     */
    @Override
    public List<FicheJournaliereResponse> getByDateRange(LocalDate dateDebut, LocalDate dateFin) {
        log.debug("Getting fiches for date range: {} to {}", dateDebut, dateFin);
        
        if (dateDebut.isAfter(dateFin)) {
            throw new BusinessException("Date début must be before date fin");
        }
        
        return ficheRepository.findByDateFicheBetweenOrderByDateFicheDesc(dateDebut, dateFin)
                .stream()
                .map(ficheMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all fiches for a site
     */
    @Override
    public List<FicheJournaliereResponse> getBySite(Long siteId) {
        log.debug("Getting fiches for site: {}", siteId);
        
        return ficheRepository.findBySiteIdOrderByDateFicheDesc(siteId)
                .stream()
                .map(ficheMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all fiches by status
     */
    @Override
    public List<FicheJournaliereResponse> getByStatut(String statut) {
        log.debug("Getting fiches by statut: {}", statut);
        
        StatutFicheJournaliere status = StatutFicheJournaliere.valueOf(statut);
        
        return ficheRepository.findByStatutOrderByDateFicheDesc(status)
                .stream()
                .map(ficheMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all draft (BROUILLON) fiches
     */
    @Override
    public List<FicheJournaliereResponse> getDraftFiches() {
        log.debug("Getting draft fiches");
        
        return ficheRepository.findDraftFiches()
                .stream()
                .map(ficheMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update fiche (only BROUILLON can be updated)
     */
    @Override
    public FicheJournaliereResponse update(Long id, UpdateFicheJournaliereRequest request) {
        log.info("Updating fiche: id={}", id);

        FicheJournaliereAgentTerrain fiche = ficheRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fiche not found: " + id));

        // Only BROUILLON can be edited
        if (fiche.getStatut() != StatutFicheJournaliere.BROUILLON) {
            throw new BusinessException("Only BROUILLON fiches can be updated. Current status: " + fiche.getStatut());
        }

        // Apply updates
        ficheMapper.updateFromRequest(request, fiche);

        // Save
        fiche = ficheRepository.save(fiche);
        log.info("Fiche updated: id={}", fiche.getId());

        return ficheMapper.toResponse(fiche);
    }

    /**
     * Delete fiche (only BROUILLON can be deleted)
     */
    @Override
    public void delete(Long id) {
        log.info("Deleting fiche: id={}", id);

        FicheJournaliereAgentTerrain fiche = ficheRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fiche not found: " + id));

        // Only BROUILLON can be deleted
        if (fiche.getStatut() != StatutFicheJournaliere.BROUILLON) {
            throw new BusinessException("Only BROUILLON fiches can be deleted. Current status: " + fiche.getStatut());
        }

        ficheRepository.delete(fiche);
        log.info("Fiche deleted: id={}", id);
    }

    /**
     * Check if fiche exists
     */
    @Override
    public boolean exists(Long agentTerrainId, LocalDate dateFiche) {
        return ficheRepository.existsByAgentTerrainIdAndDateFiche(agentTerrainId, dateFiche);
    }

    /**
     * Count fiches by status
     */
    @Override
    public long countByStatut(String statut) {
        StatutFicheJournaliere status = StatutFicheJournaliere.valueOf(statut);
        return ficheRepository.countByStatut(status);
    }
}
