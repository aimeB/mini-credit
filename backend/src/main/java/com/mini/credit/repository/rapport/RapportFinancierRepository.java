package com.mini.credit.repository.rapport;

import com.mini.credit.entity.rapport.RapportFinancier;
import com.mini.credit.enums.StatutRapport;
import com.mini.credit.enums.TypeRapport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

/**
 * PHASE 12: Repository Rapport Financier
 */
public interface RapportFinancierRepository extends JpaRepository<RapportFinancier, Long> {

    // Recherche par type de rapport
    List<RapportFinancier> findByTypeRapportOrderByDateGenerationDesc(TypeRapport typeRapport);

    // Recherche par statut
    List<RapportFinancier> findByStatutOrderByDateGenerationDesc(StatutRapport statut);

    // Recherche par type et date
    List<RapportFinancier> findByTypeRapportAndDateDebutAndDateFinOrderByDateGenerationDesc(
            TypeRapport typeRapport, LocalDate dateDebut, LocalDate dateFin);

    // Recherche rapports récents (non archivés)
    List<RapportFinancier> findByStatutNotOrderByDateGenerationDesc(StatutRapport statut);

    // Vérifier si rapport existe pour période
    boolean existsByTypeRapportAndDateDebutAndDateFin(TypeRapport typeRapport, LocalDate dateDebut, LocalDate dateFin);

    // Rapports en attente de validation
    List<RapportFinancier> findByStatutOrderByDateGenerationAsc(StatutRapport statut);

    // Derniers rapports par type
    @Query(value = "SELECT rf FROM RapportFinancier rf WHERE rf.typeRapport = ?1 ORDER BY rf.dateGeneration DESC LIMIT 5")
    List<RapportFinancier> findTop5ByTypeRapport(TypeRapport typeRapport);
}
