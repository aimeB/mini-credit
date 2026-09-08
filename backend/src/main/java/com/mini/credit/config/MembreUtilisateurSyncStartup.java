package com.mini.credit.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

/**
 * 🔧 Correction des relations Membre <-> Utilisateur au démarrage
 * 
 * Exécutée UNE FOIS au démarrage de l'application
 * Synchronise la base de données pour éviter les incohérences
 */
@Component
public class MembreUtilisateurSyncStartup {
    
    private static final Logger logger = LoggerFactory.getLogger(MembreUtilisateurSyncStartup.class);
    private final JdbcTemplate jdbcTemplate;
    
    public MembreUtilisateurSyncStartup(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    /**
     * Exécutée après le démarrage complet de l'application et l'initialisation de la DB
     */
    @EventListener(ApplicationReadyEvent.class)
    public void syncMembreUtilisateurRelationship() {
        logger.info("🔄 Vérification de la cohérence Membre <-> Utilisateur...");
        
        try {
            // 1️⃣ SYNCHRONISATION: Corriger les utilisateur_id NULL
            int syncedCount = jdbcTemplate.update(
                "UPDATE membre m " +
                "SET utilisateur_id = (SELECT u.id FROM utilisateur u WHERE u.membre_id = m.id) " +
                "WHERE utilisateur_id IS NULL AND id IN (SELECT membre_id FROM utilisateur WHERE membre_id IS NOT NULL)"
            );
            
            if (syncedCount > 0) {
                logger.warn("⚠️  {} Membros avec utilisateur_id=NULL ont été synchronisés", syncedCount);
            } else {
                logger.info("✅ Aucun Membre à corriger");
            }
            
            // 2️⃣ VALIDATION: Chercher les incohérences restantes
            Integer inconsistencyCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM membre m " +
                "LEFT JOIN utilisateur u ON m.utilisateur_id = u.id " +
                "WHERE m.utilisateur_id IS NOT NULL AND u.membre_id != m.id",
                Integer.class
            );
            
            if (inconsistencyCount != null && inconsistencyCount > 0) {
                logger.error("❌ {} incohérence(s) Membre<->Utilisateur détectée(s) après synchronisation!", 
                            inconsistencyCount);
                
                // Afficher les détails
                jdbcTemplate.query(
                    "SELECT m.id as membre_id, m.utilisateur_id, u.id as user_id, u.membre_id as user_membre_id " +
                    "FROM membre m " +
                    "LEFT JOIN utilisateur u ON m.utilisateur_id = u.id " +
                    "WHERE m.utilisateur_id IS NOT NULL AND u.membre_id != m.id",
                    rs -> {
                        logger.error("  Membre_id={}, Utilisateur_id={}, Utilisateur.membre_id={}",
                                   rs.getLong("membre_id"),
                                   rs.getLong("utilisateur_id"),
                                   rs.getObject("user_membre_id") != null ? rs.getLong("user_membre_id") : "NULL");
                    }
                );
            } else {
                logger.info("✅ Toutes les relations Membre<->Utilisateur sont cohérentes");
            }
            
            // 3️⃣ LOG: Statistiques
            Integer totalMembers = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM membre",
                Integer.class
            );
            
            Integer withUser = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM membre WHERE utilisateur_id IS NOT NULL",
                Integer.class
            );
            
            logger.info("📊 Statistiques: Total Members={}, With User={}, Without User={}",
                       totalMembers, withUser, totalMembers - withUser);
            
        } catch (Exception e) {
            logger.error("❌ ERREUR lors de la vérification des relations Membre<->Utilisateur", e);
            // NE PAS arrêter l'application, mais logger l'erreur
        }
    }
}
