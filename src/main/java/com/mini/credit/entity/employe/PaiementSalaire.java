package com.mini.credit.entity.employe;

import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.enums.ModePaiement;
import com.mini.credit.enums.StatutPaiement;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "paiement_salaire", indexes = {
        @Index(name = "idx_employe_id", columnList = "employe_id"),
        @Index(name = "idx_date_paiement", columnList = "date_paiement")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaiementSalaire extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "employe_id", nullable = false, foreignKey = @ForeignKey(name = "fk_paiement_employe"))
    private Employe employe;

    @Column(name = "date_paiement", nullable = false)
    private LocalDate datePaiement;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal montant;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_paiement", nullable = false)
    private ModePaiement modePaiement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutPaiement statut;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "numero_operation_caisse")
    private Long numeroOperationCaisse;

    @Column(name = "reference_externe", length = 100)
    private String referenceExterne;
}
