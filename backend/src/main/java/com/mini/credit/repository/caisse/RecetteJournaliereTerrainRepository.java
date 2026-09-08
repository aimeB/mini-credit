package com.mini.credit.repository.caisse;

import com.mini.credit.entity.caisse.RecetteJournaliereTerrain;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.StatutRecetteJournaliere;
import com.mini.credit.enums.TypeRecette;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * PHASE 6: Repository pour RecetteJournaliereTerrain.
 */
@Repository
public interface RecetteJournaliereTerrainRepository extends JpaRepository<RecetteJournaliereTerrain, Long> {

    /**
     * Récupère les recettes d'un jour donné
     */
    List<RecetteJournaliereTerrain> findByDateJourOrderByDateCreationAsc(LocalDate dateJour);

    /**
     * Récupère les recettes d'un agent pour un jour
     */
    List<RecetteJournaliereTerrain> findByAgentIdAndDateJourOrderByDateCreationAsc(Long agentId, LocalDate dateJour);

    /**
     * Récupère les recettes par statut
     */
    List<RecetteJournaliereTerrain> findByStatutOrderByDateJourDesc(StatutRecetteJournaliere statut);

    /**
     * Récupère les recettes en attente de validation pour un jour
     */
    List<RecetteJournaliereTerrain> findByDateJourAndStatutOrderByDateCreationAsc(
            LocalDate dateJour, StatutRecetteJournaliere statut);

    /**
     * Somme des recettes pour un jour donné
     */
    @Query("SELECT SUM(r.montant) FROM RecetteJournaliereTerrain r WHERE r.dateJour = :dateJour")
    BigDecimal sumMontantByDateJour(@Param("dateJour") LocalDate dateJour);

    /**
     * Somme des recettes validées pour un jour
     */
    @Query("SELECT SUM(r.montant) FROM RecetteJournaliereTerrain r WHERE r.dateJour = :dateJour AND r.statut = :statut")
    BigDecimal sumMontantByDateJourAndStatut(@Param("dateJour") LocalDate dateJour, @Param("statut") StatutRecetteJournaliere statut);

    /**
     * Récupère recettes par type
     */
    List<RecetteJournaliereTerrain> findByTypeRecetteAndDateJourOrderByDateCreationAsc(
            TypeRecette typeRecette, LocalDate dateJour);

    // ========== PHASE 6B.2: CONSOLIDATION QUERIES ==========

    /**
     * Find all distinct agents with receipts in date range
     * Used by consolidation service to identify which agents to consolidate
     * PHASE 6B.2
     */
    @Query("""
        SELECT DISTINCT r.agent
        FROM RecetteJournaliereTerrain r
        WHERE r.dateJour BETWEEN :debut AND :fin
        ORDER BY r.agent.id
        """)
    List<Utilisateur> findDistinctAgentsByDateRange(
        @Param("debut") LocalDate debut,
        @Param("fin") LocalDate fin);

    /**
     * Find receipts for agent + date with specific status
     * Used by consolidation service to aggregate receipts into fiche
     * PHASE 6B.2
     */
    @Query("""
        SELECT r FROM RecetteJournaliereTerrain r
        WHERE r.agent.id = :agentId
        AND r.dateJour = :date
        AND r.statut = :statut
        ORDER BY r.id
        """)
    List<RecetteJournaliereTerrain> findByAgentTerrainAndDateAndStatut(
        @Param("agentId") Long agentId,
        @Param("date") LocalDate date,
        @Param("statut") StatutRecetteJournaliere statut);

    /**
     * Find receipts by date range
     * Used by consolidation service
     * PHASE 6B.2
     */
    List<RecetteJournaliereTerrain> findByDateJourBetween(LocalDate debut, LocalDate fin);
}
