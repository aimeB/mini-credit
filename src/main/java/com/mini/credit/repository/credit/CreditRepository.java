package com.mini.credit.repository.credit;

import com.mini.credit.entity.credit.Credit;
import com.mini.credit.enums.StatutCredit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CreditRepository extends JpaRepository<Credit, Long> {
    Optional<Credit> findByNumeroCredit(String numeroCredit);
    List<Credit> findByMembreId(Long membreId);
    List<Credit> findByStatut(StatutCredit statut);

    boolean existsByMembreIdAndStatutIn(Long membreId, List<StatutCredit> statuts);
}
