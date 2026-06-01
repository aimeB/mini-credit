package com.mini.credit.repository.caisse;

import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.enums.StatutSessionCaisse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SessionCaisseRepository extends JpaRepository<SessionCaisse, Long> {

    List<SessionCaisse> findAllByOrderByDateOuvertureDesc();

    Optional<SessionCaisse> findFirstByStatutOrderByDateOuvertureDesc(StatutSessionCaisse statut);

    boolean existsByCaisseIdAndStatut(Long caisseId, StatutSessionCaisse statut);
    boolean existsByUtilisateurIdAndStatut(Long utilisateurId, StatutSessionCaisse statut);
}