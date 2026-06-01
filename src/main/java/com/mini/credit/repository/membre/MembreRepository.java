package com.mini.credit.repository.membre;

import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.StatutMembre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MembreRepository extends JpaRepository<Membre, Long> {
    Optional<Membre> findByCodeMembre(String codeMembre);
    Optional<Membre> findByTelephonePrincipal(String telephonePrincipal);
    Optional<Membre> findByUtilisateur(Utilisateur utilisateur);
    
    /**
     * ✅ GLOBAL - Inclut les membres CLOTURE
     * À utiliser pour vérifier l'unicité du code membre (historique).
     * Un code clôturé ne doit jamais être réutilisé.
     */
    boolean existsByCodeMembre(String codeMembre);
    
    List<Membre> findByStatutNot(StatutMembre statut);
    
    @Query("SELECT m FROM Membre m LEFT JOIN FETCH m.site LEFT JOIN FETCH m.agent WHERE m.utilisateur = :utilisateur")
    Optional<Membre> findByUtilisateurWithEagerLoad(@Param("utilisateur") Utilisateur utilisateur);
    
    @Query("SELECT m FROM Membre m LEFT JOIN FETCH m.site LEFT JOIN FETCH m.agent WHERE m.utilisateur.username = :username")
    Optional<Membre> findByUtilisateurUsernameWithEagerLoad(@Param("username") String username);
    
    @Query("SELECT m FROM Membre m LEFT JOIN FETCH m.site LEFT JOIN FETCH m.agent WHERE m.id = :id")
    Optional<Membre> findByIdWithEagerLoad(@Param("id") Long id);
    
    @Query("SELECT DISTINCT m FROM Membre m LEFT JOIN FETCH m.site LEFT JOIN FETCH m.agent WHERE m.statut != :statut")
    List<Membre> findByStatutNotWithEagerLoad(@Param("statut") StatutMembre statut);

    // ========== SECURED METHODS (excluent CLOTURE) ==========
    // À utiliser pour toutes les opérations normales de l'application (consultation, modification)
    
    /**
     * ✅ Récupère un membre ACTIF par ID avec eager loading.
     * Exclut les membres CLOTURE.
     */
    @Query("SELECT m FROM Membre m LEFT JOIN FETCH m.site LEFT JOIN FETCH m.agent " +
           "WHERE m.id = :id AND m.statut != 'CLOTURE'")
    Optional<Membre> findByIdActiveWithEagerLoad(@Param("id") Long id);

    /**
     * ✅ Récupère un membre ACTIF par username avec eager loading.
     * Exclut les membres CLOTURE. Utilisé pour l'authentification.
     */
    @Query("SELECT m FROM Membre m LEFT JOIN FETCH m.site LEFT JOIN FETCH m.agent " +
           "WHERE m.utilisateur.username = :username AND m.statut != 'CLOTURE'")
    Optional<Membre> findByUtilisateurUsernameActiveWithEagerLoad(@Param("username") String username);

    /**
     * ✅ Récupère tous les membres ACTIFS avec eager loading.
     * Exclut les membres CLOTURE.
     */
    @Query("SELECT DISTINCT m FROM Membre m LEFT JOIN FETCH m.site LEFT JOIN FETCH m.agent " +
           "WHERE m.statut != 'CLOTURE'")
    List<Membre> findAllActive();

    // ========== GENERATION/VALIDATION METHODS (GLOBAL) ==========
    // À utiliser pour les validations d'unicité historique
    
    /**
     * ✅ Récupère tous les usernames existants (y compris clôturés).
     * À utiliser UNIQUEMENT pour générer de nouveaux usernames uniques.
     * ⚠️ Ne pas réutiliser automatiquement un username clôturé.
     */
    @Query("SELECT u.username FROM ReferentielUtilisateur u")
    List<String> findAllExistingUsernames();

    // ========== AUDIT/HISTORIQUE METHODS ==========
    // À utiliser uniquement pour les rapports d'audit et l'historique
    
    /**
     * 🔍 Récupère un membre (même clôturé) par ID.
     * Pour accès audit uniquement.
     */
    @Query("SELECT m FROM Membre m LEFT JOIN FETCH m.site LEFT JOIN FETCH m.agent WHERE m.id = :id")
    Optional<Membre> findByIdForAudit(@Param("id") Long id);

    /**
     * 🔍 Récupère tous les membres (y compris clôturés) pour audit.
     */
    @Query("SELECT DISTINCT m FROM Membre m LEFT JOIN FETCH m.site LEFT JOIN FETCH m.agent")
    List<Membre> findAllForAudit();
}
