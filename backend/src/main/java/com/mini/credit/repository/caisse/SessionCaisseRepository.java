package com.mini.credit.repository.caisse;

import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.enums.StatutSessionCaisse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface SessionCaisseRepository extends JpaRepository<SessionCaisse, Long> {

    List<SessionCaisse> findAllByOrderByDateComptableDescDateOuvertureDescIdDesc();

    List<SessionCaisse> findByDateComptableBetweenOrderByDateComptableDesc(LocalDate dateDebut, LocalDate dateFin);
    List<SessionCaisse> findTop50ByCaisseIdOrderByDateOuvertureDesc(Long caisseId);

    Optional<SessionCaisse> findFirstByStatutOrderByDateOuvertureDesc(StatutSessionCaisse statut);
    Optional<SessionCaisse> findFirstByUtilisateurIdAndStatutOrderByDateOuvertureDesc(Long utilisateurId, StatutSessionCaisse statut);
    Optional<SessionCaisse> findFirstByUtilisateurIdAndDateComptableAndStatutOrderByDateOuvertureDesc(Long utilisateurId, LocalDate dateComptable, StatutSessionCaisse statut);
    List<SessionCaisse> findByUtilisateurIdAndDateComptableAndStatutOrderByDateOuvertureDesc(Long utilisateurId, LocalDate dateComptable, StatutSessionCaisse statut);
    Optional<SessionCaisse> findFirstByCaisseIdAndStatutOrderByDateOuvertureDesc(Long caisseId, StatutSessionCaisse statut);
    Optional<SessionCaisse> findFirstByCaisse_Agence_IdAndDateComptableAndStatutOrderByDateOuvertureDesc(Long antenneId, LocalDate dateComptable, StatutSessionCaisse statut);
    List<SessionCaisse> findByCaisse_Agence_IdAndDateComptableAndStatutOrderByDateOuvertureDesc(Long antenneId, LocalDate dateComptable, StatutSessionCaisse statut);
        Optional<SessionCaisse> findFirstByDateComptableAndStatutInOrderByDateOuvertureDesc(LocalDate dateComptable, List<StatutSessionCaisse> statuts);
        Optional<SessionCaisse> findFirstByCaisseIdAndDateComptableAndStatutInOrderByDateOuvertureDesc(Long caisseId, LocalDate dateComptable, List<StatutSessionCaisse> statuts);
        Optional<SessionCaisse> findFirstByCaisseIdAndStatutInOrderByDateOuvertureDesc(Long caisseId, List<StatutSessionCaisse> statuts);
        Optional<SessionCaisse> findFirstByCaisseIdAndDateComptableOrderByDateOuvertureDesc(Long caisseId, LocalDate dateComptable);
        Optional<SessionCaisse> findFirstByDateComptableBeforeAndStatutInOrderByDateComptableDescDateOuvertureDesc(LocalDate dateComptable, List<StatutSessionCaisse> statuts);
        Optional<SessionCaisse> findFirstByCaisseIdAndDateComptableBeforeAndStatutInOrderByDateComptableDescDateOuvertureDesc(Long caisseId, LocalDate dateComptable, List<StatutSessionCaisse> statuts);
        Optional<SessionCaisse> findFirstByCaisseIdAndStatutOrderByDateComptableDescDateClotureDesc(Long caisseId, StatutSessionCaisse statut);

    boolean existsByCaisseIdAndStatut(Long caisseId, StatutSessionCaisse statut);
    boolean existsByUtilisateurIdAndStatut(Long utilisateurId, StatutSessionCaisse statut);
        boolean existsByCaisseIdAndDateComptable(Long caisseId, LocalDate dateComptable);
    boolean existsByCaisseIdAndDateComptableAndStatutIn(Long caisseId, LocalDate dateComptable, List<StatutSessionCaisse> statuts);

    boolean existsByCaisseIdAndStatutIn(Long caisseId, List<StatutSessionCaisse> statuts);
    boolean existsByUtilisateurIdAndStatutIn(Long utilisateurId, List<StatutSessionCaisse> statuts);

        List<SessionCaisse> findByCaisseIdAndDateComptableBeforeAndStatutInOrderByDateComptableAscDateOuvertureAsc(Long caisseId, LocalDate dateComptable, List<StatutSessionCaisse> statuts);

        @Query("""
                select s
                from SessionCaisse s
                where s.caisse.id = :caisseId
                    and s.statut = com.mini.credit.enums.StatutSessionCaisse.CLOTUREE
                    and (
                        s.dateComptable < :dateComptable
                        or (
                            s.dateComptable = :dateComptable
                            and (:dateOuverture is null or s.dateCloture is null or s.dateCloture <= :dateOuverture)
                        )
                    )
                order by s.dateComptable desc,
                    case when s.dateCloture is null then 1 else 0 end,
                    s.dateCloture desc,
                    s.dateOuverture desc,
                    s.id desc
                """)
        List<SessionCaisse> findClosedSessionsForOpening(@Param("caisseId") Long caisseId,
                                                         @Param("dateComptable") LocalDate dateComptable,
                                                         @Param("dateOuverture") LocalDateTime dateOuverture);

    @Query("""
        select count(s) > 0
        from SessionCaisse s
        where s.caisse.id = :caisseId
          and s.id <> :sessionId
          and (
              s.dateComptable > :dateComptable
              or (
                  s.dateComptable = :dateComptable
                  and (
                      s.dateOuverture > :dateOuverture
                      or (s.dateOuverture = :dateOuverture and s.id > :sessionId)
                  )
              )
          )
        """)
    boolean existsLaterSessionForCaisse(@Param("sessionId") Long sessionId,
                                        @Param("caisseId") Long caisseId,
                                        @Param("dateComptable") LocalDate dateComptable,
                                        @Param("dateOuverture") LocalDateTime dateOuverture);

    @Query("""
        select s
        from SessionCaisse s
        join fetch s.caisse c
        left join fetch c.site st
        join fetch s.utilisateur u
        where s.id = :sessionId
        """)
    Optional<SessionCaisse> findByIdWithAuditContext(@Param("sessionId") Long sessionId);

    @Query("""
        select coalesce(sum(s.soldeTheorique), 0)
        from SessionCaisse s
        where s.statut in :statuts
          and (:agenceId is null or s.caisse.agence.id = :agenceId)
        """)
    BigDecimal sumSoldeTheoriqueByStatutsAndAgence(@Param("statuts") List<StatutSessionCaisse> statuts,
                                                   @Param("agenceId") Long agenceId);
}