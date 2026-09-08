package com.mini.credit.repository.caisse;

import com.mini.credit.entity.caisse.RemboursementApportProprietaire;
import com.mini.credit.enums.StatutRemboursementApport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface RemboursementApportProprietaireRepository extends JpaRepository<RemboursementApportProprietaire, Long> {
    Optional<RemboursementApportProprietaire> findByReference(String reference);
    List<RemboursementApportProprietaire> findByStatutOrderByDateDemandeDesc(StatutRemboursementApport statut);

    @Query("""
        select coalesce(sum(r.montant), 0)
        from RemboursementApportProprietaire r
        where r.statut = com.mini.credit.enums.StatutRemboursementApport.PAYEE
          and (:agenceId is null or r.antenne.id = :agenceId)
        """)
    BigDecimal sumRemboursementsPayesByAgence(@Param("agenceId") Long agenceId);
}
