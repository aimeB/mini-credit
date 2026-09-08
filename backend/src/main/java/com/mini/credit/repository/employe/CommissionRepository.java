package com.mini.credit.repository.employe;

import com.mini.credit.entity.employe.Commission;
import com.mini.credit.enums.StatutCommission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * PHASE 8: Repository pour Commission.
 */
@Repository
public interface CommissionRepository extends JpaRepository<Commission, Long> {

    /**
     * Récupère les commissions d'un agent
     */
    List<Commission> findByAgentIdOrderByDatePeriodeDebutDesc(Long agentId);

    /**
     * Récupère les commissions pour une période donnée
     */
    List<Commission> findByDatePeriodeDebutAndDatePeriodeFinOrderByAgentIdAsc(
            LocalDate datePeriodeDebut, LocalDate datePeriodeFin);

    /**
     * Récupère les commissions par statut
     */
    List<Commission> findByStatutOrderByDateCreationAsc(StatutCommission statut);

    /**
     * Récupère les commissions créées pour un agent et une période
     */
    Optional<Commission> findByAgentIdAndDatePeriodeDebutAndDatePeriodeFin(
            Long agentId, LocalDate dateDebut, LocalDate dateFin);

    /**
     * Total commissions payées par agent (somme)
     */
    @Query("SELECT SUM(c.montantCommission) FROM Commission c WHERE c.agent.id = :agentId AND c.statut = 'PAYEE'")
    BigDecimal sumMontantPayeByAgentId(@Param("agentId") Long agentId);

    /**
     * Total commissions validées non payées
     */
    @Query("SELECT SUM(c.montantCommission) FROM Commission c WHERE c.statut = 'VALIDEE'")
    BigDecimal sumMontantValideeNonPayee();

    /**
     * Commissions en attente de paiement pour un agent
     */
    List<Commission> findByAgentIdAndStatutOrderByDatePeriodeDebutDesc(Long agentId, StatutCommission statut);
}
