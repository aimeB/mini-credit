package com.mini.credit.repository.agentTerrain;

import com.mini.credit.entity.referentiel.AgentTerrain;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AgentTerrainRepository extends JpaRepository<AgentTerrain, Long> {

    @Override
    @EntityGraph(attributePaths = {"utilisateur", "site", "sitesAffectes", "gestionnaire"})
    List<AgentTerrain> findAll();

    @EntityGraph(attributePaths = {"utilisateur", "utilisateur.role", "utilisateur.employe", "utilisateur.employe.site", "utilisateur.employe.agence", "site", "site.agence", "sitesAffectes", "sitesAffectes.agence"})
    @Query("SELECT DISTINCT a FROM AgentTerrain a LEFT JOIN a.sitesAffectes s WHERE a.actif = true")
    List<AgentTerrain> findAllActifsWithSalaryBeneficiaryContext();

    /**
     * Cherche un agent terrain par son utilisateur avec eager loading.
     */
    @EntityGraph(attributePaths = {"utilisateur", "site", "sitesAffectes", "gestionnaire"})
    Optional<AgentTerrain> findByUtilisateurId(Long utilisateurId);

    /**
     * Retourne tous les AgentTerrain supervisés par un gestionnaire donné.
     * Utilisé pour calculer le portefeuille du gestionnaire.
     */
    @EntityGraph(attributePaths = {"utilisateur", "site", "sitesAffectes", "gestionnaire"})
    List<AgentTerrain> findByGestionnaireIdAndActifTrue(Long gestionnaireId);

    /**
     * Retourne les sites distincts affectés à tous les agents d'un gestionnaire.
     * Requête JPQL pour calculer le portefeuille.
     */
    @Query("SELECT DISTINCT s FROM AgentTerrain at JOIN at.sitesAffectes s " +
           "WHERE at.gestionnaire.id = :gestionnaireId AND at.actif = true AND s.actif = true")
    List<com.mini.credit.entity.referentiel.Site> findSitesByGestionnaireId(@Param("gestionnaireId") Long gestionnaireId);

    /**
     * Compte les membres distincts affiliés aux sites actifs couverts par un gestionnaire.
     */
    @Query("SELECT COUNT(DISTINCT m) FROM AgentTerrain at JOIN at.sitesAffectes s JOIN s.membres m " +
           "WHERE at.gestionnaire.id = :gestionnaireId AND at.actif = true AND s.actif = true")
    long countMembresByGestionnaireId(@Param("gestionnaireId") Long gestionnaireId);

    /**
     * Vérifie si un matricule est déjà utilisé (hors entité donnée).
     */
    boolean existsByMatricule(String matricule);

    /**
     * Retourne tous les agents actifs affiliés à un site donné.
     * Couvre les deux cas :
     *   1. agent.site.id = siteId   (site principal)
     *   2. siteId dans agent.sitesAffectes (sites supplémentaires)
     * Utilise DISTINCT pour éviter les doublons si les deux conditions sont vraies.
     */
    @Query("""
        SELECT DISTINCT a FROM AgentTerrain a
        LEFT JOIN a.sitesAffectes sa
        WHERE a.actif = true
          AND a.utilisateur.actif = true
          AND a.utilisateur.isEnabled = true
          AND a.utilisateur.isLocked = false
          AND a.utilisateur.accountLocked = false
          AND a.utilisateur.role.code = com.mini.credit.enums.security.RoleCode.AGENT_TERRAIN
          AND a.utilisateur.employe.actif = true
          AND a.utilisateur.employe.fonction = com.mini.credit.enums.PosteEmploye.AGENT_TERRAIN
          AND (a.site.id = :siteId OR sa.id = :siteId)
        """)
    @EntityGraph(attributePaths = {"utilisateur", "utilisateur.employe", "site", "sitesAffectes", "gestionnaire"})
    List<AgentTerrain> findActifsBySiteId(@Param("siteId") Long siteId);

    /**
     * Détecte les incohérences d'affectation pour une agence donnée.
     * Retourne les agents terrain dont :
     *   - le site principal appartient à l'agence cible
     *   - ET soit l'employé n'est pas dans cette agence
     *   - ET soit le gestionnaire n'est pas dans cette agence
     */
    @Query("""
        SELECT DISTINCT a FROM AgentTerrain a
        WHERE a.site.agence.id = :agenceId
          AND a.actif = true
          AND (
            a.utilisateur.employe.agence.id != :agenceId
            OR (a.gestionnaire IS NOT NULL AND a.gestionnaire.agence.id != :agenceId)
          )
        """)
    @EntityGraph(attributePaths = {"utilisateur", "utilisateur.employe", "utilisateur.employe.agence",
        "site", "site.agence", "sitesAffectes", "gestionnaire", "gestionnaire.agence"})
    List<AgentTerrain> findAnomaliesByAgenceId(@Param("agenceId") Long agenceId);
}