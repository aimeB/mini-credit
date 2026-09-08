package com.mini.credit.repository.credit;

import com.mini.credit.entity.credit.DemandeCredit;
import com.mini.credit.enums.StatutDemandeCredit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.time.LocalDate;

public interface DemandeCreditRepository extends JpaRepository<DemandeCredit, Long> {
    List<DemandeCredit> findByMembreId(Long membreId);
    List<DemandeCredit> findByStatut(StatutDemandeCredit statut);

    @Query("""
        select d
        from DemandeCredit d
        left join fetch d.site s
        left join fetch s.agence sa
        left join fetch d.membre m
        left join fetch m.site ms
        left join fetch ms.agence msa
        left join fetch d.createdBy cb
        where d.dateDemande between :dateDebut and :dateFin
          and (:agenceId is null or s.agence.id = :agenceId or ms.agence.id = :agenceId)
        order by d.dateDemande desc, d.id desc
        """)
    List<DemandeCredit> findForRevenueControls(
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin,
            @Param("agenceId") Long agenceId
    );

    @Query("""
        select d
        from DemandeCredit d
        left join fetch d.site s
        left join fetch s.agence a
        left join fetch d.membre m
        where d.fraisDemande > d.fraisDemandePayes
          and d.statut not in (com.mini.credit.enums.StatutDemandeCredit.REJETEE, com.mini.credit.enums.StatutDemandeCredit.ANNULEE)
        order by d.dateDemande desc, d.id desc
        """)
    List<DemandeCredit> findFraisCreditAEncaisser();
}
