package com.mini.credit.repository.caisse;

import com.mini.credit.entity.caisse.ReconciliationCaisse;
import com.mini.credit.enums.StatutReconciliation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * PHASE 11: Repository Réconciliation Caisse
 */
public interface ReconciliationCaisseRepository extends JpaRepository<ReconciliationCaisse, Long> {

    // Recherche par session caisse
    List<ReconciliationCaisse> findBySessionCaisseIdOrderByDateCreationReconciliationDesc(Long sessionCaisseId);

    // Recherche par statut
    List<ReconciliationCaisse> findByStatutOrderByDateCreationReconciliationDesc(StatutReconciliation statut);

    // Recherche par statut et date
    List<ReconciliationCaisse> findByStatutAndDateCreationReconciliationAfterOrderByDateCreationReconciliationDesc(
            StatutReconciliation statut, LocalDateTime dateCreation);

    // Recherche écarts non rapprochés
    List<ReconciliationCaisse> findByStatutInOrderByMontantEcartDescDateCreationReconciliationAsc(
            List<StatutReconciliation> statuts);

    // Somme des écarts en attente (CREEE)
    @Query("SELECT SUM(CASE WHEN rc.montantEcart < 0 THEN -rc.montantEcart ELSE rc.montantEcart END) " +
           "FROM ReconciliationCaisse rc WHERE rc.statut = ?1")
    BigDecimal sumMontantEcartByStatut(StatutReconciliation statut);

    // Nombre de réconciliations en attente
    @Query("SELECT COUNT(rc) FROM ReconciliationCaisse rc WHERE rc.statut = com.mini.credit.enums.StatutReconciliation.CREEE")
    Long countByStatutCreee();

    // Vérifier si réconciliation existe déjà pour une session
    boolean existsBySessionCaisseId(Long sessionCaisseId);

    // Chercher par session et statut
    List<ReconciliationCaisse> findBySessionCaisseIdAndStatutOrderByDateCreationReconciliationDesc(
            Long sessionCaisseId, StatutReconciliation statut);
}
