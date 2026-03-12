package com.mini.credit.repository.epargne;

import com.mini.credit.entity.epargne.CompteEpargne;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompteEpargneRepository extends JpaRepository<CompteEpargne, Long> {
    Optional<CompteEpargne> findByNumeroCompte(String numeroCompte);
    List<CompteEpargne> findByMembreId(Long membreId);
}