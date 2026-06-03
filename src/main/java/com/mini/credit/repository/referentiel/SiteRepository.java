package com.mini.credit.repository.referentiel;

import com.mini.credit.entity.referentiel.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

/**
 * Repository pour la gestion des sites/antennes.
 */
@Repository
public interface SiteRepository extends JpaRepository<Site, Long> {

    /**
     * Recherche un site par son code unique
     */
    Optional<Site> findByCodeSite(String codeSite);

    /**
     * Recherche un site par son nom
     */
    Optional<Site> findByNomSite(String nomSite);

    /**
     * Récupère tous les sites actifs
     */
    List<Site> findAllByActifTrue();

    /**
     * Vérifie si un code site existe
     */
    boolean existsByCodeSite(String codeSite);
}
