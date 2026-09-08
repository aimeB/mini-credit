package com.mini.credit.repository;

import com.mini.credit.entity.agence.Agence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour l'entité Agence
 * Gère l'accès aux données d'agence
 */
@Repository
public interface AgenceRepository extends JpaRepository<Agence, Long> {

    /**
     * Rechercher une agence par code
     * @param codeAgence Code unique de l'agence
     * @return Agence trouvée
     */
    Optional<Agence> findByCodeAgence(String codeAgence);

    /**
     * Rechercher une agence par code insensible à la casse
     * @param codeAgence Code de l'agence
     * @return Agence trouvée
     */
    @Query("SELECT a FROM Agence a WHERE UPPER(a.codeAgence) = UPPER(:codeAgence)")
    Optional<Agence> findByCodeAgenceIgnoreCase(@Param("codeAgence") String codeAgence);

    /**
     * Récupérer toutes les agences actives
     * @return Liste des agences actives
     */
    List<Agence> findByActifTrue();

    /**
     * Récupérer toutes les agences inactives
     * @return Liste des agences inactives
     */
    List<Agence> findByActifFalse();

    /**
     * Vérifier si un code d'agence existe
     * @param codeAgence Code de l'agence
     * @return true si existe, false sinon
     */
    boolean existsByCodeAgence(String codeAgence);

    /**
     * Vérifier si un code d'agence existe en ignorant la casse
     * @param codeAgence Code de l'agence
     * @return true si existe, false sinon
     */
    @Query("SELECT COUNT(a) > 0 FROM Agence a WHERE UPPER(a.codeAgence) = UPPER(:codeAgence)")
    boolean existsByCodeAgenceIgnoreCase(@Param("codeAgence") String codeAgence);

    /**
     * Rechercher par ville
     * @param ville Ville
     * @return Liste des agences de cette ville
     */
    List<Agence> findByVilleAndActifTrue(String ville);

    /**
     * Compter les agences actives
     * @return Nombre d'agences actives
     */
    long countByActifTrue();
}
