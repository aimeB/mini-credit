package com.mini.credit.repository.agentTerrain;

import com.mini.credit.entity.referentiel.AgentTerrain;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgentTerrainRepository extends JpaRepository<AgentTerrain, Long> {

    @Override
    @EntityGraph(attributePaths = {"utilisateur", "site"})
    List<AgentTerrain> findAll();

    /**
     * Cherche un agent terrain par son utilisateur avec eager loading du site.
     * Étape 9 : Scope implementation
     */
    @EntityGraph(attributePaths = {"utilisateur", "site"})
    Optional<AgentTerrain> findByUtilisateurId(Long utilisateurId);
}