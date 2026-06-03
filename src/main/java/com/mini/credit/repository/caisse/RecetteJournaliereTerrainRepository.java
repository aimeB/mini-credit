package com.mini.credit.repository.caisse;

import com.mini.credit.entity.caisse.RecetteJournaliereTerrain;
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
}
