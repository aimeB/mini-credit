package com.mini.credit.repository.collecteTerrain;

import com.mini.credit.entity.referentiel.CollecteMembreLigne;
import com.mini.credit.enums.TypeLigneCollecte;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface CollecteMembreLigneRepository extends JpaRepository<CollecteMembreLigne, Long> {

    @EntityGraph(attributePaths = {"membre", "compteEpargne", "credit", "demandeCredit"})
    List<CollecteMembreLigne> findByCollecteId(Long collecteId);

    boolean existsByCollecteIdAndMembreIdAndTypeLigne(Long collecteId, Long membreId, TypeLigneCollecte typeLigne);

    List<CollecteMembreLigne> findByCollecteIdAndMembreIdAndTypeLigne(Long collecteId, Long membreId, TypeLigneCollecte typeLigne);

        @Query("""
                select l
                from CollecteMembreLigne l
                join fetch l.collecte c
                left join fetch c.site s
                left join fetch s.agence sa
                left join fetch l.membre m
                where l.typeLigne = com.mini.credit.enums.TypeLigneCollecte.CARNET
                    and c.statut = com.mini.credit.enums.RecetteStatut.VALIDEE
                    and c.dateCollecte between :dateDebut and :dateFin
                    and (:agenceId is null or c.antenneId = :agenceId or s.agence.id = :agenceId)
                    and coalesce(l.montant, 0) > 0
                order by c.dateCollecte desc, l.id desc
                """)
        List<CollecteMembreLigne> findCarnetRevenueLines(
                        @Param("dateDebut") LocalDate dateDebut,
                        @Param("dateFin") LocalDate dateFin,
                        @Param("agenceId") Long agenceId
        );

        @Query("""
            select l
            from CollecteMembreLigne l
            join fetch l.collecte c
            left join fetch c.site s
            left join fetch s.agence sa
            left join fetch l.membre m
            where l.typeLigne = :typeLigne
                and c.statut = com.mini.credit.enums.RecetteStatut.VALIDEE
                and c.dateCollecte between :dateDebut and :dateFin
                and (:agenceId is null or c.antenneId = :agenceId or s.agence.id = :agenceId)
                and coalesce(l.montant, 0) > 0
            order by c.dateCollecte desc, l.id desc
            """)
        List<CollecteMembreLigne> findPositiveLinesByTypeAndPeriod(
                @Param("typeLigne") TypeLigneCollecte typeLigne,
                @Param("dateDebut") LocalDate dateDebut,
                @Param("dateFin") LocalDate dateFin,
                @Param("agenceId") Long agenceId
        );

        @Query("""
            select coalesce(sum(l.montant), 0)
            from CollecteMembreLigne l
            join l.collecte c
            left join c.site s
            where l.typeLigne = :typeLigne
                and c.statut = com.mini.credit.enums.RecetteStatut.VALIDEE
                and c.dateCollecte between :dateDebut and :dateFin
                and (:agenceId is null or c.antenneId = :agenceId or s.agence.id = :agenceId)
            """)
        BigDecimal sumByTypeAndPeriod(
                @Param("typeLigne") TypeLigneCollecte typeLigne,
                @Param("dateDebut") LocalDate dateDebut,
                @Param("dateFin") LocalDate dateFin,
                @Param("agenceId") Long agenceId
        );

        @Query("""
            select coalesce(sum(l.montant), 0)
            from CollecteMembreLigne l
            join l.collecte c
            where c.agentTerrain.id = :agentTerrainId
                and c.statut = com.mini.credit.enums.RecetteStatut.VALIDEE
                and c.dateCollecte between :dateDebut and :dateFin
                and l.typeLigne = :typeLigne
            """)
        BigDecimal sumValidatedAmountByAgentAndTypeAndPeriod(
                @Param("agentTerrainId") Long agentTerrainId,
                @Param("typeLigne") TypeLigneCollecte typeLigne,
                @Param("dateDebut") LocalDate dateDebut,
                @Param("dateFin") LocalDate dateFin
        );

        @Query("""
            select coalesce(sum(l.quantite), 0)
            from CollecteMembreLigne l
            join l.collecte c
            where c.agentTerrain.id = :agentTerrainId
                and c.statut = com.mini.credit.enums.RecetteStatut.VALIDEE
                and c.dateCollecte between :dateDebut and :dateFin
                and l.typeLigne = com.mini.credit.enums.TypeLigneCollecte.CARNET
            """)
        Long sumValidatedCarnetsByAgentAndPeriod(
                @Param("agentTerrainId") Long agentTerrainId,
                @Param("dateDebut") LocalDate dateDebut,
                @Param("dateFin") LocalDate dateFin
        );
}
