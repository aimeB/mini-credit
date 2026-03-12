package com.mini.credit.repository.credit;

import com.mini.credit.entity.credit.DemandeCredit;
import com.mini.credit.enums.StatutDemandeCredit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DemandeCreditRepository extends JpaRepository<DemandeCredit, Long> {
    List<DemandeCredit> findByMembreId(Long membreId);
    List<DemandeCredit> findByStatut(StatutDemandeCredit statut);
}
