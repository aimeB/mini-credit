package com.mini.credit.repository.caisse;

import com.mini.credit.entity.caisse.FicheJournaliereAgentTerrain;
import com.mini.credit.enums.StatutFicheJournaliere;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * PHASE 6B.1: Repository pour FicheJournaliereAgentTerrain
 *
 * Handles CRUD and query operations for consolidated daily records.
 */
@Repository
public interface FicheJournaliereRepository extends JpaRepository<FicheJournaliereAgentTerrain, Long> {

    /**
     * Find fiche by agent and date (should be unique)
     */
    Optional<FicheJournaliereAgentTerrain> findByAgentTerrainIdAndDateFiche(Long agentTerrainId, LocalDate dateFiche);

    /**
     * Find all fiches for an agent
     */
    List<FicheJournaliereAgentTerrain> findByAgentTerrainIdOrderByDateFicheDesc(Long agentTerrainId);

    /**
     * Find all fiches for a date (across all agents)
     */
    List<FicheJournaliereAgentTerrain> findByDateFicheOrderByAgentTerrainId(LocalDate dateFiche);

    /**
     * Find all fiches for a date range
     */
    List<FicheJournaliereAgentTerrain> findByDateFicheBetweenOrderByDateFicheDesc(LocalDate dateDebut, LocalDate dateFin);

    /**
     * Find fiches by status
     */
    List<FicheJournaliereAgentTerrain> findByStatutOrderByDateFicheDesc(StatutFicheJournaliere statut);

    /**
     * Find fiches by agent and status
     */
    List<FicheJournaliereAgentTerrain> findByAgentTerrainIdAndStatutOrderByDateFicheDesc(Long agentTerrainId, StatutFicheJournaliere statut);

    /**
     * Find fiches by site
     */
    List<FicheJournaliereAgentTerrain> findBySiteIdOrderByDateFicheDesc(Long siteId);

    /**
     * Find fiches by site and date range
     */
    List<FicheJournaliereAgentTerrain> findBySiteIdAndDateFicheBetweenOrderByDateFicheDesc(Long siteId, LocalDate dateDebut, LocalDate dateFin);

    /**
     * Find pending fiches (SOUMISE) for validation
     */
    List<FicheJournaliereAgentTerrain> findByStatutAndDateFicheGreaterThanEqualOrderByDateFicheDesc(
            StatutFicheJournaliere statut, LocalDate dateFiche);

    /**
     * Count fiches by status
     */
    long countByStatut(StatutFicheJournaliere statut);

    /**
     * Count fiches by agent and status
     */
    long countByAgentTerrainIdAndStatut(Long agentTerrainId, StatutFicheJournaliere statut);

    /**
     * Check if fiche exists for agent and date
     */
    boolean existsByAgentTerrainIdAndDateFiche(Long agentTerrainId, LocalDate dateFiche);

    /**
     * Find fiches with variance (for audit/investigation)
     */
    @Query("SELECT f FROM FicheJournaliereAgentTerrain f WHERE f.variance > 0 ORDER BY f.dateFiche DESC")
    List<FicheJournaliereAgentTerrain> findFichesWithVariance();

    /**
     * Find fiches with high variance percentage
     */
    @Query("SELECT f FROM FicheJournaliereAgentTerrain f WHERE f.variancePercentage > :threshold ORDER BY f.variancePercentage DESC")
    List<FicheJournaliereAgentTerrain> findFichesWithHighVariance(@Param("threshold") Double threshold);

    /**
     * Find all draft fiches (BROUILLON) - editable
     */
    @Query("SELECT f FROM FicheJournaliereAgentTerrain f WHERE f.statut = 'BROUILLON' ORDER BY f.dateFiche DESC")
    List<FicheJournaliereAgentTerrain> findDraftFiches();

    /**
     * Find draft fiches for an agent
     */
    @Query("SELECT f FROM FicheJournaliereAgentTerrain f WHERE f.agentTerrain.id = :agentId AND f.statut = 'BROUILLON' ORDER BY f.dateFiche DESC")
    List<FicheJournaliereAgentTerrain> findDraftFichesByAgent(@Param("agentId") Long agentId);
}
