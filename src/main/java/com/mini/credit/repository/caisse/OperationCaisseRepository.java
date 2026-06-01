package com.mini.credit.repository.caisse;

import com.mini.credit.entity.caisse.OperationCaisse;
import com.mini.credit.enums.TypeOperationCaisse;
import com.mini.credit.repository.projection.CategorieMontantProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface OperationCaisseRepository extends JpaRepository<OperationCaisse, Long> {

    List<OperationCaisse> findByCaisseIdOrderByDateOperationDesc(Long caisseId);

    List<OperationCaisse> findBySessionCaisseIdOrderByDateOperationDesc(Long sessionCaisseId);

    @Query("""
        select coalesce(sum(o.montant), 0)
        from OperationCaisse o
        where o.sessionCaisse.id = :sessionId
          and o.typeOperation = :typeOperation
        """)
    BigDecimal sumBySessionAndType(Long sessionId, TypeOperationCaisse typeOperation);

    @Query("""
        select
            cast(o.categorieOperation as string) as categorie,
            coalesce(sum(o.montant), 0) as montant
        from OperationCaisse o
        where o.sessionCaisse.id = :sessionId
          and o.typeOperation = :typeOperation
        group by o.categorieOperation
        order by sum(o.montant) desc
        """)
    List<CategorieMontantProjection> sumBySessionAndTypeGroupByCategorie(Long sessionId, TypeOperationCaisse typeOperation);

    List<OperationCaisse> findByOperationEpargneIdOrderByDateOperationDesc(Long operationEpargneId);

    @Query("""
        select coalesce(sum(o.montant), 0)
        from OperationCaisse o
        where o.typeOperation = :typeOperation
          and o.dateOperation between :dateDebut and :dateFin
        """)
    BigDecimal findTotalByTypeAndDate(String typeOperation, LocalDateTime dateDebut, LocalDateTime dateFin);

    @Query("""
        select count(o)
        from OperationCaisse o
        where o.dateOperation between :dateDebut and :dateFin
        """)
    Long countByDateBetween(LocalDateTime dateDebut, LocalDateTime dateFin);

    long countByOperationEpargneId(Long operationEpargneId);
}