package com.mini.credit.repository.referentiel;

import com.mini.credit.entity.referentiel.TransportSiteParametre;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransportSiteParametreRepository extends JpaRepository<TransportSiteParametre, Long> {

    @EntityGraph(attributePaths = {"site", "site.agence"})
    List<TransportSiteParametre> findAllByOrderBySiteNomSiteAscDateDebutValiditeDesc();

    @Query("""
            select p
            from TransportSiteParametre p
            join fetch p.site s
            left join fetch s.agence a
            where p.actif = true
              and p.dateDebutValidite <= :date
              and (p.dateFinValidite is null or p.dateFinValidite >= :date)
            order by s.nomSite asc
            """)
    List<TransportSiteParametre> findActifsAtDate(@Param("date") LocalDate date);

    @Query("""
            select p
            from TransportSiteParametre p
            join fetch p.site s
            left join fetch s.agence a
            where s.id = :siteId
              and p.actif = true
              and p.dateDebutValidite <= :date
              and (p.dateFinValidite is null or p.dateFinValidite >= :date)
            order by p.dateDebutValidite desc, p.id desc
            """)
    Optional<TransportSiteParametre> findActiveBySiteAtDate(@Param("siteId") Long siteId, @Param("date") LocalDate date);
}
