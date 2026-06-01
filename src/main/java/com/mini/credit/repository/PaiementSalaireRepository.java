package com.mini.credit.repository;

import com.mini.credit.entity.employe.PaiementSalaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PaiementSalaireRepository extends JpaRepository<PaiementSalaire, Long> {
    List<PaiementSalaire> findByEmployeId(Long employeId);
    List<PaiementSalaire> findByDatePaiementBetween(LocalDate startDate, LocalDate endDate);
    List<PaiementSalaire> findByEmployeIdAndDatePaiementBetween(Long employeId, LocalDate startDate, LocalDate endDate);
}
