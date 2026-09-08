package com.mini.credit.repository.collecteTerrain;

import com.mini.credit.entity.referentiel.CollecteJournaliereTerrain;
import com.mini.credit.enums.RecetteStatut;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CollecteJournaliereTerrainRepository extends JpaRepository<CollecteJournaliereTerrain, Long> {

    @EntityGraph(attributePaths = {"agentTerrain", "agentTerrain.utilisateur", "site"})
    Optional<CollecteJournaliereTerrain> findByAgentTerrainIdAndDateCollecte(Long agentTerrainId, LocalDate dateCollecte);

    @EntityGraph(attributePaths = {"agentTerrain", "agentTerrain.utilisateur", "site", "validatedBy"})
    List<CollecteJournaliereTerrain> findByStatutAndAntenneId(RecetteStatut statut, Long antenneId);

    @EntityGraph(attributePaths = {"agentTerrain", "agentTerrain.utilisateur", "site", "validatedBy"})
    List<CollecteJournaliereTerrain> findByStatutAndSiteId(RecetteStatut statut, Long siteId);

        @Query("""
                select distinct c
                from CollecteJournaliereTerrain c
                left join fetch c.site s
                left join fetch s.agence sa
                left join fetch c.lignes l
                where c.dateCollecte between :dateDebut and :dateFin
                    and (:agenceId is null or c.antenneId = :agenceId or s.agence.id = :agenceId)
                order by c.dateCollecte desc, c.id desc
                """)
        List<CollecteJournaliereTerrain> findForCarnetControls(
                        @Param("dateDebut") LocalDate dateDebut,
                        @Param("dateFin") LocalDate dateFin,
                        @Param("agenceId") Long agenceId
        );

        @Query("""
            select count(c)
            from CollecteJournaliereTerrain c
            left join c.site s
            where c.statut = com.mini.credit.enums.RecetteStatut.VALIDEE
                and c.dateCollecte between :dateDebut and :dateFin
                and (:agenceId is null or c.antenneId = :agenceId or s.agence.id = :agenceId)
            """)
        long countValideesByAgenceAndPeriod(
                @Param("dateDebut") LocalDate dateDebut,
                @Param("dateFin") LocalDate dateFin,
                @Param("agenceId") Long agenceId
        );
}
