package com.mini.credit.repository.utilisateur;

import com.mini.credit.entity.referentiel.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("referentielUtilisateurRepository")
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
}