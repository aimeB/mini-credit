package com.mini.credit.service;

import com.mini.credit.dto.referentiel.CreateRecetteTerrainRequest;
import com.mini.credit.dto.referentiel.RecetteTerrainResponse;
import com.mini.credit.dto.referentiel.UpdateRecetteTerrainRequest;
import com.mini.credit.dto.referentiel.ValidateRecetteTerrainRequest;
import com.mini.credit.enums.RecetteStatut;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

/**
 * Service pour gestion des recettes terrain journalières
 * Contient la logique métier pour création, validation, workflow
 */
public interface RecetteTerrainService {

    // ========== CRUD ==========

    /**
     * Crée une nouvelle recette terrain (statut BROUILLON)
     * Validations:
     * - Agent existe et actif
     * - Site existe et actif
     * - Agent et Site même agence
     * - Agent affecté au site
     * - Pas de doublon (agent + date)
     */
    RecetteTerrainResponse create(CreateRecetteTerrainRequest request);

    /**
     * Modifie une recette existante (seulement si BROUILLON)
     * Recalcule les écarts
     */
    RecetteTerrainResponse update(Long id, UpdateRecetteTerrainRequest request);

    /**
     * Récupère une recette par ID
     */
    RecetteTerrainResponse getById(Long id);

    /**
     * Récupère toutes les recettes actives
     */
    List<RecetteTerrainResponse> getAll();

    /**
     * Soft-delete d'une recette (actif = false)
     * Possible seulement si BROUILLON
     */
    void delete(Long id);

    // ========== RECHERCHES ==========

    /**
     * Récupère les recettes d'un agent terrain
     */
    List<RecetteTerrainResponse> getByAgentTerrain(Long agentTerrainId);

    /**
     * Récupère les recettes de l'agent terrain connecté.
     */
    List<RecetteTerrainResponse> getMine();

    /**
     * Récupère les recettes d'un site
     */
    List<RecetteTerrainResponse> getBySite(Long siteId);

    /**
     * Récupère les recettes d'une date donnée
     */
    List<RecetteTerrainResponse> getByDateRecette(LocalDate date);

    /**
     * Récupère les recettes par statut
     */
    List<RecetteTerrainResponse> getByStatut(RecetteStatut statut);

    /**
     * Récupère les recettes d'un site par statut
     */
    List<RecetteTerrainResponse> getBySiteAndStatut(Long siteId, RecetteStatut statut);

    /**
     * Récupère les recettes d'une plage de dates
     */
    List<RecetteTerrainResponse> getByDateRange(LocalDate debut, LocalDate fin);

    /**
     * Récupère les recettes en attente de validation pour un site
     */
    List<RecetteTerrainResponse> getEnAttenteValidation(Long siteId);

    // ========== WORKFLOW ==========

    /**
     * Soumet une recette pour validation (BROUILLON → SOUMISE)
     * Vérifications:
     * - Statut = BROUILLON
     * - Tous champs obligatoires présents (isComplete)
     * - Montants cohérents
     */
    RecetteTerrainResponse soumettre(Long id);

    /**
     * Valide ou rejette une recette soumise (SOUMISE → VALIDEE/REJETEE)
     * Réservé aux responsables site et contrôleurs
     * Si REJETEE: motif requis
     */
    RecetteTerrainResponse valider(Long id, ValidateRecetteTerrainRequest request);

    /**
     * Rejette une recette (raccourci pour valider avec REJETEE)
     */
    RecetteTerrainResponse rejeter(Long id, String motifRejet, Long validePar);

    // ========== MÉTIERS ==========

    /**
     * Calcule/recalcule les écarts (excédent ou manquant)
     * Appelé automatiquement à la création/modification
     */
    RecetteTerrainResponse calculerEcarts(Long id);

    /**
     * Récupère le total collecté pour un agent à une date donnée
     */
    java.math.BigDecimal getTotalCollecteByAgentAndDate(Long agentTerrainId, LocalDate date);

    /**
     * Valide qu'un agent terrain existe et est actif
     */
    void validerAgent(Long agentTerrainId);

    /**
     * Valide qu'un site existe et est actif
     */
    void validerSite(Long siteId);

    /**
     * Valide que l'agent est affecté au site
     */
    void validerAgentAffecteAuSite(Long agentTerrainId, Long siteId);

    /**
     * Valide que crédit et site sont même agence
     */
    void validerMemeAgence(Long agentTerrainId, Long siteId);

    // ========== PAGINATION ==========

    /**
     * Récupère toutes les recettes paginées
     */
    Page<RecetteTerrainResponse> getAllPaginated(Pageable pageable);

    /**
     * Recherche et filtre les recettes avec pagination
     * Paramètres optionnels: statut, siteId, agentTerrainId, dateDebut, dateFin
     * @param pageable pagination parameters (page, size, sort)
     * @param statut filter by statut (nullable)
     * @param siteId filter by site (nullable)
     * @param agentTerrainId filter by agent (nullable)
     * @param dateDebut filter by start date (nullable)
     * @param dateFin filter by end date (nullable)
     */
    Page<RecetteTerrainResponse> searchAndFilter(
        Pageable pageable,
        RecetteStatut statut,
        Long siteId,
        Long agentTerrainId,
        LocalDate dateDebut,
        LocalDate dateFin);
}
