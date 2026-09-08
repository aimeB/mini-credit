package com.mini.credit.repository.credit;

import com.mini.credit.entity.credit.GarantieCredit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GarantieCreditRepository extends JpaRepository<GarantieCredit, Long> {
    Optional<GarantieCredit> findByDemandeCreditId(Long demandeCreditId);
    List<GarantieCredit> findByDemandeCreditIdIn(List<Long> demandeCreditIds);
    List<GarantieCredit> findByMembreId(Long membreId);
}