package com.mini.credit.repository.caisse;

import com.mini.credit.entity.caisse.DepenseCaisse;
import com.mini.credit.enums.DepenseCaisseStatus;
import com.mini.credit.enums.TypeChargeFixe;
import com.mini.credit.enums.TypePaiementPersonnel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DepenseCaisseRepository extends JpaRepository<DepenseCaisse, Long> {

    List<DepenseCaisse> findAllByOrderByDateDemandeDesc();

    List<DepenseCaisse> findByStatutOrderByDateDemandeDesc(DepenseCaisseStatus statut);

    List<DepenseCaisse> findByCaisseIdOrderByDateDemandeDesc(Long caisseId);

    List<DepenseCaisse> findBySiteIdOrderByDateDemandeDesc(Long siteId);

    List<DepenseCaisse> findBySessionCaisseIdOrderByDateDemandeDesc(Long sessionCaisseId);

    List<DepenseCaisse> findByDateDemandeBetweenOrderByDateDemandeDesc(LocalDateTime dateDebut, LocalDateTime dateFin);

        @Query("""
            select coalesce(sum(d.montant), 0)
            from DepenseCaisse d
            join d.caisse c
            left join c.site cs
            left join d.site st
            where d.statut = com.mini.credit.enums.DepenseCaisseStatus.PAYEE
              and d.datePaiement between :dateDebut and :dateFin
              and (:agenceId is null or c.agence.id = :agenceId or st.agence.id = :agenceId or cs.agence.id = :agenceId)
            """)
        BigDecimal sumDepensesPayeesByPeriodAndAgence(
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin,
            @Param("agenceId") Long agenceId
        );

        @Query("""
                        select case when count(d) > 0 then true else false end
                        from DepenseCaisse d
                        where d.statut = :statut
                            and (
                                        (d.sessionCaisse is not null and d.sessionCaisse.id = :sessionId)
                                        or
                                        (d.sessionCaisse is null and d.caisse.id = :caisseId and d.dateDemande >= :startOfDay and d.dateDemande < :endOfDay)
                                    )
                        """)
        boolean existsPendingForSessionClosure(@Param("sessionId") Long sessionId,
                                                                                     @Param("caisseId") Long caisseId,
                                                                                     @Param("startOfDay") LocalDateTime startOfDay,
                                                                                     @Param("endOfDay") LocalDateTime endOfDay,
                                                                                     @Param("statut") DepenseCaisseStatus statut);

    boolean existsByOperationCaisseId(Long operationCaisseId);

        @Query("""
                        select case when count(d) > 0 then true else false end
                        from DepenseCaisse d
                        where d.employe.id = :employeId
                            and d.periodePaie = :periodePaie
                            and d.typePaiementPersonnel = :typePaiementPersonnel
                            and d.statut in :statuts
                        """)
        boolean existsPersonnelPaymentForPeriod(@Param("employeId") Long employeId,
                                                                                        @Param("periodePaie") String periodePaie,
                                                                                        @Param("typePaiementPersonnel") TypePaiementPersonnel typePaiementPersonnel,
                                                                                        @Param("statuts") List<DepenseCaisseStatus> statuts);

        @Query("""
                        select d
                        from DepenseCaisse d
                        left join fetch d.employe e
                        left join fetch d.operationCaisse oc
                        where d.employe.id = :employeId
                            and d.periodePaie = :periodePaie
                            and d.statut in :statuts
                            and (:excludeId is null or d.id <> :excludeId)
                        """)
        List<DepenseCaisse> findPersonnelPaymentsForPeriod(@Param("employeId") Long employeId,
                                                                                        @Param("periodePaie") String periodePaie,
                                                                                        @Param("statuts") List<DepenseCaisseStatus> statuts,
                                                                                        @Param("excludeId") Long excludeId);

    @Query("""
            select d
            from DepenseCaisse d
            join fetch d.caisse c
            left join fetch c.agence ca
            left join fetch c.site st
            left join fetch st.agence sta
            left join fetch d.sessionCaisse s
            left join fetch d.demandePar dp
            left join fetch d.validePar vp
            left join fetch d.payePar pp
            left join fetch d.operationCaisse oc
            left join fetch d.employe e
            where d.id = :id
            """)
    Optional<DepenseCaisse> findByIdWithContext(@Param("id") Long id);

            @Query("""
                select d
                from DepenseCaisse d
                join fetch d.caisse c
                left join fetch c.agence ca
                left join fetch c.site cs
                left join fetch d.site st
                left join fetch st.agence sta
                left join fetch d.demandePar dp
                left join fetch d.validePar vp
                left join fetch d.payePar pp
                left join fetch d.operationCaisse oc
                left join fetch d.employe e
                where d.statut = com.mini.credit.enums.DepenseCaisseStatus.PAYEE
                  and d.datePaiement between :dateDebut and :dateFin
                  and (:agenceId is null or c.agence.id = :agenceId or st.agence.id = :agenceId or cs.agence.id = :agenceId)
                order by d.datePaiement desc, d.id desc
                """)
            List<DepenseCaisse> findDepensesPayeesWithContext(
                @Param("dateDebut") LocalDateTime dateDebut,
                @Param("dateFin") LocalDateTime dateFin,
                @Param("agenceId") Long agenceId
            );

                        @Query("""
                                select d
                                from DepenseCaisse d
                                join fetch d.caisse c
                                left join fetch c.agence ca
                                left join fetch c.site cs
                                left join fetch d.site st
                                left join fetch st.agence sta
                                left join fetch d.employe e
                                left join fetch d.operationCaisse oc
                                where d.statut = com.mini.credit.enums.DepenseCaisseStatus.PAYEE
                                    and d.categorie = com.mini.credit.enums.DepenseCaisseCategorie.SALAIRE
                                    and (
                                        d.typePaiementPersonnel is null
                                        or d.typePaiementPersonnel in (
                                            com.mini.credit.enums.TypePaiementPersonnel.SALAIRE,
                                            com.mini.credit.enums.TypePaiementPersonnel.SALAIRE_COMPLET,
                                            com.mini.credit.enums.TypePaiementPersonnel.SALAIRE_PARTIEL,
                                            com.mini.credit.enums.TypePaiementPersonnel.AVANCE,
                                            com.mini.credit.enums.TypePaiementPersonnel.AVANCE_SALAIRE,
                                            com.mini.credit.enums.TypePaiementPersonnel.RETENUE_SALAIRE,
                                            com.mini.credit.enums.TypePaiementPersonnel.PRIME,
                                            com.mini.credit.enums.TypePaiementPersonnel.COMMISSION,
                                            com.mini.credit.enums.TypePaiementPersonnel.REGULARISATION,
                                            com.mini.credit.enums.TypePaiementPersonnel.AUTRE
                                        )
                                    )
                                    and (
                                        d.periodePaie = :periodePaie
                                        or (d.periodePaie is null and d.datePaiement between :dateDebut and :dateFin)
                                    )
                                    and (:agenceId is null or c.agence.id = :agenceId or st.agence.id = :agenceId or cs.agence.id = :agenceId or e.agence.id = :agenceId)
                                order by d.datePaiement desc, d.id desc
                                """)
                        List<DepenseCaisse> findSalaryPaymentsForPayrollPeriod(
                                @Param("periodePaie") String periodePaie,
                                @Param("dateDebut") LocalDateTime dateDebut,
                                @Param("dateFin") LocalDateTime dateFin,
                                @Param("agenceId") Long agenceId
                        );

                            @Query("""
                                select d
                                from DepenseCaisse d
                                join fetch d.caisse c
                                left join fetch c.agence ca
                                left join fetch c.site cs
                                left join fetch d.site st
                                left join fetch st.agence sta
                                left join fetch d.employe e
                                left join fetch e.site es
                                left join fetch d.siteCharge sc
                                left join fetch d.operationCaisse oc
                                where d.statut = com.mini.credit.enums.DepenseCaisseStatus.PAYEE
                                    and d.categorie = com.mini.credit.enums.DepenseCaisseCategorie.TRANSPORT
                                    and (
                                    d.periodeCharge = :periodeCharge
                                    or (d.periodeCharge is null and d.datePaiement between :dateDebut and :dateFin)
                                    )
                                    and (:agenceId is null or c.agence.id = :agenceId or st.agence.id = :agenceId or cs.agence.id = :agenceId or e.agence.id = :agenceId or sc.agence.id = :agenceId)
                                order by d.datePaiement desc, d.id desc
                                """)
                            List<DepenseCaisse> findTransportPaymentsForPeriod(
                                @Param("periodeCharge") String periodeCharge,
                                @Param("dateDebut") LocalDateTime dateDebut,
                                @Param("dateFin") LocalDateTime dateFin,
                                @Param("agenceId") Long agenceId
                            );

                        @Query("""
                                select coalesce(sum(d.montant), 0)
                                from DepenseCaisse d
                                join d.caisse c
                                left join c.site cs
                                left join d.site st
                                where d.statut = com.mini.credit.enums.DepenseCaisseStatus.VALIDEE
                                    and d.operationCaisse is null
                                    and (:agenceId is null or c.agence.id = :agenceId or st.agence.id = :agenceId or cs.agence.id = :agenceId)
                                """)
                        BigDecimal sumDepensesValideesNonPayeesByAgence(@Param("agenceId") Long agenceId);
}