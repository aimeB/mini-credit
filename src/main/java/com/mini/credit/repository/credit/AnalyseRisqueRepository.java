package com.mini.credit.repository.credit;

import com.mini.credit.entity.credit.AnalyseRisque;
import com.mini.credit.entity.credit.DemandeCredit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalyseRisqueRepository extends JpaRepository<AnalyseRisque, Long> {

    /**
     * PHASE 4: Récupère l'analyse risque pour une demande crédit
     */
    AnalyseRisque findByDemandeCredit(DemandeCredit demandeCredit);

    /**
     * Récupère l'analyse risque par ID de demande crédit
     */
    AnalyseRisque findByDemandeCreditId(Long demandeCreditId);
}