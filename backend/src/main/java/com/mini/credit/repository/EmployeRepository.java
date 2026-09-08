package com.mini.credit.repository;

import com.mini.credit.entity.employe.Employe;
import com.mini.credit.enums.PosteEmploye;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeRepository extends JpaRepository<Employe, Long> {
    Optional<Employe> findByMatricule(String matricule);
    List<Employe> findByActifTrue();
    boolean existsByMatricule(String matricule);

        @Query("""
                SELECT COUNT(e) > 0
                FROM Employe e
                WHERE REPLACE(TRIM(e.telephone), ' ', '') = :telephone
                    AND (:excludedId IS NULL OR e.id <> :excludedId)
        """)
        boolean existsByNormalizedTelephoneExcludingId(@Param("telephone") String telephone, @Param("excludedId") Long excludedId);

        @Query("""
                SELECT COUNT(e) > 0
                FROM Employe e
                WHERE UPPER(TRIM(e.prenom)) = :prenom
                    AND UPPER(TRIM(e.nom)) = :nom
                    AND (:excludedId IS NULL OR e.id <> :excludedId)
        """)
        boolean existsByNormalizedPrenomAndNomExcludingId(
                        @Param("prenom") String prenom,
                        @Param("nom") String nom,
                        @Param("excludedId") Long excludedId);

    /**
     * Employés actifs sans compte utilisateur lié.
     * La condition porte sur la relation inverse (utilisateur.employe_id via mappedBy) :
     * aucun Utilisateur n'a employe_id = cet employé.
     */
    @Query("SELECT e FROM Employe e WHERE e.actif = true AND e.utilisateur IS NULL")
    List<Employe> findDisponibles();

    // Agence-related queries
    List<Employe> findByAgenceId(Long agenceId);
    List<Employe> findByAgenceIdAndActifTrue(Long agenceId);
    List<Employe> findByAgenceIdAndActifTrueOrderByNomAsc(Long agenceId);
    Long countByAgenceId(Long agenceId);

    /**
     * Employés actifs NON rattachés à l'agence donnée.
     * Utilisé par la modal "Affecter un employé existant" dans le détail Agence.
     */
    List<Employe> findByAgenceIdNotAndActifTrue(Long agenceId);

    /**
     * Retourne les employés actifs ayant une fonction spécifique.
     * Utilisé pour lister les Gestionnaires disponibles.
     */
    List<Employe> findByFonctionAndActifTrue(PosteEmploye fonction);

    /**
     * Récupère tous les matricules dont le début correspond au préfixe donné.
     * Utilisé par MatriculeGeneratorService pour calculer la prochaine séquence.
     *
     * Exemple de préfixe : "DEL1-GES-26-" — retourne ["DEL1-GES-26-001", "DEL1-GES-26-002", …]
     */
    @org.springframework.data.jpa.repository.Query(
        "SELECT e.matricule FROM Employe e WHERE e.matricule LIKE :prefix%"
    )
    List<String> findMatriculesByPrefix(
        @org.springframework.data.repository.query.Param("prefix") String prefix
    );
}
