package com.mini.credit.repository.epargne;

import com.mini.credit.entity.epargne.CompteEpargne;
import com.mini.credit.enums.StatutCompte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CompteEpargneRepository extends JpaRepository<CompteEpargne, Long> {
    Optional<CompteEpargne> findByNumeroCompte(String numeroCompte);
    List<CompteEpargne> findByMembreId(Long membreId);
    List<CompteEpargne> findByMembreSiteId(Long siteId);
    Optional<CompteEpargne> findFirstByMembreIdAndStatut(Long membreId, StatutCompte statut);
    boolean existsByMembreIdAndStatut(Long membreId, StatutCompte statut);
    
    // PHASE 9: Intérêts Épargne - comptes actifs
    List<CompteEpargne> findByStatut(StatutCompte statut);

        @Query("""
                select coalesce(sum(c.soldeDisponible), 0)
                from CompteEpargne c
                left join c.membre m
                left join m.site ms
                where c.statut = com.mini.credit.enums.StatutCompte.ACTIF
                    and (:agenceId is null or ms.agence.id = :agenceId)
                """)
        BigDecimal sumSoldeDisponibleActifByAgence(@Param("agenceId") Long agenceId);

        @Query("""
                select coalesce(sum(c.soldeBloque), 0)
                from CompteEpargne c
                left join c.membre m
                left join m.site ms
                where c.statut = com.mini.credit.enums.StatutCompte.ACTIF
                    and (:agenceId is null or ms.agence.id = :agenceId)
                """)
        BigDecimal sumSoldeBloqueActifByAgence(@Param("agenceId") Long agenceId);
}