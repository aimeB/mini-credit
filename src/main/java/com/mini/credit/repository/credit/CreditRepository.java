package com.mini.credit.repository.credit;

import com.mini.credit.entity.credit.Credit;
import com.mini.credit.enums.StatutCredit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CreditRepository extends JpaRepository<Credit, Long> {
    Optional<Credit> findByNumeroCredit(String numeroCredit);
    List<Credit> findByMembreId(Long membreId);
    List<Credit> findByStatut(StatutCredit statut);

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
}
