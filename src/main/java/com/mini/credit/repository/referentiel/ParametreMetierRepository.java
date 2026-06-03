package com.mini.credit.repository.referentiel;

import com.mini.credit.entity.referentiel.ParametreMetier;
import com.mini.credit.enums.CategorieParametre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParametreMetierRepository extends JpaRepository<ParametreMetier, Long> {

    /**
     * Trouve un paramètre par sa clé unique.
     */
    Optional<ParametreMetier> findByCle(String cle);

    /**
     * Trouve tous les paramètres d'une catégorie.
     */
    List<ParametreMetier> findByCategorie(CategorieParametre categorie);

    /**
     * Trouve tous les paramètres actifs.
     */
    @Query("SELECT p FROM ParametreMetier p WHERE p.actif = true")
    List<ParametreMetier> findAllActifs();

    /**
     * Trouve tous les paramètres d'une catégorie et actifs.
     */
    @Query("SELECT p FROM ParametreMetier p WHERE p.categorie = :categorie AND p.actif = true")
    List<ParametreMetier> findByCategorieAndActif(@Param("categorie") CategorieParametre categorie);

    /**
     * Vérifie l'existence d'un paramètre par sa clé.
     */
    boolean existsByCle(String cle);
}
