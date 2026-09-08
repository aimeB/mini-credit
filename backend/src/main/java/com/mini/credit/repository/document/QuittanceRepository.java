package com.mini.credit.repository.document;

import com.mini.credit.entity.document.Quittance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuittanceRepository extends JpaRepository<Quittance, Long> {
    List<Quittance> findByMembreIdOrderByDateEmissionDesc(Long membreId);
    List<Quittance> findByReferenceOperationOrderByDateEmissionDesc(String referenceOperation);
}