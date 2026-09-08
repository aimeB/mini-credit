package com.mini.credit.repository.credit;

import com.mini.credit.entity.credit.RemboursementCredit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RemboursementCreditRepository extends JpaRepository<RemboursementCredit, Long> {
	Optional<RemboursementCredit> findTopByCreditIdOrderByIdDesc(Long creditId);
	List<RemboursementCredit> findByCreditIdOrderByDatePaiementAscIdAsc(Long creditId);

		@Query("""
				select r
				from RemboursementCredit r
				join fetch r.credit c
				left join fetch c.site cs
				left join fetch cs.agence csa
				left join fetch r.membre m
				left join fetch m.site ms
				left join fetch ms.agence msa
				left join fetch r.createdBy u
				where r.datePaiement between :dateDebut and :dateFin
					and (:agenceId is null or cs.agence.id = :agenceId or ms.agence.id = :agenceId)
					and (coalesce(r.montantInteret, 0) > 0 or coalesce(r.montantPenalite, 0) > 0)
				order by r.datePaiement desc, r.id desc
				""")
		List<RemboursementCredit> findRevenueRemboursements(
						@Param("dateDebut") LocalDateTime dateDebut,
						@Param("dateFin") LocalDateTime dateFin,
						@Param("agenceId") Long agenceId
		);

		@Query("""
				select coalesce(sum(r.montantPrincipal), 0)
				from RemboursementCredit r
				left join r.credit c
				left join c.site cs
				left join r.membre m
				left join m.site ms
				where r.datePaiement between :dateDebut and :dateFin
					and (:agenceId is null or cs.agence.id = :agenceId or ms.agence.id = :agenceId)
				""")
		BigDecimal sumPrincipalRembourseByPeriodAndAgence(
						@Param("dateDebut") LocalDateTime dateDebut,
						@Param("dateFin") LocalDateTime dateFin,
						@Param("agenceId") Long agenceId
		);

		@Query("""
				select r
				from RemboursementCredit r
				join fetch r.credit c
				left join fetch c.site cs
				left join fetch cs.agence csa
				left join fetch r.membre m
				left join fetch m.site ms
				left join fetch ms.agence msa
				left join fetch r.createdBy u
				where r.datePaiement between :dateDebut and :dateFin
					and (:agenceId is null or cs.agence.id = :agenceId or ms.agence.id = :agenceId)
					and coalesce(r.montantPrincipal, 0) > 0
				order by r.datePaiement desc, r.id desc
				""")
		List<RemboursementCredit> findPrincipalRemboursements(
						@Param("dateDebut") LocalDateTime dateDebut,
						@Param("dateFin") LocalDateTime dateFin,
						@Param("agenceId") Long agenceId
		);

		@Query("""
				select coalesce(sum(r.montantInteret), 0)
				from RemboursementCredit r
				left join r.credit c
				left join c.site cs
				left join r.membre m
				left join m.site ms
				where r.datePaiement between :dateDebut and :dateFin
					and (:agenceId is null or cs.agence.id = :agenceId or ms.agence.id = :agenceId)
				""")
		BigDecimal sumInteretsByPeriodAndAgence(
						@Param("dateDebut") LocalDateTime dateDebut,
						@Param("dateFin") LocalDateTime dateFin,
						@Param("agenceId") Long agenceId
		);

		@Query("""
				select coalesce(sum(r.montantPenalite), 0)
				from RemboursementCredit r
				left join r.credit c
				left join c.site cs
				left join r.membre m
				left join m.site ms
				where r.datePaiement between :dateDebut and :dateFin
					and (:agenceId is null or cs.agence.id = :agenceId or ms.agence.id = :agenceId)
				""")
		BigDecimal sumPenalitesByPeriodAndAgence(
						@Param("dateDebut") LocalDateTime dateDebut,
						@Param("dateFin") LocalDateTime dateFin,
						@Param("agenceId") Long agenceId
		);

		@Query("""
				select r
				from RemboursementCredit r
				join fetch r.credit c
				left join fetch c.site cs
				left join fetch cs.agence csa
				left join fetch r.membre m
				left join fetch m.site ms
				left join fetch ms.agence msa
				where r.datePaiement between :dateDebut and :dateFin
					and (:agenceId is null or cs.agence.id = :agenceId or ms.agence.id = :agenceId)
					and coalesce(r.montantTotal, 0) > 0
					and coalesce(r.montantPrincipal, 0) = 0
					and coalesce(r.montantInteret, 0) = 0
					and coalesce(r.montantPenalite, 0) = 0
				order by r.datePaiement desc, r.id desc
				""")
		List<RemboursementCredit> findRemboursementsSansVentilation(
						@Param("dateDebut") LocalDateTime dateDebut,
						@Param("dateFin") LocalDateTime dateFin,
						@Param("agenceId") Long agenceId
		);
}