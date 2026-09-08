package com.mini.credit.util;

import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.Utilisateur;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashSet;
import java.util.Set;

/**
 * 🔒 Validateur de synchronisation Membre <-> Utilisateur
 * 
 * Garantit que la relation bidirectionnelle est toujours en cohérence:
 * - Si Utilisateur.membre = M, alors Membre.utilisateur = U
 * - Si Membre.utilisateur = U, alors Utilisateur.membre = M
 */
public class MembreUtilisateurValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(MembreUtilisateurValidator.class);
    
    /**
     * Valide et synchronise la relation Membre <-> Utilisateur
     * 
     * @param utilisateur L'utilisateur à vérifier
     * @param membre Le membre à vérifier
     * @throws IllegalStateException si la relation est incohérente ET impossible à corriger
     */
    public static void validateAndSync(Utilisateur utilisateur, Membre membre) {
        if (utilisateur == null || membre == null) {
            return; // Pas de relation à valider
        }
        
        boolean userPointsToMember = utilisateur.getMembre() != null && 
                                    utilisateur.getMembre().getId() != null &&
                                    utilisateur.getMembre().getId().equals(membre.getId());
        
        boolean memberPointsToUser = membre.getUtilisateur() != null && 
                                    membre.getUtilisateur().getId() != null &&
                                    membre.getUtilisateur().getId().equals(utilisateur.getId());
        
        // ✅ Les deux côtés sont cohérents
        if (userPointsToMember && memberPointsToUser) {
            return;
        }
        
        // ❌ Incohérence détectée
        if (userPointsToMember != memberPointsToUser) {
            logger.warn("⚠️  INCOHÉRENCE détectée [Utilisateur_id={}, Membre_id={}]: " +
                       "userPointsToMember={}, memberPointsToUser={}", 
                       utilisateur.getId(), membre.getId(), 
                       userPointsToMember, memberPointsToUser);
            
            // AUTO-CORRECTION: Synchroniser le côté manquant
            if (userPointsToMember && !memberPointsToUser) {
                logger.info("🔧 AUTO-CORRECTION: Synchronizing Membre.utilisateur");
                membre.setUtilisateur(utilisateur);
            } else if (!userPointsToMember && memberPointsToUser) {
                logger.info("🔧 AUTO-CORRECTION: Synchronizing Utilisateur.membre");
                utilisateur.setMembre(membre);
            }
        }
    }
    
    /**
     * Valide une collection de Members pour les incohérences
     * Retourne un rapport de validation
     */
    public static ValidatorReport validateMembers(java.util.List<Membre> membres) {
        ValidatorReport report = new ValidatorReport();
        
        for (Membre membre : membres) {
            Utilisateur utilisateur = membre.getUtilisateur();
            
            if (utilisateur != null) {
                boolean userPointsToMember = utilisateur.getMembre() != null && 
                                            utilisateur.getMembre().getId() != null &&
                                            utilisateur.getMembre().getId().equals(membre.getId());
                
                if (!userPointsToMember) {
                    report.addInconsistency(membre.getId(), utilisateur.getId(),
                            "Utilisateur pointe vers Membre, mais Membre.utilisateur != ce Utilisateur");
                }
            }
        }
        
        return report;
    }
    
    /**
     * 📊 Rapport de validation
     */
    public static class ValidatorReport {
        private final Set<String> inconsistencies = new HashSet<>();
        private int totalChecked = 0;
        
        public void addInconsistency(Long membreId, Long userId, String reason) {
            inconsistencies.add(String.format("[M%d,U%d] %s", membreId, userId, reason));
        }
        
        public boolean hasInconsistencies() {
            return !inconsistencies.isEmpty();
        }
        
        public int getInconsistencyCount() {
            return inconsistencies.size();
        }
        
        @Override
        public String toString() {
            if (inconsistencies.isEmpty()) {
                return "✅ Aucune incohérence détectée";
            }
            return "❌ " + inconsistencies.size() + " incohérences:\n" + 
                   String.join("\n", inconsistencies);
        }
    }
}
