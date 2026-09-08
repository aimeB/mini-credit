package com.mini.credit.repository.recetteTerrain;

import com.mini.credit.entity.referentiel.RecetteTerrainJournaliere;
import com.mini.credit.enums.RecetteStatut;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour RecetteTerrainJournaliere
 * Gère la persistance des recettes journalières terrain
 */
@Repository
public interface RecetteTerrainRepository extends JpaRepository<RecetteTerrainJournaliere, Long> {

    /**
     * Récupère les recettes d'un agent terrain
     */
    List<RecetteTerrainJournaliere> findByAgentTerrainIdAndActifTrue(Long agentTerrainId);

    /**
     * Récupère les recettes d'un site
     */
    List<RecetteTerrainJournaliere> findBySiteIdAndActifTrue(Long siteId);

    /**
     * Récupère les recettes d'une date donnée
     */
    List<RecetteTerrainJournaliere> findByDateRecetteAndActifTrue(LocalDate date);

    /**
     * Récupère la recette unique d'un agent pour une date donnée
     * Validation de doublon
     */
    Optional<RecetteTerrainJournaliere> findByAgentTerrainIdAndDateRecetteAndActifTrue(
        Long agentTerrainId, LocalDate dateRecette);

    /**
     * Récupère les recettes par statut
     */
    List<RecetteTerrainJournaliere> findByStatutAndActifTrue(RecetteStatut statut);

    /**
     * Récupère les recettes d'un site par statut
     * Utilisé pour lister les recettes en attente de validation
     */
    List<RecetteTerrainJournaliere> findBySiteIdAndStatutAndActifTrue(Long siteId, RecetteStatut statut);

    /**
     * Récupère les recettes d'une plage de dates
     */
    List<RecetteTerrainJournaliere> findByDateRecetteBetweenAndActifTrue(
        LocalDate dateDebut, LocalDate dateFin);

    /**
     * Récupère les recettes en attente de validation pour un site
     * Triées par date récente en premier
     */
    @Query("SELECT r FROM RecetteTerrainJournaliere r " +
           "WHERE r.site.id = :siteId AND r.statut = 'SOUMISE' AND r.actif = true " +
           "ORDER BY r.dateRecette DESC, r.dateCreation DESC")
    List<RecetteTerrainJournaliere> findEnAttenteValidationBySite(@Param("siteId") Long siteId);

    /**
     * Récupère les recettes validées d'une date donnée
     */
    List<RecetteTerrainJournaliere> findByDateRecetteAndStatutAndActifTrue(
        LocalDate date, RecetteStatut statut);

    /**
     * Compte le nombre de recettes d'un agent pour une date donnée
     */
    long countByAgentTerrainIdAndDateRecetteAndActifTrue(Long agentTerrainId, LocalDate date);

    /**
     * Vérifie l'existence d'une recette pour agent + date
     */
    boolean existsByAgentTerrainIdAndDateRecetteAndActifTrue(Long agentTerrainId, LocalDate date);

    /**
     * Récupère les recettes d'un agent par statut
     */
    List<RecetteTerrainJournaliere> findByAgentTerrainIdAndStatutAndActifTrue(
        Long agentTerrainId, RecetteStatut statut);

    // ========== PAGINATION ==========

    /**
     * Récupère toutes les recettes paginées et triées
     */
    Page<RecetteTerrainJournaliere> findByActifTrue(Pageable pageable);

    /**
     * Récupère les recettes paginées par statut
     */
    Page<RecetteTerrainJournaliere> findByActifTrueAndStatut(RecetteStatut statut, Pageable pageable);

    /**
     * Récupère les recettes paginées par site
     */
    Page<RecetteTerrainJournaliere> findBySiteIdAndActifTrue(Long siteId, Pageable pageable);

    /**
     * Récupère les recettes paginées par agent
     */
    Page<RecetteTerrainJournaliere> findByAgentTerrainIdAndActifTrue(Long agentTerrainId, Pageable pageable);

    /**
     * Récupère les recettes paginées par plage de dates
     */
    Page<RecetteTerrainJournaliere> findByDateRecetteBetweenAndActifTrue(
        LocalDate dateDebut, LocalDate dateFin, Pageable pageable);

    /**
     * Recherche complexe avec tous les filtres
     */
    @Query("SELECT r FROM RecetteTerrainJournaliere r WHERE r.actif = true " +
           "AND (:statut IS NULL OR r.statut = :statut) " +
           "AND (:siteId IS NULL OR r.site.id = :siteId) " +
           "AND (:agentId IS NULL OR r.agentTerrain.id = :agentId) " +
           "AND (:dateDebut IS NULL OR r.dateRecette >= :dateDebut) " +
           "AND (:dateFin IS NULL OR r.dateRecette <= :dateFin)")
    Page<RecetteTerrainJournaliere> searchRecettes(
        @Param("statut") RecetteStatut statut,
        @Param("siteId") Long siteId,
        @Param("agentId") Long agentId,
        @Param("dateDebut") LocalDate dateDebut,
        @Param("dateFin") LocalDate dateFin,
        Pageable pageable);
}
