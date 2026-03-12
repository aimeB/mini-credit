package com.mini.credit.entity.credit;

import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.enums.StatutEcheance;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "echeance_credit",
        uniqueConstraints = @UniqueConstraint(name = "uq_credit_num_echeance", columnNames = {"credit_id", "numero_echeance"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcheanceCredit extends BaseEntity {



    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "credit_id", nullable = false)
    private Credit credit;

    @Column(name = "numero_echeance", nullable = false)
    private Integer numeroEcheance;

    @Column(name = "date_echeance", nullable = false)
    private LocalDate dateEcheance;

    @Column(name = "principal_prevu", nullable = false, precision = 18, scale = 2)
    private BigDecimal principalPrevu;

    @Column(name = "interet_prevu", nullable = false, precision = 18, scale = 2)
    private BigDecimal interetPrevu;

    @Column(name = "penalite_cumulee", nullable = false, precision = 18, scale = 2)
    private BigDecimal penaliteCumulee = BigDecimal.ZERO;

    @Column(name = "total_prevu", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalPrevu;

    @Column(name = "principal_paye", nullable = false, precision = 18, scale = 2)
    private BigDecimal principalPaye = BigDecimal.ZERO;

    @Column(name = "interet_paye", nullable = false, precision = 18, scale = 2)
    private BigDecimal interetPaye = BigDecimal.ZERO;

    @Column(name = "penalite_payee", nullable = false, precision = 18, scale = 2)
    private BigDecimal penalitePayee = BigDecimal.ZERO;

    @Column(name = "total_paye", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalPaye = BigDecimal.ZERO;

    @Column(name = "reste_a_payer", nullable = false, precision = 18, scale = 2)
    private BigDecimal resteAPayer;

    @Column(name = "date_dernier_paiement")
    private LocalDate dateDernierPaiement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutEcheance statut = StatutEcheance.A_PAYER;
}
