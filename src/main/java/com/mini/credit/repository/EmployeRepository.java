package com.mini.credit.repository;

import com.mini.credit.entity.employe.Employe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeRepository extends JpaRepository<Employe, Long> {
    Optional<Employe> findByMatricule(String matricule);
    Optional<Employe> findByUtilisateurId(Long utilisateurId);
    List<Employe> findByActifTrue();
    boolean existsByMatricule(String matricule);
}
