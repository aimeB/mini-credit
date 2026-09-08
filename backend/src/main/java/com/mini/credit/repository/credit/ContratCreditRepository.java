package com.mini.credit.repository.credit;


import com.mini.credit.entity.credit.ContratCredit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContratCreditRepository extends JpaRepository<ContratCredit, Long> {
    Optional<ContratCredit> findByCreditId(Long creditId);
    Optional<ContratCredit> findByNumeroContrat(String numeroContrat);
}