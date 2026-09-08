package com.mini.credit.repository.credit;

import com.mini.credit.entity.credit.GarantieMaterielle;
import com.mini.credit.enums.StatutGarantieMaterielle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface GarantieMaterielleRepository extends JpaRepository<GarantieMaterielle, Long> {
    List<GarantieMaterielle> findByGarantieCreditIdOrderByDateCreationDesc(Long garantieCreditId);
    List<GarantieMaterielle> findByGarantieCreditDemandeCreditIdOrderByDateCreationDesc(Long demandeCreditId);
    long countByGarantieCreditDemandeCreditIdAndStatut(Long demandeCreditId, StatutGarantieMaterielle statut);

    @Query("select coalesce(sum(g.valeurEstimee), 0) from GarantieMaterielle g where g.garantieCredit.demandeCredit.id = :demandeCreditId and g.statut = :statut")
    BigDecimal sumValeurByDemandeCreditIdAndStatut(@Param("demandeCreditId") Long demandeCreditId,
                                                   @Param("statut") StatutGarantieMaterielle statut);
}