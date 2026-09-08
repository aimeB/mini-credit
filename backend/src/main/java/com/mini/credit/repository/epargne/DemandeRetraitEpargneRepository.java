package com.mini.credit.repository.epargne;

import com.mini.credit.entity.epargne.CompteEpargne;
import com.mini.credit.entity.epargne.DemandeRetraitEpargne;
import com.mini.credit.enums.StatutDemandeRetrait;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour les demandes de retrait épargne (PHASE 5).
 */
@Repository
public interface DemandeRetraitEpargneRepository extends JpaRepository<DemandeRetraitEpargne, Long> {

    /**
     * Récupère les demandes par compte épargne
     */
    List<DemandeRetraitEpargne> findByCompteEpargne(CompteEpargne compteEpargne);

    /**
     * Récupère les demandes par statut
     */
    List<DemandeRetraitEpargne> findByStatut(StatutDemandeRetrait statut);

    /**
     * Récupère les demandes en attente de validation
     */
    List<DemandeRetraitEpargne> findByStatutOrderByDateDemandeAsc(StatutDemandeRetrait statut);

    List<DemandeRetraitEpargne> findByStatutInOrderByDateDemandeAsc(Collection<StatutDemandeRetrait> statuts);

    /**
     * Récupère les demandes par compte et statut
     */
    List<DemandeRetraitEpargne> findByCompteEpargneAndStatut(CompteEpargne compteEpargne, StatutDemandeRetrait statut);

        @Query("""
                select coalesce(sum(d.montantDemande), 0)
                from DemandeRetraitEpargne d
                left join d.membre m
                left join m.site ms
                where d.statut = com.mini.credit.enums.StatutDemandeRetrait.VALIDEE
                    and (:agenceId is null or ms.agence.id = :agenceId)
                """)
        BigDecimal sumRetraitsValidesNonPayesByAgence(@Param("agenceId") Long agenceId);
}
