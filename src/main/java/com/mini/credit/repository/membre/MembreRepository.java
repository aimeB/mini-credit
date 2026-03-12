package com.mini.credit.repository.membre;


import com.mini.credit.entity.membre.Membre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MembreRepository extends JpaRepository<Membre, Long> {
    Optional<Membre> findByCodeMembre(String codeMembre);
    Optional<Membre> findByTelephonePrincipal(String telephonePrincipal);
    boolean existsByCodeMembre(String codeMembre);
}
