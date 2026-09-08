package com.mini.credit.repository;

import com.mini.credit.entity.referentiel.ActivationToken;
import com.mini.credit.entity.referentiel.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository pour gérer les tokens d'activation
 */
@Repository
public interface ActivationTokenRepository extends JpaRepository<ActivationToken, Long> {
    
    /**
     * Trouver un token par son code unique
     */
    Optional<ActivationToken> findByCode(String code);
    
    /**
     * Trouver le dernier token actif (non utilisé) pour un utilisateur
     */
    @Query("SELECT at FROM ActivationToken at WHERE at.utilisateur = :utilisateur AND at.isUsed = false ORDER BY at.dateCreation DESC LIMIT 1")
    Optional<ActivationToken> findActiveTokenByUtilisateur(@Param("utilisateur") Utilisateur utilisateur);
    
    /**
     * Vérifier si un token existe et est valide
     */
    boolean existsByCodeAndIsUsedFalse(String code);
}
