package com.mini.credit.repository.referentiel;

import com.mini.credit.entity.referentiel.Employe;
import com.mini.credit.enums.PosteEmploye;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour la gestion des employés.
 * Permet la persistence et la consultation des données d'employés.
 */
@Repository
public interface EmployeRepository extends JpaRepository<Employe, Long> {

    /**
     * Recherche un employé par son code unique
     */
    Optional<Employe> findByCodeEmploye(String codeEmploye);

    /**
     * Cherche un employé par son nom complet
     */
    Optional<Employe> findByNomComplet(String nomComplet);

    /**
     * Récupère tous les employés d'un site
     */
    List<Employe> findBySiteIdAndActifTrue(Long siteId);

    /**
     * Récupère tous les employés d'un poste sur tous les sites
     */
    List<Employe> findByPosteAndActifTrue(PosteEmploye poste);

    /**
     * Récupère tous les employés d'un poste dans un site spécifique
     */
    List<Employe> findBySiteIdAndPosteAndActifTrue(Long siteId, PosteEmploye poste);

    /**
     * Récupère tous les employés actifs
     */
    List<Employe> findAllByActifTrue();

    /**
     * Vérifie si un code employé existe
     */
    boolean existsByCodeEmploye(String codeEmploye);
}
