package com.mini.credit.service;

import com.mini.credit.dto.parametrage.ParametreMetierDTO;
import com.mini.credit.enums.CategorieParametre;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Service pour gérer les paramètres métier du système.
 * Fournit des méthodes pour récupérer les paramètres avec cache.
 *
 * PHASE 1: Interface de base pour l'accès aux paramètres.
 */
public interface ParametreMetierService {

    /**
     * Récupère un paramètre decimal par sa clé.
     */
    BigDecimal getDecimal(String cle);

    /**
     * Récupère un paramètre entier par sa clé.
     */
    Long getEntier(String cle);

    /**
     * Récupère un paramètre texte par sa clé.
     */
    String getTexte(String cle);

    /**
     * Récupère un paramètre par sa clé sous forme d'Object.
     */
    Object getValeur(String cle);

    /**
     * Récupère un paramètre par sa clé sous forme d'Optional DTO.
     */
    Optional<ParametreMetierDTO> getByKey(String cle);

    /**
     * Récupère tous les paramètres d'une catégorie.
     */
    List<ParametreMetierDTO> getByCategorie(CategorieParametre categorie);

    /**
     * Récupère tous les paramètres actifs.
     */
    List<ParametreMetierDTO> getAllActifs();

    /**
     * Recharge le cache des paramètres.
     * À appeler après modification en base de données.
     */
    void rechargerCache();

    /**
     * Valide l'existence d'une clé de paramètre.
     */
    boolean existsKey(String cle);
}
