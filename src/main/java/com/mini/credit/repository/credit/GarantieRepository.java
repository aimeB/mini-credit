package com.mini.credit.repository.credit;

import com.mini.credit.entity.credit.Garantie;
import com.mini.credit.entity.credit.DemandeCredit;
import com.mini.credit.enums.StatutGarantie;
import com.mini.credit.enums.TypeGarantie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GarantieRepository extends JpaRepository<Garantie, Long> {

    // Recherches par crédit
    List<Garantie> findByCreditId(Long creditId);

    // Recherches par demande de crédit
    List<Garantie> findByDemandeCreditId(Long demandeCreditId);

    // PHASE 4: Recherche par entité DemandeCredit
    List<Garantie> findByDemandeCredit(DemandeCredit demandeCredit);

    // Recherches par membre
    List<Garantie> findByMembreId(Long membreId);
    
    @Query("SELECT g FROM Garantie g LEFT JOIN FETCH g.credit LEFT JOIN FETCH g.demandeCredit LEFT JOIN FETCH g.membre WHERE g.membre.id = :membreId")
    List<Garantie> findByMembreIdWithEagerLoad(@org.springframework.data.repository.query.Param("membreId") Long membreId);

    // Recherches par type
    List<Garantie> findByTypeGarantie(TypeGarantie typeGarantie);

    // Recherches par statut
    List<Garantie> findByStatut(StatutGarantie statut);

    // Requête personnalisée pour chercher par date
    @Query("SELECT g FROM Garantie g WHERE g.dateCreation BETWEEN :debut AND :fin")
    List<Garantie> findByDateRange(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    // Vérifier si une garantie existe pour un crédit
    boolean existsByCreditId(Long creditId);

    // Compteur par statut
    long countByStatut(StatutGarantie statut);

    // Compteur par type
    long countByTypeGarantie(TypeGarantie typeGarantie);
}
