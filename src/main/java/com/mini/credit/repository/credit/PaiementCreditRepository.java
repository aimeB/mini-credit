package com.mini.credit.repository.credit;

import com.mini.credit.entity.credit.PaiementCredit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaiementCreditRepository extends JpaRepository<PaiementCredit, Long> {
    Optional<PaiementCredit> findByReferenceInterne(String referenceInterne);
    Optional<PaiementCredit> findByReferenceExterne(String referenceExterne);
    List<PaiementCredit> findByContratCreditId(Long contratId);
}
