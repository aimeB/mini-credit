package com.mini.credit.repository.caisse;

import com.mini.credit.entity.caisse.Caisse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CaisseRepository extends JpaRepository<Caisse, Long> {
    Optional<Caisse> findByCodeCaisse(String codeCaisse);
}
