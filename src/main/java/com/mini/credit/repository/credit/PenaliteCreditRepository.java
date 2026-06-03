package com.mini.credit.repository.credit;

import com.mini.credit.entity.credit.PenaliteCredit;
import com.mini.credit.enums.StatutPenalite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * PHASE 10: Repository pour PenaliteCredit
 */
public interface PenaliteCreditRepository extends JpaRepository<PenaliteCredit, Long> {

    /**
     * Récupère les pénalités d'un crédit
     */
    List<PenaliteCredit> findByCreditIdOrderByDateCreationPenaliteDesc(Long creditId);

    /**
     * Récupère les pénalités d'une demande de crédit
     */
    List<PenaliteCredit> findByDemandeCreditIdOrderByDateEchéanceAsc(Long demandeCreditId);

    /**
     * Récupère les pénalités par statut
     */
    List<PenaliteCredit> findByStatutOrderByDateCreationPenaliteDesc(StatutPenalite statut);

    /**
     * Pénalités créées à une date donnée
     */
    List<PenaliteCredit> findByDateCreationPenaliteAndStatutOrderByMontantPenaliteDesc(
            java.time.LocalDateTime date, StatutPenalite statut);

    /**
     * Pénalités en attente (non payées)
     */
    List<PenaliteCredit> findByStatutInOrderByDateEchéanceAsc(List<StatutPenalite> statuts);

    /**
     * Somme des pénalités créées pour un crédit
     */
    @Query("SELECT COALESCE(SUM(p.montantPenalite), 0) FROM PenaliteCredit p WHERE p.credit.id = :creditId")
    BigDecimal sumMontantPenaliteByCredit(@Param("creditId") Long creditId);

    /**
     * Somme des pénalités CREEES (non acquittées) pour un crédit
     */
    @Query("SELECT COALESCE(SUM(p.montantPenalite), 0) FROM PenaliteCredit p WHERE p.credit.id = :creditId AND p.statut = 'CREEE'")
    BigDecimal sumMontantPenaliteCreeeByCredit(@Param("creditId") Long creditId);

    /**
     * Pénalités d'une demande de crédit par statut
     */
    List<PenaliteCredit> findByDemandeCreditIdAndStatutOrderByDateEchéanceAsc(
            Long demandeCreditId, StatutPenalite statut);

    /**
     * Récupère les pénalités antérieures à une date (pour recherche/rapports)
     */
    List<PenaliteCredit> findByDateEchéanceBeforeAndStatutOrderByDateEchéanceAsc(
            LocalDate date, StatutPenalite statut);
}
