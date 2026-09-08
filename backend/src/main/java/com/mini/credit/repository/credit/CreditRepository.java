package com.mini.credit.repository.credit;

import com.mini.credit.entity.credit.Credit;
import com.mini.credit.enums.StatutCredit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CreditRepository extends JpaRepository<Credit, Long> {
    Optional<Credit> findByNumeroCredit(String numeroCredit);
    Optional<Credit> findByDemandeCreditId(Long demandeCreditId);
    List<Credit> findByMembreId(Long membreId);
    List<Credit> findByStatut(StatutCredit statut);
    List<Credit> findByStatutInAndClotureAtIsNullOrderByDateDecaissementDescDateApprobationDescIdDesc(List<StatutCredit> statuts);

    @Query("""
        select distinct c
        from Credit c
        where c.statut = :statutRembourse
           or (
                c.statut in :statutsSoldables
                and exists (
                    select e1.id
                    from EcheanceCredit e1
                    where e1.credit = c
                )
                and not exists (
                    select e2.id
                    from EcheanceCredit e2
                    where e2.credit = c
                      and coalesce(e2.resteAPayer, 0) > 0
                )
           )
        order by c.clotureAt desc, c.dateDecaissement desc, c.dateApprobation desc, c.id desc
        """)
    List<Credit> findCreditsRemboursesOuSoldes(@Param("statutRembourse") StatutCredit statutRembourse,
                                               @Param("statutsSoldables") List<StatutCredit> statutsSoldables);

    boolean existsByMembreIdAndStatutIn(Long membreId, List<StatutCredit> statuts);
    long countByStatutIn(List<StatutCredit> statuts);

    long countByStatut(StatutCredit statut);

    @Query("select coalesce(sum(c.montantOctroye), 0) from Credit c")
    BigDecimal sumMontantOctroye();

    @Query("select coalesce(sum(c.encoursPrincipal), 0) from Credit c")
    BigDecimal sumEncoursPrincipal();

    @Query("select coalesce(sum(c.interetTotal), 0) from Credit c")
    BigDecimal sumInteretTotal();

    @Query("select coalesce(sum(c.penaliteTotal), 0) from Credit c")
    BigDecimal sumPenaliteTotale();

        @Query("""
                select coalesce(sum(c.montantOctroye), 0)
                from Credit c
                left join c.site cs
                left join c.membre m
                left join m.site ms
                where c.dateDecaissement between :dateDebut and :dateFin
                    and c.statut <> :statutAnnule
                    and (:agenceId is null or cs.agence.id = :agenceId or ms.agence.id = :agenceId)
                """)
        BigDecimal sumCapitalDecaisseByPeriodAndAgence(
                        @Param("dateDebut") LocalDate dateDebut,
                        @Param("dateFin") LocalDate dateFin,
                        @Param("agenceId") Long agenceId,
                        @Param("statutAnnule") StatutCredit statutAnnule
        );

        @Query("""
                select count(c)
                from Credit c
                left join c.site cs
                left join c.membre m
                left join m.site ms
                where c.statut in :statuts
                    and (:agenceId is null or cs.agence.id = :agenceId or ms.agence.id = :agenceId)
                """)
        long countByStatutInAndAgence(
                        @Param("statuts") List<StatutCredit> statuts,
                        @Param("agenceId") Long agenceId
        );

        @Query("""
                select count(distinct c)
                from Credit c
                left join c.site cs
                left join c.membre m
                left join m.site ms
                where c.statut in :statutsRembourses
                    and (:agenceId is null or cs.agence.id = :agenceId or ms.agence.id = :agenceId)
                    and (
                        c.clotureAt between :dateDebut and :dateFin
                        or exists (
                            select r.id
                            from RemboursementCredit r
                            where r.credit = c
                              and r.datePaiement between :dateDebut and :dateFin
                        )
                    )
                """)
        long countRemboursesByPeriodAndAgence(
                        @Param("dateDebut") LocalDateTime dateDebut,
                        @Param("dateFin") LocalDateTime dateFin,
                        @Param("agenceId") Long agenceId,
                        @Param("statutsRembourses") List<StatutCredit> statutsRembourses
        );

        @Query("""
                select c
                from Credit c
                left join fetch c.site cs
                left join fetch cs.agence csa
                left join fetch c.membre m
                left join fetch m.site ms
                left join fetch ms.agence msa
                where c.dateDecaissement between :dateDebut and :dateFin
                    and c.statut <> :statutAnnule
                    and (:agenceId is null or cs.agence.id = :agenceId or ms.agence.id = :agenceId)
                    and (c.montantOctroye is null or c.montantOctroye <= 0)
                order by c.dateDecaissement desc, c.id desc
                """)
        List<Credit> findDecaissesWithoutMontantAccorde(
                        @Param("dateDebut") LocalDate dateDebut,
                        @Param("dateFin") LocalDate dateFin,
                        @Param("agenceId") Long agenceId,
                        @Param("statutAnnule") StatutCredit statutAnnule
        );

        @Query("""
                select c
                from Credit c
                left join fetch c.site cs
                left join fetch cs.agence csa
                left join fetch c.membre m
                left join fetch m.site ms
                left join fetch ms.agence msa
                where c.statut in :statutsActifs
                    and (:agenceId is null or cs.agence.id = :agenceId or ms.agence.id = :agenceId)
                    and (c.encoursPrincipal is null or c.encoursPrincipal < 0 or c.encoursPrincipal > c.principalTotal)
                order by c.dateDecaissement desc, c.id desc
                """)
        List<Credit> findActiveCreditsWithInconsistentEncours(
                        @Param("statutsActifs") List<StatutCredit> statutsActifs,
                        @Param("agenceId") Long agenceId
        );

        @Query("""
            select coalesce(sum(c.montantOctroye), 0)
            from Credit c
            left join c.site cs
            left join c.membre m
            left join m.site ms
            where c.statut = com.mini.credit.enums.StatutCredit.APPROUVE
                and c.dateDecaissement is null
                and (:agenceId is null or cs.agence.id = :agenceId or ms.agence.id = :agenceId)
            """)
        BigDecimal sumCreditsApprouvesNonDecaissesByAgence(@Param("agenceId") Long agenceId);
}
