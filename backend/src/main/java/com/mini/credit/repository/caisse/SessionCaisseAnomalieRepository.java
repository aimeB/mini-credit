package com.mini.credit.repository.caisse;

import com.mini.credit.entity.caisse.SessionCaisseAnomalie;
import com.mini.credit.enums.StatutDossierAnomalieSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SessionCaisseAnomalieRepository extends JpaRepository<SessionCaisseAnomalie, Long> {

    List<SessionCaisseAnomalie> findBySessionIdOrderByDateDemandeDesc(Long sessionId);

    List<SessionCaisseAnomalie> findAllByOrderByDateDemandeDesc();

    Optional<SessionCaisseAnomalie> findFirstBySessionIdAndStatutDossierOrderByDateDemandeDesc(
            Long sessionId,
            StatutDossierAnomalieSession statutDossier
    );

    Optional<SessionCaisseAnomalie> findFirstBySessionIdOrderByDateDemandeDesc(Long sessionId);
}
