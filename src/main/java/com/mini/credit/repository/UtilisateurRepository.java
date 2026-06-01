package com.mini.credit.repository;

import com.mini.credit.entity.referentiel.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    
    /**
     * Charge l'utilisateur avec ses rôles et permissions en une seule requête JOIN FETCH.
     * Évite le problème N+1 et garantit que les permissions sont disponibles hors de la session.
     */
    @Query("SELECT DISTINCT u FROM ReferentielUtilisateur u " +
           "JOIN FETCH u.role r " +
           "LEFT JOIN FETCH r.permissions rp " +
           "LEFT JOIN FETCH rp.permission p " +
           "WHERE u.username = :username")
    Optional<Utilisateur> findByUsername(@Param("username") String username);

    Optional<Utilisateur> findByEmail(String email);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);
}
