package com.mini.credit.service;

import com.mini.credit.dto.caisse.RecetteJournaliereTerrainDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * PHASE 6: Service pour recettes journalières terrain.
 */
public interface RecetteJournaliereTerrainService {

    /**
     * Crée une nouvelle recette journalière
     */
    RecetteJournaliereTerrainDTO creerRecette(
            Long agentId, Long membreId, LocalDate dateJour, String typeRecette,
            BigDecimal montant, String observation, String referencePapier);

    /**
     * Récupère une recette par ID
     */
    RecetteJournaliereTerrainDTO getById(Long id);

    /**
     * Récupère les recettes d'un jour donné
     */
    List<RecetteJournaliereTerrainDTO> getByDateJour(LocalDate dateJour);

    /**
     * Récupère les recettes d'un agent pour un jour
     */
    List<RecetteJournaliereTerrainDTO> getByAgentAndDateJour(Long agentId, LocalDate dateJour);

    /**
     * Récupère les recettes en attente de validation
     */
    List<RecetteJournaliereTerrainDTO> getEnAttenteValidation();

    /**
     * Récupère les recettes en attente pour un jour
     */
    List<RecetteJournaliereTerrainDTO> getEnAttenteValidationByDateJour(LocalDate dateJour);

    /**
     * PHASE 6: Valide une recette (calcul variance)
     * cashRemis = cash physique remis pour cette recette
     * variance = abs(cashRemis - montant)
     */
    RecetteJournaliereTerrainDTO validerRecette(Long recetteId, BigDecimal cashRemis);

    /**
     * Rejette une recette
     */
    RecetteJournaliereTerrainDTO rejeterRecette(Long recetteId, String motif);

    /**
     * Annule une recette
     */
    RecetteJournaliereTerrainDTO annulerRecette(Long recetteId);

    /**
     * Somme des montants pour un jour
     */
    BigDecimal getSommeByDateJour(LocalDate dateJour);

    /**
     * Somme des montants validés pour un jour
     */
    BigDecimal getSommeValideeByDateJour(LocalDate dateJour);
}
