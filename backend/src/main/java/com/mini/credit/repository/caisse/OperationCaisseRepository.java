package com.mini.credit.repository.caisse;

import com.mini.credit.entity.caisse.OperationCaisse;
import com.mini.credit.enums.CategorieOperationCaisse;
import com.mini.credit.enums.NatureFinancementApprovisionnement;
import com.mini.credit.enums.SourceOperationCaisse;
import com.mini.credit.enums.TypeOperationCaisse;
import com.mini.credit.repository.projection.CategorieMontantProjection;
import com.mini.credit.repository.projection.OperationCaisseDiagnosticProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OperationCaisseRepository extends JpaRepository<OperationCaisse, Long> {

  boolean existsByNumeroPiece(String numeroPiece);

    List<OperationCaisse> findByCaisseIdOrderByDateOperationDesc(Long caisseId);

    List<OperationCaisse> findBySessionCaisseIdOrderByDateOperationDesc(Long sessionCaisseId);

    long countBySessionCaisseId(Long sessionCaisseId);

    List<OperationCaisse> findByDateOperationBetweenOrderByDateOperationAsc(LocalDateTime dateDebut, LocalDateTime dateFin);

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

    List<OperationCaisse> findByRetraitEpargneIdOrderByDateOperationDesc(Long retraitEpargneId);

    Optional<OperationCaisse> findFirstByCreditIdAndSourceOrderByDateOperationDescIdDesc(
        Long creditId,
        SourceOperationCaisse source);

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

    /**
     * PATCH 3 — Q3: Validation pré-clôture.
     * Compte les opérations source=RECETTE_JOURNALIERE sans recette_id (orphelines).
     * Une telle opération viole la règle de traçabilité 3N.
     */
    long countBySessionCaisseIdAndSourceAndRecetteIdIsNull(
            Long sessionCaisseId, SourceOperationCaisse source);

    /**
     * PATCH 5 — Idempotence: vérifie si des opérations ont déjà été générées pour cette recette.
     * Utilisé par RecetteOperationGenerationService pour éviter la double génération.
     */
    boolean existsByRecetteId(Long recetteId);

    @Query(value = """
        select o.id as operationId,
               o.session_caisse_id as sessionId,
               o.caisse_id as caisseId,
               c.site_id as siteId
        from operation_caisse o
        left join caisse c on c.id = o.caisse_id
        where o.session_caisse_id is null
        """, nativeQuery = true)
    List<OperationCaisseDiagnosticProjection> findOperationsWithoutSessionReference();

    @Query(value = """
        select o.id as operationId,
               o.session_caisse_id as sessionId,
               o.caisse_id as caisseId,
               c.site_id as siteId
        from operation_caisse o
        left join session_caisse s on s.id = o.session_caisse_id
        left join caisse c on c.id = o.caisse_id
        where o.session_caisse_id is not null
          and s.id is null
        """, nativeQuery = true)
    List<OperationCaisseDiagnosticProjection> findOperationsWithMissingSessionReference();

    @Query(value = """
        select o.id as operationId,
               o.session_caisse_id as sessionId,
               o.caisse_id as caisseId,
               c.site_id as siteId
        from operation_caisse o
        left join caisse c on c.id = o.caisse_id
        where o.source_operation = 'AJUSTEMENT'
          and (o.observation is null or trim(o.observation) = '')
        """, nativeQuery = true)
    List<OperationCaisseDiagnosticProjection> findAdjustmentOperationsWithoutObservation();

      @Query("""
        select o
        from OperationCaisse o
        join fetch o.caisse c
        left join fetch c.site st
        left join fetch o.sessionCaisse s
        left join fetch s.utilisateur u
        where o.id = :operationId
        """)
      java.util.Optional<OperationCaisse> findByIdWithAuditContext(@Param("operationId") Long operationId);

    @Query("""
        select o
        from OperationCaisse o
        join o.caisse c
        left join c.site st
        left join o.utilisateur u
        left join o.createdBy cb
        where (:sessionCaisseId is null or o.sessionCaisse.id = :sessionCaisseId)
          and (:caisseId is null or o.caisse.id = :caisseId)
          and (:agenceId is null or c.agence.id = :agenceId)
          and (:siteId is null or o.site.id = :siteId or c.site.id = :siteId)
          and (:utilisateurId is null or o.utilisateur.id = :utilisateurId or cb.id = :utilisateurId)
          and (:typeOperation is null or o.typeOperation = :typeOperation)
          and (:categorie is null or o.categorieOperation = :categorie)
          and (:source is null or o.source = :source)
          and (:dateDebut is null or o.dateOperation >= :dateDebut)
          and (:dateFin is null or o.dateOperation <= :dateFin)
          and (:referenceMetier is null or lower(o.referenceMetier) like lower(concat('%', :referenceMetier, '%'))
            or lower(o.referenceExterne) like lower(concat('%', :referenceMetier, '%')))
          and (:recetteId is null or o.recetteId = :recetteId)
          and (:depenseCaisseId is null or o.depenseCaisseId = :depenseCaisseId)
          and (:creditId is null or o.credit.id = :creditId)
          and (:retraitEpargneId is null or o.retraitEpargneId = :retraitEpargneId)
          and (:operationEpargneId is null or o.operationEpargne.id = :operationEpargneId)
        """)
    Page<OperationCaisse> findJournal(
            @Param("sessionCaisseId") Long sessionCaisseId,
            @Param("caisseId") Long caisseId,
            @Param("agenceId") Long agenceId,
            @Param("siteId") Long siteId,
            @Param("utilisateurId") Long utilisateurId,
            @Param("typeOperation") TypeOperationCaisse typeOperation,
            @Param("categorie") CategorieOperationCaisse categorie,
            @Param("source") SourceOperationCaisse source,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin,
            @Param("referenceMetier") String referenceMetier,
            @Param("recetteId") Long recetteId,
            @Param("depenseCaisseId") Long depenseCaisseId,
            @Param("creditId") Long creditId,
            @Param("retraitEpargneId") Long retraitEpargneId,
            @Param("operationEpargneId") Long operationEpargneId,
            Pageable pageable
    );

    @Query("""
        select o
        from OperationCaisse o
        join fetch o.caisse c
        left join fetch c.agence ca
        left join fetch c.site cs
        left join fetch o.site st
        left join fetch st.agence sta
        left join fetch o.membre m
        left join fetch o.utilisateur u
        left join fetch o.createdBy cb
        where o.typeOperation = com.mini.credit.enums.TypeOperationCaisse.ENTREE
          and o.dateOperation between :dateDebut and :dateFin
          and (:agenceId is null or c.agence.id = :agenceId or st.agence.id = :agenceId or cs.agence.id = :agenceId)
          and o.categorieOperation in :categories
        order by o.dateOperation desc, o.id desc
        """)
    List<OperationCaisse> findRevenueOperations(
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin,
            @Param("agenceId") Long agenceId,
            @Param("categories") List<CategorieOperationCaisse> categories
    );

    @Query("""
        select coalesce(sum(o.montant), 0)
        from OperationCaisse o
        join o.caisse c
        left join c.site cs
        left join o.site st
        where o.typeOperation = com.mini.credit.enums.TypeOperationCaisse.ENTREE
          and o.dateOperation between :dateDebut and :dateFin
          and (:agenceId is null or c.agence.id = :agenceId or st.agence.id = :agenceId or cs.agence.id = :agenceId)
          and o.categorieOperation in :categories
        """)
    BigDecimal sumEntreesByCategories(
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin,
            @Param("agenceId") Long agenceId,
            @Param("categories") List<CategorieOperationCaisse> categories
    );

    @Query("""
        select o
        from OperationCaisse o
        join fetch o.caisse c
        left join fetch c.agence ca
        left join fetch c.site cs
        left join fetch cs.agence csa
        left join fetch o.site st
        left join fetch st.agence sta
        left join fetch o.membre m
        left join fetch o.utilisateur u
        left join fetch o.createdBy cb
        where o.dateOperation between :dateDebut and :dateFin
          and (:agenceId is null or c.agence.id = :agenceId or st.agence.id = :agenceId or cs.agence.id = :agenceId)
          and o.categorieOperation in :categories
        order by o.dateOperation desc, o.id desc
        """)
    List<OperationCaisse> findAccountingMovementOperations(
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin,
            @Param("agenceId") Long agenceId,
            @Param("categories") List<CategorieOperationCaisse> categories
    );

        @Query("""
      select coalesce(sum(o.montant), 0)
      from OperationCaisse o
      join o.caisse c
      left join c.site cs
      left join o.site st
      where o.typeOperation = com.mini.credit.enums.TypeOperationCaisse.ENTREE
        and o.dateOperation <= :dateFin
        and (:agenceId is null or c.agence.id = :agenceId or st.agence.id = :agenceId or cs.agence.id = :agenceId)
        and o.categorieOperation = com.mini.credit.enums.CategorieOperationCaisse.APPROVISIONNEMENT
        and o.natureFinancement = :natureFinancement
      """)
        BigDecimal sumApprovisionnementsByNatureUntil(
          @Param("dateFin") LocalDateTime dateFin,
          @Param("agenceId") Long agenceId,
          @Param("natureFinancement") NatureFinancementApprovisionnement natureFinancement
        );

    @Query("""
        select o
        from OperationCaisse o
        join o.caisse c
        where (:agenceId is null or c.agence.id = :agenceId)
        order by o.dateOperation desc
        """)
    Page<OperationCaisse> findAllByAgenceId(@Param("agenceId") Long agenceId, Pageable pageable);
}