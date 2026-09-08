package com.mini.credit.repository;

import com.mini.credit.entity.referentiel.Utilisateur;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    
    /**
     * Charge l'utilisateur avec ses rôles, permissions, employé, agence et site en une seule requête.
     * Évite le problème N+1 et garantit que le contexte métier est disponible hors de la session.
     */
    @EntityGraph(attributePaths = {
            "role",
            "role.permissions",
            "role.permissions.permission",
            "employe",
            "employe.agence",
            "employe.site",
            "site"
    })
    @Query("SELECT u FROM ReferentielUtilisateur u WHERE u.username = :username")
    Optional<Utilisateur> findByUsername(@Param("username") String username);

        @EntityGraph(attributePaths = {
            "role",
            "role.permissions",
            "role.permissions.permission",
            "employe",
            "employe.agence",
            "employe.site",
            "site"
        })
        @Query("SELECT u FROM ReferentielUtilisateur u WHERE u.id = :id")
        Optional<Utilisateur> findByIdWithValidationContext(@Param("id") Long id);

        @EntityGraph(attributePaths = {
            "role",
            "role.permissions",
            "role.permissions.permission",
            "employe",
            "employe.agence",
            "employe.site",
            "site"
        })
        @Query("SELECT u FROM ReferentielUtilisateur u WHERE u.username = :username")
        Optional<Utilisateur> findByUsernameWithValidationContext(@Param("username") String username);

    Optional<Utilisateur> findByEmail(String email);

        @EntityGraph(attributePaths = {
            "role",
            "employe",
            "employe.agence",
            "employe.site",
            "site",
            "site.agence"
        })
        @Query("SELECT u FROM ReferentielUtilisateur u")
        List<Utilisateur> findAllWithSalaryBeneficiaryContext();

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);

    @EntityGraph(attributePaths = {
        "role",
        "employe",
        "employe.agence",
        "employe.site",
        "employe.site.agence"
    })
    @Query("""
        SELECT u FROM ReferentielUtilisateur u
        WHERE u.actif = true
          AND u.isEnabled = true
          AND u.isLocked = false
          AND u.accountLocked = false
          AND u.role.code = com.mini.credit.enums.security.RoleCode.AGENT_TERRAIN
          AND u.employe.actif = true
          AND u.employe.fonction = com.mini.credit.enums.PosteEmploye.AGENT_TERRAIN
          AND u.employe.site.id = :siteId
        """)
    List<Utilisateur> findSelectableAgentsTerrainByEmployeSiteId(@Param("siteId") Long siteId);
}
