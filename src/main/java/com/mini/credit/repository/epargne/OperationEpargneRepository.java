package com.mini.credit.repository.epargne;

import com.mini.credit.entity.epargne.OperationEpargne;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OperationEpargneRepository extends JpaRepository<OperationEpargne, Long> {
    List<OperationEpargne> findByCompteEpargneIdOrderByDateOperationDesc(Long compteEpargneId);
    List<OperationEpargne> findByMembreIdOrderByDateOperationDesc(Long membreId);
    List<OperationEpargne> findAllByOrderByDateOperationDesc();
}