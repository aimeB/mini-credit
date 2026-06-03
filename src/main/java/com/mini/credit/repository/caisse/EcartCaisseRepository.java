package com.mini.credit.repository.caisse;

import com.mini.credit.entity.caisse.EcartCaisse;
import com.mini.credit.enums.StatutEcartCaisse;
import com.mini.credit.enums.TypeEcartCaisse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * PHASE 7: Repository pour EcartCaisse.
 */
@Repository
public interface EcartCaisseRepository extends JpaRepository<EcartCaisse, Long> {

    /**
     * Écarts détectés pour un jour
     */
    List<EcartCaisse> findByDateJourOrderByMontantEcartDesc(LocalDate dateJour);

    /**
     * Écarts par statut
     */
    List<EcartCaisse> findByStatutOrderByDateJourDesc(StatutEcartCaisse statut);

    /**
     * Écarts en investigation (CONTROLEUR)
     */
    List<EcartCaisse> findByStatutInOrderByDateJourDesc(java.util.Collection<StatutEcartCaisse> statuts);

    /**
     * Écarts nécessitant validation R.C.I. (montant > seuil)
     */
    @Query("SELECT e FROM EcartCaisse e WHERE e.seuilDepassé = true AND e.statut IN (:statuts) ORDER BY e.dateJour DESC")
    List<EcartCaisse> findEcartsRequiringRCIValidation(@Param("statuts") java.util.Collection<StatutEcartCaisse> statuts);

    /**
     * Écarts par type
     */
    List<EcartCaisse> findByTypeEcartOrderByDateJourDesc(TypeEcartCaisse typeEcart);

    /**
     * Écarts par session caisse
     */
    List<EcartCaisse> findBySessionCaisseIdOrderByDateCreationAsc(Long sessionCaisseId);

    /**
     * Écarts par recette journalière
     */
    List<EcartCaisse> findByRecetteIdOrderByDateCreationAsc(Long recetteId);

    /**
     * Total écarts non résolus pour un jour (somme montants)
     */
    @Query("SELECT SUM(e.montantEcart) FROM EcartCaisse e WHERE e.dateJour = :dateJour AND e.statut != 'RESOLU'")
    BigDecimal sumEcartsNonResolusByDateJour(@Param("dateJour") LocalDate dateJour);
}
