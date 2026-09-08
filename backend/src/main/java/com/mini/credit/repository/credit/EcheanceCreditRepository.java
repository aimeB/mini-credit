package com.mini.credit.repository.credit;

import com.mini.credit.entity.credit.EcheanceCredit;
import com.mini.credit.repository.projection.RetardEcheanceProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface EcheanceCreditRepository extends JpaRepository<EcheanceCredit, Long> {



        List<EcheanceCredit> findByCreditIdOrderByNumeroEcheanceAsc(Long creditId);

        @Query("""
                select count(e)
                from EcheanceCredit e
                where e.dateEcheance < :dateReference
                  and e.resteAPayer > 0
                """)
        long countEcheancesEnRetard(LocalDate dateReference);

        @Query("""
                select count(distinct e.credit.id)
                from EcheanceCredit e
                where e.dateEcheance < :dateReference
                  and e.resteAPayer > 0
                """)
        long countCreditsEnRetard(LocalDate dateReference);

        @Query("""
                select coalesce(sum(e.resteAPayer), 0)
                from EcheanceCredit e
                where e.dateEcheance < :dateReference
                  and e.resteAPayer > 0
                """)
        BigDecimal sumMontantEnRetard(LocalDate dateReference);

        @Query("""
                select coalesce(sum(e.penaliteCumulee), 0)
                from EcheanceCredit e
                where e.dateEcheance < :dateReference
                  and e.resteAPayer > 0
                """)
        BigDecimal sumPenalitesEnRetard(LocalDate dateReference);

        @Query("""
                select
                    c.id as creditId,
                    c.numeroCredit as numeroCredit,
                    m.id as membreId,
                    m.nomComplet as membreNomComplet,
                    e.id as echeanceId,
                    e.numeroEcheance as numeroEcheance,
                    e.dateEcheance as dateEcheance,
                    e.resteAPayer as resteAPayer,
                    e.penaliteCumulee as penaliteCumulee
                from EcheanceCredit e
                join e.credit c
                join c.membre m
                where e.dateEcheance < :dateReference
                  and e.resteAPayer > 0
                order by e.dateEcheance asc
                """)
        List<RetardEcheanceProjection> findEcheancesEnRetard(LocalDate dateReference);



    }