package com.mini.credit.entity.credit;

import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.enums.StatutGarantie;
import com.mini.credit.enums.TypeGarantie;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "garantie")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Garantie extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_credit_id")
    private DemandeCredit demandeCredit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_id")
    private Credit credit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membre_id")
    private Membre membre;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_garantie", nullable = false, length = 30)
    private TypeGarantie typeGarantie;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "valeur_estimee", precision = 18, scale = 2)
    private BigDecimal valeurEstimee;

    @Column(name = "taux", precision = 5, scale = 2)
    private BigDecimal taux;

    @Column(name = "montant_bloque", precision = 18, scale = 2)
    private BigDecimal montantBloque;

    @Column(name = "localisation", columnDefinition = "TEXT")
    private String localisation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutGarantie statut = StatutGarantie.ACTIF;

    private String notes;
}
