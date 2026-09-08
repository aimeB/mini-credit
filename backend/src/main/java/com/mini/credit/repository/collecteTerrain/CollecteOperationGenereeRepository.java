package com.mini.credit.repository.collecteTerrain;

import com.mini.credit.entity.referentiel.CollecteOperationGeneree;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CollecteOperationGenereeRepository extends JpaRepository<CollecteOperationGeneree, Long> {

    boolean existsByCollecteIdAndLigneCollecteIdAndTypeOperation(Long collecteId, Long ligneCollecteId, String typeOperation);

    boolean existsByCollecteIdAndTypeOperation(Long collecteId, String typeOperation);

    List<CollecteOperationGeneree> findByCollecteId(Long collecteId);
}
