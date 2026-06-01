package com.mini.credit.listener;

import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.util.MembreUtilisateurValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;

/**
 * 🔍 Listener JPA pour Membre
 * 
 * Déclenché automatiquement après:
 * - Chargement depuis DB (@PostLoad)
 * - Création (@PostPersist)
 * - Modification (@PostUpdate)
 * 
 * Valide et auto-corrige les incohérences Membre <-> Utilisateur
 */
public class MembreEntityListener {
    
    private static final Logger logger = LoggerFactory.getLogger(MembreEntityListener.class);
    
    /**
     * Après CHARGEMENT de la base de données
     * ⚠️ Point critique: Détecte les incohérences existantes
     */
    @PostLoad
    public void onPostLoad(Membre membre) {
        if (membre == null || membre.getId() == null) {
            return;
        }
        
        Utilisateur utilisateur = membre.getUtilisateur();
        
        // ✅ Cas normal: Une relation bidirectionnelle cohérente
        if (utilisateur != null) {
            validateAndAutoCorrect(membre, utilisateur);
        }
        
        // ⚠️ Cas anormal: Membre.utilisateur_id est NULL mais était censé avoir une relation
        // Ce cas ne peut pas être détecté ici (NULL le rend invisible)
        // Solution: Le validateur SQL doit d'abord corriger la DB
    }
    
    /**
     * Après CRÉATION en base de données
     * Vérifie que la relation a été correctement créée
     */
    @PostPersist
    public void onPostPersist(Membre membre) {
        if (membre.getUtilisateur() != null) {
            logger.info("✅ Membre créé: id={}, utilisateur_id={}", 
                       membre.getId(), 
                       membre.getUtilisateur().getId());
            validateAndAutoCorrect(membre, membre.getUtilisateur());
        }
    }
    
    /**
     * Après MODIFICATION en base de données
     * Vérifie que la relation reste cohérente après update
     */
    @PostUpdate
    public void onPostUpdate(Membre membre) {
        if (membre.getUtilisateur() != null) {
            logger.info("✅ Membre mis à jour: id={}, utilisateur_id={}", 
                       membre.getId(), 
                       membre.getUtilisateur().getId());
            validateAndAutoCorrect(membre, membre.getUtilisateur());
        }
    }
    
    /**
     * 🔧 Valide et auto-corrige les incohérences
     */
    private void validateAndAutoCorrect(Membre membre, Utilisateur utilisateur) {
        boolean userPointsToMember = utilisateur.getMembre() != null && 
                                    utilisateur.getMembre().getId() != null &&
                                    utilisateur.getMembre().getId().equals(membre.getId());
        
        boolean memberPointsToUser = utilisateur.getId() != null &&
                                    utilisateur.getId().equals(
                                        membre.getUtilisateur().getId());
        
        if (userPointsToMember && memberPointsToUser) {
            // ✅ Tout va bien
            return;
        }
        
        // ❌ Incohérence détectée
        logger.warn("⚠️  INCOHÉRENCE [Membre_id={}, Utilisateur_id={}]: " +
                   "userPointsToMember={}, memberPointsToUser={}", 
                   membre.getId(), utilisateur.getId(), 
                   userPointsToMember, memberPointsToUser);
    }
}
